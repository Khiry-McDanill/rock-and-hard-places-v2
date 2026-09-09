import { Link, NavLink } from "react-router";
import type { Profile } from "../api/types";
import { Brand } from "./Brand";
import { Portrait } from "./ui";

export function AppShell({
  profile,
  children,
}: {
  profile: Profile;
  children: React.ReactNode;
}) {
  const theme = profile.role.toLowerCase();
  const roleLabel = profile.role === "HOMEOWNER" ? "Homeowner" : "Tradesperson";
  return (
    <div className="app-shell" data-role={theme}>
      <a className="skip-link" href="#main-content">
        Skip to workspace
      </a>
      <Sidebar profile={profile} roleLabel={roleLabel} />
      <div className="workspace">
        <Link className="mobile-brand" to="/" aria-label="Rock & Hard Places home">
          <Brand />
        </Link>
        {children}
      </div>
      <MobileNav profile={profile} />
    </div>
  );
}

function Navigation({
  profile,
  mobile = false,
}: {
  profile: Profile;
  mobile?: boolean;
}) {
  const home = profile.role === "HOMEOWNER";
  const items = home
    ? [
        ["/overview", "Dashboard"],
        ["/projects", mobile ? "Projects" : "My projects"],
        ["/people", mobile ? "Find people" : "Find tradespeople"],
        ["/messages", "Messages"],
        ["/profile", mobile ? "Profile" : "My profile"],
      ]
    : [
        ["/overview", "Dashboard"],
        ["/work", "My work"],
        ["/opportunities", "Find work"],
        ...(!mobile ? [["/bids", "My bids"]] : []),
        ["/messages", "Messages"],
        ["/profile", mobile ? "Profile" : "My profile"],
      ];
  return (
    <nav
      className={mobile ? "mobile-nav" : "main-nav"}
      aria-label={mobile ? "Mobile navigation" : "Main navigation"}
    >
      {items.map(([path, label], index) => (
        <NavLink key={path} to={path}>
          <NavIcon index={index} />
          {label}
        </NavLink>
      ))}
    </nav>
  );
}

function Sidebar({
  profile,
  roleLabel,
}: {
  profile: Profile;
  roleLabel: string;
}) {
  return (
    <aside className="sidebar">
      <Link
        className="wordmark"
        to="/"
        aria-label="Rock & Hard Places home"
      >
        <Brand />
      </Link>
      <p className="shell-role">{roleLabel}</p>
      <Navigation profile={profile} />
      <p className="shell-note">
        Built on trust.
        <br />
        Backed by skill.
      </p>
      <div className="sidebar-account">
        <Portrait name={profile.displayName} reference={profile.profileImageReference} />
        <div>
          <strong>{profile.displayName}</strong>
          <small>{roleLabel}</small>
        </div>
        <button
          className="sidebar-switch"
          onClick={() => document.getElementById("profile-switch")?.focus()}
        >
          Switch profile ↗
        </button>
      </div>
    </aside>
  );
}
function MobileNav({ profile }: { profile: Profile }) {
  return <Navigation profile={profile} mobile />;
}
function NavIcon({ index }: { index: number }) {
  const paths = [
    "M3 11 12 3l9 8M5 10v11h5v-7h4v7h5V10",
    "M3 6h7l2 3h9v12H3z",
    "M16 21v-3a4 4 0 0 0-8 0v3M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M20 12v6M17 15h6",
    "M3 4h18v13H9l-6 4zM7 8h10M7 12h7",
    "M4 21a8 8 0 0 1 16 0M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8",
  ];
  return (
    <svg
      className="nav-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={paths[index % paths.length]} />
    </svg>
  );
}
