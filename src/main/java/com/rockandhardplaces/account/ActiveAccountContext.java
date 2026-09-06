package com.rockandhardplaces.account;

public interface ActiveAccountContext {
    User currentUser();
    AccountRole activeRole();
    Object activeProfile();
    void switchTo(AccountRole role);
}
