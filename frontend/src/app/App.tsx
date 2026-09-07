import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router';
import { api } from '../api/client';
import type { HomeownerDashboard, TradespersonDashboard, Profile, Role } from '../api/types';
import { accountKey, profileKey, queryClient } from './query';

export function useRoleTheme(profile: Profile) {
  return { theme: profile.role.toLowerCase(), roleLabel: profile.role === 'HOMEOWNER' ? 'Homeowner' : 'Tradesperson' };
}

function Overview({ profile }: { profile: Profile }) {
  const summary = useQuery<HomeownerDashboard | TradespersonDashboard>({ queryKey: profileKey(profile, 'overview'), queryFn: ({ signal }) =>
    profile.role === 'HOMEOWNER' ? api.homeownerDashboard(signal) : api.tradespersonDashboard(signal) });
  return <section aria-labelledby="welcome-title">
    <p className="eyebrow">Craft × Tech × Imagination</p>
    <h1 id="welcome-title">Room for what you imagine.</h1>
    <p>From homes and barns to buses, tiny homes, RVs and container conversions. A place to bring thoughtful work together.</p>
    <div className="workspace-panel">
      <h2>Your workspace</h2>
      {summary.isPending ? <p role="status">Loading your workspace…</p> : summary.isError ?
        <div role="alert"><p>{summary.error.message}</p><button onClick={() => void summary.refetch()}>Try again</button></div> :
        <p role="status">Your {profile.role === 'HOMEOWNER' ? 'Homeowner' : 'Tradesperson'} workspace is connected. Overview tools are coming next.</p>}
    </div>
  </section>;
}

export function App() {
  const [switching, setSwitching] = useState(false);
  const account = useQuery({ queryKey: accountKey, queryFn: ({ signal }) => api.account(signal), enabled: !switching });
  const navigate = useNavigate();
  const changeProfile = useMutation({
    mutationFn: async (role: Role) => {
      setSwitching(true);
      await queryClient.cancelQueries();
      return api.switchProfile(role);
    },
    onSuccess: (next) => {
      queryClient.removeQueries({ queryKey: ['profile'] });
      queryClient.setQueryData(accountKey, next);
      navigate('/overview', { replace: true });
    },
    // A lost response can follow a successful server switch: re-bootstrap either way.
    onSettled: async () => {
      await queryClient.cancelQueries();
      queryClient.removeQueries({ queryKey: ['profile'] });
      // Explicit fetch also runs while the account observer is disabled for switching.
      try {
        await queryClient.fetchQuery({ queryKey: accountKey, queryFn: ({ signal }) => api.account(signal), staleTime: 0 });
      } finally {
        setSwitching(false);
      }
    },
  });
  if (account.isPending) return <main><p role="status">Opening Rock &amp; Hard Places…</p></main>;
  if (account.isError) return <main role="alert"><h1>We couldn’t open your workspace.</h1><p>{account.error.message}</p><button onClick={() => void account.refetch()}>Try again</button></main>;
  const { profile, profiles } = account.data;
  return <Shell profile={profile}>
    <header className="account-bar">
      <div><strong>{profile.displayName}</strong><span>{profile.role === 'HOMEOWNER' ? 'Homeowner' : 'Tradesperson'} mode</span></div>
      <label>Active profile<select value={profile.role} disabled={switching} onChange={event => changeProfile.mutate(event.target.value as Role)}>
        {profiles.map(p => <option key={`${p.role}-${p.id}`} value={p.role}>{p.role === 'HOMEOWNER' ? 'Homeowner' : 'Tradesperson'}</option>)}
      </select></label>
    </header>
    {changeProfile.isError && <p role="alert">Profile switch could not be confirmed. {changeProfile.error.message}</p>}
    <main id="main-content">
      {switching ? <p role="status">Switching profile…</p> : <Routes>
        <Route path="/" element={<Navigate to="/overview" replace />} />
        <Route path="/overview" element={<Overview key={`${profile.role}-${profile.id}`} profile={profile} />} />
        <Route path="*" element={<section><h1>Page not found</h1><Link to="/overview">Return to your workspace</Link></section>} />
      </Routes>}
    </main>
  </Shell>;
}

function Shell({ profile, children }: { profile: Profile; children: React.ReactNode }) {
  const { theme, roleLabel } = useRoleTheme(profile);
  return <div className="app-shell" data-role={theme}>
    <a className="skip-link" href="#main-content">Skip to workspace</a>
    <aside className="sidebar"><Link className="wordmark" to="/overview">Rock &amp;<br />Hard Places</Link>
      <p className="shell-role">{roleLabel} workspace</p><nav aria-label="Main navigation"><Link to="/overview" aria-current="page">Overview</Link></nav>
      <p className="shell-note">Built on trust.<br />Backed by skill.</p>
    </aside><div className="workspace">{children}</div>
  </div>;
}
