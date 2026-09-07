package com.rockandhardplaces.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.rockandhardplaces.account.ActiveAccountContext;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
class AccountController {
    private final ActiveAccountContext accountContext;

    AccountController(ActiveAccountContext accountContext) {
        this.accountContext = accountContext;
    }

    @GetMapping
    ApiDtos.AccountResponse account() {
        return ApiDtos.AccountResponse.from(accountContext);
    }

    @PostMapping("/switch")
    @ResponseStatus(HttpStatus.OK)
    ApiDtos.AccountResponse switchRole(@Valid @RequestBody ApiDtos.SwitchRoleRequest request) {
        accountContext.switchTo(request.role());
        return ApiDtos.AccountResponse.from(accountContext);
    }
}
