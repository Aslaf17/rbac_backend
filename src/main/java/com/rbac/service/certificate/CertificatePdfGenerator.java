package com.rbac.service.certificate;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.rbac.model.certificate.Certificate;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Component
public class CertificatePdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public byte[] generate(Certificate certificate) {
        try {
            Document document = new Document(PageSize.A4.rotate(), 40, 40, 40, 40);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new BorderEvent());

            document.open();

            Font titleFont = new Font(Font.HELVETICA, 30, Font.BOLD, new Color(25, 42, 86));
            Font subTitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL, Color.DARK_GRAY);
            Font nameFont = new Font(Font.TIMES_ROMAN, 26, Font.BOLDITALIC, new Color(25, 42, 86));
            Font bodyFont = new Font(Font.HELVETICA, 13, Font.NORMAL, Color.DARK_GRAY);
            Font courseFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
            Font smallFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);

            Paragraph spacer = new Paragraph(" ");

            Paragraph title = new Paragraph("CERTIFICATE OF COMPLETION", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("This certificate is proudly presented to", subTitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingBefore(20);
            document.add(subtitle);

            document.add(spacer);

            Paragraph name = new Paragraph(certificate.getStudentName(), nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingBefore(10);
            document.add(name);

            document.add(spacer);

            Paragraph body = new Paragraph(
                    "for successfully completing the course",
                    bodyFont);
            body.setAlignment(Element.ALIGN_CENTER);
            document.add(body);

            Paragraph course = new Paragraph(certificate.getCourseName(), courseFont);
            course.setAlignment(Element.ALIGN_CENTER);
            course.setSpacingBefore(10);
            course.setSpacingAfter(10);
            document.add(course);

            String completionStr = certificate.getCompletionDate() != null
                    ? certificate.getCompletionDate().format(DATE_FORMAT) : "-";
            Paragraph completion = new Paragraph(
                    "Completion Date: " + completionStr, bodyFont);
            completion.setAlignment(Element.ALIGN_CENTER);
            document.add(completion);

            document.add(spacer);
            document.add(spacer);

            // Footer table: certificate id / issue date / verification code
            PdfContentByte cb = writer.getDirectContent();
            Font footerFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

            String issueStr = certificate.getIssueDate() != null
                    ? certificate.getIssueDate().format(DATE_FORMAT) : "-";

            Paragraph footer = new Paragraph();
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            footer.add(new Chunk("Certificate ID: " + certificate.getCertificateId() + "      ", footerFont));
            footer.add(new Chunk("Issue Date: " + issueStr, footerFont));
            document.add(footer);

            Paragraph verify = new Paragraph(
                    "Verify this certificate using code: " + certificate.getVerificationCode(),
                    smallFont);
            verify.setAlignment(Element.ALIGN_CENTER);
            verify.setSpacingBefore(8);
            document.add(verify);

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate certificate PDF", e);
        }
    }

    /** Draws a simple decorative border on every page. */
    private static class BorderEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle rect = document.getPageSize();
            canvas.setColorStroke(new Color(25, 42, 86));
            canvas.setLineWidth(3);
            canvas.rectangle(20, 20, rect.getWidth() - 40, rect.getHeight() - 40);
            canvas.stroke();
        }
    }
}
