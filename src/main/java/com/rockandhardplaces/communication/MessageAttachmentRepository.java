package com.rockandhardplaces.communication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment,Long>{ List<MessageAttachment> findByMessage(Message message); }
