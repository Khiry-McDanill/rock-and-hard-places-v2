package com.rockandhardplaces.communication;
import java.util.*;
import com.rockandhardplaces.account.User;
import com.rockandhardplaces.project.Project;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface MessageRequestRepository extends JpaRepository<MessageRequest,Long>{
 @Query("select r from MessageRequest r where r.project=:project and r.status=:status and ((r.requester=:a and r.recipient=:b) or (r.requester=:b and r.recipient=:a))")
 List<MessageRequest> between(@Param("a") User a,@Param("b") User b,@Param("project") Project project,@Param("status") MessageRequestStatus status);
}
