package com.topaz.back.controllers;

import com.topaz.back.dtos.PatientDTO;
import com.topaz.back.services.PatientService;
import com.topaz.back.services.PdfService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService;
    private final PdfService pdfService;

    @GetMapping
    public ResponseEntity<List<PatientDTO>> getAll() {
        LOGGER.info("Fetching all patients");
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDTO> getById(@PathVariable Integer id) {
        LOGGER.info("Fetching patient with id: {}", id);
        return patientService.getPatientById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    LOGGER.warn("Patient not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<PatientDTO> create(@RequestBody PatientDTO dto) {
        LOGGER.info("Creating patient: {} {}", dto.getNom(), dto.getPrenom());
        try {
            return ResponseEntity.status(201).body(patientService.createPatient(dto));
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid patient data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientDTO> update(@PathVariable Integer id, @RequestBody PatientDTO dto) {
        LOGGER.info("Updating patient with id: {}", id);
        try {
            return ResponseEntity.ok(patientService.updatePatient(id, dto));
        } catch (EntityNotFoundException e) {
            LOGGER.warn("Patient not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid patient data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        LOGGER.info("Deleting patient with id: {}", id);
        try {
            patientService.deletePatient(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            LOGGER.warn("Patient not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/cnss-pdf")
    public ResponseEntity<byte[]> getCnssPdf(@PathVariable Integer id) {
        LOGGER.info("Generating CNSS PDF for patient id: {}", id);
        try {
            PatientDTO patient = patientService.getPatientById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));

            byte[] pdfBytes = pdfService.generateCnssPdfWithPatientData(patient);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "cnss_patient_" + id + ".pdf");
            headers.setContentLength(pdfBytes.length);

            LOGGER.info("CNSS PDF generated successfully for patient id: {}", id);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (EntityNotFoundException e) {
            LOGGER.warn("Patient not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            LOGGER.error("Failed to generate CNSS PDF for patient id: {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            LOGGER.error("Unexpected error generating CNSS PDF for patient id: {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}/cnss-pdf-debug")
    public ResponseEntity<byte[]> getCnssPdfDebug(@PathVariable Integer id) {
        LOGGER.info("Generating debug CNSS PDF for patient id: {}", id);
        try {
            PatientDTO patient = patientService.getPatientById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));

            byte[] pdfBytes = pdfService.generateDebugPdf(patient);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "cnss_patient_debug_" + id + ".pdf");
            headers.setContentLength(pdfBytes.length);

            LOGGER.info("Debug CNSS PDF generated successfully for patient id: {}", id);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (EntityNotFoundException e) {
            LOGGER.warn("Patient not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            LOGGER.error("Failed to generate debug CNSS PDF for patient id: {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            LOGGER.error("Unexpected error generating debug CNSS PDF for patient id: {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // Global exception handler for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(Exception e) {
        LOGGER.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(500).body("An unexpected error occurred");
    }
}
