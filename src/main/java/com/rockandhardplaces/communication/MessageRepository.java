package com.rockandhardplaces.communication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;
public interface MessageRepository extends Repository<Message,Long>{
 Message save(Message message);
 Message saveAndFlush(Message message);
 Optional<Message> findById(Long id);
 List<Message> findByConversationOrderByCreatedAt(Conversation conversation);
}
