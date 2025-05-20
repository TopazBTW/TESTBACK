package com.topaz.back.dtos;

import lombok.*;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PatientDTO {
    private Integer id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String cin;
    private String sexe;
    private String adresse;
    private String typedesoin;
    private String inp;
}
