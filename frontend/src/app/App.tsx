import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Link, Navigate, Route, Routes, useNavigate } from "react-router";
import { api } from "../api/client";
import type { Profile, Role } from "../api/types";
import { Overview, Projects } from "../features/overview";
import { ProjectForm, ProjectWorkspace } from "../features/projects";
import { People, PersonProfile } from "../features/people";
import { Opportunities, OpportunityDetail, MyBids } from "../features/work";
import { Messages } from "../features/messages";
import { Portrait, Empty, productError } from "../components/ui";
import { AppShell } from "../components/AppShell";
import { InspirationPage } from "../features/public/InspirationPage";
import { Homepage } from "../features/public/Homepage";
import { accountKey, queryClient } from "./query";

export function useRoleTheme(profile: Profile) {
  return {
    theme: profile.role.toLowerCase(),
    roleLabel: profile.role === "HOMEOWNER" ? "Homeowner" : "Tradesperson",
  };
}

export function App() {
  return <Routes><Route path="/" element={<Homepage />} /><Route path="/ideas/:slug" element={<InspirationPage />} /><Route path="/ideas/*" element={<InspirationPage />} /><Route path="*" element={<WorkspaceApp />} /></Routes>;
}

function WorkspaceApp() {
  const [switching, setSwitching] = useState(false);
  const account = useQuery({
    queryKey: accountKey,
    queryFn: ({ signal }) => api.account(signal),
    enabled: !switching,
  });
  const navigate = useNavigate();
  const changeProfile = useMutation({
    mutationKey: ["switch"],
    mutationFn: async (role: Role) => {
      setSwitching(true);
      await queryClient.cancelQueries();
      return api.switchProfile(role);
    },
    onSuccess: (next) => {
      queryClient.removeQueries({ queryKey: ["profile"] });
      queryClient.setQueryData(accountKey, next);
      navigate(window.location.pathname === "/projects/new" && next.profile.role === "HOMEOWNER" ? "/projects/new" : window.location.pathname === "/opportunities" && next.profile.role === "TRADESPERSON" ? "/opportunities" : "/overview", { replace: true });
    },
    // A lost response can follow a successful server switch: re-bootstrap either way.
    onSettled: async () => {
      await queryClient.cancelQueries();
      queryClient.removeQueries({ queryKey: ["profile"] });
      // Explicit fetch also runs while the account observer is disabled for switching.
      try {
        await queryClient.fetchQuery({
          queryKey: accountKey,
          queryFn: ({ signal }) => api.account(signal),
          staleTime: 0,
        });
      } finally {
        setSwitching(false);
      }
    },
  });
  if (account.isPending)
    return (
      <main>
        <p role="status">Opening Rock &amp; Hard Places…</p>
      </main>
    );
  if (account.isError)
    return (
      <main role="alert">
        <h1>We couldn’t open your workspace.</h1>
        <p>{productError(account.error)}</p>
        <button onClick={() => void account.refetch()}>Try again</button>
      </main>
    );
  const { profile, profiles } = account.data;
  return (
    <AppShell profile={profile}>
      <header className="account-bar">
        <div className="account-identity">
          <Portrait name={profile.displayName} reference={profile.profileImageReference} />
          <div>
            <strong>{profile.displayName}</strong>
            <span>
              {profile.role === "HOMEOWNER" ? "Homeowner" : "Tradesperson"}
            </span>
          </div>
        </div>
        <label className="profile-switch-control">
          Switch profile
          <select
            id="profile-switch"
            value={profile.role}
            disabled={switching}
            onChange={(event) => {
              if (queryClient.isMutating() > 0) {
                window.alert(
                  "Wait for the current save to finish before switching profiles.",
                );
                return;
              }
              if (
                document.querySelector("form") &&
                !window.confirm(
                  "Switch profiles and leave any unsaved form changes?",
                )
              )
                return;
              changeProfile.mutate(event.target.value as Role);
            }}
          >
            {profiles.map((p) => (
              <option key={`${p.role}-${p.id}`} value={p.role}>
                {p.role === "HOMEOWNER" ? "Homeowner" : "Tradesperson"}
              </option>
            ))}
          </select>
        </label>
      </header>
      {changeProfile.isError && (
        <p role="alert">
          Profile switch could not be confirmed. {productError(changeProfile.error)}
        </p>
      )}
      <main id="main-content">
        {switching ? (
          <p role="status">Switching profile…</p>
        ) : profile.accountStatus !== "ACTIVE" ? (
          <Empty title={`Account ${profile.accountStatus.toLowerCase()}`}>
            This profile cannot access active work. You can switch to another
            available profile.
          </Empty>
        ) : (
          <Routes key={`${profile.role}-${profile.id}`}>
            <Route path="/" element={<Navigate to="/overview" replace />} />
            {profile.role !== "HOMEOWNER" && <Route path="/projects/new" element={<Empty title="Start with your homeowner profile">Use Switch profile above to choose Homeowner and start your project.</Empty>} />}
            {profile.role !== "TRADESPERSON" && <Route path="/opportunities" element={<Empty title="Find work with your tradesperson profile">Use Switch profile above to choose Tradesperson and explore opportunities.</Empty>} />}
            <Route
              path="/overview"
              element={
                <Overview
                  key={`${profile.role}-${profile.id}`}
                  profile={profile}
                />
              }
            />
            <Route path="/people/:personId" element={<PersonProfile profile={profile} />} />
            {profile.role === "HOMEOWNER" ? (
              <>
                <Route
                  path="/projects"
                  element={<Projects profile={profile} />}
                />
                <Route
                  path="/projects/new"
                  element={<ProjectForm profile={profile} />}
                />
                <Route path="/people" element={<People profile={profile} />} />

              </>
            ) : (
              <>
                <Route path="/work" element={<Projects profile={profile} />} />
                <Route
                  path="/opportunities"
                  element={<Opportunities profile={profile} />}
                />
                <Route
                  path="/opportunities/:requirementId"
                  element={<OpportunityDetail profile={profile} />}
                />
                <Route path="/bids" element={<MyBids profile={profile} />} />
              </>
            )}
            <Route
              path="/projects/:projectId/:section?"
              element={
                <ProjectWorkspace
                  profile={profile}
                  userId={account.data.userId}
                />
              }
            />
            <Route
              path="/messages"
              element={
                <Messages profile={profile} userId={account.data.userId} />
              }
            />
            <Route
              path="/profile"
              element={<PersonProfile profile={profile} own />}
            />
            <Route
              path="*"
              element={
                <section>
                  <h1>Page not found</h1>
                  <Link to="/overview">Return to your workspace</Link>
                </section>
              }
            />
          </Routes>
        )}
      </main>
    </AppShell>
  );
}
