import { BuildSketch } from './BuildSketch';
import type { DetailDrawing, IdeaVisual } from './inspirationData';

const cabinet = 'M63 181V51h194v130ZM74 62h57v68H74Zm72 0h98v24h-98Zm0 37h98v31h-98ZM63 141h194M127 141v40m64-40v40M79 155h29m35 0h30m35 0h30M85 130V96h11v34m5 0V83h10v47m53-44V70h16v16m29 44v-21h22v21';
const mobileShell = 'M32 176V89q0-27 27-27h191l35 45v69ZM32 108h253M49 176a15 15 0 0 0 30 0m158 0a15 15 0 0 0 30 0';
const busShell = 'M26 176V71q0-12 12-12h222l33 44v73ZM26 103h267M44 72h26v20H44Zm39 0h26v20H83Zm39 0h26v20h-26Zm39 0h26v20h-26Zm39 0h26v20h-26Zm39 0h15l15 20h-30M57 176a15 15 0 0 0 30 0m151 0a15 15 0 0 0 30 0';
const barnFrame = 'M39 182V103l40-45 81-26 81 26 40 45v79M39 103h242M79 58v124m81-150v150m81-124v124M39 103l40 35m202-35-40 35';
const containerShell = 'M34 180V66h252v114ZM34 79h252M44 85v85m11-85v85m210-85v85m11-85v85';

/** Concept-specific cutaways and elevations, using the established RH&P drawing palette. */
const details: Record<DetailDrawing, { outline: string; accent: string }> = {
  kitchen: {
    outline: 'M45 182V110h230v72ZM45 123h230M104 123v59m90-59v59M118 144h60m-60 21h60M125 110V96q0-11 10-11t10 11M50 47h77v47H50Zm38 0v47m-38-24h77M182 51h83m-83 27h83M211 102h41m-31-5v10m11-10v10',
    accent: 'M42 110h236v12H42Z',
  },
  addition: {
    outline: 'M40 179V96l66-48 65 48v83ZM54 110h28v32H54Zm67 0h28v32h-28ZM171 179V94h105v85M190 105h66v74m-44-74v74m22-74v74M164 86h119v9H164M39 179h248M200 189h80',
    accent: 'm33 96 73-55 70 51-6 7-64-47-67 50Z',
  },
  storage: { outline: cabinet, accent: 'M64 132h192v8H64Z' },
  'barn-workshop': {
    outline: barnFrame+'M48 143h98v9H48Zm7 9v30m85-30v30M176 142h93v10h-93Zm7 10v30m79-30v30M109 160h101v8H109Zm7 8v14m88-14v14M95 82h38v38H95Zm19 0v38m-19-19h38M189 80h32m-32 13h32m-27 0v22m10-22v14m10-14v22',
    accent: 'M108 152h104v8H108Z',
  },
  'barn-living': {
    outline: barnFrame+'M53 181v-45h71v45m-71-25h71M176 182v-56h88v56m-88-37h88M92 132v-18h45v18M178 119V83h53v36ZM185 182v-28h19v28M228 182v-28h19v28',
    accent: 'M47 151h83v8H47Zm123-25h101v9H170Z',
  },
  'barn-studio': {
    outline: barnFrame+'M87 124V78h50v46H87Zm25-46v46M46 142h224m-213 0v40m201-40v40M181 129l13-31 18 6m-29 25h26M121 181v-26h72v26',
    accent: 'M44 134h228v8H44Z',
  },
  'rv-interior': {
    outline: mobileShell+'M45 80h46v20H45Zm91 0h51v20h-51ZM44 166v-41h30v41m-30-12h68v12H44Zm76 1v-43h56v43m-28-43v43M119 125h60M142 119v-7m67 55v-37h61v37m-61-24h61M206 130v-9h65v9',
    accent: 'M118 118h62v8h-62Zm87 24h68v8h-68Z',
  },
  'rv-kitchen': {
    outline: 'M47 177V68q0-22 23-22h167l35 43v88ZM64 60h71v40H64Zm35 0v40M156 61h76v29h-76ZM58 177v-60h197v60M58 129h197M119 129v48m75-48v48M134 144h45m-45 17h45M78 117v-10q0-10 9-10t9 10M201 112h38m-28-5v10m17-10v10M68 177a17 17 0 0 0 34 0m124 0a17 17 0 0 0 34 0',
    accent: 'M55 118h203v10H55Z',
  },
  'rv-systems': {
    outline: 'M44 181V63h232v118ZM44 85h232M56 102h61v62H56Zm8-8h12v8m21-8h12v8M68 117h14m-7-7v14m16-7h14M136 98h72v67h-72Zm0 20h72m-72 29h72M217 105h46v61h-46Zm0 20h46m-46 20h46M82 164v9h160v-7M170 98V72h-55M60 181a15 15 0 0 0 30 0m147 0a15 15 0 0 0 30 0',
    accent: 'M136 140h72v25h-72Z',
  },
  'bus-cutaway': {
    outline: busShell+'M37 163v-42h34v42Zm0-13h63v13H37Zm74 14v-44h60v44m-30-44v44M184 163v-39h70v39m-70-24h70M193 124v-10h50v10M267 159v-29l14-8',
    accent: 'M108 114h66v9h-66Zm75 25h74v8h-74Z',
  },
  'bus-layout': {
    outline: 'M25 155V70q0-12 14-12h254v122H39q-14 0-14-12ZM49 58v122m14-106h57v30H63Zm0 63h57v28H63ZM134 74h41v29h-41Zm0 44h41v46h-41ZM202 74h74v90h-74Zm10 10h24v20h-24Zm30 0h24v20h-24ZM36 80v27m0 20v27M202 58v16m0 90v16M25 192h268',
    accent: 'M67 118h48v9H67Z',
  },
  'bus-utility': {
    outline: busShell+'M42 118h66v45H42Zm0 20h66M126 116h79v47h-79Zm10 10h24v24h-24Zm32 0h26v24h-26ZM221 115h55v48h-55Zm0 14h55m-55 17h55M140 116v-7h97v6M65 163v8h183v-8',
    accent: 'M43 151h64v12H43Z',
  },
  'tiny-section': {
    outline: 'M54 182V103l106-61 106 61v79ZM54 103h100m-79 0v-30m27 30V57M72 126h59v44H72Zm0 24h59M154 182v-50h38v50M203 182v-44h48v44m-48-28h48M225 131V92h28v39M181 73v46h73M193 119v-10h50v10',
    accent: 'm46 102 114-68 114 68-7 7-107-63-107 63Z',
  },
  'tiny-layout': {
    outline: 'M47 179V56h226v123ZM60 70h72v42H60Zm0 21h72M60 134h61v31H60ZM147 56v49m0 35v39M161 69h98v36h-98Zm0 62h98v34h-98ZM171 138h33v21h-33Zm43 0h34v21h-34M132 113h40v18h-40M47 193h226',
    accent: 'M63 123h56v9H63Z',
  },
  'tiny-storage': {
    outline: 'M47 182V70l113-32 113 32v112M66 177V90h75v87ZM66 131h75m-37-41v41M155 177V69h99v108ZM155 99h99m-99 30h99m-68 0v48m34-48v48M79 114V99h13v15m23 11v-18h13v18M169 87V76h15v11m20 0V73h33v14M168 145h11m16 0h16m19 0h15',
    accent: 'M65 165h77v12H65Z',
  },
  'container-living': {
    outline: containerShell+'M68 93h65v35H68Zm32 0v35M68 168v-23h74v23m-74-13h74M162 95h83v72h-83Zm0 22h83m-53 0v50M175 143h9m21 0h27M156 168h95',
    accent: 'M158 109h91v9h-91Z',
  },
  'container-studio': {
    outline: containerShell+'M68 91h98v44H68Zm49 0v44m-49-22h98M68 146h180m-174 0v34m167-34v34M185 94h57v30h-57Zm0 15h57M145 181v-24h43v24M181 138l11-30 16 6',
    accent: 'M65 138h185v8H65Z',
  },
  'container-office': {
    outline: 'M39 62h242v119H39ZM49 72h114v30H49Zm0 0v99h44v-57M176 62v46m0 40v33M190 77h77v30h-77Zm0 50h77v40h-77ZM109 137h52v29h-52M39 193h242M39 189v8m242-8v8',
    accent: 'M51 88h109v12H51Z',
  },
  pergola: {
    outline: 'm35 93 64-42h150l40 42ZM52 93v89m219-89v89M99 51v131m150-131v131M35 93h254M70 93l44-42m-7 42 39-42m-3 42 31-42m7 42 24-42m12 42 20-42M95 145h137m-127 0v37m117-37v37M76 155h15v27m155-27h15v27',
    accent: 'M35 93h254v9H35Z',
  },
  'garden-deck': {
    outline: 'm39 148 71-51h127l46 51v28H39Zm0 0h244M110 97v51m24-51v51m25-51v51m25-51v51m25-51v51m25-51v51M48 176v12m224-12v12M76 139v-25h47v25m-47-11h47M177 126h40m-33 0v22m26-22v22M254 126V71m0 27-18-17m18 5 18-24M61 102V66m0 16L45 67m16 8 13-19',
    accent: 'M40 149h242v10H40Z',
  },
  'outdoor-kitchen': {
    outline: 'M49 181v-64h222v64ZM49 130h222M120 130v51m74-51v51M57 114h56V93H57Zm0-21q28-35 56 0M62 99h45m-45 7h45M132 142h47m-47 18h47M217 118v-15q0-10 9-10t9 10M40 181h241M267 99V62m0 16-14-14m14 8 15-19',
    accent: 'M46 119h228v10H46Z',
  },
  'greenhouse-interior': {
    outline: 'M37 182V94l123-57 123 57v88M37 94h246M80 75v107m80-145v145M240 75v107M37 136h246M80 75h160M57 151h81v9H57Zm6 9v22m69-22v22M182 147h83v12h-83Zm6 12v23m71-23v23M196 147v-27m0 11-11-12m11 5 11-13M222 147v-23m0 11 10-12m-10 6-10-13M245 147v-24M75 144v-14h46v14',
    accent: 'M54 143h87v8H54Z',
  },
  observatory: {
    outline: 'M70 180v-67h181v67ZM70 113a91 75 0 0 1 181 0M150 39q-35 36-35 74m49-74q-8 37 17 74M86 130h31v31H86Zm111 50v-46h33v46M132 180l28-48 29 48m-29-48v48m-13-52 32-24 8 10-32 24ZM256 51h15m-8-7v14M43 83h14m-7-7v14',
    accent: 'M69 109h183v9H69Z',
  },
  railcar: {
    outline: 'M28 171V79q0-17 18-17h233q14 0 14 17v92ZM28 82h265M41 92h28v39H41Zm41 0h28v39H82Zm41 0h28v39h-28Zm41 0h28v39h-28Zm41 0h28v39h-28Zm43 0h30v64h-30M28 143h209M24 183h274m-274 8h274M64 171a10 10 0 0 0 20 0m7 0a10 10 0 0 0 20 0m102 0a10 10 0 0 0 20 0m7 0a10 10 0 0 0 20 0M36 183l-6 8m33-8-6 8m33-8-6 8m33-8-6 8m33-8-6 8m33-8-6 8m33-8-6 8m33-8-6 8m33-8-6 8',
    accent: 'M29 145h207v12H29Z',
  },
};

export function InspirationVisual({ visual, category, eager = false }: { visual: IdeaVisual; category: string; eager?: boolean }) {
  if ('photo' in visual) return <img src={visual.photo} alt={visual.alt} loading={eager ? 'eager' : 'lazy'} />;
  if (visual.drawing === 'exterior') return <BuildSketch kind={category} />;
  const drawing = details[visual.drawing];
  return <svg className="build-sketch" viewBox="0 0 320 230" fill="none" aria-hidden="true">
    <path d="M0 198 63 167l69 27 81-33 107 37v32H0Z" fill="#5B6B4F" opacity=".35" />
    <g stroke="#F5EFE6" strokeWidth="2" strokeLinejoin="round">
      <path d={drawing.outline} /><path d={drawing.accent} fill="#C56A3D" />
      <path d="M24 198h274" opacity=".5" />
    </g>
  </svg>;
}
