package com.rockandhardplaces.communication;

import java.util.Objects;
import com.rockandhardplaces.account.User;
import com.rockandhardplaces.account.AccountAuthorizationService;
import com.rockandhardplaces.project.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunicationService {
 private final MessageRequestRepository requests; private final ConversationRepository conversations;
 private final ConversationParticipantRepository participants; private final MessageRepository messages;
 private final MessageAttachmentRepository attachments; private final ProjectTeamRepository teams;
 private final AccountAuthorizationService authorization;
 public CommunicationService(MessageRequestRepository requests,ConversationRepository conversations,ConversationParticipantRepository participants,MessageRepository messages,MessageAttachmentRepository attachments,ProjectTeamRepository teams,AccountAuthorizationService authorization){
  this.requests=requests;this.conversations=conversations;this.participants=participants;this.messages=messages;this.attachments=attachments;this.teams=teams;
  this.authorization=authorization;
 }
 @Transactional public MessageRequest requestCommunication(User requester,User recipient,Project project){
  authorization.requireDifferentUsers(requester,recipient);
  if(!requests.between(requester,recipient,project,MessageRequestStatus.PENDING).isEmpty()) throw new IllegalStateException("A pending message request already exists for these users and project");
  return requests.saveAndFlush(new MessageRequest(requester,recipient,project));
 }
 @Transactional public Conversation accept(MessageRequest request,User recipient){
  if(!same(request.getRecipient(),recipient)) throw new SecurityException("Only the recipient may accept a message request");
  request.accept();requests.saveAndFlush(request);return privateConversation(request.getRequester(),request.getRecipient(),request.getProject());
 }
 @Transactional public void decline(MessageRequest request,User recipient){if(!same(request.getRecipient(),recipient))throw new SecurityException("Only the recipient may decline");request.decline();requests.saveAndFlush(request);}
 @Transactional public void cancel(MessageRequest request,User requester){if(!same(request.getRequester(),requester))throw new SecurityException("Only the requester may cancel");request.cancel();requests.saveAndFlush(request);}
 @Transactional public Conversation privateConversation(User a,User b,Project project){
  authorization.requireDifferentUsers(a,b);
  if(!authorized(a,b,project)) throw new SecurityException("Communication requires an accepted request or active project relationship");
  for(Conversation c:conversations.findAllByProjectAndType(project,ConversationType.PRIVATE))
   if(participants.findByConversation(c).stream().filter(ConversationParticipant::isActive).map(ConversationParticipant::getUser).map(User::getId).collect(java.util.stream.Collectors.toSet()).equals(java.util.Set.of(a.getId(),b.getId()))) return c;
  Conversation c=conversations.saveAndFlush(new Conversation(ConversationType.PRIVATE,project));join(c,a);join(c,b);return c;
 }
 @Transactional public Conversation synchronizeProjectTeam(ProjectTeam membership){
  Conversation c=conversations.findByProjectAndType(membership.getProject(),ConversationType.PROJECT_TEAM).orElseGet(()->conversations.saveAndFlush(new Conversation(ConversationType.PROJECT_TEAM,membership.getProject())));
  join(c,membership.getProject().getHomeowner().getUser());
  for(ProjectTeam team:teams.findByProject(membership.getProject())){
   ConversationParticipant p=participants.findByConversationAndUser(c,team.getTradesperson().getUser()).orElse(null);
   if(team.getStatus()==ProjectTeamStatus.ACTIVE){if(p==null)join(c,team.getTradesperson().getUser());else if(!p.isActive()){p.activate();participants.save(p);}}
   else if(p!=null&&p.isActive()){p.revoke();participants.save(p);}
  }
  return c;
 }
 @Transactional public Conversation updateMembership(ProjectTeam membership,ProjectTeamStatus status){
  membership.setStatus(status);teams.saveAndFlush(membership);return synchronizeProjectTeam(membership);
 }
 @Transactional public Message send(Conversation c,User sender,String body){requireAccess(c,sender);return messages.saveAndFlush(new Message(c,sender,body));}
 @Transactional public Message edit(Message m,User editor,String body){requireAccess(m.getConversation(),editor);if(!same(m.getSender(),editor))throw new SecurityException("Only the sender may edit a message");m.edit(body);return messages.saveAndFlush(m);}
 @Transactional public Message remove(Message m,User actor){requireAccess(m.getConversation(),actor);if(!same(m.getSender(),actor))throw new SecurityException("Only the sender may remove a message");m.remove();return messages.saveAndFlush(m);}
 @Transactional public MessageAttachment attach(Message m,User uploader,String filename,String contentType,long size,String key){requireAccess(m.getConversation(),uploader);if(!same(m.getSender(),uploader))throw new SecurityException("Attachments must be uploaded by the message sender");return attachments.saveAndFlush(new MessageAttachment(m,filename,contentType,size,key,uploader));}
 @Transactional(readOnly=true) public MessageAttachment attachment(Long id,User viewer){MessageAttachment a=attachments.findById(id).orElseThrow();requireAccess(a.getMessage().getConversation(),viewer);return a;}
 @Transactional(readOnly=true) public boolean canAccess(Conversation c,User user){return participants.existsByConversationAndUserAndActiveTrue(c,user);}
 private boolean authorized(User a,User b,Project p){if(!requests.between(a,b,p,MessageRequestStatus.ACCEPTED).isEmpty())return true;return projectAuthorized(a,p)&&projectAuthorized(b,p);}
 private boolean projectAuthorized(User u,Project p){if(same(p.getHomeowner().getUser(),u))return true;return teams.findByProject(p).stream().anyMatch(t->t.getStatus()==ProjectTeamStatus.ACTIVE&&same(t.getTradesperson().getUser(),u));}
 private void join(Conversation c,User u){ConversationParticipant p=participants.findByConversationAndUser(c,u).orElse(null);if(p==null)participants.saveAndFlush(new ConversationParticipant(c,u));else if(!p.isActive()){p.activate();participants.saveAndFlush(p);}}
 private void requireAccess(Conversation c,User u){if(!canAccess(c,u))throw new SecurityException("User cannot access this conversation");}
 private boolean same(User a,User b){return a==b||Objects.equals(a.getId(),b.getId());}
}
