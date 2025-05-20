package com.topaz.back.services;

import com.topaz.back.dtos.PatientDTO;
import com.topaz.back.entities.Patient;
import com.topaz.back.repositories.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final PdfService pdfService;

    public List<PatientDTO> getAllPatients() {
        LOGGER.info("Fetching all patients");
        return patientRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<PatientDTO> getPatientById(Integer id) {
        LOGGER.info("Fetching patient with id: {}", id);
        return patientRepository.findById(id)
                .map(this::convertToDTO);
    }

    public PatientDTO createPatient(PatientDTO dto) {
        LOGGER.info("Creating patient: {} {}", dto.getNom(), dto.getPrenom());
        validatePatientDTO(dto);
        Patient patient = convertToEntity(dto);
        return convertToDTO(patientRepository.save(patient));
    }

    public void deletePatient(Integer id) {
        LOGGER.info("Deleting patient with id: {}", id);
        if (!patientRepository.existsById(id)) {
            LOGGER.warn("Patient not found with id: {}", id);
            throw new EntityNotFoundException("Patient not found with id: " + id);
        }
        patientRepository.deleteById(id);
    }

    public PatientDTO updatePatient(Integer id, PatientDTO dto) {
        LOGGER.info("Updating patient with id: {}", id);
        validatePatientDTO(dto);
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("Patient not found with id: {}", id);
                    return new EntityNotFoundException("Patient not found with id: " + id);
                });

        patient.setNom(dto.getNom());
        patient.setPrenom(dto.getPrenom());
        patient.setDateNaissance(localDateToDate(dto.getDateNaissance()));
        patient.setCin(dto.getCin());
        patient.setSexe(dto.getSexe());
        patient.setAdresse(dto.getAdresse());
        patient.setTypedesoin(dto.getTypedesoin() != null ? dto.getTypedesoin().toLowerCase() : null);
        patient.setInp(dto.getInp());

        return convertToDTO(patientRepository.save(patient));
    }

    public byte[] generateCnssPdf(Integer id) throws IOException {
        LOGGER.info("Generating CNSS PDF for patient id: {}", id);
        PatientDTO patient = getPatientById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("Patient not found with id: {}", id);
                    return new EntityNotFoundException("Patient not found with id: " + id);
                });
        try {
            byte[] pdf = pdfService.generateCnssPdfWithPatientData(patient);
            LOGGER.info("CNSS PDF generated successfully for patient id: {}", id);
            return pdf;
        } catch (IOException e) {
            LOGGER.error("Failed to generate CNSS PDF for patient id: {}: {}", id, e.getMessage());
            throw e;
        }
    }

    private void validatePatientDTO(PatientDTO dto) {
        if (dto == null) {
            LOGGER.error("PatientDTO is null");
            throw new IllegalArgumentException("PatientDTO cannot be null");
        }
        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            LOGGER.error("Patient name is null or empty");
            throw new IllegalArgumentException("Patient name cannot be null or empty");
        }
        if (dto.getPrenom() == null || dto.getPrenom().trim().isEmpty()) {
            LOGGER.error("Patient first name is null or empty");
            throw new IllegalArgumentException("Patient first name cannot be null or empty");
        }
    }

    private PatientDTO convertToDTO(Patient patient) {
        PatientDTO dto = new PatientDTO();
        dto.setId(patient.getId());
        dto.setNom(patient.getNom());
        dto.setPrenom(patient.getPrenom());
        dto.setCin(patient.getCin());
        dto.setSexe(patient.getSexe());
        dto.setAdresse(patient.getAdresse());
        dto.setTypedesoin(patient.getTypedesoin());
        dto.setInp(patient.getInp());
        dto.setDateNaissance(dateToLocalDate(patient.getDateNaissance()));
        return dto;
    }

    private Patient convertToEntity(PatientDTO dto) {
        Patient patient = new Patient();
        patient.setId(dto.getId());
        patient.setNom(dto.getNom());
        patient.setPrenom(dto.getPrenom());
        patient.setCin(dto.getCin());
        patient.setSexe(dto.getSexe());
        patient.setAdresse(dto.getAdresse());
        patient.setTypedesoin(dto.getTypedesoin() != null ? dto.getTypedesoin().toLowerCase() : null);
        patient.setInp(dto.getInp());
        patient.setDateNaissance(localDateToDate(dto.getDateNaissance()));
        return patient;
    }

    private LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date localDateToDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
