package com.rockandhardplaces.communication;

import java.time.Instant;
import java.util.*;
import com.rockandhardplaces.account.User;
import jakarta.persistence.*;

@Entity @Table(name="messages")
public class Message {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="conversation_id",nullable=false) private Conversation conversation;
    @ManyToOne(optional=false) @JoinColumn(name="sender_id",nullable=false) private User sender;
    @Column(nullable=false,length=10000) private String body;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="edited_at") private Instant editedAt;
    @Column(name="removed_at") private Instant removedAt;
    @OneToMany(mappedBy="message") private List<MessageAttachment> attachments=new ArrayList<>();
    protected Message() {}
    public Message(Conversation conversation, User sender, String body){this.conversation=conversation;this.sender=sender;this.body=body;this.createdAt=Instant.now();}
    public void edit(String body){if(isRemoved())throw new IllegalStateException("A removed message cannot be edited");this.body=body;this.editedAt=Instant.now();} public void remove(){this.removedAt=Instant.now();}
    public Long getId(){return id;} public Conversation getConversation(){return conversation;} public User getSender(){return sender;}
    public String getBody(){return body;} public Instant getCreatedAt(){return createdAt;} public Instant getEditedAt(){return editedAt;}
    public Instant getRemovedAt(){return removedAt;} public boolean isRemoved(){return removedAt!=null;} public List<MessageAttachment> getAttachments(){return attachments;}
}
