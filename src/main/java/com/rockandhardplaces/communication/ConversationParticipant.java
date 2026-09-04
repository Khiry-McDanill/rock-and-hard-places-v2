package com.rockandhardplaces.communication;

import java.time.Instant;
import com.rockandhardplaces.account.User;
import jakarta.persistence.*;

@Entity
@Table(name="conversation_participants", uniqueConstraints=@UniqueConstraint(name="uk_conversation_participant", columnNames={"conversation_id","user_id"}))
public class ConversationParticipant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="conversation_id", nullable=false) private Conversation conversation;
    @ManyToOne(optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false) private boolean active;
    @Column(name="joined_at", nullable=false) private Instant joinedAt;
    @Column(name="revoked_at") private Instant revokedAt;
    protected ConversationParticipant() {}
    public ConversationParticipant(Conversation conversation, User user) {
        this.conversation=conversation;
        this.user=user;
        this.joinedAt=Instant.now();
        activate();
    }
    public void activate(){active=true; revokedAt=null;} public void revoke(){active=false; revokedAt=Instant.now();}
    public Long getId(){return id;} public Conversation getConversation(){return conversation;} public User getUser(){return user;}
    public boolean isActive(){return active;} public Instant getJoinedAt(){return joinedAt;} public Instant getRevokedAt(){return revokedAt;}
}
