package com.rockandhardplaces.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.rockandhardplaces.catalog.PersonSpecialty;
import com.rockandhardplaces.catalog.PersonSpecialtyRepository;
import com.rockandhardplaces.catalog.Specialty;
import com.rockandhardplaces.catalog.SpecialtyRepository;
import com.rockandhardplaces.catalog.Trade;
import com.rockandhardplaces.catalog.TradeRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TradespersonDiscoveryPersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TradespersonRepository tradespersonRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private PersonSpecialtyRepository personSpecialtyRepository;

    @Test
    void persistsTradespersonBaseZipAndServiceRadius() {
        Tradesperson tradesperson = createTradesperson("location@example.com", "Location worker",
                "90210", 50, AvailabilityStatus.AVAILABLE_NOW, null);

        Tradesperson saved = tradespersonRepository.findById(tradesperson.getId()).orElseThrow();

        assertThat(saved.getBaseZip()).isEqualTo("90210");
        assertThat(saved.getServiceRadius()).isEqualTo(50);
    }

    @ParameterizedTest
    @EnumSource(AvailabilityStatus.class)
    void persistsEachAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        Tradesperson tradesperson = createTradesperson(availabilityStatus.name().toLowerCase()
                + "@example.com", availabilityStatus.name(), "90210", 25, availabilityStatus, null);

        assertThat(tradespersonRepository.findById(tradesperson.getId()).orElseThrow()
                .getAvailabilityStatus()).isEqualTo(availabilityStatus);
    }

    @Test
    void persistsOptionalAvailableStartDate() {
        LocalDate availableStartDate = LocalDate.of(2026, 10, 15);
        Tradesperson tradesperson = createTradesperson("soon@example.com", "Soon worker", "10001",
                100, AvailabilityStatus.AVAILABLE_SOON, availableStartDate);

        assertThat(tradespersonRepository.findById(tradesperson.getId()).orElseThrow()
                .getAvailableStartDate()).isEqualTo(availableStartDate);
    }

    @Test
    void allowsOneTradespersonToSelectMultipleSpecialties() {
        Tradesperson tradesperson = createTradesperson("specialties@example.com", "Specialist",
                "90210", 25, AvailabilityStatus.AVAILABLE_NOW, null);
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Specialty framing = specialtyRepository.saveAndFlush(new Specialty("Framing", carpentry));
        Specialty cabinetry = specialtyRepository.saveAndFlush(new Specialty("Cabinetry", carpentry));

        personSpecialtyRepository.saveAndFlush(new PersonSpecialty(tradesperson, framing));
        personSpecialtyRepository.saveAndFlush(new PersonSpecialty(tradesperson, cabinetry));

        assertThat(personSpecialtyRepository.findByTradesperson(tradesperson))
                .extracting(PersonSpecialty::getSpecialty)
                .containsExactlyInAnyOrder(framing, cabinetry);
    }

    @Test
    void allowsMultipleTradespeopleToSelectOneSpecialty() {
        Tradesperson first = createTradesperson("first-specialty@example.com", "First worker",
                "90210", 25, AvailabilityStatus.AVAILABLE_NOW, null);
        Tradesperson second = createTradesperson("second-specialty@example.com", "Second worker",
                "90210", 25, AvailabilityStatus.AVAILABLE_NOW, null);
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Specialty framing = specialtyRepository.saveAndFlush(new Specialty("Framing", carpentry));

        personSpecialtyRepository.saveAndFlush(new PersonSpecialty(first, framing));
        personSpecialtyRepository.saveAndFlush(new PersonSpecialty(second, framing));

        assertThat(personSpecialtyRepository.findBySpecialty(framing))
                .extracting(PersonSpecialty::getTradesperson)
                .containsExactlyInAnyOrder(first, second);
    }

    @Test
    void rejectsDuplicateTradespersonSpecialty() {
        Tradesperson tradesperson = createTradesperson("duplicate-specialty@example.com", "Specialist",
                "90210", 25, AvailabilityStatus.AVAILABLE_NOW, null);
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Specialty framing = specialtyRepository.saveAndFlush(new Specialty("Framing", carpentry));
        personSpecialtyRepository.saveAndFlush(new PersonSpecialty(tradesperson, framing));

        assertThatThrownBy(() -> personSpecialtyRepository
                .saveAndFlush(new PersonSpecialty(tradesperson, framing)))
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void specialtyRemainsAssociatedWithItsTrade() {
        Tradesperson tradesperson = createTradesperson("trade-link@example.com", "Specialist", "90210",
                25, AvailabilityStatus.AVAILABLE_NOW, null);
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Specialty framing = specialtyRepository.saveAndFlush(new Specialty("Framing", carpentry));
        PersonSpecialty personSpecialty = personSpecialtyRepository
                .saveAndFlush(new PersonSpecialty(tradesperson, framing));

        assertThat(personSpecialtyRepository.findById(personSpecialty.getId()).orElseThrow()
                .getSpecialty().getTrade().getId()).isEqualTo(carpentry.getId());
    }

    private Tradesperson createTradesperson(String email, String displayName, String baseZip,
            Integer serviceRadius, AvailabilityStatus availabilityStatus, LocalDate availableStartDate) {
        User user = userRepository.saveAndFlush(new User(email));
        return tradespersonRepository.saveAndFlush(new Tradesperson(user, displayName, baseZip,
                serviceRadius, availabilityStatus, availableStartDate));
    }
}