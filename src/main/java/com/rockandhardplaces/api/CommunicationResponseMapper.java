package com.rockandhardplaces.api;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.communication.*;
import com.rockandhardplaces.project.*;

/** Maps display-only identity after the controller's conversation access check. */
@Component
@Transactional(readOnly = true)
class CommunicationResponseMapper {
    private final ConversationParticipantRepository participants;
    private final HomeownerRepository homeowners;
    private final TradespersonRepository tradespeople;
    private final ProjectTeamRepository teams;

    CommunicationResponseMapper(ConversationParticipantRepository participants,
            HomeownerRepository homeowners, TradespersonRepository tradespeople, ProjectTeamRepository teams) {
        this.participants = participants; this.homeowners = homeowners;
        this.tradespeople = tradespeople; this.teams = teams;
    }

    public ApiDtos.ConversationResponse conversation(Conversation conversation) {
        return ApiDtos.ConversationResponse.from(conversation, participants.findByConversation(conversation)
                .stream().filter(ConversationParticipant::isActive)
                .map(p -> identity(p.getUser(), conversation.getProject())).toList());
    }

    public ApiDtos.MessageResponse message(Message message) {
        return ApiDtos.MessageResponse.from(message,
                identity(message.getSender(), message.getConversation().getProject()));
    }

    private ApiDtos.ParticipantResponse identity(User user, Project project) {
        Homeowner owner = project.getHomeowner();
        if (Objects.equals(owner.getUser().getId(), user.getId())) {
            return new ApiDtos.ParticipantResponse(user.getId(), owner.getDisplayName(), List.of());
        }
        var tradesperson = tradespeople.findByUser(user);
        if (tradesperson.isPresent()) {
            List<String> trades = teams.findByProject(project).stream()
                    .filter(t -> t.getStatus() == ProjectTeamStatus.ACTIVE)
                    .filter(t -> Objects.equals(t.getTradesperson().getUser().getId(), user.getId()))
                    .flatMap(t -> t.getProjectTeamTrades().stream())
                    .map(t -> t.getTrade().getName()).distinct().sorted().toList();
            return new ApiDtos.ParticipantResponse(user.getId(), tradesperson.get().getDisplayName(), trades);
        }
        return new ApiDtos.ParticipantResponse(user.getId(),
                homeowners.findByUser(user).map(Homeowner::getDisplayName).orElse(null), List.of());
    }
}
