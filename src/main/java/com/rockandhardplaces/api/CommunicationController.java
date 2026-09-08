package com.rockandhardplaces.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.rockandhardplaces.account.ActiveAccountContext;
import com.rockandhardplaces.communication.*;
import com.rockandhardplaces.project.Project;

@RestController
class CommunicationController {
    private final ActiveAccountContext account;
    private final ApiAccessService access;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final CommunicationService communication;
    private final CommunicationResponseMapper responses;

    CommunicationController(ActiveAccountContext account, ApiAccessService access,
            ConversationRepository conversations, MessageRepository messages,
            CommunicationService communication, CommunicationResponseMapper responses) {
        this.account = account; this.access = access; this.conversations = conversations;
        this.messages = messages; this.communication = communication; this.responses = responses;
    }

    @GetMapping("/api/projects/{projectId}/conversations")
    List<ApiDtos.ConversationResponse> conversations(@PathVariable Long projectId) {
        Project project = access.project(projectId, account.activeProfile());
        return conversations.findByProjectOrderByCreatedAt(project).stream()
                .filter(c -> communication.canAccess(c, account.currentUser()))
                .map(responses::conversation).toList();
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    List<ApiDtos.MessageResponse> messages(@PathVariable Long conversationId) {
        Conversation conversation = conversation(conversationId);
        return messages.findByConversationOrderByCreatedAt(conversation).stream()
                .map(responses::message).toList();
    }

    @PostMapping("/api/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.MessageResponse send(@PathVariable Long conversationId,
            @Valid @RequestBody ApiDtos.MessageRequest request) {
        return responses.message(communication.send(conversation(conversationId),
                account.currentUser(), request.body()));
    }

    private Conversation conversation(Long id) {
        Conversation conversation = conversations.findById(id).orElseThrow(ResourceNotFoundException::new);
        if (!communication.canAccess(conversation, account.currentUser()))
            throw new SecurityException("User cannot access this conversation");
        return conversation;
    }
}
