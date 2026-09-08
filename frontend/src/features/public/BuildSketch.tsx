/** Shared architectural sketches for public inspiration. */
export function BuildSketch({ kind }: { kind: string }) {
  return <svg className="build-sketch" viewBox="0 0 320 230" fill="none" aria-hidden="true">
    <circle cx="256" cy="48" r="25" fill="#C56A3D" opacity=".65" />
    <path d="M0 191 66 157l57 22 80-34 117 46v39H0Z" fill="#5B6B4F" opacity=".35" />
    <g stroke="#F5EFE6" strokeWidth="2" strokeLinejoin="round">
      {kind === "Homes" ? <><path d="m54 116 74-61 74 61h-17l-57-46-57 46Z" fill="#C56A3D" /><path d="m185 116-30-25h75l35 25Z" fill="#C56A3D" /><path d="M71 116v65h179v-65M185 116v65M91 126h27v28H91zm0 14h27m-14-14v28M136 181v-55h30v55M205 130h26v24h-26M63 181h195" /></> :
      kind === "Barns" ? <><path d="m55 117 30-48 66-25 65 25 30 48h-15l-26-38-54-21-55 21-26 38Z" fill="#C56A3D" /><path d="M70 117v64h161v-64M112 181v-65h78v65Zm0-65 78 65m0-65-78 65m39-65v65M137 78h28v23h-28ZM84 126v43m14-43v43m105-43v43m14-43v43M61 181h179" /></> :
      kind === "RVs" ? <><path d="M57 166V112q0-38 40-38h114q41 0 47 40l8 52Z" fill="#aeb4a8" /><path d="M83 94h46v34H83zm66 0h44v34h-44zm64 4h27l9 30h-36M165 166v-28h29v28M56 146h208" /></> :
      kind === "Buses / Skoolies" ? <><path d="M43 163V88q0-10 12-10h189l28 40v45Z" fill="#aa733f" /><path d="M59 94h28v29H59zm41 0h28v29h-28zm41 0h28v29h-28zm41 0h28v29h-28zm43 0h14l21 29h-35M44 140h171M225 163v-31h35v31" /></> :
      kind === "Tiny Homes" ? <><path d="m68 113 80-68 99 68-12 5-87-57-68 57Z" fill="#C56A3D" /><path d="M80 118v63h155v-63M148 61v120M99 131h29v35H99zm67-15h47v65h-47M167 147h46" /></> :
      kind === "Containers" ? <><path d="m50 98 156-26 66 25v83H50Z" fill="#183A5A" /><path d="M50 98h156v82m0-82 66-1M62 111v57m12-57v57m12-57v57m109-57v57M99 115h82v65H99zm40 0v65M220 110l38-1v56l-38 8Z" /></> :
      kind === "Outdoor Spaces" ? <><path d="M54 97h215l-31-26H83ZM67 97v88m186-88v88M99 74l-14 23m45-23-7 23m39-23v23m32-23 7 23m22-23 14 23M110 149h115v30H110zm-11 30h139M63 183h195M39 180v-35m0 17-17-17m17 9 18-18" /></> : <><path d="m44 184 82-131 65 108 35-70 60 93M89 113l37-60 37 61-23-8-14 17-15-18ZM191 161l35-70 23 45-21-8-10 13" /><path d="M108 177v-30l20-17 20 17v30Zm12 0v-18h16v18" fill="#C56A3D" /></>}
      {(kind === "RVs" || kind === "Buses / Skoolies") && <><circle cx="92" cy="166" r="14" fill="#1F1F1F" /><circle cx="229" cy="166" r="14" fill="#1F1F1F" /><circle cx="92" cy="166" r="5" /><circle cx="229" cy="166" r="5" /></>}
      <path d="M27 191h266" opacity=".5" />
    </g>
  </svg>;
}
