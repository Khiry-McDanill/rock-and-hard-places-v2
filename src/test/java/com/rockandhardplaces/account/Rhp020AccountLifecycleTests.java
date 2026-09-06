package com.rockandhardplaces.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.rockandhardplaces.project.*;

class Rhp020AccountLifecycleTests {
    private final AccountAuthorizationService authorization = new AccountAuthorizationService();

    @Test
    void suspendedAndDeactivatedProfilesCannotActButKeepTheirIdentity() {
        User user = new User("lifecycle@example.com");
        Homeowner homeowner = new Homeowner(user, "Owner");
        Tradesperson tradesperson = new Tradesperson(user, "Worker");

        homeowner.setAccountStatus(AccountStatus.SUSPENDED);
        tradesperson.setAccountStatus(AccountStatus.DEACTIVATED);

        assertThatThrownBy(() -> authorization.requireActive(homeowner)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> authorization.requireActive(tradesperson)).isInstanceOf(SecurityException.class);
        assertThat(homeowner.getUser()).isSameAs(user);
        assertThat(tradesperson.getUser()).isSameAs(user);
    }

    @Test
    void onlyVerifiedTradespeopleMaySubmitBids() {
        Tradesperson bidder = new Tradesperson(new User("bidder@example.com"), "Bidder");
        assertThat(bidder.getVerificationStatus()).isEqualTo(TradespersonVerificationStatus.NOT_SUBMITTED);
        assertThatThrownBy(() -> authorization.requireVerifiedBidder(bidder)).isInstanceOf(SecurityException.class);

        bidder.setVerificationStatus(TradespersonVerificationStatus.PENDING);
        assertThatThrownBy(() -> authorization.requireVerifiedBidder(bidder)).isInstanceOf(SecurityException.class);
        bidder.setVerificationStatus(TradespersonVerificationStatus.VERIFIED);
        authorization.requireVerifiedBidder(bidder);
    }

    @Test
    void dualProfileUserSwitchesActiveContextWithoutChangingIdentity() {
        User user = new User(DemoActiveAccountContext.DEMO_EMAIL);
        Homeowner homeowner = new Homeowner(user, "Owner");
        Tradesperson tradesperson = new Tradesperson(user, "Worker");
        UserRepository users = mock(UserRepository.class);
        HomeownerRepository homeowners = mock(HomeownerRepository.class);
        TradespersonRepository tradespeople = mock(TradespersonRepository.class);
        when(users.findByEmail(DemoActiveAccountContext.DEMO_EMAIL)).thenReturn(Optional.of(user));
        when(homeowners.findByUser(user)).thenReturn(Optional.of(homeowner));
        when(tradespeople.findByUser(user)).thenReturn(Optional.of(tradesperson));
        DemoActiveAccountContext context = new DemoActiveAccountContext(users, homeowners, tradespeople);

        assertThat(context.activeProfile()).isSameAs(homeowner);
        context.switchTo(AccountRole.TRADESPERSON);
        assertThat(context.currentUser()).isSameAs(user);
        assertThat(context.activeProfile()).isSameAs(tradesperson);
    }

    @Test
    void bidSubmissionRejectsOppositeProfileOwnedBySameUser() {
        User user = new User("dual@example.com");
        Homeowner homeowner = new Homeowner(user, "Owner");
        Tradesperson tradesperson = new Tradesperson(user, "Worker");
        tradesperson.setVerificationStatus(TradespersonVerificationStatus.VERIFIED);
        Project project = new Project("Project", "Description", ProjectStatus.PLANNING, "12345", homeowner);
        Task task = new Task("Task", "Description", TaskStatus.PLANNING, project, null);
        BidSubmissionService service = new BidSubmissionService(mock(BidRepository.class), authorization);

        assertThatThrownBy(() -> service.submit(task, mock(TaskTrade.class), tradesperson,
                BigDecimal.TEN, null)).isInstanceOf(SecurityException.class)
                .hasMessageContaining("own opposite profile");
    }
}
