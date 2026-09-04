package com.rockandhardplaces.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rockandhardplaces.account.Tradesperson;

public interface PersonSpecialtyRepository extends JpaRepository<PersonSpecialty, Long> {

    List<PersonSpecialty> findByTradesperson(Tradesperson tradesperson);

    List<PersonSpecialty> findBySpecialty(Specialty specialty);
}