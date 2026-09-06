package com.rockandhardplaces.communication;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;
import com.rockandhardplaces.account.AccountAuthorizationService;

@DataJpaTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({
    CommunicationService.class,
    ProjectTeamSchemaMigration.class,
    AccountAuthorizationService.class
})

class CommunicationServiceTests {
 @Autowired CommunicationService service; @Autowired UserRepository users; @Autowired HomeownerRepository homeowners;
 @Autowired TradespersonRepository tradespeople; @Autowired ProjectRepository projects; @Autowired ProjectTeamRepository teams;
 @Autowired MessageRequestRepository requests; @Autowired ConversationRepository conversations; @Autowired ConversationParticipantRepository participants;
 @Autowired MessageRepository messages; @Autowired MessageAttachmentRepository attachments; @PersistenceContext EntityManager em;

 @Test void coldContactRequiresRequestInBothDirectionsAndIsProjectSpecific(){
  Fixture f=fixture(); Project other=project(f.homeowner,"Other");
  assertThatThrownBy(()->service.privateConversation(f.owner,f.trade,f.project)).isInstanceOf(SecurityException.class);
  MessageRequest outbound=service.requestCommunication(f.owner,f.trade,f.project); assertThat(outbound.getStatus()).isEqualTo(MessageRequestStatus.PENDING);
  service.cancel(outbound,f.owner);
  MessageRequest reverse=service.requestCommunication(f.trade,f.owner,f.project); Conversation accepted=service.accept(reverse,f.owner);
  assertThat(accepted.getType()).isEqualTo(ConversationType.PRIVATE);
  assertThatThrownBy(()->service.privateConversation(f.owner,f.trade,other)).isInstanceOf(SecurityException.class);
 }
 @Test void pendingIsUniqueButDeclinedAndCancelledRemainHistoryAndPermitRetry(){
  Fixture f=fixture(); MessageRequest first=service.requestCommunication(f.owner,f.trade,f.project);
  assertThatThrownBy(()->service.requestCommunication(f.trade,f.owner,f.project)).isInstanceOf(IllegalStateException.class);
  service.decline(first,f.trade); MessageRequest second=service.requestCommunication(f.trade,f.owner,f.project); service.cancel(second,f.trade);
  assertThat(service.requestCommunication(f.owner,f.trade,f.project).getStatus()).isEqualTo(MessageRequestStatus.PENDING);
  assertThat(requests.count()).isGreaterThanOrEqualTo(3);
 }
 @Test void activeTeamUsesOnePersistentConversationAndPrivateHistoryStaysPrivate(){
  Fixture f=fixture(); ProjectTeam one=teams.saveAndFlush(new ProjectTeam(f.project,f.tradesperson,ProjectTeamStatus.ACTIVE));
  Conversation team=service.synchronizeProjectTeam(one); Message old=service.send(team,f.owner,"existing history");
  Tradesperson carpenter=trade("carpenter"); ProjectTeam two=teams.saveAndFlush(new ProjectTeam(f.project,carpenter,ProjectTeamStatus.ACTIVE));
  Conversation same=service.synchronizeProjectTeam(two);
  assertThat(same.getId()).isEqualTo(team.getId()); assertThat(service.canAccess(same,carpenter.getUser())).isTrue();
  assertThat(messages.findByConversationOrderByCreatedAt(same)).contains(old);
  MessageRequest request=service.requestCommunication(f.owner,f.trade,f.project); Conversation privateChat=service.accept(request,f.trade);
  assertThat(service.canAccess(privateChat,carpenter.getUser())).isFalse();
  assertThat(conversations.findAllByProjectAndType(f.project,ConversationType.PROJECT_TEAM)).hasSize(1);
 }
 @Test void nonActiveCannotAccessAndSuspendThenReactivateReusesConversationWithoutDeletingHistory(){
  Fixture f=fixture(); ProjectTeam membership=teams.saveAndFlush(new ProjectTeam(f.project,f.tradesperson,ProjectTeamStatus.PENDING));
  Conversation team=service.synchronizeProjectTeam(membership); assertThat(service.canAccess(team,f.trade)).isFalse();
  service.updateMembership(membership,ProjectTeamStatus.ACTIVE); Message message=service.send(team,f.trade,"kept");
  ConversationParticipant participant=participants.findByConversationAndUser(team,f.trade).orElseThrow();
  var originalJoinedAt=participant.getJoinedAt();
  service.updateMembership(membership,ProjectTeamStatus.SUSPENDED); assertThat(service.canAccess(team,f.trade)).isFalse();
  assertThatThrownBy(()->service.send(team,f.trade,"denied")).isInstanceOf(SecurityException.class);
  assertThatThrownBy(()->service.edit(message,f.trade,"denied edit")).isInstanceOf(SecurityException.class);
  assertThatThrownBy(()->service.remove(message,f.trade)).isInstanceOf(SecurityException.class);
  Conversation restored=service.updateMembership(membership,ProjectTeamStatus.ACTIVE);
  assertThat(restored.getId()).isEqualTo(team.getId()); assertThat(service.canAccess(team,f.trade)).isTrue(); assertThat(messages.findById(message.getId())).isPresent();
  assertThat(participants.findByConversationAndUser(team,f.trade).orElseThrow().getJoinedAt()).isEqualTo(originalJoinedAt);
 }
 @Test void editsAndRemovalAreHistoricalAndAttachmentAuthorizationFollowsConversation(){
  Fixture f=fixture(); MessageRequest request=service.requestCommunication(f.owner,f.trade,f.project); Conversation c=service.accept(request,f.trade);
  Message m=service.send(c,f.owner,"before"); service.edit(m,f.owner,"after"); assertThat(m.getEditedAt()).isNotNull();
  MessageAttachment a=service.attach(m,f.owner,"photo.jpg","image/jpeg",42,"messages/key");
  assertThat(a.getOriginalFilename()).isEqualTo("photo.jpg"); assertThat(a.getContentType()).isEqualTo("image/jpeg"); assertThat(a.getFileSize()).isEqualTo(42); assertThat(a.getUploader()).isEqualTo(f.owner); assertThat(a.getCreatedAt()).isNotNull();
  User stranger=user("stranger"); assertThatThrownBy(()->service.attachment(a.getId(),stranger)).isInstanceOf(SecurityException.class);
  service.remove(m,f.owner); assertThat(m.isRemoved()).isTrue(); assertThat(messages.findById(m.getId())).isPresent(); assertThat(attachments.findByMessage(m)).containsExactly(a);
  assertThatThrownBy(()->service.edit(m,f.owner,"cannot edit removed message")).isInstanceOf(IllegalStateException.class);
 }
 private Fixture fixture(){User owner=user("owner"),tradeUser=user("trade");Homeowner h=homeowners.saveAndFlush(new Homeowner(owner,"Owner"));Tradesperson t=tradespeople.saveAndFlush(new Tradesperson(tradeUser,"Trade"));return new Fixture(owner,tradeUser,h,t,project(h,"Project"));}
 private User user(String prefix){return users.saveAndFlush(new User(prefix+UUID.randomUUID()+"@example.com"));}
 private Tradesperson trade(String prefix){return tradespeople.saveAndFlush(new Tradesperson(user(prefix),prefix));}
 private Project project(Homeowner h,String title){return projects.saveAndFlush(new Project(title,"Description",ProjectStatus.PLANNING,"12345",h));}
 private record Fixture(User owner,User trade,Homeowner homeowner,Tradesperson tradesperson,Project project){}
}
