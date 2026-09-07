package com.rockandhardplaces.api;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.portfolio.*;

@RestController
@RequestMapping("/api/tradespeople")
class PortfolioController {
    private final TradespersonRepository tradespeople;
    private final PortfolioItemRepository items;
    private final PortfolioPublicationRequestRepository publications;

    PortfolioController(TradespersonRepository tradespeople, PortfolioItemRepository items,
            PortfolioPublicationRequestRepository publications) {
        this.tradespeople = tradespeople; this.items = items; this.publications = publications;
    }

    @GetMapping("/{tradespersonId}/portfolio")
    List<ApiDtos.PortfolioResponse> portfolio(@PathVariable Long tradespersonId) {
        Tradesperson person = tradespeople.findById(tradespersonId)
                .orElseThrow(ResourceNotFoundException::new);
        return items.findByTradespersonOrderByCreatedAtDesc(person).stream()
                .map(item -> new Published(item, publications.findByPortfolioItemAndStatus(
                        item, PublicationStatus.APPROVED)))
                .filter(Published::publiclyVisible)
                .map(p -> ApiDtos.PortfolioResponse.from(p.item(), p.approvals())).toList();
    }

    private record Published(PortfolioItem item, List<PortfolioPublicationRequest> approvals) {
        boolean publiclyVisible() {
            return item.getProvenance() != PortfolioProvenance.RHP_VERIFIED || !approvals.isEmpty();
        }
    }
}
