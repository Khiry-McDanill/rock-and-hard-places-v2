import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import { workspaceApi } from "../api/workspace";
import type { Profile, Task } from "../api/types";
import { refreshProfileData } from "../app/query";
import { MutationNotice, State, useData } from "../components/ui";
export function TaskReview({
  profile,
  task,
}: {
  profile: Profile;
  task: Task;
}) {
  const assignments = useData(profile, `assignments-${task.id}`, (signal) =>
    api.taskAssignments(task.id, signal),
  );
  const reviews = useData(profile, `reviews-${task.id}`, (signal) => workspaceApi.taskReviews(task.id, signal));
  const eligible = assignments.data?.filter(a => !reviews.data?.some(r => r.tradespersonId === a.tradespersonId)) ?? [];
  const mutation = useMutation({
    mutationFn: (form: FormData) =>
      workspaceApi.createReview({
        tradespersonId: Number(form.get("person")),
        projectId: task.projectId,
        taskId: task.id,
        level: "TASK",
        overallRating: Number(form.get("rating")),
        body: String(form.get("body")),
      }),
    onSuccess: refreshProfileData,
  });
  return (
    <State query={assignments}><State query={reviews}>
      {!!assignments.data?.length && (
        <details>
          <summary>Review completed task work</summary>
          <p>
            Share your experience with this task and the person who performed it.
          </p>
          {reviews.data?.map(review => <article key={review.id}>
            <h3>{review.withdrawn ? "Withdrawn review" : "Published review"}</h3>
            <p>{review.authorDisplayName} reviewed {review.tradespersonDisplayName}</p>
            <p>{review.overallRating} / 5 · {review.body}</p>
            <p>Published <time dateTime={review.createdAt}>{new Date(review.createdAt).toLocaleDateString()}</time></p>
          </article>)}
          {mutation.isSuccess && !reviews.data?.some(r => r.id === mutation.data.id) ? (
            <div role="status">
              <h3>Your review was saved.</h3>
              <p>
                {mutation.data.overallRating} / 5 · {mutation.data.body}
              </p>
            </div>
          ) : eligible.length > 0 && reviews.data ? (
            <form
              onSubmit={(event) => {
                event.preventDefault();
                mutation.mutate(new FormData(event.currentTarget));
              }}
            >
              <label>
                Tradesperson
                <select name="person" required>
                  {eligible.map((a) => (
                    <option key={a.id} value={a.tradespersonId}>
                      {a.displayName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Overall rating
                <select name="rating" required defaultValue="">
                  <option value="" disabled>
                    Choose a rating
                  </option>
                  {[1, 2, 3, 4, 5].map((rating) => (
                    <option key={rating} value={rating}>
                      {rating} out of 5
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Your experience
                <textarea name="body" required rows={4} />
              </label>
              <button disabled={mutation.isPending}>
                {mutation.isPending ? "Saving review…" : "Publish task review"}
              </button>
              <MutationNotice mutation={mutation} />
            </form>
          ) : null}
        </details>
      )}
    </State></State>
  );
}
