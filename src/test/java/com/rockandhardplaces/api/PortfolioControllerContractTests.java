package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.portfolio.*;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean TradespersonRepository tradespeople;
    @MockitoBean PortfolioItemRepository items;
    @MockitoBean PortfolioPublicationRequestRepository publications;

    @Test
    void hidesUnapprovedRhpProvenanceButShowsPublicProvenance() throws Exception {
        Tradesperson person = mock(Tradesperson.class);
        PortfolioItem pendingRhp = item(person, 1L, PortfolioProvenance.RHP_VERIFIED);
        PortfolioItem selfReported = item(person, 2L, PortfolioProvenance.SELF_REPORTED);
        when(tradespeople.findById(12L)).thenReturn(Optional.of(person));
        when(items.findByTradespersonOrderByCreatedAtDesc(person))
                .thenReturn(List.of(pendingRhp, selfReported));
        when(publications.findByPortfolioItemAndStatus(any(), eq(PublicationStatus.APPROVED)))
                .thenReturn(List.of());

        mvc.perform(get("/api/tradespeople/12/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].provenance").value("SELF_REPORTED"));
    }

    private PortfolioItem item(Tradesperson person, Long id, PortfolioProvenance provenance) {
        PortfolioItem item = mock(PortfolioItem.class);
        when(item.getId()).thenReturn(id); when(item.getTradesperson()).thenReturn(person);
        when(person.getId()).thenReturn(12L); when(item.getProvenance()).thenReturn(provenance);
        return item;
    }
}
