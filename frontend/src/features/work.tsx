import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link, useParams } from "react-router";
import { api } from "../api/client";
import type { Profile } from "../api/types";
import { refreshProfileData } from "../app/query";
import {
  Empty,
  Header,
  MutationNotice,
  State,
  useData,
} from "../components/ui";
import { BidCard } from "./projects";
export function Opportunities({ profile }: { profile: Profile }) {
  const [zip, setZip] = useState("");
  const [trade, setTrade] = useState("");
  const catalog = useData(profile, "catalog", api.trades);
  const query = useData(profile, `opportunities-${zip}-${trade}`, (signal) =>
    api.opportunities(
      { jobZip: zip, tradeId: trade ? Number(trade) : undefined },
      signal,
    ),
  );
  return (
    <>
      <Header eyebrow="Find work" title="Good work starts with clear scope.">
        Explore open requirements that match your recorded trades.
      </Header>
      <div className="filters">
        <label>
          Project ZIP
          <input
            value={zip}
            onChange={(e) => setZip(e.target.value)}
            placeholder="Any ZIP"
          />
        </label>
        <label>
          Required trade
          <select value={trade} onChange={(e) => setTrade(e.target.value)}>
            <option value="">All matching trades</option>
            {catalog.data?.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      <State query={query}>
        <div className="opportunity-grid">
          {query.data?.map((o) => (
            <article className="opportunity-card" key={o.requiredTrade.id}>
              <p className="eyebrow">
                {o.requiredTrade.tradeName} · ZIP {o.project.jobZip}
              </p>
              <h2>{o.task.title}</h2>
              <p>{o.project.title}</p>
              <p>{o.task.description}</p>
              <p>Homeowner: {o.homeownerDisplayName}</p>
              <Link
                className="button"
                to={`/opportunities/${o.requiredTrade.id}`}
              >
                Explore this scope →
              </Link>
            </article>
          ))}
        </div>
        {query.data?.length === 0 && (
          <Empty title="No matching open scopes right now.">
            Try another ZIP or check back as homeowners plan new work.
          </Empty>
        )}
      </State>
    </>
  );
}
export function OpportunityDetail({ profile }: { profile: Profile }) {
  const { requirementId } = useParams();
  const id = Number(requirementId);
  const query = useData(profile, `opportunity-${id}`, (signal) =>
    api.opportunity(id, signal),
  );
  const mutation = useMutation({
    mutationFn: (form: FormData) =>
      api.submitBid(query.data!.task.id, {
        taskTradeId: query.data!.requiredTrade.id,
        amount: Number(form.get("amount")),
        message: String(form.get("message")),
      }),
    onSuccess: refreshProfileData,
  });
  return (
    <>
      <Link to="/opportunities">← Back to opportunities</Link>
      <State query={query}>
        {query.data && (
          <>
            <Header
              eyebrow={`${query.data.requiredTrade.tradeName} · ZIP ${query.data.project.jobZip}`}
              title={query.data.task.title}
            >
              {query.data.project.title}
            </Header>
            <div className="split">
              <section className="panel">
                <h2>The scope</h2>
                <p>{query.data.task.description}</p>
                <h3>The bigger vision</h3>
                <p>{query.data.project.description}</p>
                <p>Homeowner: {query.data.homeownerDisplayName}</p>
                <p>
                  This proposal covers the named task and required trade.
                  Project team conversations become available only through
                  permitted membership.
                </p>
              </section>
              <section className="panel">
                <h2>Put your skills forward</h2>
                {query.data.bidding.allowed ? (
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      mutation.mutate(new FormData(e.currentTarget));
                    }}
                  >
                    <label>
                      Proposal amount (USD)
                      <input
                        name="amount"
                        type="number"
                        min="0.01"
                        step="0.01"
                        required
                      />
                    </label>
                    <label>
                      Your proposal
                      <textarea
                        name="message"
                        rows={5}
                        required
                        placeholder="Explain your approach to this scope."
                      />
                    </label>
                    <button disabled={mutation.isPending || mutation.isSuccess}>
                      {mutation.isPending
                        ? "Submitting…"
                        : mutation.isSuccess
                          ? "Proposal submitted"
                          : "Submit task-trade bid"}
                    </button>
                    <MutationNotice mutation={mutation} />
                  </form>
                ) : (
                  <Empty title="Bidding is unavailable">
                    {query.data.bidding.reason}
                  </Empty>
                )}
              </section>
            </div>
            <h2>Your proposals for this scope</h2>
            {query.data.ownBids.map((bid) => (
              <BidCard
                key={bid.id}
                bid={bid}
                task={query.data!.task}
                profile={profile}
              />
            ))}
          </>
        )}
      </State>
    </>
  );
}
export function MyBids({ profile }: { profile: Profile }) {
  const query = useData(profile, "overview", api.tradespersonDashboard);
  return (
    <>
      <Header eyebrow="Your proposals" title="My bids">
        Track your submitted proposals, each tied to a specific piece of work.
      </Header>
      <State query={query}>
        {query.data?.activeBids.map((item) => (
          <BidCard
            key={item.bid.id}
            profile={profile}
            bid={item.bid}
            task={item.task}
          />
        ))}
        {query.data?.activeBids.length === 0 && (
          <Empty title="No submitted bids.">
            <Link to="/opportunities">Find your next opportunity →</Link>
          </Empty>
        )}
      </State>
      <p className="footnote">
        Your submitted bids appear here. To see accepted, rejected, or withdrawn
        proposals, open the bids for the project task.
      </p>
    </>
  );
}
