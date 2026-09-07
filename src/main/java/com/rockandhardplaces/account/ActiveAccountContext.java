package com.rockandhardplaces.account;

import java.util.List;

public interface ActiveAccountContext {
    User currentUser();
    AccountRole activeRole();
    Object activeProfile();
    List<Object> availableProfiles();
    void switchTo(AccountRole role);
}
