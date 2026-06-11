package com.restaurent.nammakadai.service;

import com.restaurent.nammakadai.dto.AuthResponse;
import com.restaurent.nammakadai.dto.LoginRequest;
import com.restaurent.nammakadai.dto.RegisterRequest;
import com.restaurent.nammakadai.dto.ResetPasswordRequest;
import com.restaurent.nammakadai.entity.Role;
import com.restaurent.nammakadai.entity.User;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.RoleRepository;
import com.restaurent.nammakadai.repository.UserRepository;
import com.restaurent.nammakadai.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       @Lazy AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       JwtUtil jwtUtil, EmailService emailService, OtpService otpService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.otpService = otpService;
    }

    @Value("${spring.mail.username}")
    private String adminEmail;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String roleName;
        if ("ADMIN".equalsIgnoreCase(request.getRole())) roleName = "ADMIN";
        else if ("STAFF".equalsIgnoreCase(request.getRole())) roleName = "STAFF";
        else roleName = "CUSTOMER";

        // OTP verification — ADMIN uses owner email, others use their own
        String otpEmail = roleName.equals("ADMIN") ? adminEmail : request.getEmail();
        if (!otpService.verifyOtp(otpEmail, request.getOtp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role not found"));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRoles(Set.of(role));

        if ("STAFF".equals(roleName)) {
            user.setEnabled(false);
            user.setApprovalStatus("PENDING");
        } else {
            user.setEnabled(true);
            user.setApprovalStatus("APPROVED");
        }

        userRepository.save(user);
        otpService.deleteOtp(otpEmail);

        if ("STAFF".equals(roleName)) {
            try {
                String html = EmailTemplates.staffApprovalRequest(
                        user.getName(), user.getEmail(), user.getPhone());
                emailService.sendHtml(adminEmail,
                        "New Staff Registration Pending Approval — NammaKadai", html);
            } catch (Exception ignored) {}
            throw new IllegalArgumentException("Registration submitted. Awaiting admin approval before you can login.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        return new AuthResponse(token, user.getEmail(), user.getName(), roleName);
    }

    public AuthResponse login(LoginRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            if ("PENDING".equals(u.getApprovalStatus()))
                throw new IllegalArgumentException("Your account is pending admin approval.");
            if ("REJECTED".equals(u.getApprovalStatus()))
                throw new IllegalArgumentException("Your account has been rejected by admin.");
        });

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String roleName = user.getRoles().stream().findFirst()
                .map(Role::getRoleName).orElse("CUSTOMER");
        return new AuthResponse(token, user.getEmail(), user.getName(), roleName);
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpService.deleteOtp(request.getEmail());
    }
}
