package com.rockandhardplaces.communication;

import java.time.Instant;
import java.util.*;
import com.rockandhardplaces.project.Project;
import jakarta.persistence.*;

@Entity
@Table(name = "conversations")
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ConversationType type;
    @ManyToOne(optional = false) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @OneToMany(mappedBy = "conversation") private List<ConversationParticipant> participants = new ArrayList<>();
    @OneToMany(mappedBy = "conversation") private List<Message> messages = new ArrayList<>();
    protected Conversation() {}
    public Conversation(ConversationType type, Project project) { this.type=type; this.project=project; this.createdAt=Instant.now(); }
    public Long getId(){return id;} public ConversationType getType(){return type;} public Project getProject(){return project;}
    public Instant getCreatedAt(){return createdAt;} public List<ConversationParticipant> getParticipants(){return participants;}
    public List<Message> getMessages(){return messages;}
}
