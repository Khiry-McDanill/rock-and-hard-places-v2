import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { workspaceApi } from "../api/workspace";
import type { Profile } from "../api/types";
import { refreshProfileData } from "../app/query";
import {
  Empty,
  Header,
  MutationNotice,
  State,
  useData,
} from "../components/ui";
export function Messages({
  profile,
  userId,
  projectId,
}: {
  profile: Profile;
  userId: number;
  projectId?: number;
}) {
  const projects = useData(profile, "projects", workspaceApi.projects);
  const [selected, setSelected] = useState("");
  const id =
    projectId ?? (selected ? Number(selected) : projects.data?.[0]?.id);
  return (
    <>
      <Header eyebrow="Keep the work connected" title="Messages" />
      <State query={projects}>
        {!projectId && (
          <label className="project-select">
            Project conversation
            <select
              value={id ?? ""}
              onChange={(e) => setSelected(e.target.value)}
            >
              {projects.data?.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.title}
                </option>
              ))}
            </select>
          </label>
        )}
        {id ? (
          <ProjectConversations
            key={id}
            profile={profile}
            userId={userId}
            projectId={id}
            projectTitle={projects.data?.find((project) => project.id === id)?.title}
          />
        ) : (
          <Empty title="No project conversations yet.">
            Conversations appear here for projects you can access.
          </Empty>
        )}
      </State>
    </>
  );
}
function ProjectConversations({
  profile,
  userId,
  projectId,
  projectTitle,
}: {
  profile: Profile;
  userId: number;
  projectId: number;
  projectTitle?: string;
}) {
  const query = useData(profile, `conversations-${projectId}`, (signal) =>
    workspaceApi.conversations(projectId, signal),
  );
  const [selected, setSelected] = useState<number | null>(null);
  return (
    <State query={query}>
      <div className={`messages-layout ${selected ? "thread-open" : ""}`}>
        <nav className="conversation-list" aria-label="Project conversations">
          {query.data?.map((conversation) => (
            <button
              className={
                selected === conversation.id ? "selected" : "secondary"
              }
              key={conversation.id}
              onClick={() => setSelected(conversation.id)}
            >
              <strong>
                {conversation.type === "PROJECT_TEAM"
                  ? projectTitle?.trim()
                    ? `${projectTitle.trim()} Project Team`
                    : "Project Team"
                  : "Direct conversation"}
              </strong>
              <small>
                Opened {new Date(conversation.createdAt).toLocaleDateString()}
              </small>
            </button>
          ))}
        </nav>
        {selected ? (
          <Thread
            key={selected}
            profile={profile}
            userId={userId}
            id={selected}
            back={() => setSelected(null)}
          />
        ) : (
          <Empty
            title={
              query.data?.length
                ? "Choose a conversation."
                : "No accessible conversations."
            }
          >
            Keep project questions, decisions, and updates in one place.
          </Empty>
        )}
      </div>
    </State>
  );
}
function Thread({
  profile,
  userId,
  id,
  back,
}: {
  profile: Profile;
  userId: number;
  id: number;
  back: () => void;
}) {
  const query = useData(profile, `messages-${id}`, (signal) =>
    workspaceApi.messages(id, signal),
  );
  const [body, setBody] = useState("");
  const mutation = useMutation({
    mutationFn: () => workspaceApi.send(id, body),
    onSuccess: async () => {
      setBody("");
      await refreshProfileData();
    },
  });
  return (
    <section className="thread">
      <div className="section-heading">
        <button className="secondary" onClick={back}>
          ← Conversations
        </button>
        <button className="secondary" onClick={() => void query.refetch()}>
          Refresh messages
        </button>
      </div>
      <State query={query}>
        <div className="message-stream">
          {query.data?.map((message) => (
            <article
              key={message.id}
              className={`message ${message.senderId === userId ? "mine" : ""}`}
            >
              <small>
                {message.senderId === userId
                  ? "You"
                  : `Participant ${message.senderId}`}{" "}
                · {new Date(message.createdAt).toLocaleString()}
              </small>
              <p>
                {message.removed ? "This message was removed." : message.body}
              </p>
            </article>
          ))}
          {query.data?.length === 0 && <p>No messages yet.</p>}
        </div>
      </State>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          mutation.mutate();
        }}
      >
        <label>
          Message
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            required
            rows={3}
          />
        </label>
        <button disabled={mutation.isPending || !body.trim()}>
          {mutation.isPending ? "Sending…" : "Send message"}
        </button>
        <MutationNotice mutation={mutation} />
      </form>
    </section>
  );
}
