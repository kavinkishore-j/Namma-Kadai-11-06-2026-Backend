package com.restaurent.nammakadai.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.restaurent.nammakadai.dto.BillResponse;
import com.restaurent.nammakadai.entity.Order;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.AddressRepository;
import com.restaurent.nammakadai.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;

    private static final DeviceRgb ORANGE      = new DeviceRgb(249, 115, 22);
    private static final DeviceRgb ORANGE_LIGHT = new DeviceRgb(255, 237, 213);
    private static final DeviceRgb GRAY_DARK   = new DeviceRgb(31, 41, 55);
    private static final DeviceRgb GRAY_MID    = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb GRAY_LIGHT  = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb WHITE       = new DeviceRgb(255, 255, 255);

    @Transactional(readOnly = true)
    public BillResponse getBill(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        BillResponse bill = new BillResponse();
        bill.setOrderId(order.getOrderId());
        bill.setCustomerName(order.getUser().getName());
        bill.setCustomerEmail(order.getUser().getEmail());
        bill.setCustomerPhone(order.getUser().getPhone());
        bill.setOrderedAt(order.getCreatedAt());
        bill.setOrderStatus(order.getStatus());
        bill.setTaxableAmount(order.getTotalAmount());
        bill.setCgst(order.getCgstAmount());
        bill.setSgst(order.getSgstAmount());
        bill.setGrandTotal(order.getGrandTotal());
        bill.setDiscountAmount(order.getDiscountAmount());
        bill.setCouponCode(order.getCouponCode());

        addressRepository.findByOrderOrderId(order.getOrderId()).ifPresent(addr -> {
            BillResponse.DeliveryAddress da = new BillResponse.DeliveryAddress();
            da.setStreet(addr.getStreet());
            da.setCity(addr.getCity());
            da.setState(addr.getState());
            da.setPincode(addr.getPincode());
            da.setLandmark(addr.getLandmark());
            bill.setDeliveryAddress(da);
        });

        bill.setItems(order.getOrderItems().stream().map(item -> {
            BillResponse.BillItem billItem = new BillResponse.BillItem();
            billItem.setItemName(item.getMenuItem().getName());
            billItem.setQuantity(item.getQuantity());
            billItem.setUnitPrice(item.getPrice());
            billItem.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return billItem;
        }).collect(Collectors.toList()));

        return bill;
    }

    public byte[] generatePdfBill(Long orderId) {
        BillResponse bill = getBill(orderId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            // Narrow receipt-style page
            Document doc = new Document(pdf, new PageSize(340, 700));
            doc.setMargins(24, 24, 24, 24);

            // ── Header ──────────────────────────────────────────────────────
            Paragraph brand = new Paragraph("NAMMA KADAI")
                    .setFontSize(22).setBold()
                    .setFontColor(ORANGE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2);
            doc.add(brand);

            doc.add(new Paragraph("GST TAX INVOICE")
                    .setFontSize(9).setFontColor(GRAY_MID)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2));

            doc.add(new Paragraph("Cash on Delivery")
                    .setFontSize(8).setFontColor(GRAY_MID)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            doc.add(divider(ORANGE));

            // ── Order meta ──────────────────────────────────────────────────
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            doc.add(infoRow("Order #", String.valueOf(bill.getOrderId())));
            doc.add(infoRow("Date", bill.getOrderedAt().format(fmt)));
            doc.add(infoRow("Status", bill.getOrderStatus()));

            doc.add(divider(GRAY_LIGHT));

            // ── Customer ────────────────────────────────────────────────────
            doc.add(sectionLabel("CUSTOMER"));
            doc.add(infoRow("Name", bill.getCustomerName()));
            doc.add(infoRow("Email", bill.getCustomerEmail()));
            if (bill.getCustomerPhone() != null)
                doc.add(infoRow("Phone", bill.getCustomerPhone()));

            // ── Delivery address ────────────────────────────────────────────
            if (bill.getDeliveryAddress() != null) {
                doc.add(divider(GRAY_LIGHT));
                doc.add(sectionLabel("DELIVERY ADDRESS"));
                BillResponse.DeliveryAddress da = bill.getDeliveryAddress();
                doc.add(new Paragraph(da.getStreet())
                        .setFontSize(9).setFontColor(GRAY_DARK).setMarginBottom(1));
                String line2 = da.getCity() + ", " + da.getState() + " - " + da.getPincode();
                doc.add(new Paragraph(line2)
                        .setFontSize(9).setFontColor(GRAY_DARK).setMarginBottom(1));
                if (da.getLandmark() != null && !da.getLandmark().isBlank())
                    doc.add(new Paragraph("Landmark: " + da.getLandmark())
                            .setFontSize(8).setFontColor(GRAY_MID).setMarginBottom(1));
            }

            // ── Items table ─────────────────────────────────────────────────
            doc.add(divider(GRAY_LIGHT));
            doc.add(sectionLabel("ORDER ITEMS"));

            Table table = new Table(UnitValue.createPercentArray(new float[]{45, 10, 20, 25}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Header row
            for (String h : new String[]{"Item", "Qty", "Rate", "Amount"}) {
                table.addHeaderCell(
                        new Cell().add(new Paragraph(h).setFontSize(8).setBold().setFontColor(WHITE))
                                .setBackgroundColor(ORANGE)
                                .setBorder(Border.NO_BORDER)
                                .setPadding(5)
                                .setTextAlignment(h.equals("Item") ? TextAlignment.LEFT : TextAlignment.RIGHT)
                );
            }

            // Item rows
            boolean alt = false;
            for (BillResponse.BillItem item : bill.getItems()) {
                DeviceRgb rowBg = alt ? GRAY_LIGHT : WHITE;
                table.addCell(styledCell(item.getItemName(), 8, GRAY_DARK, rowBg, TextAlignment.LEFT));
                table.addCell(styledCell(String.valueOf(item.getQuantity()), 8, GRAY_DARK, rowBg, TextAlignment.RIGHT));
                table.addCell(styledCell(fmt(item.getUnitPrice()), 8, GRAY_DARK, rowBg, TextAlignment.RIGHT));
                table.addCell(styledCell(fmt(item.getSubtotal()), 8, GRAY_DARK, rowBg, TextAlignment.RIGHT));
                alt = !alt;
            }

            doc.add(table);

            // ── Totals ──────────────────────────────────────────────────────
            doc.add(divider(GRAY_LIGHT));

            doc.add(totalRow("Taxable Amount", fmt(bill.getTaxableAmount()), false));
            doc.add(totalRow("CGST (2.5%)", fmt(bill.getCgst()), false));
            doc.add(totalRow("SGST (2.5%)", fmt(bill.getSgst()), false));

            if (bill.getDiscountAmount() != null && bill.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                String couponLabel = bill.getCouponCode() != null
                        ? "Discount (" + bill.getCouponCode() + ")" : "Discount";
                doc.add(totalRow(couponLabel, "- " + fmt(bill.getDiscountAmount()), false));
            }

            doc.add(divider(ORANGE));

            // Grand total highlighted box
            Table grandBox = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                    .setWidth(UnitValue.createPercentValue(100));
            grandBox.addCell(new Cell().add(new Paragraph("GRAND TOTAL").setFontSize(11).setBold().setFontColor(WHITE))
                    .setBackgroundColor(ORANGE).setBorder(Border.NO_BORDER).setPadding(6));
            grandBox.addCell(new Cell().add(new Paragraph(fmt(bill.getGrandTotal())).setFontSize(11).setBold().setFontColor(WHITE))
                    .setBackgroundColor(ORANGE).setBorder(Border.NO_BORDER).setPadding(6).setTextAlignment(TextAlignment.RIGHT));
            doc.add(grandBox);

            // ── Footer ──────────────────────────────────────────────────────
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph(bill.getGstNote())
                    .setFontSize(7).setFontColor(GRAY_MID)
                    .setTextAlignment(TextAlignment.CENTER).setItalic());
            doc.add(new Paragraph("Thank you for dining with us! 🍽")
                    .setFontSize(9).setBold().setFontColor(ORANGE)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(6));

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private LineSeparator divider(DeviceRgb color) {
        SolidLine line = new SolidLine(0.5f);
        line.setColor(color);
        return new LineSeparator(line).setMarginTop(6).setMarginBottom(6);
    }

    private Paragraph sectionLabel(String text) {
        return new Paragraph(text)
                .setFontSize(7).setBold().setFontColor(GRAY_MID)
                .setMarginBottom(4);
    }

    private Paragraph infoRow(String label, String value) {
        return new Paragraph()
                .add(new Text(label + ": ").setFontSize(8).setFontColor(GRAY_MID))
                .add(new Text(value).setFontSize(8).setBold().setFontColor(GRAY_DARK))
                .setMarginBottom(2);
    }

    private Paragraph totalRow(String label, String value, boolean bold) {
        Paragraph p = new Paragraph()
                .add(new Text(label).setFontSize(8).setFontColor(GRAY_MID))
                .add(new Text("  " + value).setFontSize(8).setFontColor(GRAY_DARK))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(2);
        if (bold) p.setBold();
        return p;
    }

    private Cell styledCell(String text, float size, DeviceRgb color, DeviceRgb bg, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(size).setFontColor(color))
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GRAY_LIGHT, 0.5f))
                .setPadding(4)
                .setTextAlignment(align);
    }

    private String fmt(BigDecimal val) {
        if (val == null) return "₹0.00";
        return "₹" + String.format("%.2f", val);
    }
}
