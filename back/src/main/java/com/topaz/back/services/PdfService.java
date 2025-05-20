package com.topaz.back.services;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.*;
import com.topaz.back.dtos.PatientDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String TEMPLATE_PATH = "/templates/cnss_form_template.pdf";

    // Define form field coordinates - these should match the actual form layout
    private static class FormFields {
        // Assuré(e) section
        static final int ASSURE_NOM_X = 150;
        static final int ASSURE_NOM_Y = 720;

        static final int ASSURE_INP_X = 450;
        static final int ASSURE_INP_Y = 720;

        static final int ASSURE_CIN_X = 450;
        static final int ASSURE_CIN_Y = 700;

        static final int LUI_MEME_X = 100;
        static final int LUI_MEME_Y = 680;

        static final int ASSURE_ADRESSE_X = 150;
        static final int ASSURE_ADRESSE_Y = 660;

        // Bénéficiaire section
        static final int BENEFICIAIRE_NOM_X = 150;
        static final int BENEFICIAIRE_NOM_Y = 620;

        static final int BENEFICIAIRE_DATE_NAISSANCE_X = 450;
        static final int BENEFICIAIRE_DATE_NAISSANCE_Y = 620;

        static final int BENEFICIAIRE_CIN_X = 450;
        static final int BENEFICIAIRE_CIN_Y = 600;

        static final int SEXE_M_X = 100;
        static final int SEXE_M_Y = 580;

        static final int SEXE_F_X = 150;
        static final int SEXE_F_Y = 580;

        // Dentiste section
        static final int DENTISTE_INP_X = 450;
        static final int DENTISTE_INP_Y = 540;

        // Type de soin section
        static final int SOIN_X = 100;
        static final int SOIN_Y = 500;

        static final int PROTHESE_X = 100;
        static final int PROTHESE_Y = 480;

        static final int ORTHODONTIE_X = 100;
        static final int ORTHODONTIE_Y = 460;

        static final int AUTRE_X = 100;
        static final int AUTRE_Y = 440;

        // Signature section
        static final int PATIENT_VILLE_X = 150;
        static final int PATIENT_VILLE_Y = 380;

        static final int PATIENT_DATE_X = 150;
        static final int PATIENT_DATE_Y = 360;

        static final int DENTISTE_VILLE_X = 450;
        static final int DENTISTE_VILLE_Y = 380;

        static final int DENTISTE_DATE_X = 450;
        static final int DENTISTE_DATE_Y = 360;
    }

    public byte[] generateCnssPdfWithPatientData(PatientDTO patient) throws IOException {
        if (patient == null) {
            throw new IllegalArgumentException("PatientDTO cannot be null");
        }
        LOGGER.info("Generating CNSS PDF for patient: {} {}", patient.getNom(), patient.getPrenom());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream template = getClass().getResourceAsStream(TEMPLATE_PATH);
        if (template == null) {
            LOGGER.error("Template not found at {}", TEMPLATE_PATH);
            throw new IOException("PDF template not found: " + TEMPLATE_PATH);
        }

        try (PdfReader reader = new PdfReader(template)) {
            PdfStamper stamper = new PdfStamper(reader, baos);

            // Get the content byte for writing on the PDF
            PdfContentByte canvas = stamper.getOverContent(1);

            // Set up the font
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            canvas.setFontAndSize(baseFont, 10);

            // Format patient data
            String fullName = (patient.getNom() != null ? patient.getNom() : "") + " " +
                    (patient.getPrenom() != null ? patient.getPrenom() : "");
            String formattedBirthDate = patient.getDateNaissance() != null ?
                    patient.getDateNaissance().format(DATE_FORMATTER) : "";
            String currentDate = LocalDate.now().format(DATE_FORMATTER);

            // Fill in the form with patient data

            // Assuré(e) section
            writeText(canvas, fullName, FormFields.ASSURE_NOM_X, FormFields.ASSURE_NOM_Y);
            writeText(canvas, patient.getInp(), FormFields.ASSURE_INP_X, FormFields.ASSURE_INP_Y);
            writeText(canvas, patient.getCin(), FormFields.ASSURE_CIN_X, FormFields.ASSURE_CIN_Y);
            writeText(canvas, "X", FormFields.LUI_MEME_X, FormFields.LUI_MEME_Y); // Lui-même checkbox
            writeText(canvas, patient.getAdresse(), FormFields.ASSURE_ADRESSE_X, FormFields.ASSURE_ADRESSE_Y);

            // Bénéficiaire section
            writeText(canvas, fullName, FormFields.BENEFICIAIRE_NOM_X, FormFields.BENEFICIAIRE_NOM_Y);
            writeText(canvas, formattedBirthDate, FormFields.BENEFICIAIRE_DATE_NAISSANCE_X, FormFields.BENEFICIAIRE_DATE_NAISSANCE_Y);
            writeText(canvas, patient.getCin(), FormFields.BENEFICIAIRE_CIN_X, FormFields.BENEFICIAIRE_CIN_Y);

            // Gender
            if ("M".equalsIgnoreCase(patient.getSexe())) {
                writeText(canvas, "X", FormFields.SEXE_M_X, FormFields.SEXE_M_Y);
            } else if ("F".equalsIgnoreCase(patient.getSexe())) {
                writeText(canvas, "X", FormFields.SEXE_F_X, FormFields.SEXE_F_Y);
            }

            // Dentiste section
            writeText(canvas, patient.getInp(), FormFields.DENTISTE_INP_X, FormFields.DENTISTE_INP_Y);

            // Type de soin section
            String typeDeSoin = patient.getTypedesoin() != null ? patient.getTypedesoin().toLowerCase() : "";
            if (typeDeSoin.contains("soin")) {
                writeText(canvas, "X", FormFields.SOIN_X, FormFields.SOIN_Y);
            }
            if (typeDeSoin.contains("prothese") || typeDeSoin.contains("prothèse")) {
                writeText(canvas, "X", FormFields.PROTHESE_X, FormFields.PROTHESE_Y);
            }
            if (typeDeSoin.contains("orthodontie")) {
                writeText(canvas, "X", FormFields.ORTHODONTIE_X, FormFields.ORTHODONTIE_Y);
            }
            if (!typeDeSoin.isEmpty() &&
                    !typeDeSoin.contains("soin") &&
                    !typeDeSoin.contains("prothese") &&
                    !typeDeSoin.contains("prothèse") &&
                    !typeDeSoin.contains("orthodontie")) {
                writeText(canvas, "X", FormFields.AUTRE_X, FormFields.AUTRE_Y);
            }

            // Signature section
            writeText(canvas, "Casablanca", FormFields.PATIENT_VILLE_X, FormFields.PATIENT_VILLE_Y);
            writeText(canvas, currentDate, FormFields.PATIENT_DATE_X, FormFields.PATIENT_DATE_Y);
            writeText(canvas, "Casablanca", FormFields.DENTISTE_VILLE_X, FormFields.DENTISTE_VILLE_Y);
            writeText(canvas, currentDate, FormFields.DENTISTE_DATE_X, FormFields.DENTISTE_DATE_Y);

            // Close the stamper to finalize the PDF
            stamper.close();
            LOGGER.info("PDF generated successfully for patient: {} {}", patient.getNom(), patient.getPrenom());
            return baos.toByteArray();

        } catch (DocumentException e) {
            LOGGER.error("PDF document error: {}", e.getMessage(), e);
            throw new IOException("PDF document error: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error generating PDF: {}", e.getMessage(), e);
            throw new IOException("Unexpected error generating PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to write text at specific coordinates on the PDF
     */
    private void writeText(PdfContentByte canvas, String text, float x, float y) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        canvas.beginText();
        canvas.setTextMatrix(x, y);
        canvas.showText(text);
        canvas.endText();
    }

    /**
     * Helper method to create a debug version of the PDF with visible field positions
     * This is useful for development to see where text is being placed
     */
    public byte[] generateDebugPdf(PatientDTO patient) throws IOException {
        if (patient == null) {
            throw new IllegalArgumentException("PatientDTO cannot be null");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream template = getClass().getResourceAsStream(TEMPLATE_PATH);
        if (template == null) {
            throw new IOException("PDF template not found: " + TEMPLATE_PATH);
        }

        try (PdfReader reader = new PdfReader(template)) {
            PdfStamper stamper = new PdfStamper(reader, baos);
            PdfContentByte canvas = stamper.getOverContent(1);

            // Draw coordinate grid
            drawCoordinateGrid(canvas);

            // Draw field positions
            drawFieldPositions(canvas);

            // Fill with sample data
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            canvas.setFontAndSize(baseFont, 10);

            // Fill in the form with patient data (same as in the main method)
            String fullName = (patient.getNom() != null ? patient.getNom() : "") + " " +
                    (patient.getPrenom() != null ? patient.getPrenom() : "");
            String formattedBirthDate = patient.getDateNaissance() != null ?
                    patient.getDateNaissance().format(DATE_FORMATTER) : "";
            String currentDate = LocalDate.now().format(DATE_FORMATTER);

            // Assuré(e) section
            writeText(canvas, fullName, FormFields.ASSURE_NOM_X, FormFields.ASSURE_NOM_Y);
            writeText(canvas, patient.getInp(), FormFields.ASSURE_INP_X, FormFields.ASSURE_INP_Y);
            writeText(canvas, patient.getCin(), FormFields.ASSURE_CIN_X, FormFields.ASSURE_CIN_Y);
            writeText(canvas, "X", FormFields.LUI_MEME_X, FormFields.LUI_MEME_Y);
            writeText(canvas, patient.getAdresse(), FormFields.ASSURE_ADRESSE_X, FormFields.ASSURE_ADRESSE_Y);

            // Bénéficiaire section
            writeText(canvas, fullName, FormFields.BENEFICIAIRE_NOM_X, FormFields.BENEFICIAIRE_NOM_Y);
            writeText(canvas, formattedBirthDate, FormFields.BENEFICIAIRE_DATE_NAISSANCE_X, FormFields.BENEFICIAIRE_DATE_NAISSANCE_Y);
            writeText(canvas, patient.getCin(), FormFields.BENEFICIAIRE_CIN_X, FormFields.BENEFICIAIRE_CIN_Y);

            // Gender
            if ("M".equalsIgnoreCase(patient.getSexe())) {
                writeText(canvas, "X", FormFields.SEXE_M_X, FormFields.SEXE_M_Y);
            } else if ("F".equalsIgnoreCase(patient.getSexe())) {
                writeText(canvas, "X", FormFields.SEXE_F_X, FormFields.SEXE_F_Y);
            }

            // Dentiste section
            writeText(canvas, patient.getInp(), FormFields.DENTISTE_INP_X, FormFields.DENTISTE_INP_Y);

            // Type de soin section
            String typeDeSoin = patient.getTypedesoin() != null ? patient.getTypedesoin().toLowerCase() : "";
            if (typeDeSoin.contains("soin")) {
                writeText(canvas, "X", FormFields.SOIN_X, FormFields.SOIN_Y);
            }
            if (typeDeSoin.contains("prothese") || typeDeSoin.contains("prothèse")) {
                writeText(canvas, "X", FormFields.PROTHESE_X, FormFields.PROTHESE_Y);
            }
            if (typeDeSoin.contains("orthodontie")) {
                writeText(canvas, "X", FormFields.ORTHODONTIE_X, FormFields.ORTHODONTIE_Y);
            }
            if (!typeDeSoin.isEmpty() &&
                    !typeDeSoin.contains("soin") &&
                    !typeDeSoin.contains("prothese") &&
                    !typeDeSoin.contains("prothèse") &&
                    !typeDeSoin.contains("orthodontie")) {
                writeText(canvas, "X", FormFields.AUTRE_X, FormFields.AUTRE_Y);
            }

            // Signature section
            writeText(canvas, "Casablanca", FormFields.PATIENT_VILLE_X, FormFields.PATIENT_VILLE_Y);
            writeText(canvas, currentDate, FormFields.PATIENT_DATE_X, FormFields.PATIENT_DATE_Y);
            writeText(canvas, "Casablanca", FormFields.DENTISTE_VILLE_X, FormFields.DENTISTE_VILLE_Y);
            writeText(canvas, currentDate, FormFields.DENTISTE_DATE_X, FormFields.DENTISTE_DATE_Y);

            stamper.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("PDF document error: " + e.getMessage(), e);
        }
    }

    /**
     * Draw a coordinate grid on the PDF for debugging purposes
     */
    private void drawCoordinateGrid(PdfContentByte canvas) throws DocumentException, IOException {
        canvas.setLineWidth(0.2f);
        canvas.setRGBColorStroke(200, 200, 200); // Light gray

        // Draw vertical lines every 50 units
        for (int x = 0; x <= 600; x += 50) {
            canvas.moveTo(x, 0);
            canvas.lineTo(x, 850);
            canvas.stroke();

            // Label the line
            canvas.beginText();
            canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED), 6);
            canvas.setTextMatrix(x, 5);
            canvas.showText(String.valueOf(x));
            canvas.endText();
        }

        // Draw horizontal lines every 50 units
        for (int y = 0; y <= 850; y += 50) {
            canvas.moveTo(0, y);
            canvas.lineTo(600, y);
            canvas.stroke();

            // Label the line
            canvas.beginText();
            canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED), 6);
            canvas.setTextMatrix(5, y);
            canvas.showText(String.valueOf(y));
            canvas.endText();
        }
    }

    /**
     * Draw field positions on the PDF for debugging purposes
     */
    private void drawFieldPositions(PdfContentByte canvas) throws DocumentException, IOException {
        canvas.setLineWidth(0.5f);
        canvas.setRGBColorStroke(255, 0, 0); // Red

        // Draw a small rectangle at each field position
        drawFieldMarker(canvas, FormFields.ASSURE_NOM_X, FormFields.ASSURE_NOM_Y, "Nom Assuré");
        drawFieldMarker(canvas, FormFields.ASSURE_INP_X, FormFields.ASSURE_INP_Y, "INP Assuré");
        drawFieldMarker(canvas, FormFields.ASSURE_CIN_X, FormFields.ASSURE_CIN_Y, "CIN Assuré");
        drawFieldMarker(canvas, FormFields.LUI_MEME_X, FormFields.LUI_MEME_Y, "Lui-même");
        drawFieldMarker(canvas, FormFields.ASSURE_ADRESSE_X, FormFields.ASSURE_ADRESSE_Y, "Adresse");

        drawFieldMarker(canvas, FormFields.BENEFICIAIRE_NOM_X, FormFields.BENEFICIAIRE_NOM_Y, "Nom Bénéficiaire");
        drawFieldMarker(canvas, FormFields.BENEFICIAIRE_DATE_NAISSANCE_X, FormFields.BENEFICIAIRE_DATE_NAISSANCE_Y, "Date Naissance");
        drawFieldMarker(canvas, FormFields.BENEFICIAIRE_CIN_X, FormFields.BENEFICIAIRE_CIN_Y, "CIN Bénéficiaire");
        drawFieldMarker(canvas, FormFields.SEXE_M_X, FormFields.SEXE_M_Y, "M");
        drawFieldMarker(canvas, FormFields.SEXE_F_X, FormFields.SEXE_F_Y, "F");

        drawFieldMarker(canvas, FormFields.DENTISTE_INP_X, FormFields.DENTISTE_INP_Y, "INP Dentiste");

        drawFieldMarker(canvas, FormFields.SOIN_X, FormFields.SOIN_Y, "Soin");
        drawFieldMarker(canvas, FormFields.PROTHESE_X, FormFields.PROTHESE_Y, "Prothèse");
        drawFieldMarker(canvas, FormFields.ORTHODONTIE_X, FormFields.ORTHODONTIE_Y, "Orthodontie");
        drawFieldMarker(canvas, FormFields.AUTRE_X, FormFields.AUTRE_Y, "Autre");

        drawFieldMarker(canvas, FormFields.PATIENT_VILLE_X, FormFields.PATIENT_VILLE_Y, "Ville Patient");
        drawFieldMarker(canvas, FormFields.PATIENT_DATE_X, FormFields.PATIENT_DATE_Y, "Date Patient");
        drawFieldMarker(canvas, FormFields.DENTISTE_VILLE_X, FormFields.DENTISTE_VILLE_Y, "Ville Dentiste");
        drawFieldMarker(canvas, FormFields.DENTISTE_DATE_X, FormFields.DENTISTE_DATE_Y, "Date Dentiste");
    }

    /**
     * Draw a marker at a field position with a label
     */
    private void drawFieldMarker(PdfContentByte canvas, float x, float y, String label) throws DocumentException, IOException {
        // Draw a small rectangle at the position
        canvas.rectangle(x - 2, y - 2, 4, 4);
        canvas.stroke();

        // Add a label
        canvas.beginText();
        canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED), 6);
        canvas.setRGBColorFill(255, 0, 0); // Red text
        canvas.setTextMatrix(x + 5, y);
        canvas.showText(label);
        canvas.endText();

        canvas.setRGBColorFill(0, 0, 0); // Reset to black
    }
}
