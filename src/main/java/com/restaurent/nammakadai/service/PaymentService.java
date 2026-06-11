package com.restaurent.nammakadai.service;

import com.restaurent.nammakadai.dto.BillResponse;
import com.restaurent.nammakadai.dto.PaymentRequest;
import com.restaurent.nammakadai.dto.PaymentResponse;
import com.restaurent.nammakadai.entity.Address;
import com.restaurent.nammakadai.entity.Coupon;
import com.restaurent.nammakadai.entity.Order;
import com.restaurent.nammakadai.entity.Payment;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.AddressRepository;
import com.restaurent.nammakadai.repository.OrderRepository;
import com.restaurent.nammakadai.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final DiamondService diamondService;
    private final BillService billService;
    private final EmailService emailService;
    private final OrderService orderService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request,
                                           org.springframework.security.core.userdetails.UserDetails userDetails) {
        // Validate address first — fail fast before touching DB
        if (request.getStreet() == null || request.getStreet().isBlank()
                || request.getCity() == null || request.getCity().isBlank()
                || request.getState() == null || request.getState().isBlank()
                || request.getPincode() == null || request.getPincode().isBlank()) {
            throw new IllegalArgumentException("Delivery address is required");
        }

        Order order;
        if (Boolean.TRUE.equals(request.getFromCart())) {
            // Atomic: create order from cart inside this same transaction
            order = orderService.placeOrderFromCartInternal(userDetails.getUsername());
        } else {
            order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
        }

        // Apply coupon discount if provided
        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = diamondService.validateCoupon(couponCode);
            discount = coupon.getDiscountAmount() != null ? coupon.getDiscountAmount() : BigDecimal.ZERO;
            if (coupon.getMinOrderAmount() != null && order.getGrandTotal().compareTo(coupon.getMinOrderAmount()) < 0) {
                orderRepository.delete(order);
                throw new IllegalArgumentException("Order total does not meet minimum amount for this coupon");
            }
            BigDecimal newTotal = order.getGrandTotal().subtract(discount).max(BigDecimal.ZERO);
            order.setDiscountAmount(discount);
            order.setCouponCode(couponCode);
            order.setGrandTotal(newTotal);
            orderRepository.save(order);
            diamondService.markCouponUsed(couponCode);
        }

        // Save payment
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("CASH");
        payment.setAmount(order.getGrandTotal());
        payment.setPaymentStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        PaymentResponse response = toResponse(saved);

        // Save delivery address
        Address address = new Address();
        address.setUser(order.getUser());
        address.setOrder(order);
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLandmark(request.getLandmark());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        addressRepository.save(address);

        // Award diamonds if order qualifies (use original total before discount)
        diamondService.awardDiamonds(order.getUser(), order.getTotalAmount());

        // Send order confirmation email with PDF bill attached
        try {
            byte[] pdf = billService.generatePdfBill(order.getOrderId());
            BillResponse bill = billService.getBill(order.getOrderId());
            String html = buildOrderConfirmationEmail(bill);
            emailService.sendHtmlWithAttachment(
                    order.getUser().getEmail(),
                    "Order Confirmed #" + order.getOrderId() + " — NammaKadai",
                    html,
                    pdf,
                    "bill-order-" + order.getOrderId() + ".pdf"
            );
        } catch (Exception ignored) {
            // Don't fail the payment if email sending fails
        }

        return response;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrder(Long orderId) {
        Payment p = paymentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return toResponse(p);
    }

    private PaymentResponse toResponse(Payment p) {
        PaymentResponse dto = new PaymentResponse();
        dto.setPaymentId(p.getPaymentId());
        dto.setOrderId(p.getOrder().getOrderId());
        dto.setPaymentMethod(p.getPaymentMethod());
        dto.setPaymentStatus(p.getPaymentStatus());
        dto.setAmount(p.getAmount());
        dto.setPaidAt(p.getPaidAt());
        return dto;
    }

    private String buildOrderConfirmationEmail(BillResponse bill) {
        StringBuilder items = new StringBuilder();
        for (BillResponse.BillItem item : bill.getItems()) {
            items.append(EmailTemplates.itemRow(
                    item.getItemName(), item.getQuantity(),
                    item.getUnitPrice().doubleValue(), item.getSubtotal().doubleValue()));
        }

        String addressHtml = "";
        if (bill.getDeliveryAddress() != null) {
            BillResponse.DeliveryAddress da = bill.getDeliveryAddress();
            addressHtml = EmailTemplates.addressBlock(
                    da.getStreet(), da.getCity(), da.getState(), da.getPincode(), da.getLandmark());
        }

        String discountRowHtml = "";
        if (bill.getDiscountAmount() != null && bill.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountRowHtml = EmailTemplates.discountRow(
                    bill.getCouponCode(), bill.getDiscountAmount().doubleValue());
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return EmailTemplates.orderConfirmation(
                bill.getCustomerName(),
                bill.getOrderId(),
                bill.getOrderedAt().format(fmt),
                bill.getOrderStatus(),
                items.toString(),
                bill.getTaxableAmount().doubleValue(),
                bill.getCgst().doubleValue(),
                bill.getSgst().doubleValue(),
                discountRowHtml,
                bill.getGrandTotal().doubleValue(),
                addressHtml,
                bill.getGstNote()
        );
    }
}
