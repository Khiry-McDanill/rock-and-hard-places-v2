import { useQueries } from "@tanstack/react-query";
import { api } from "../../../api/client";
import { profileKey } from "../../../app/query";
import type { Profile, Task } from "../../../api/types";
export function useProjectAssignments(profile: Profile, tasks: Task[]) {
  const queries = useQueries({
    queries: tasks.map((task) => ({
      queryKey: profileKey(profile, `assignments-${task.id}`),
      queryFn: ({ signal }: { signal: AbortSignal }) =>
        api.taskAssignments(task.id, signal),
    })),
  });
  return {
    data: queries.flatMap((query, index) =>
      (query.data ?? []).map((assignment) => ({
        assignment,
        task: tasks[index],
      })),
    ),
    isPending: queries.some((query) => query.isPending),
    isError: queries.some((query) => query.isError),
    error: queries.find((query) => query.error)?.error ?? null,
    refetch: () => Promise.all(queries.map((query) => query.refetch())),
  };
}
