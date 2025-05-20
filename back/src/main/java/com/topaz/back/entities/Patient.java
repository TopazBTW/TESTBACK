package com.topaz.back.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "patients")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_naissance")
    private Date dateNaissance;

    @Column(name = "cin", unique = true, length = 10)
    private String cin;

    @Column(name = "sexe")
    private String sexe;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "type_de_soin")
    private String typedesoin;

    @Column(name = "inp")
    private String inp;
}
