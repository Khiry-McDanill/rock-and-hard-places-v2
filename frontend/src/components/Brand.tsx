/** Shared vector rendering of the roof/mountain lockup on the approved RH&P board. */
export function Brand() {
  return (
    <span className="brand-lockup">
      <svg
        className="brand-mark"
        viewBox="0 0 124 80"
        fill="none"
        aria-hidden="true"
      >
        <path d="M2 53 33 20l12 13-12 13L2 78Z" fill="var(--brand-stone, #EEE8DC)" />
        <path d="m33 20 12 13-12 13Z" fill="var(--brand-stone-facet, #D4D0C5)" />
        <path d="M33 39 66 2l28 31-4 22-24-29-27 29-6-6Z" fill="#C56A3D" />
        <path d="m66 2 28 31-4 22-24-29Z" fill="#AB6038" />
        <path d="m94 23 28 31v24L90 45l-1-16Z" fill="#C56A3D" />
        <path d="m94 23 28 31v24L94 48Z" fill="#AD623A" />
        <path d="m42 72 24-25 23 25H70l-4-6-5 6Z" fill="#556B4F" />
        <path d="m66 47 23 25H70l-4-6Z" fill="#DAD8C8" />
      </svg>
      <span className="brand-name">
        <span className="brand-name-top">ROCK &amp;</span>
        <span className="brand-name-bottom">HARD PLACES</span>
      </span>
    </span>
  );
}
