package com.topaz.back.repositories;

import com.topaz.back.entities.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
    boolean existsByCin(String cin);
}
