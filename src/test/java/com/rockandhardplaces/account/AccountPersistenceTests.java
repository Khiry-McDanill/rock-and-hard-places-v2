package com.rockandhardplaces.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountPersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeownerRepository homeownerRepository;

    @Autowired
    private TradespersonRepository tradespersonRepository;

    @Test
    void persistsUserIdentity() {
        User savedUser = userRepository.saveAndFlush(new User("homeowner@example.com"));

        assertThat(savedUser.getId()).isNotNull();
        assertThat(userRepository.findById(savedUser.getId()))
                .get()
                .extracting(User::getEmail)
                .isEqualTo("homeowner@example.com");
    }

    @Test
    void persistsIndependentProfilesForTheSameUser() {
        long homeownerCountBefore = homeownerRepository.count();
        long tradespersonCountBefore = tradespersonRepository.count();
        User user = userRepository.saveAndFlush(new User("person@example.com"));

        Homeowner homeowner = homeownerRepository.saveAndFlush(
                new Homeowner(user, "Homeowner profile"));
        Tradesperson tradesperson = tradespersonRepository.saveAndFlush(
                new Tradesperson(user, "Tradesperson profile"));

        assertThat(homeownerRepository.findById(homeowner.getId()))
                .get()
                .extracting(Homeowner::getUser)
                .extracting(User::getId)
                .isEqualTo(user.getId());
        assertThat(tradespersonRepository.findById(tradesperson.getId()))
                .get()
                .extracting(Tradesperson::getUser)
                .extracting(User::getId)
                .isEqualTo(user.getId());
        assertThat(homeownerRepository.count()).isEqualTo(homeownerCountBefore + 1);
        assertThat(tradespersonRepository.count()).isEqualTo(tradespersonCountBefore + 1);
    }
}
