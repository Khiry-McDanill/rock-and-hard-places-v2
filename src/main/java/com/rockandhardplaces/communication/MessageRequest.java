package com.rockandhardplaces.communication;

import java.time.Instant;
import com.rockandhardplaces.account.User;
import com.rockandhardplaces.project.Project;
import jakarta.persistence.*;

@Entity
@Table(name = "message_requests")
public class MessageRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "requester_id", nullable = false) private User requester;
    @ManyToOne(optional = false) @JoinColumn(name = "recipient_id", nullable = false) private User recipient;
    @ManyToOne(optional = false) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MessageRequestStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "resolved_at") private Instant resolvedAt;

    protected MessageRequest() {}
    public MessageRequest(User requester, User recipient, Project project) {
        if (requester == recipient || (requester.getId() != null && requester.getId().equals(recipient.getId())))
            throw new IllegalArgumentException("A message request requires two different users");
        this.requester = requester; this.recipient = recipient; this.project = project;
        this.status = MessageRequestStatus.PENDING; this.createdAt = Instant.now();
    }
    public void accept() { resolve(MessageRequestStatus.ACCEPTED); }
    public void decline() { resolve(MessageRequestStatus.DECLINED); }
    public void cancel() { resolve(MessageRequestStatus.CANCELLED); }
    private void resolve(MessageRequestStatus next) {
        if (status != MessageRequestStatus.PENDING) throw new IllegalStateException("Only a pending request can be resolved");
        status = next; resolvedAt = Instant.now();
    }
    public Long getId() { return id; } public User getRequester() { return requester; }
    public User getRecipient() { return recipient; } public Project getProject() { return project; }
    public MessageRequestStatus getStatus() { return status; } public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
