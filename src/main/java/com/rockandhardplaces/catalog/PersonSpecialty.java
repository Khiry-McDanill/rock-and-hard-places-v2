package com.rockandhardplaces.catalog;

import com.rockandhardplaces.account.Tradesperson;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "person_specialties", uniqueConstraints = {
        @UniqueConstraint(name = "uk_person_specialties_person_specialty", columnNames = {
                "tradesperson_id", "specialty_id" })
})
public class PersonSpecialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tradesperson_id", nullable = false)
    private Tradesperson tradesperson;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    protected PersonSpecialty() {
    }

    public PersonSpecialty(Tradesperson tradesperson, Specialty specialty) {
        this.tradesperson = tradesperson;
        this.specialty = specialty;
    }

    public Long getId() {
        return id;
    }

    public Tradesperson getTradesperson() {
        return tradesperson;
    }

    public Specialty getSpecialty() {
        return specialty;
    }
}