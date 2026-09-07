package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.communication.*;

@WebMvcTest(CommunicationController.class)
class CommunicationControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ApiAccessService access;
    @MockitoBean ConversationRepository conversations;
    @MockitoBean MessageRepository messages;
    @MockitoBean CommunicationService communication;

    @Test
    void nonParticipantCannotReadMessages() throws Exception {
        User user = mock(User.class); Conversation conversation = mock(Conversation.class);
        when(account.currentUser()).thenReturn(user);
        when(conversations.findById(77L)).thenReturn(Optional.of(conversation));
        when(communication.canAccess(conversation, user)).thenReturn(false);
        mvc.perform(get("/api/conversations/77/messages"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(messages);
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
