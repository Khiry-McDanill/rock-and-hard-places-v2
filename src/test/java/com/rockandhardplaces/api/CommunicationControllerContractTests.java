package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;
import java.util.List;
import org.springframework.context.annotation.Import;
import com.rockandhardplaces.project.*;
import com.rockandhardplaces.catalog.Trade;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.communication.*;

@WebMvcTest(CommunicationController.class)
@Import(CommunicationResponseMapper.class)
class CommunicationControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ApiAccessService access;
    @MockitoBean ConversationRepository conversations;
    @MockitoBean MessageRepository messages;
    @MockitoBean CommunicationService communication;

    @MockitoBean ConversationParticipantRepository participants;
    @MockitoBean HomeownerRepository homeowners;
    @MockitoBean TradespersonRepository tradespeople;
    @MockitoBean ProjectTeamRepository teams;

    @Test
    void authorizedResponsesUseProfileIdentityAndProjectTradesOnly() throws Exception {
        User owner = mock(User.class), sender = mock(User.class);
        when(owner.getId()).thenReturn(41L);
        when(sender.getId()).thenReturn(93L);
        Homeowner homeowner = new Homeowner(owner, "Alex Owner");
        Tradesperson person = new Tradesperson(sender, "Nina Alvarez");
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(12L);
        when(project.getHomeowner()).thenReturn(homeowner);
        Conversation conversation = mock(Conversation.class);
        when(conversation.getId()).thenReturn(77L);
        when(conversation.getProject()).thenReturn(project);
        when(conversation.getType()).thenReturn(ConversationType.PRIVATE);
        ProjectTeam team = new ProjectTeam(project, person, ProjectTeamStatus.ACTIVE);
        Trade trade = mock(Trade.class);
        when(trade.getName()).thenReturn("Plumbing");
        team.getProjectTeamTrades().add(new ProjectTeamTrade(team, trade));
        when(teams.findByProject(project)).thenReturn(List.of(team));
        when(tradespeople.findByUser(sender)).thenReturn(Optional.of(person));
        when(account.currentUser()).thenReturn(owner);
        when(conversations.findById(77L)).thenReturn(Optional.of(conversation));
        when(communication.canAccess(conversation, owner)).thenReturn(true);
        Message message = new Message(conversation, sender, "Ready for inspection");
        when(messages.findByConversationOrderByCreatedAt(conversation)).thenReturn(List.of(message));
        mvc.perform(get("/api/conversations/77/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senderId").value(93))
                .andExpect(jsonPath("$[0].senderDisplayName").value("Nina Alvarez"))
                .andExpect(jsonPath("$[0].senderTrades[0]").value("Plumbing"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].sender").doesNotExist());
        when(communication.send(conversation, owner, "Thanks")).thenReturn(new Message(conversation, owner, "Thanks"));
        mvc.perform(post("/api/conversations/77/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Thanks\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderDisplayName").value("Alex Owner"))
                .andExpect(jsonPath("$.senderTrades").isEmpty());
        when(access.project(eq(12L), any())).thenReturn(project);
        Conversation hidden = mock(Conversation.class);
        when(conversations.findByProjectOrderByCreatedAt(project)).thenReturn(List.of(conversation, hidden));
        ConversationParticipant revoked = new ConversationParticipant(conversation, mock(User.class));
        revoked.revoke();
        when(participants.findByConversation(conversation)).thenReturn(List.of(
                new ConversationParticipant(conversation, owner), new ConversationParticipant(conversation, sender), revoked));
        mvc.perform(get("/api/projects/12/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].participants.length()").value(2))
                .andExpect(jsonPath("$[0].participants[1].userId").value(93))
                .andExpect(jsonPath("$[0].participants[1].displayName").value("Nina Alvarez"))
                .andExpect(jsonPath("$[0].participants[1].trades[0]").value("Plumbing"))
                .andExpect(jsonPath("$[0].participants[1].email").doesNotExist());
        verify(participants, never()).findByConversation(hidden);
        when(teams.findByProject(project)).thenReturn(List.of());
        mvc.perform(get("/api/conversations/77/messages"))
                .andExpect(jsonPath("$[0].senderDisplayName").value("Nina Alvarez"))
                .andExpect(jsonPath("$[0].senderTrades").isEmpty());
        when(tradespeople.findByUser(sender)).thenReturn(Optional.empty());
        mvc.perform(get("/api/conversations/77/messages"))
                .andExpect(jsonPath("$[0].senderDisplayName").isEmpty())
                .andExpect(jsonPath("$[0].senderTrades").isEmpty());
    }

    @Test
    void nonParticipantCannotReadMessages() throws Exception {
        User user = mock(User.class); Conversation conversation = mock(Conversation.class);
        when(account.currentUser()).thenReturn(user);
        when(conversations.findById(77L)).thenReturn(Optional.of(conversation));
        when(communication.canAccess(conversation, user)).thenReturn(false);
        mvc.perform(get("/api/conversations/77/messages"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(messages, participants, homeowners, tradespeople, teams);
        mvc.perform(post("/api/conversations/77/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Denied\"}"))
                .andExpect(status().isForbidden());
        verify(communication, never()).send(any(), any(), any());
    }

    @Test
    void missingConversationUsesSharedNotFoundShape() throws Exception {
        when(conversations.findById(88L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/conversations/88/messages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }
}
