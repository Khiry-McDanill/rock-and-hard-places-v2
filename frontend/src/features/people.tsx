import { useState } from "react";
import { PortfolioCard } from "../components/PortfolioCard";
import "../styles/portfolio.css";
import { Link, useParams } from "react-router";
import { api } from "../api/client";
import { workspaceApi } from "../api/workspace";
import type { Person, Profile } from "../api/types";
import {
  Empty,
  Header,
  Portrait,
  State,
  useData,
  words,
} from "../components/ui";
export function People({ profile }: { profile: Profile }) {
  const [q, setQ] = useState("");
  const [trade, setTrade] = useState("");
  const [selected, setSelected] = useState<number[]>([]);
  const catalog = useData(profile, "catalog", api.trades);
  const people = useData(profile, `people-${q}-${trade}`, (signal) =>
    api.people({ q, tradeId: trade ? Number(trade) : undefined }, signal),
  );
  const all = useData(profile, "people", (signal) => api.people({}, signal));
  const comparison =
    all.data?.filter((p) => selected.includes(p.profile.id)) ?? [];
  return (
    <>
      <Header
        eyebrow="People who care about the work"
        title="Find your kind of craft."
      >
        Discover qualified trades and individual specialties. Find the people
        who can help make your idea real.
      </Header>
      <div className="filters">
        <label>
          Search by name
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Find a person"
          />
        </label>
        <label>
          Qualified trade
          <select value={trade} onChange={(e) => setTrade(e.target.value)}>
            <option value="">All trades</option>
            {catalog.data?.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      {selected.length > 0 && (
        <a className="comparison-jump button secondary" href="#comparison">
          Compare selected people ({selected.length}/2) ↓
        </a>
      )}
      <State query={people}>
        <div className="people-grid">
          {people.data?.map((person) => (
            <article className="person-card" key={person.profile.id}>
              <PersonFacts person={person} />
              <Link className="text-link" to={`/people/${person.profile.id}`}>
                View profile & portfolio →
              </Link>
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={selected.includes(person.profile.id)}
                  disabled={
                    selected.length >= 2 &&
                    !selected.includes(person.profile.id)
                  }
                  onChange={(e) =>
                    setSelected(
                      e.target.checked
                        ? [...selected, person.profile.id]
                        : selected.filter((id) => id !== person.profile.id),
                    )
                  }
                />
                Compare this person
              </label>
            </article>
          ))}
        </div>
        {people.data?.length === 0 && (
          <Empty title="No people match these filters.">
            Try another trade or a shorter name.
          </Empty>
        )}
      </State>
      {selected.length > 0 && (
        <section className="comparison" id="comparison" tabIndex={-1}>
          <div className="section-heading">
            <h2>Compare your people</h2>
            <button className="secondary" onClick={() => setSelected([])}>
              Clear comparison
            </button>
          </div>
          <p>
            Select two people to compare their qualifications, specialties, and
            availability.
          </p>
          <div className="comparison-grid">
            {comparison.map((person) => (
              <div className="panel" key={person.profile.id}>
                <PersonFacts person={person} />
                <Link to={`/people/${person.profile.id}`}>Full profile →</Link>
              </div>
            ))}
          </div>
        </section>
      )}
    </>
  );
}
function PersonFacts({ person }: { person: Person }) {
  return (
    <>
      <Portrait name={person.profile.displayName} reference={person.profile.profileImageReference} />
      <h2>{person.profile.displayName}</h2>
      <dl>
        <dt>Qualified trades</dt>
        <dd>
          {person.qualifications.map((t) => t.name).join(", ") ||
            "None recorded"}
        </dd>
        <dt>Specialties</dt>
        <dd>
          {person.specialties.map((t) => t.name).join(", ") || "None recorded"}
        </dd>
        <dt>Availability</dt>
        <dd>
          {person.profile.availabilityStatus
            ? words(person.profile.availabilityStatus)
            : "Not provided"}
        </dd>
        <dt>Service area</dt>
        <dd>
          Base ZIP {person.profile.baseZip || "not provided"}
        </dd>
      </dl>
    </>
  );
}
export function PersonProfile({
  profile,
  own = false,
}: {
  profile: Profile;
  own?: boolean;
}) {
  const { personId } = useParams();
  const id = own ? profile.id : Number(personId);
  const person = useData(profile, `person-${id}`, (signal) =>
    own ? Promise.resolve(profile) : workspaceApi.person(id, signal),
  );
  const people = useData(profile, "people", (signal) =>
    profile.role === "HOMEOWNER" && !own
      ? api.people({}, signal)
      : Promise.resolve([] as Person[]),
  );
  const facts = people.data?.find((p) => p.profile.id === id);
  return (
    <State query={person}>
      {person.data && (
        <>
          <Header
            eyebrow={own ? "My profile" : "Meet the craftsperson"}
            title={person.data.displayName}
          />
          <section className="profile-intro">
            <Portrait name={person.data.displayName} reference={person.data.profileImageReference} />
            <div>
              <p>
                {words(person.data.role)} · Account{" "}
                {words(person.data.accountStatus)}
              </p>
              {person.data.verificationStatus && (
                <p>
                  Account verification: {words(person.data.verificationStatus)}
                  <br />
                  <small>
                    This is not verification of licenses or insurance.
                  </small>
                </p>
              )}
            </div>
          </section>
          {facts ? (
            <section className="panel">
              <PersonFacts person={facts} />
            </section>
          ) : (
            person.data.role === "TRADESPERSON" && (
              <section className="panel">
                <h2>Availability & service area</h2>
                <p>
                  {person.data.availabilityStatus
                    ? words(person.data.availabilityStatus)
                    : "Availability not provided"}{" "}
                  · Base ZIP {person.data.baseZip || "not provided"}
                </p>
              </section>
            )
          )}
          {person.data.role === "TRADESPERSON" && (
            <Portfolio profile={profile} id={id} name={person.data.displayName} />
          )}
        </>
      )}
    </State>
  );
}
function Portfolio({ profile, id, name }: { profile: Profile; id: number; name: string }) {
  const query = useData(profile, `portfolio-${id}`, (signal) =>
    workspaceApi.portfolio(id, signal),
  );
  return (
    <section>
      <h2>Work with a story</h2>
      <State query={query}>
        <section className="portfolio-section" aria-label="RH&P Projects">
          <h3>RH&P Projects</h3>
          <p>Work completed through Rock &amp; Hard Places.</p>
          <div className="portfolio-grid">
            {query.data?.filter((item) => item.provenance === "RHP_VERIFIED").map((item) => (
              <PortfolioCard key={item.id} item={item} name={name} />
            ))}
          </div>
          {query.data && !query.data.some((item) => item.provenance === "RHP_VERIFIED") &&
            <p>No published RH&amp;P projects yet.</p>}
        </section>
        {query.data?.some((item) => item.provenance !== "RHP_VERIFIED") && (
          <section className="portfolio-section" aria-label="External Portfolio">
            <h3>External Portfolio</h3>
            <p>Work performed outside Rock &amp; Hard Places.</p>
            <div className="portfolio-grid">
              {query.data.filter((item) => item.provenance !== "RHP_VERIFIED").map((item) => (
                <PortfolioCard key={item.id} item={item} name={name} />
              ))}
            </div>
          </section>
        )}
        {query.data?.length === 0 && (
          <Empty title="No published portfolio work yet.">
            Published work will appear here with its provenance.
          </Empty>
        )}
      </State>
    </section>
  );
}
