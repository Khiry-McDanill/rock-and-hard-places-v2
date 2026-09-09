import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link, useParams } from "react-router";
import { api, request } from "../api/client";
import type { Profile } from "../api/types";
import { refreshProfileData } from "../app/query";
import {
  Empty,
  words,
  Header,
  MutationNotice,
  State,
  useData,
} from "../components/ui";
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
        Find open project scopes matching your qualified trades.
      </Header>
      <div className="filters">
        <label>
          Work ZIP
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
            Projects without an open trade requirement won’t appear. Try another ZIP or check back for new work.
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
                {query.data.ownBids.length > 0 ? (
                  <p>Your proposal is shown below. Its status determines the available actions.</p>
                ) : query.data.bidding.allowed ? (
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
              <ProposalCard key={bid.id} item={{bid, task: query.data!.task, project: query.data!.project}} />
            ))}
          </>
        )}
      </State>
    </>
  );
}
export function MyBids({ profile }: { profile: Profile }) {
  const query = useData(profile, "my-bids", () => request<ProposalSummary[]>("/bids"));
  return (
    <>
      <Header eyebrow="Your proposals" title="My bids">
        View your proposals and their history. Edit or withdraw a submitted proposal before a decision.
      </Header>
      <State query={query}>
        {query.data?.map((item) => (
          <ProposalCard key={item.bid.id} item={item} />
        ))}
        {query.data?.length === 0 && (
          <Empty title="No proposals yet.">
            <Link to="/opportunities">Find your next opportunity →</Link>
          </Empty>
        )}
      </State>

    </>
  );
}

type ProposalSummary = {bid: {id:number;amount:number;message:string|null;status:string;tradespersonId:number};task:{title:string}|null;project:{title:string}|null};
export function ProposalCard({item}: {item:ProposalSummary}) {
 const {bid}=item;const [view,setView]=useState(false);const [editing,setEditing]=useState(false);
 const mutation=useMutation({mutationFn: async (action:{withdraw?:boolean;form?:FormData}) => request(`/bids/${bid.id}${action.withdraw?'/withdraw':''}`,{method:action.withdraw?'POST':'PUT',...(action.form?{body:JSON.stringify({amount:Number(action.form.get('amount')),message:String(action.form.get('message'))})}:{})}),onSuccess:async()=>{setEditing(false);await refreshProfileData();}});
 return <article className="panel proposal-card"><h2>{item.task?.title || "Proposal history"}</h2><p>{item.project?.title || "Scope details are no longer available."}</p><p>{new Intl.NumberFormat("en-US",{style:"currency",currency:"USD"}).format(bid.amount)} · {words(bid.status)}</p>
 <div className="actions"><button className="secondary" aria-expanded={view} onClick={()=>setView(!view)}>{view?'Close proposal':'View proposal'}</button>
 {bid.status==='SUBMITTED' && <><button className="secondary" disabled={mutation.isPending} onClick={()=>setEditing(!editing)}>Edit proposal</button><button className="secondary" disabled={mutation.isPending} onClick={()=>{if(window.confirm('Withdraw this proposal? It will remain in your history.'))mutation.mutate({withdraw:true});}}>Withdraw proposal</button></>}</div>
 {view && <p className="proposal-message">{bid.message || 'No proposal message provided.'}</p>}
 {editing && bid.status==='SUBMITTED' && <form className="profile-editor" onSubmit={e=>{e.preventDefault();mutation.mutate({form:new FormData(e.currentTarget)});}}><label>Proposal amount (USD)<input name="amount" type="number" min="0.01" step="0.01" max="9999999999.99" defaultValue={bid.amount} required /></label><label>Your proposal<textarea name="message" defaultValue={bid.message || ''} maxLength={2000}/></label><button disabled={mutation.isPending}>Save proposal</button><button className="secondary" type="button" onClick={()=>setEditing(false)}>Cancel</button></form>}
 {mutation.isError && <p role="alert">Your proposal could not be changed. It may already have a decision. Refresh and try again.</p>}
 </article>;
}
