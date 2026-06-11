package com.restaurent.nammakadai.service;

import com.restaurent.nammakadai.entity.Otp;
import com.restaurent.nammakadai.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Transactional(timeout = 5)
    public void sendOtp(String email) {
        otpRepository.deleteByEmail(email);
        String code = String.format("%06d", new Random().nextInt(1000000));
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtp(code);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        otpRepository.save(otp);
        String html = EmailTemplates.otp(email.split("@")[0], code,
                "Use the OTP below to verify your identity on NammaKadai.");
        emailService.sendHtml(email, "Your OTP — NammaKadai", html);
    }

    public boolean verifyOtp(String email, String code) {
        return otpRepository.findTopByEmailOrderByExpiryTimeDesc(email)
                .filter(o -> o.getOtp().equals(code) && o.getExpiryTime().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public void deleteOtp(String email) {
        otpRepository.deleteByEmail(email);
    }
}
