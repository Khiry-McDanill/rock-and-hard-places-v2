package com.rockandhardplaces.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.rockandhardplaces.catalog.Specialty;
import com.rockandhardplaces.catalog.SpecialtyRepository;
import com.rockandhardplaces.catalog.Trade;
import com.rockandhardplaces.catalog.TradeRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonTradePersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TradespersonRepository tradespersonRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PersonTradeRepository personTradeRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Test
    void allowsOneTradespersonToHaveMultipleTrades() {
        Tradesperson tradesperson = createTradesperson("carpenter@example.com", "Carpenter");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Trade plumbing = tradeRepository.saveAndFlush(new Trade("Plumbing"));

        personTradeRepository.saveAndFlush(new PersonTrade(tradesperson, carpentry));
        personTradeRepository.saveAndFlush(new PersonTrade(tradesperson, plumbing));

        assertThat(personTradeRepository.findByTradesperson(tradesperson))
                .extracting(PersonTrade::getTrade)
                .extracting(Trade::getName)
                .containsExactlyInAnyOrder("Carpentry", "Plumbing");
    }

    @Test
    void allowsMultipleTradespeopleToShareOneTrade() {
        Tradesperson first = createTradesperson("first@example.com", "First carpenter");
        Tradesperson second = createTradesperson("second@example.com", "Second carpenter");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));

        personTradeRepository.saveAndFlush(new PersonTrade(first, carpentry));
        personTradeRepository.saveAndFlush(new PersonTrade(second, carpentry));

        assertThat(personTradeRepository.findByTrade(carpentry))
                .extracting(PersonTrade::getTradesperson)
                .extracting(Tradesperson::getDisplayName)
                .containsExactlyInAnyOrder("First carpenter", "Second carpenter");
    }

    @Test
    void rejectsDuplicateTradespersonTradeQualification() {
        Tradesperson tradesperson = createTradesperson("duplicate@example.com", "Carpenter");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        personTradeRepository.saveAndFlush(new PersonTrade(tradesperson, carpentry));

        assertThatThrownBy(() -> personTradeRepository.saveAndFlush(new PersonTrade(tradesperson, carpentry)))
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void retrievesQualificationsInBothDirections() {
        Tradesperson tradesperson = createTradesperson("retrieve@example.com", "Carpenter");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        PersonTrade qualification = personTradeRepository.saveAndFlush(new PersonTrade(tradesperson, carpentry));

        assertThat(personTradeRepository.findByTradesperson(tradesperson)).containsExactly(qualification);
        assertThat(personTradeRepository.findByTrade(carpentry)).containsExactly(qualification);
    }

    @Test
    void tradespersonHasNoDirectTradeIdField() {
        assertThat(Tradesperson.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("trade", "tradeId");
    }

    @Test
    void tradeSpecialtyRelationshipRemainsIntact() {
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Specialty specialty = specialtyRepository.saveAndFlush(new Specialty("Framing", carpentry));

        assertThat(specialtyRepository.findById(specialty.getId()))
                .get()
                .extracting(Specialty::getTrade)
                .extracting(Trade::getId)
                .isEqualTo(carpentry.getId());
    }

    private Tradesperson createTradesperson(String email, String displayName) {
        User user = userRepository.saveAndFlush(new User(email));
        return tradespersonRepository.saveAndFlush(new Tradesperson(user, displayName));
    }
}