package com.restaurent.nammakadai.service;

/**
 * Centralised HTML email templates — NammaKadai brand (orange #f97316 / dark #0f172a).
 */
public final class EmailTemplates {

    private EmailTemplates() {}

    // ── Shared wrapper ────────────────────────────────────────────────────────
    private static String wrap(String title, String badgeColor, String badge, String body) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/>
            <title>%s</title></head>
            <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:32px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                         style="max-width:560px;width:100%%;background:#ffffff;border-radius:16px;
                                overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.10);">
                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#f97316 0%%,#ea580c 100%%);
                                 padding:32px 28px;text-align:center;">
                        <div style="font-size:36px;margin-bottom:8px;">🍽️</div>
                        <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:800;
                                   letter-spacing:1px;text-transform:uppercase;">NammaKadai</h1>
                        <p style="margin:6px 0 0;color:#fed7aa;font-size:13px;font-weight:500;">
                          Restaurant Management System
                        </p>
                        <span style="display:inline-block;margin-top:14px;background:%s;
                                     color:#fff;font-size:11px;font-weight:700;
                                     padding:4px 14px;border-radius:20px;letter-spacing:1px;">
                          %s
                        </span>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding:28px 32px;">
                        %s
                      </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="background:#fff7ed;border-top:1px solid #fed7aa;
                                 padding:16px 32px;text-align:center;">
                        <p style="margin:0;font-size:12px;color:#f97316;font-weight:600;">
                          🍽️ NammaKadai — Fresh food, fast orders
                        </p>
                        <p style="margin:4px 0 0;font-size:11px;color:#9ca3af;">
                          This is an automated email. Please do not reply.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body></html>
            """.formatted(title, badgeColor, badge, body);
    }

    // ── OTP email ─────────────────────────────────────────────────────────────
    public static String otp(String name, String otpCode, String purpose) {
        String body = """
            <p style="margin:0 0 6px;font-size:16px;color:#111827;">Hi <strong>%s</strong>,</p>
            <p style="margin:0 0 20px;color:#6b7280;font-size:14px;">%s</p>

            <div style="background:#fff7ed;border:2px dashed #f97316;border-radius:12px;
                        padding:24px;text-align:center;margin:20px 0;">
              <p style="margin:0 0 8px;font-size:12px;color:#9ca3af;letter-spacing:2px;
                        text-transform:uppercase;font-weight:600;">Your One-Time Password</p>
              <div style="font-size:42px;font-weight:800;letter-spacing:12px;color:#f97316;
                          font-family:'Courier New',monospace;">%s</div>
              <p style="margin:10px 0 0;font-size:12px;color:#ef4444;font-weight:600;">
                ⏳ Valid for 10 minutes only
              </p>
            </div>

            <div style="background:#fef2f2;border-left:4px solid #ef4444;border-radius:4px;
                        padding:12px 16px;margin-top:20px;">
              <p style="margin:0;font-size:12px;color:#7f1d1d;">
                🔒 <strong>Never share this OTP</strong> with anyone.
                NammaKadai will never ask for your OTP over call or chat.
              </p>
            </div>
            """.formatted(name, purpose, otpCode);
        return wrap("OTP — NammaKadai", "#ef4444", "SECURE OTP", body);
    }

    // ── Order confirmation email ───────────────────────────────────────────────
    public static String orderConfirmation(
            String customerName, long orderId, String orderedAt,
            String orderStatus, String itemsHtml,
            double taxable, double cgst, double sgst,
            String discountRowHtml, double grandTotal,
            String addressHtml, String gstNote) {

        String body = """
            <p style="margin:0 0 4px;font-size:16px;color:#111827;">
              Hi <strong>%s</strong>, your order is confirmed! 🎉
            </p>
            <p style="margin:0 0 20px;color:#6b7280;font-size:14px;">
              We're preparing your food right now. Sit back and relax!
            </p>

            <!-- Order badge -->
            <div style="background:#fff7ed;border:1px solid #fed7aa;border-radius:10px;
                        padding:14px 18px;margin-bottom:20px;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td>
                    <p style="margin:0;font-size:15px;font-weight:700;color:#111827;">
                      Order #%d
                    </p>
                    <p style="margin:3px 0 0;font-size:12px;color:#9ca3af;">%s</p>
                  </td>
                  <td align="right">
                    <span style="background:#dcfce7;color:#16a34a;font-size:11px;font-weight:700;
                                 padding:4px 10px;border-radius:20px;">✓ %s</span>
                  </td>
                </tr>
              </table>
            </div>

            %s

            <!-- Items table -->
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-collapse:collapse;border-radius:8px;overflow:hidden;
                          border:1px solid #f3f4f6;margin-bottom:16px;">
              <thead>
                <tr style="background:#f97316;">
                  <th style="padding:10px 12px;color:#fff;text-align:left;font-size:12px;
                             font-weight:700;text-transform:uppercase;">Item</th>
                  <th style="padding:10px 12px;color:#fff;text-align:center;font-size:12px;
                             font-weight:700;text-transform:uppercase;">Qty</th>
                  <th style="padding:10px 12px;color:#fff;text-align:right;font-size:12px;
                             font-weight:700;text-transform:uppercase;">Rate</th>
                  <th style="padding:10px 12px;color:#fff;text-align:right;font-size:12px;
                             font-weight:700;text-transform:uppercase;">Amount</th>
                </tr>
              </thead>
              <tbody>%s</tbody>
            </table>

            <!-- Tax breakdown -->
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#f9fafb;border-radius:8px;padding:4px 0;
                          border:1px solid #f3f4f6;margin-bottom:8px;">
              <tr>
                <td style="padding:7px 16px;font-size:13px;color:#6b7280;">Taxable Amount</td>
                <td style="padding:7px 16px;font-size:13px;color:#374151;text-align:right;">
                  &#8377;%.2f</td>
              </tr>
              <tr>
                <td style="padding:7px 16px;font-size:13px;color:#6b7280;">CGST (2.5%%)</td>
                <td style="padding:7px 16px;font-size:13px;color:#374151;text-align:right;">
                  &#8377;%.2f</td>
              </tr>
              <tr>
                <td style="padding:7px 16px;font-size:13px;color:#6b7280;">SGST (2.5%%)</td>
                <td style="padding:7px 16px;font-size:13px;color:#374151;text-align:right;">
                  &#8377;%.2f</td>
              </tr>
              %s
              <tr style="border-top:2px solid #f97316;">
                <td style="padding:10px 16px;font-size:15px;font-weight:800;color:#111827;">
                  Grand Total</td>
                <td style="padding:10px 16px;font-size:15px;font-weight:800;color:#f97316;
                           text-align:right;">&#8377;%.2f</td>
              </tr>
            </table>

            <p style="margin:16px 0 0;font-size:11px;color:#9ca3af;text-align:center;">%s</p>
            <p style="margin:8px 0 0;font-size:13px;color:#6b7280;text-align:center;">
              💎 Your bill invoice is attached as a PDF.
            </p>
            """.formatted(customerName, orderId, orderedAt, orderStatus,
                          addressHtml, itemsHtml,
                          taxable, cgst, sgst, discountRowHtml, grandTotal, gstNote);

        return wrap("Order Confirmed — NammaKadai", "#16a34a", "ORDER CONFIRMED", body);
    }

    // ── Delivery OTP email ────────────────────────────────────────────────────
    public static String deliveryOtp(String customerName, long orderId, String otpCode) {
        String purpose = "Your delivery agent has arrived for <strong>Order #" + orderId + "</strong>. "
                + "Please share the OTP below with the agent to confirm delivery.";
        return otp(customerName, otpCode, purpose);
    }

    // ── Staff approval notification to admin ──────────────────────────────────
    public static String staffApprovalRequest(String staffName, String staffEmail, String staffPhone) {
        String body = """
            <p style="margin:0 0 20px;font-size:15px;color:#111827;">
              A new <strong>Staff / Delivery Agent</strong> has registered and is awaiting your approval.
            </p>

            <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:10px;padding:18px 20px;
                        margin-bottom:20px;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td width="36" valign="top">
                    <div style="width:36px;height:36px;background:#16a34a;border-radius:50%;
                                text-align:center;line-height:36px;color:#fff;font-size:16px;">👤</div>
                  </td>
                  <td style="padding-left:14px;">
                    <p style="margin:0;font-size:15px;font-weight:700;color:#111827;">%s</p>
                    <p style="margin:2px 0;font-size:13px;color:#6b7280;">%s</p>
                    <p style="margin:2px 0;font-size:13px;color:#6b7280;">📞 %s</p>
                  </td>
                </tr>
              </table>
            </div>

            <div style="background:#fff7ed;border-left:4px solid #f97316;border-radius:4px;
                        padding:12px 16px;">
              <p style="margin:0;font-size:13px;color:#92400e;">
                🔐 Login to the <strong>Admin Panel → 👥 Staff</strong> section to
                <strong>Approve</strong> or <strong>Reject</strong> this registration.
              </p>
            </div>
            """.formatted(staffName, staffEmail, staffPhone != null ? staffPhone : "Not provided");

        return wrap("New Staff Registration — NammaKadai", "#7c3aed", "ACTION REQUIRED", body);
    }

    // ── Item row for order confirmation table ─────────────────────────────────
    public static String itemRow(String name, int qty, double unitPrice, double subtotal) {
        return """
            <tr style="background:#ffffff;">
              <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;font-size:13px;
                         color:#111827;">%s</td>
              <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;font-size:13px;
                         color:#374151;text-align:center;">%d</td>
              <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;font-size:13px;
                         color:#374151;text-align:right;">&#8377;%.2f</td>
              <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;font-size:13px;
                         font-weight:600;color:#f97316;text-align:right;">&#8377;%.2f</td>
            </tr>
            """.formatted(name, qty, unitPrice, subtotal);
    }

    // ── Address block ──────────────────────────────────────────────────────────
    public static String addressBlock(String street, String city, String state,
                                       String pincode, String landmark) {
        String lm = (landmark != null && !landmark.isBlank())
                ? "<p style='margin:4px 0 0;font-size:12px;color:#9ca3af;'>📍 " + landmark + "</p>"
                : "";
        return """
            <div style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:10px;
                        padding:14px 18px;margin-bottom:20px;">
              <p style="margin:0 0 6px;font-size:12px;font-weight:700;color:#1d4ed8;
                        text-transform:uppercase;letter-spacing:1px;">📦 Delivery Address</p>
              <p style="margin:0;font-size:14px;font-weight:600;color:#111827;">%s</p>
              <p style="margin:2px 0;font-size:13px;color:#374151;">%s, %s — %s</p>
              %s
            </div>
            """.formatted(street, city, state, pincode, lm);
    }

    // ── Discount row ───────────────────────────────────────────────────────────
    public static String discountRow(String couponCode, double amount) {
        String label = (couponCode != null && !couponCode.isBlank())
                ? "Coupon Discount (" + couponCode + ")" : "Coupon Discount";
        return """
            <tr>
              <td style="padding:7px 16px;font-size:13px;color:#16a34a;font-weight:600;">%s</td>
              <td style="padding:7px 16px;font-size:13px;color:#16a34a;font-weight:600;
                         text-align:right;">− &#8377;%.2f</td>
            </tr>
            """.formatted(label, amount);
    }
}
