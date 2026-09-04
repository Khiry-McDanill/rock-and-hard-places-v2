package com.rockandhardplaces.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TradeSpecialtyPersistenceTests {

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Test
    void persistsAndRetrievesTrade() {
        Trade savedTrade = tradeRepository.saveAndFlush(new Trade("Carpentry"));

        assertThat(savedTrade.getId()).isNotNull();
        assertThat(tradeRepository.findById(savedTrade.getId()))
                .get()
                .extracting(Trade::getName)
                .isEqualTo("Carpentry");
    }

    @Test
    void persistsSpecialtyWithItsParentTrade() {
        Trade trade = tradeRepository.saveAndFlush(new Trade("Plumbing"));

        Specialty specialty = specialtyRepository.saveAndFlush(
                new Specialty("Fixture Installation", trade));

        assertThat(specialtyRepository.findById(specialty.getId()))
                .get()
                .extracting(Specialty::getTrade)
                .extracting(Trade::getId)
                .isEqualTo(trade.getId());
    }

    @Test
    void allowsMultipleSpecialtiesForOneTrade() {
        Trade trade = tradeRepository.saveAndFlush(new Trade("Carpentry"));

        specialtyRepository.saveAndFlush(new Specialty("Framing", trade));
        specialtyRepository.saveAndFlush(new Specialty("Cabinetry", trade));

        assertThat(specialtyRepository.findAll())
                .extracting(Specialty::getTrade)
                .allMatch(savedTrade -> savedTrade.getId().equals(trade.getId()));
        assertThat(specialtyRepository.count()).isEqualTo(2);
    }

    @Test
    void rejectsDuplicateSpecialtyNameWithinOneTrade() {
        Trade trade = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        specialtyRepository.saveAndFlush(new Specialty("Framing", trade));

        assertThatThrownBy(() -> specialtyRepository.saveAndFlush(new Specialty("Framing", trade)))
            .hasMessageContaining("UNIQUE constraint failed");
    }
}