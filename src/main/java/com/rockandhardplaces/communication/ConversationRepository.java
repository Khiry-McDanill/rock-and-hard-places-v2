package com.rockandhardplaces.communication;
import java.util.*;
import com.rockandhardplaces.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationRepository extends JpaRepository<Conversation,Long>{
 List<Conversation> findByProjectOrderByCreatedAt(Project project);
 Optional<Conversation> findByProjectAndType(Project project,ConversationType type);
 List<Conversation> findAllByProjectAndType(Project project,ConversationType type);
}
