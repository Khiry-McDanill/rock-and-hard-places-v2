package com.rockandhardplaces.communication;

import java.time.Instant;
import com.rockandhardplaces.account.User;
import jakarta.persistence.*;

@Entity @Table(name="message_attachments")
public class MessageAttachment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="message_id",nullable=false) private Message message;
    @Column(name="original_filename",nullable=false) private String originalFilename;
    @Column(name="content_type",nullable=false) private String contentType;
    @Column(name="file_size",nullable=false) private long fileSize;
    @Column(name="storage_key",nullable=false,unique=true) private String storageKey;
    @ManyToOne(optional=false) @JoinColumn(name="uploader_id",nullable=false) private User uploader;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    protected MessageAttachment() {}
    public MessageAttachment(Message message,String originalFilename,String contentType,long fileSize,String storageKey,User uploader){
        if(fileSize<0) throw new IllegalArgumentException("File size cannot be negative");
        this.message=message;this.originalFilename=originalFilename;this.contentType=contentType;this.fileSize=fileSize;this.storageKey=storageKey;this.uploader=uploader;this.createdAt=Instant.now();
    }
    public Long getId(){return id;} public Message getMessage(){return message;} public String getOriginalFilename(){return originalFilename;}
    public String getContentType(){return contentType;} public long getFileSize(){return fileSize;} public String getStorageKey(){return storageKey;}
    public User getUploader(){return uploader;} public Instant getCreatedAt(){return createdAt;}
}
