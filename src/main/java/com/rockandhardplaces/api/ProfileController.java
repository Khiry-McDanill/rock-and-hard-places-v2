package com.rockandhardplaces.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rockandhardplaces.account.HomeownerRepository;
import com.rockandhardplaces.account.TradespersonRepository;

@RestController
@RequestMapping("/api")
class ProfileController {
    private final HomeownerRepository homeowners;
    private final TradespersonRepository tradespeople;

    ProfileController(HomeownerRepository homeowners, TradespersonRepository tradespeople) {
        this.homeowners = homeowners;
        this.tradespeople = tradespeople;
    }

    @GetMapping("/homeowners/{id}")
    ApiDtos.ProfileResponse homeowner(@PathVariable Long id) {
        return homeowners.findById(id)
                .map(ApiDtos.ProfileResponse::from)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @GetMapping("/tradespeople/{id}")
    ApiDtos.ProfileResponse tradesperson(@PathVariable Long id) {
        return tradespeople.findById(id)
                .map(ApiDtos.ProfileResponse::from)
                .orElseThrow(ResourceNotFoundException::new);
    }
}
