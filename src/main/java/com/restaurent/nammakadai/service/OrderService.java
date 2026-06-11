package com.restaurent.nammakadai.service;

import com.restaurent.nammakadai.dto.OrderItemResponse;
import com.restaurent.nammakadai.dto.OrderRequest;
import com.restaurent.nammakadai.dto.OrderResponse;
import com.restaurent.nammakadai.entity.*;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CartRepository cartRepository;
    private final OtpService otpService;
    private final EmailService emailService;

    private static final BigDecimal CGST_RATE = new BigDecimal("0.025");
    private static final BigDecimal SGST_RATE = new BigDecimal("0.025");

    @Transactional
    public OrderResponse placeOrder(String email, OrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING");

        BigDecimal total = BigDecimal.ZERO;
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemReq.getItemId()));
            if (!menuItem.getAvailable()) {
                throw new IllegalArgumentException("Item not available: " + menuItem.getName());
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPrice(menuItem.getPrice());
            order.getOrderItems().add(orderItem);
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }
        order.setTotalAmount(total);
        applyGst(order, total);
        Order saved = orderRepository.save(order);
        recordStatusHistory(saved, "PENDING");
        return toOrderResponse(saved);
    }

    @Transactional
    public void cancelPendingOrder(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getUser().getEmail().equals(email))
            throw new IllegalArgumentException("Not authorized");
        if (!"PENDING".equals(order.getStatus()))
            throw new IllegalArgumentException("Only PENDING orders can be cancelled");
        orderRepository.delete(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        order.setStatus(status);
        Order updated = orderRepository.save(order);
        recordStatusHistory(updated, status);
        return toOrderResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUserUserId(user.getUserId())
                .stream().map(this::toOrderResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream().map(this::toOrderResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStaff(Long staffId) {
        return orderRepository.findByAssignedStaffUserId(staffId)
                .stream().map(this::toOrderResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toOrderResponse(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId)));
    }

    @Transactional
    public OrderResponse placeOrderFromCart(String email) {
        return toOrderResponse(placeOrderFromCartInternal(email));
    }

    /** Package-visible: returns the managed Order entity for use by PaymentService within the same transaction. */
    @Transactional
    Order placeOrderFromCartInternal(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Cart> cartItems = cartRepository.findByUserUserId(user.getUserId());
        if (cartItems.isEmpty()) throw new IllegalArgumentException("Cart is empty");

        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING");

        BigDecimal total = BigDecimal.ZERO;
        for (Cart cartItem : cartItems) {
            MenuItem menuItem = cartItem.getMenuItem();
            if (!menuItem.getAvailable()) {
                throw new IllegalArgumentException("Item not available: " + menuItem.getName());
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            order.getOrderItems().add(orderItem);
            total = total.add(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(total);
        applyGst(order, total);

        Order saved = orderRepository.save(order);
        recordStatusHistory(saved, "PENDING");
        cartRepository.deleteByUserUserId(user.getUserId());
        return saved;
    }

    @Transactional
    public void sendDeliveryOtp(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        otpService.sendOtp(order.getUser().getEmail());
    }

    @Transactional
    public OrderResponse confirmDelivery(Long orderId, String otp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        String customerEmail = order.getUser().getEmail();
        if (!otpService.verifyOtp(customerEmail, otp)) {
            throw new IllegalArgumentException("Invalid or expired delivery OTP");
        }
        otpService.deleteOtp(customerEmail);
        order.setStatus("DELIVERED");
        Order saved = orderRepository.save(order);
        recordStatusHistory(saved, "DELIVERED");
        return toOrderResponse(saved);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    public OrderResponse toOrderResponse(Order order) {
        OrderResponse dto = new OrderResponse();
        dto.setOrderId(order.getOrderId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCgstAmount(order.getCgstAmount());
        dto.setSgstAmount(order.getSgstAmount());
        dto.setGrandTotal(order.getGrandTotal());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setCouponCode(order.getCouponCode());
        dto.setCreatedAt(order.getCreatedAt());

        User u = order.getUser();
        dto.setUserId(u.getUserId());
        dto.setCustomerName(u.getName());
        dto.setCustomerEmail(u.getEmail());
        dto.setCustomerPhone(u.getPhone());

        if (order.getAssignedStaff() != null) {
            dto.setAssignedStaffId(order.getAssignedStaff().getUserId());
            dto.setAssignedStaffName(order.getAssignedStaff().getName());
            dto.setAssignedStaffPhone(order.getAssignedStaff().getPhone());
        }

        dto.setOrderItems(order.getOrderItems().stream().map(oi -> {
            OrderItemResponse oir = new OrderItemResponse();
            oir.setOrderItemId(oi.getOrderItemId());
            oir.setQuantity(oi.getQuantity());
            oir.setPrice(oi.getPrice());
            MenuItem mi = oi.getMenuItem();
            oir.setItemId(mi.getItemId());
            oir.setItemName(mi.getName());
            oir.setImageUrl(mi.getImageUrl());
            return oir;
        }).toList());

        return dto;
    }

    private void applyGst(Order order, BigDecimal taxableAmount) {
        BigDecimal cgst = taxableAmount.multiply(CGST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = taxableAmount.multiply(SGST_RATE).setScale(2, RoundingMode.HALF_UP);
        order.setCgstAmount(cgst);
        order.setSgstAmount(sgst);
        order.setGrandTotal(taxableAmount.add(cgst).add(sgst));
    }

    private void recordStatusHistory(Order order, String status) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        statusHistoryRepository.save(history);
    }
}
