package com.rockandhardplaces.communication;
import java.util.*;
import com.rockandhardplaces.account.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant,Long>{
 Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation,User user);
 List<ConversationParticipant> findByConversation(Conversation conversation);
 boolean existsByConversationAndUserAndActiveTrue(Conversation conversation,User user);
}
