/** Editorial inspiration only; category keys do not represent backend project categories. */
export type DetailDrawing = 'addition' | 'barn-living' | 'barn-studio' | 'barn-workshop' | 'bus-cutaway' | 'bus-layout' | 'bus-utility' | 'container-living' | 'container-office' | 'container-studio' | 'garden-deck' | 'greenhouse-interior' | 'kitchen' | 'observatory' | 'outdoor-kitchen' | 'pergola' | 'railcar' | 'rv-interior' | 'rv-kitchen' | 'rv-systems' | 'storage' | 'tiny-layout' | 'tiny-section' | 'tiny-storage';
export type IdeaVisual = { photo: string; alt: string } | { drawing: DetailDrawing | 'exterior' };
export const buildStageLabels = ['Idea', 'Structure', 'Systems', 'Finish'] as const;
export interface BuildStage { label: typeof buildStageLabels[number]; description: string }
export interface InspirationConcept {
  key: string;
  category: string;
  title: string;
  subtype: string;
  description: string;
  narrative: string;
  conceptGoals: string[];
  possibleTrades: string[];
  buildStages: BuildStage[];
  designNotes: string[];
  materialDirection: string;
  systemsConsiderations: string;
  visual: IdeaVisual;
}
export interface InspirationCategory {
  slug: string; name: string; headline: string; statement: string; cta: string;
  hero: IdeaVisual; possibilities: InspirationConcept[];
  exploreTitle: string; exploreIntroduction: string; storyTitle: string;
  trades: string[]; materials: string; systems: string;
}
export const inspirationCategories: readonly InspirationCategory[] = [
  {
    "slug": "homes",
    "name": "Homes",
    "headline": "Homes, reimagined.",
    "statement": "Keep what gives a home its character. Make room for the way you live now, through restoration, renovation or a thoughtful addition.",
    "cta": "Start a Home Project",
    "hero": {
      "photo": "/images/kitchen-remodel.png",
      "alt": "Warm timber kitchen cabinetry, open shelving and cream tile around a bright window"
    },
    "possibilities": [
      {
        "title": "A Kitchen to Gather In",
        "subtype": "Kitchen / Bath",
        "description": "Warm cabinetry, useful work surfaces and daylight at the heart of the home.",
        "visual": {
          "drawing": "kitchen"
        },
        "key": "homes-1",
        "category": "homes",
        "narrative": "A kitchen can welcome company without making the cook work around it. This direction keeps the window as an anchor, gathers the busiest tasks into a compact run and gives everyday objects a considered place.",
        "conceptGoals": [
          "Improve preparation space",
          "Bring daylight to the worktop",
          "Balance open shelves with concealed storage"
        ],
        "possibleTrades": [
          "Carpentry",
          "Plumbing",
          "Electrical",
          "Drywall",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Map how you prepare, cook and gather."
          },
          {
            "label": "Structure",
            "description": "Explore cabinet runs around existing openings and circulation."
          },
          {
            "label": "Systems",
            "description": "Coordinate sink, appliance and lighting positions before closing walls."
          },
          {
            "label": "Finish",
            "description": "Pair timber fronts with a pale worktop and a small area of open shelving."
          }
        ],
        "designNotes": [
          "Keep frequently used objects close to the task they support.",
          "Try full-size cabinet outlines in the room before settling on an arrangement."
        ],
        "materialDirection": "Timber cabinet fronts, a durable pale worktop and warm tile.",
        "systemsConsiderations": "Water, waste, appliance power and task lighting need to be considered together."
      },
      {
        "title": "Room for the Everyday",
        "subtype": "Addition",
        "description": "An extra room that connects naturally to the house and the garden.",
        "visual": {
          "drawing": "addition"
        },
        "key": "homes-2",
        "category": "homes",
        "narrative": "An extra room should feel like a natural continuation of the home. Imagine a modest garden-facing addition that makes shared space more generous while keeping the original rooms legible.",
        "conceptGoals": [
          "Connect house and garden",
          "Improve circulation between old and new",
          "Protect daylight in existing rooms"
        ],
        "possibleTrades": [
          "Carpentry",
          "Plumbing",
          "Electrical",
          "Drywall",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Decide which daily activity needs more space."
          },
          {
            "label": "Structure",
            "description": "Explore the junction between the existing house and a new garden room."
          },
          {
            "label": "Systems",
            "description": "Consider heating, drainage and electrical routes across that junction."
          },
          {
            "label": "Finish",
            "description": "Use a consistent floor or a carefully framed opening to link the rooms."
          }
        ],
        "designNotes": [
          "A wider opening changes how adjacent rooms feel, not just how they connect.",
          "Check the impact of a new roof on windows in the existing house."
        ],
        "materialDirection": "A restrained timber extension with joinery that picks up existing proportions.",
        "systemsConsiderations": "Heating, ventilation and rainwater routes shape the connection to the original building."
      },
      {
        "title": "A Place for Everything",
        "subtype": "Custom Interior",
        "description": "Built-in storage turns an overlooked wall into a useful part of daily life.",
        "visual": {
          "drawing": "storage"
        },
        "key": "homes-3",
        "category": "homes",
        "narrative": "The best storage can become part of the room rather than another object inside it. A fitted wall combines display, closed cupboards and a useful landing place without filling the circulation space.",
        "conceptGoals": [
          "Reduce visual clutter",
          "Fit storage to actual belongings",
          "Keep the room easy to move through"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Group belongings by frequency of use and size."
          },
          {
            "label": "Structure",
            "description": "Explore cabinet depths and fixing locations against the existing wall."
          },
          {
            "label": "Systems",
            "description": "Plan access to sockets, cables and any services behind the joinery."
          },
          {
            "label": "Finish",
            "description": "Mix quiet closed fronts with a few open bays for objects worth seeing."
          }
        ],
        "designNotes": [
          "Vary shelf heights around the things you keep.",
          "Leave service access usable after cupboards are filled."
        ],
        "materialDirection": "Painted lower cupboards with timber shelves and simple hardware.",
        "systemsConsiderations": "Socket access and cable paths should be planned before fitting joinery."
      }
    ],
    "trades": [
      "Carpentry",
      "Plumbing",
      "Electrical",
      "Drywall",
      "Flooring"
    ],
    "exploreTitle": "Make the familiar work differently.",
    "exploreIntroduction": "A room can keep its character and still learn a few new habits.",
    "storyTitle": "From daily habits to a better kitchen.",
    "materials": "Timber cabinet fronts, a durable pale worktop and warm tile.",
    "systems": "Water, waste, appliance power and task lighting need to be considered together."
  },
  {
    "slug": "barns",
    "name": "Barns",
    "headline": "Old bones. New possibilities.",
    "statement": "Find a new purpose in an old structure. A workshop, living space or creative studio can begin with the character already there.",
    "cta": "Start a Barn Project",
    "hero": {
      "photo": "/images/barn-after.jpg",
      "alt": "Timber barn workshop with open doors and a standing-seam roof"
    },
    "possibilities": [
      {
        "title": "Timber Workshop Conversion",
        "subtype": "Workshop",
        "description": "Wide doors, generous working space and timber worth keeping.",
        "visual": {
          "drawing": "barn-workshop"
        },
        "key": "barns-1",
        "category": "barns",
        "narrative": "Keep the scale of the barn, then make it useful at the scale of a working day. A long bench follows one wall; a central assembly table leaves room to move timber through the original doors. Exposed framing remains the character of the space.",
        "conceptGoals": [
          "Preserve exposed timber",
          "Create generous work zones",
          "Improve daylight at the bench",
          "Plan accessible electrical service"
        ],
        "possibleTrades": [
          "Carpentry",
          "Exterior Restoration",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Map making, assembly and storage around the original entrance."
          },
          {
            "label": "Structure",
            "description": "Assess the shell and explore repairs that retain the visible frame."
          },
          {
            "label": "Systems",
            "description": "Coordinate task lighting, tool power and extraction around the work zones."
          },
          {
            "label": "Finish",
            "description": "Add a durable bench, repairable surfaces and storage that leaves the structure visible."
          }
        ],
        "designNotes": [
          "Keep long-material movement in mind when placing the central table.",
          "Use a separate layer of joinery so new storage does not obscure the old frame."
        ],
        "materialDirection": "Retained timber, plywood storage and a durable, repairable work surface.",
        "systemsConsiderations": "Tool power, task lighting and dust extraction depend on the work you intend to do."
      },
      {
        "title": "Rustic Living Barn",
        "subtype": "Living Space",
        "description": "Explore a sheltered living space within a familiar agricultural silhouette.",
        "visual": {
          "drawing": "barn-living"
        },
        "key": "barns-2",
        "category": "barns",
        "narrative": "A large barn does not have to become one large room. Smaller sheltered zones can sit within the volume, leaving the upper structure visible and the original proportions easy to understand.",
        "conceptGoals": [
          "Create comfortable living zones",
          "Retain the sense of volume",
          "Coordinate insulation and utilities"
        ],
        "possibleTrades": [
          "Carpentry",
          "Exterior Restoration",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Choose where everyday life needs enclosure and where it benefits from openness."
          },
          {
            "label": "Structure",
            "description": "Explore an interior layer that respects the existing frame."
          },
          {
            "label": "Systems",
            "description": "Bring insulation, ventilation, heating and water routes into the same conversation."
          },
          {
            "label": "Finish",
            "description": "Use warm floors and smaller-scale lighting to make the large volume welcoming."
          }
        ],
        "designNotes": [
          "Let the old structure stay readable above new partitions.",
          "Soft finishes can help balance the acoustics of a large open volume."
        ],
        "materialDirection": "Timber floors, simple interior partitions and soft furnishings against the original shell.",
        "systemsConsiderations": "Comfort depends on the relationship between the building envelope, ventilation and heating."
      },
      {
        "title": "Studio Barn Restoration",
        "subtype": "Studio",
        "description": "A broad workbench and a quiet corner for making, drawing or creating.",
        "visual": {
          "photo": "/images/barn-before.jpg",
          "alt": "Weathered barn siding and an open entrance, suggesting a shell for creative reuse"
        },
        "key": "barns-3",
        "category": "barns",
        "narrative": "Let a weathered shell hold a flexible creative workspace. Long work surfaces and movable furniture make room for messy work, quiet drawing and the occasional change of direction.",
        "conceptGoals": [
          "Retain the character of the shell",
          "Create flexible creative space",
          "Bring useful light to the work surface"
        ],
        "possibleTrades": [
          "Carpentry",
          "Exterior Restoration",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "List the activities the studio should accommodate."
          },
          {
            "label": "Structure",
            "description": "Explore repairs, openings and clear floor space around the existing frame."
          },
          {
            "label": "Systems",
            "description": "Plan adaptable lighting and power without tying the room to one furniture layout."
          },
          {
            "label": "Finish",
            "description": "Introduce a broad workbench and easy-to-maintain storage."
          }
        ],
        "designNotes": [
          "Work light and atmospheric light serve different purposes.",
          "A movable table lets the studio adapt as a practice changes."
        ],
        "materialDirection": "Repaired timber, a broad plywood bench and washable working surfaces.",
        "systemsConsiderations": "Lighting, power and ventilation should reflect the materials and tools used in the studio."
      }
    ],
    "trades": [
      "Carpentry",
      "Exterior Restoration",
      "Electrical",
      "Plumbing",
      "Flooring"
    ],
    "exploreTitle": "Keep the character. Change the purpose.",
    "exploreIntroduction": "Start with what the old structure gives you: volume, light and a frame worth seeing.",
    "storyTitle": "Let the workshop grow from the frame.",
    "materials": "Retained timber, plywood storage and a durable, repairable work surface.",
    "systems": "Tool power, task lighting and dust extraction depend on the work you intend to do."
  },
  {
    "slug": "rvs",
    "name": "RVs",
    "headline": "Make the road feel like home.",
    "statement": "Rethink a compact interior around the journeys you want to take. A small kitchen, clever storage and considered systems can change everyday life on the road.",
    "cta": "Start an RV Project",
    "hero": {
      "drawing": "exterior"
    },
    "possibilities": [
      {
        "title": "Full Interior Refresh",
        "subtype": "Interior Rebuild",
        "description": "Give a familiar camper a new rhythm with a connected living and sleeping space.",
        "visual": {
          "drawing": "rv-interior"
        },
        "key": "rvs-1",
        "category": "rvs",
        "narrative": "Rework the interior around a clear route from door to bed. A compact dinette can share space with storage, while a quieter palette makes the narrow cabin feel settled. Every new fitting also belongs in the vehicle’s weight conversation.",
        "conceptGoals": [
          "Improve circulation",
          "Reduce visual clutter",
          "Make storage work harder",
          "Consider weight as part of the design"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Map travel routines and decide which furniture needs more than one use."
          },
          {
            "label": "Structure",
            "description": "Review the cabin condition and explore lightweight interior supports."
          },
          {
            "label": "Systems",
            "description": "Coordinate access to electrical, water and ventilation systems before fitting furniture."
          },
          {
            "label": "Finish",
            "description": "Choose restrained finishes and storage designed to stay secure in motion."
          }
        ],
        "designNotes": [
          "Try the layout with the bed and table in their working positions.",
          "The vehicle’s limits and the placement of added weight belong in early planning."
        ],
        "materialDirection": "Lightweight panel cabinetry, durable upholstery and modest trim details.",
        "systemsConsiderations": "Electrical access, water storage and ventilation must work within the vehicle context."
      },
      {
        "title": "Compact Mobile Kitchen",
        "subtype": "Kitchen",
        "description": "Explore a short run of cabinetry with a sink, worktop and storage close at hand.",
        "visual": {
          "drawing": "rv-kitchen"
        },
        "key": "rvs-2",
        "category": "rvs",
        "narrative": "A mobile galley asks a lot of a short worktop. Put preparation space where it is most useful, give compact appliances their own place and design cupboards for both daily use and travel.",
        "conceptGoals": [
          "Create an efficient galley",
          "Secure everyday storage",
          "Keep appliances compact",
          "Coordinate water and electrical access"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Choose the cooking habits the galley must support."
          },
          {
            "label": "Structure",
            "description": "Fit the cabinet run to the curved cabin and available aisle."
          },
          {
            "label": "Systems",
            "description": "Plan appliance power, ventilation and accessible water connections."
          },
          {
            "label": "Finish",
            "description": "Use secure catches, useful drawer divisions and an easy-clean work surface."
          }
        ],
        "designNotes": [
          "A usable preparation area matters as much as the number of appliances.",
          "Keep service access separate from heavily packed storage."
        ],
        "materialDirection": "Lightweight cabinet panels, a compact sink and easy-clean splash surfaces.",
        "systemsConsiderations": "Water connections, appliance power and ventilation need accessible service points."
      },
      {
        "title": "Off-grid Storage & Systems",
        "subtype": "Off-grid Systems",
        "description": "Bring accessible utilities and useful storage together within the vehicle’s constraints.",
        "visual": {
          "drawing": "rv-systems"
        },
        "key": "rvs-3",
        "category": "rvs",
        "narrative": "Make the hidden parts of mobile living easier to understand. A dedicated utility bay can bring service access and organized storage together, keeping everyday luggage out of the way of equipment.",
        "conceptGoals": [
          "Keep utilities accessible",
          "Plan battery and water space early",
          "Separate equipment from belongings"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "List the comforts you want away from hookups."
          },
          {
            "label": "Structure",
            "description": "Explore supported compartments within the vehicle’s space and load limits."
          },
          {
            "label": "Systems",
            "description": "Plan water and electrical equipment with appropriate access and separation."
          },
          {
            "label": "Finish",
            "description": "Use labeled access panels and removable storage modules."
          }
        ],
        "designNotes": [
          "Equipment needs room for service as well as room to fit.",
          "Keep frequently accessed controls reachable without unpacking the vehicle."
        ],
        "materialDirection": "Removable lightweight panels, fitted compartments and robust catches.",
        "systemsConsiderations": "Battery systems, water storage and ventilation require coordinated specialist planning."
      }
    ],
    "trades": [
      "Carpentry",
      "Electrical",
      "Plumbing",
      "Flooring"
    ],
    "exploreTitle": "More life in every mile.",
    "exploreIntroduction": "Look inside the shell. The most useful changes often begin with how you move through it.",
    "storyTitle": "A cabin arranged around your journey.",
    "materials": "Lightweight panel cabinetry, durable upholstery and modest trim details.",
    "systems": "Electrical access, water storage and ventilation must work within the vehicle context."
  },
  {
    "slug": "buses",
    "name": "Buses / Skoolies",
    "headline": "Build the journey.",
    "statement": "Turn a long, open shell into a place of your own. Connect living, sleeping and utility zones with the craft of a custom interior.",
    "cta": "Start a Bus Conversion",
    "hero": {
      "drawing": "exterior"
    },
    "possibilities": [
      {
        "title": "A Bus to Call Home",
        "subtype": "Full Conversion",
        "description": "Explore a new use for a familiar form, with living space behind the driver’s seat.",
        "visual": {
          "drawing": "bus-cutaway"
        },
        "key": "buses-1",
        "category": "buses",
        "narrative": "Use the length of a bus as an opportunity to give each part of the day a place. A living zone near the entrance leads into a compact kitchen and a quieter sleeping area, with the familiar window rhythm tying it together.",
        "conceptGoals": [
          "Connect living and sleeping zones",
          "Keep a clear central passage",
          "Use the window rhythm",
          "Integrate utility access"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Arrange a day of living along the length of the bus."
          },
          {
            "label": "Structure",
            "description": "Review the shell and explore interior supports around existing openings."
          },
          {
            "label": "Systems",
            "description": "Coordinate water, electrical and ventilation routes before lining the cabin."
          },
          {
            "label": "Finish",
            "description": "Fit secure cabinetry and a consistent material palette across the zones."
          }
        ],
        "designNotes": [
          "Work with the existing window spacing when placing furniture.",
          "Check circulation with doors, drawers and sleeping furniture in use."
        ],
        "materialDirection": "Lightweight timber-faced cabinetry and durable flooring through the cabin.",
        "systemsConsiderations": "Vehicle condition, load planning and accessible utilities are part of the conversion conversation."
      },
      {
        "title": "A Layout That Flows",
        "subtype": "Interior Layout",
        "description": "Organize sleeping, cooking and seating along the length of the bus.",
        "visual": {
          "drawing": "bus-layout"
        },
        "key": "buses-2",
        "category": "buses",
        "narrative": "A long room can feel connected without feeling exposed. Offset the main furniture zones, make a useful gathering space near the front and create a calmer place to rest at the rear.",
        "conceptGoals": [
          "Create privacy without blocking the aisle",
          "Make seating adaptable",
          "Keep daylight through the cabin"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Identify shared and private activities."
          },
          {
            "label": "Structure",
            "description": "Test partitions and furniture within the bus width."
          },
          {
            "label": "Systems",
            "description": "Keep heating, ventilation and electrical access compatible with the layout."
          },
          {
            "label": "Finish",
            "description": "Use curtains, upholstery and joinery to distinguish the zones."
          }
        ],
        "designNotes": [
          "A small change in furniture alignment can create a sense of separation.",
          "Avoid turning the main aisle into the only place to open storage."
        ],
        "materialDirection": "Fitted seating, soft curtains and restrained timber details.",
        "systemsConsiderations": "Ventilation and utility access should remain effective in every furniture configuration."
      },
      {
        "title": "Built Around Your Belongings",
        "subtype": "Custom Finish",
        "description": "Fitted shelves and cabinets help an unconventional interior feel settled.",
        "visual": {
          "drawing": "bus-utility"
        },
        "key": "buses-3",
        "category": "buses",
        "narrative": "Gather the practical needs of a bus home into a considered core. Pair kitchen and washing functions where useful, while giving electrical and water equipment their own accessible spaces.",
        "conceptGoals": [
          "Organize utility routes",
          "Make equipment serviceable",
          "Protect useful living space"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Identify which functions can share a central location."
          },
          {
            "label": "Structure",
            "description": "Explore a fitted core within the shell and vehicle constraints."
          },
          {
            "label": "Systems",
            "description": "Coordinate separated water and electrical equipment with specialist input."
          },
          {
            "label": "Finish",
            "description": "Provide removable panels and clear everyday controls."
          }
        ],
        "designNotes": [
          "Keep service doors accessible when furniture is in use.",
          "A compact core should simplify maintenance, not conceal it."
        ],
        "materialDirection": "Moisture-tolerant lining where needed and removable cabinet faces.",
        "systemsConsiderations": "Water, waste, power and ventilation should be planned as an interconnected arrangement."
      }
    ],
    "trades": [
      "Carpentry",
      "Electrical",
      "Plumbing",
      "Flooring"
    ],
    "exploreTitle": "A whole day, along one aisle.",
    "exploreIntroduction": "Let the length of a bus become a sequence of places to live.",
    "storyTitle": "From open shell to everyday living.",
    "materials": "Lightweight timber-faced cabinetry and durable flooring through the cabin.",
    "systems": "Vehicle condition, load planning and accessible utilities are part of the conversion conversation."
  },
  {
    "slug": "tiny-homes",
    "name": "Tiny Homes",
    "headline": "Small footprint. Big possibility.",
    "statement": "Make a compact home feel generous through thoughtful proportions, useful storage and careful craft. Imagine a backyard retreat, small dwelling or off-grid studio.",
    "cta": "Start a Tiny Home Project",
    "hero": {
      "drawing": "exterior"
    },
    "possibilities": [
      {
        "title": "Woodland Tiny Home",
        "subtype": "Custom Build",
        "description": "A simple roofline and generous opening connect a compact home to its setting.",
        "visual": {
          "drawing": "tiny-section"
        },
        "key": "tiny-homes-1",
        "category": "tiny-homes",
        "narrative": "Let a compact shell hold a generous relationship with the outdoors. A high ceiling over the living area, carefully placed storage and a sheltered entrance can make a small footprint feel composed rather than compressed.",
        "conceptGoals": [
          "Connect daily living to the landscape",
          "Keep the main space open",
          "Integrate storage into the shell"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring",
          "Exterior Restoration"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Define the routines the small home should support."
          },
          {
            "label": "Structure",
            "description": "Explore ceiling height, openings and an efficient enclosure."
          },
          {
            "label": "Systems",
            "description": "Coordinate the kitchen, bathroom and comfort systems around available space."
          },
          {
            "label": "Finish",
            "description": "Bring warmth through timber lining and purposeful built-ins."
          }
        ],
        "designNotes": [
          "Consider the view from seated positions as well as standing ones.",
          "Compact living works best when furniture and circulation are designed together."
        ],
        "materialDirection": "Timber lining, durable flooring and carefully proportioned windows.",
        "systemsConsiderations": "Heating, ventilation, plumbing and power should be sized and arranged around the actual use."
      },
      {
        "title": "Compact Family Layout",
        "subtype": "Compact Living",
        "description": "Give shared time and quiet time their own places within a modest footprint.",
        "visual": {
          "drawing": "tiny-layout"
        },
        "key": "tiny-homes-2",
        "category": "tiny-homes",
        "narrative": "Make room for shared time and retreat without multiplying tiny rooms. A compact layout can use furniture, light and partial separation to support more than one person’s routine.",
        "conceptGoals": [
          "Balance shared and quiet time",
          "Avoid wasted passage space",
          "Keep furniture flexible"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring",
          "Exterior Restoration"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Map overlapping daily routines."
          },
          {
            "label": "Structure",
            "description": "Explore a small number of well-proportioned zones."
          },
          {
            "label": "Systems",
            "description": "Position utilities so they do not dictate every furniture decision."
          },
          {
            "label": "Finish",
            "description": "Use movable pieces and soft divisions where full walls are unnecessary."
          }
        ],
        "designNotes": [
          "Test the layout at its busiest time of day.",
          "A generous shared surface can serve meals, homework and making."
        ],
        "materialDirection": "Adaptable furniture, durable floor finishes and soft room dividers.",
        "systemsConsiderations": "Lighting and ventilation should support different activities in the same space."
      },
      {
        "title": "Storage in the Architecture",
        "subtype": "Storage",
        "description": "Use fitted cabinetry to keep the living space open and uncluttered.",
        "visual": {
          "drawing": "tiny-storage"
        },
        "key": "tiny-homes-3",
        "category": "tiny-homes",
        "narrative": "A wall of storage can do more than hide belongings. Let shelves, seating and cupboards become part of the architecture so the central floor remains useful and open.",
        "conceptGoals": [
          "Build storage into the room",
          "Keep everyday objects within reach",
          "Preserve open floor space"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Flooring",
          "Exterior Restoration"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Measure what needs a home before designing the cupboards."
          },
          {
            "label": "Structure",
            "description": "Explore built-ins around the room’s openings and structure."
          },
          {
            "label": "Systems",
            "description": "Keep utility controls and access points out of fixed storage bays."
          },
          {
            "label": "Finish",
            "description": "Combine open shelves, deep cupboards and a useful seat."
          }
        ],
        "designNotes": [
          "Frequent-use storage should be easier to reach than seasonal storage.",
          "Built-in furniture still needs comfortable space around it."
        ],
        "materialDirection": "Light timber fronts with a few open display bays and durable seat upholstery.",
        "systemsConsiderations": "Plan sockets, ventilation paths and service panels before closing the joinery."
      }
    ],
    "trades": [
      "Carpentry",
      "Electrical",
      "Plumbing",
      "Flooring",
      "Exterior Restoration"
    ],
    "exploreTitle": "Room for what matters.",
    "exploreIntroduction": "Make a small footprint generous through the choices inside it.",
    "storyTitle": "Give a small home a generous plan.",
    "materials": "Timber lining, durable flooring and carefully proportioned windows.",
    "systems": "Heating, ventilation, plumbing and power should be sized and arranged around the actual use."
  },
  {
    "slug": "containers",
    "name": "Containers",
    "headline": "Structure, transformed.",
    "statement": "Start with an industrial shell and imagine a different life inside. Explore homes, studios, workshops and offices shaped by steel, timber and light.",
    "cta": "Start a Container Project",
    "hero": {
      "drawing": "exterior"
    },
    "possibilities": [
      {
        "title": "A New Life for Steel",
        "subtype": "Living Space",
        "description": "An industrial exterior becomes the starting point for a compact place to live.",
        "visual": {
          "drawing": "container-living"
        },
        "key": "containers-1",
        "category": "containers",
        "narrative": "Keep the industrial silhouette while making the interior feel domestic. Carefully placed openings and a warm inner lining can turn a narrow steel shell into a more welcoming place to spend time.",
        "conceptGoals": [
          "Bring daylight into the shell",
          "Create comfortable living zones",
          "Balance steel with warmer materials"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Drywall",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Choose the relationship between the living space and its setting."
          },
          {
            "label": "Structure",
            "description": "Explore openings and structural implications with the right specialists."
          },
          {
            "label": "Systems",
            "description": "Coordinate insulation, moisture control, ventilation and utilities."
          },
          {
            "label": "Finish",
            "description": "Add a warm interior lining and furniture scaled to the narrow space."
          }
        ],
        "designNotes": [
          "Openings change both the view and the structure.",
          "The inner lining needs to account for the way the steel shell responds to temperature."
        ],
        "materialDirection": "Timber-faced lining against a retained corrugated steel exterior.",
        "systemsConsiderations": "Thermal bridging, condensation, ventilation and utility routes need coordinated consideration."
      },
      {
        "title": "The Focused Studio",
        "subtype": "Studio",
        "description": "A work surface, daylight and room to concentrate in a small creative space.",
        "visual": {
          "drawing": "container-studio"
        },
        "key": "containers-2",
        "category": "containers",
        "narrative": "Frame a workspace within the steel shell, with a broad opening at one end and storage along one side. The contrast between an industrial exterior and a calm working interior gives the studio its identity.",
        "conceptGoals": [
          "Create a focused working area",
          "Bring light to the desk",
          "Keep storage off the main floor"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Drywall",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Define the work and the kind of light it needs."
          },
          {
            "label": "Structure",
            "description": "Explore the opening and supporting structure."
          },
          {
            "label": "Systems",
            "description": "Plan task lighting, comfort and power around the workstation."
          },
          {
            "label": "Finish",
            "description": "Fit a generous desk and simple shelves against the interior lining."
          }
        ],
        "designNotes": [
          "Place screens and work surfaces with glare in mind.",
          "Keep the desk separate from the wall lining where future adjustment may help."
        ],
        "materialDirection": "Corrugated steel outside, pale lining and a timber work surface inside.",
        "systemsConsiderations": "Ventilation, insulation and workstation power are integral to a usable studio."
      },
      {
        "title": "Office Within the Shell",
        "subtype": "Office",
        "description": "Explore the balance between a working zone, storage and a welcoming entrance.",
        "visual": {
          "drawing": "container-office"
        },
        "key": "containers-3",
        "category": "containers",
        "narrative": "Divide a compact office into places to focus, meet and put things away. A clear entrance and a long work surface can make a narrow footprint feel purposeful.",
        "conceptGoals": [
          "Create a welcoming entrance",
          "Separate focused and shared work",
          "Use the narrow footprint well"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Drywall",
          "Flooring"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Identify the number of people and activities the office should support."
          },
          {
            "label": "Structure",
            "description": "Explore partitions and openings without losing the sense of length."
          },
          {
            "label": "Systems",
            "description": "Coordinate data, power and indoor comfort around the desks."
          },
          {
            "label": "Finish",
            "description": "Use integrated storage and a restrained palette to keep the room calm."
          }
        ],
        "designNotes": [
          "Allow room for chairs in use, not only desks at rest.",
          "Keep the entrance from becoming overflow storage."
        ],
        "materialDirection": "Pale wall lining, fitted timber storage and a durable floor.",
        "systemsConsiderations": "Data routes, electrical outlets, ventilation and heating belong in the layout discussion."
      }
    ],
    "trades": [
      "Carpentry",
      "Electrical",
      "Plumbing",
      "Drywall",
      "Flooring"
    ],
    "exploreTitle": "New life inside the steel.",
    "exploreIntroduction": "Industrial proportions can hold a surprisingly personal way of living or working.",
    "storyTitle": "Make the shell work from the inside out.",
    "materials": "Timber-faced lining against a retained corrugated steel exterior.",
    "systems": "Thermal bridging, condensation, ventilation and utility routes need coordinated consideration."
  },
  {
    "slug": "outdoor-spaces",
    "name": "Outdoor Spaces",
    "headline": "Build beyond the walls.",
    "statement": "Make more of the space outside. A deck, pergola, garden structure or outdoor kitchen can give everyday life a new setting.",
    "cta": "Start an Outdoor Project",
    "hero": {
      "drawing": "exterior"
    },
    "possibilities": [
      {
        "title": "Shade for Slow Afternoons",
        "subtype": "Pergolas",
        "description": "An open timber structure defines a place to gather without closing out the garden.",
        "visual": {
          "drawing": "pergola"
        },
        "key": "outdoor-spaces-1",
        "category": "outdoor-spaces",
        "narrative": "Give an outdoor gathering place a defined edge without turning it into another room. A rhythm of timber beams can frame the sky and connect a table, the house and the garden.",
        "conceptGoals": [
          "Define a gathering place",
          "Balance openness and shade",
          "Connect house and garden"
        ],
        "possibleTrades": [
          "Carpentry",
          "Exterior Restoration",
          "Electrical"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Observe where you want to sit at different times of day."
          },
          {
            "label": "Structure",
            "description": "Explore posts, spans and ground connections for the setting."
          },
          {
            "label": "Systems",
            "description": "Consider lighting and nearby drainage before completing the ground surface."
          },
          {
            "label": "Finish",
            "description": "Use a consistent timber rhythm and outdoor-ready fittings."
          }
        ],
        "designNotes": [
          "Open slats suggest enclosure but do not provide a weatherproof roof.",
          "Plan the structure around the space needed to pull chairs out."
        ],
        "materialDirection": "Exterior timber, suitable connectors and a durable ground surface.",
        "systemsConsiderations": "Lighting routes and drainage should be considered alongside the ground and structure."
      },
      {
        "title": "A Deck That Connects",
        "subtype": "Decks",
        "description": "Explore a simple platform that links a doorway, a table and the landscape.",
        "visual": {
          "drawing": "garden-deck"
        },
        "key": "outdoor-spaces-2",
        "category": "outdoor-spaces",
        "narrative": "A modest platform can turn the transition from house to garden into a destination. Let the shape respond to a doorway, a place to sit and the movement of the landscape.",
        "conceptGoals": [
          "Create a comfortable threshold",
          "Make room for outdoor seating",
          "Respect water movement across the site"
        ],
        "possibleTrades": [
          "Carpentry",
          "Exterior Restoration",
          "Electrical"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Map the route from the house to the garden."
          },
          {
            "label": "Structure",
            "description": "Explore platform height, support and edges in the setting."
          },
          {
            "label": "Systems",
            "description": "Consider drainage and any planned lighting routes."
          },
          {
            "label": "Finish",
            "description": "Choose a repairable board layout and useful seating positions."
          }
        ],
        "designNotes": [
          "Ground conditions help shape the support conversation.",
          "Think about how the surface will feel and wear through the seasons."
        ],
        "materialDirection": "Exterior deck boards, simple edge details and movable outdoor furniture.",
        "systemsConsiderations": "Surface drainage and lighting access should remain workable after the deck is built."
      },
      {
        "title": "Cooking in the Open Air",
        "subtype": "Outdoor Kitchens",
        "description": "Bring preparation, serving and storage together in a sheltered outdoor counter.",
        "visual": {
          "drawing": "outdoor-kitchen"
        },
        "key": "outdoor-spaces-3",
        "category": "outdoor-spaces",
        "narrative": "Give outdoor cooking a useful working edge. Group preparation, cooking and serving without isolating the cook from the table, and choose materials that belong outside.",
        "conceptGoals": [
          "Connect cooking and gathering",
          "Provide useful preparation space",
          "Make storage suitable for the weather"
        ],
        "possibleTrades": [
          "Carpentry",
          "Exterior Restoration",
          "Electrical",
          "Plumbing"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Choose the cooking equipment and the way you want to serve."
          },
          {
            "label": "Structure",
            "description": "Explore a durable counter and shelter appropriate to the location."
          },
          {
            "label": "Systems",
            "description": "Coordinate appliance connections, water and lighting with specialist input."
          },
          {
            "label": "Finish",
            "description": "Use cleanable surfaces and weather-appropriate storage."
          }
        ],
        "designNotes": [
          "Let the selected equipment guide the surrounding layout.",
          "Consider how the kitchen will be cared for between seasons."
        ],
        "materialDirection": "A durable exterior counter, weather-appropriate fronts and cleanable splash surfaces.",
        "systemsConsiderations": "Appliance services, water, drainage and outdoor electrical needs require coordinated planning."
      }
    ],
    "trades": [
      "Carpentry",
      "Exterior Restoration",
      "Electrical",
      "Plumbing"
    ],
    "exploreTitle": "Give the garden a gathering place.",
    "exploreIntroduction": "A little structure can change how you spend time outside.",
    "storyTitle": "From a patch of ground to a place to gather.",
    "materials": "Exterior timber, suitable connectors and a durable ground surface.",
    "systems": "Lighting routes and drainage should be considered alongside the ground and structure."
  },
  {
    "slug": "beyond",
    "name": "And Beyond",
    "headline": "If you can imagine it, start here.",
    "statement": "Your idea defines the project. A greenhouse studio, railcar conversion, boat shed or something without a name yet: there is room for more than a predefined category.",
    "cta": "Start Something Different",
    "hero": {
      "drawing": "exterior"
    },
    "possibilities": [
      {
        "title": "The Greenhouse Studio",
        "subtype": "Creative Reuse",
        "description": "A light-filled structure for plants, making and a different kind of working day.",
        "visual": {
          "drawing": "greenhouse-interior"
        },
        "key": "beyond-1",
        "category": "beyond",
        "narrative": "Imagine a working day that shares its space with plants. A greenhouse studio pairs a dry making bench with a planting edge, creating a place that belongs neither entirely to the garden nor entirely to the house.",
        "conceptGoals": [
          "Bring plants and making together",
          "Balance daylight with comfort",
          "Separate wet and dry activities",
          "Keep the space adaptable"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Exterior Restoration"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Decide how gardening and creative work could share the day."
          },
          {
            "label": "Structure",
            "description": "Explore a glazed shell with a useful solid working edge."
          },
          {
            "label": "Systems",
            "description": "Consider shade, ventilation, water and protected power locations together."
          },
          {
            "label": "Finish",
            "description": "Add a dry workbench, planting surfaces and storage that can change with the seasons."
          }
        ],
        "designNotes": [
          "More glass means considering summer comfort as well as daylight.",
          "Keep delicate materials away from watering and planting activities."
        ],
        "materialDirection": "A glazed frame, durable planting surfaces and a timber making bench.",
        "systemsConsiderations": "Ventilation, shading, drainage and electrical locations shape how comfortably the two uses coexist."
      },
      {
        "title": "A Window to the Night Sky",
        "subtype": "Backyard Observatory",
        "description": "Imagine a small, dedicated place for looking up and learning.",
        "visual": {
          "drawing": "observatory"
        },
        "key": "beyond-2",
        "category": "beyond",
        "narrative": "Make a small place for a very large view. A backyard observatory can give an observing hobby a dedicated home, with room for equipment, quiet preparation and a relationship to the night sky.",
        "conceptGoals": [
          "Create a dedicated observing space",
          "Keep equipment organized",
          "Limit unwanted light"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Exterior Restoration"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Choose the observing experience and equipment the space should support."
          },
          {
            "label": "Structure",
            "description": "Explore the enclosure, opening and equipment support with appropriate specialists."
          },
          {
            "label": "Systems",
            "description": "Plan low-light controls, power access and moisture management."
          },
          {
            "label": "Finish",
            "description": "Use restrained interior finishes and accessible equipment storage."
          }
        ],
        "designNotes": [
          "The view of the sky matters more than the view from the house.",
          "Equipment movement should be considered before settling the enclosure shape."
        ],
        "materialDirection": "A weather-conscious enclosure, subdued interior finishes and fitted equipment storage.",
        "systemsConsiderations": "Power, moisture management and carefully controlled lighting support the intended use."
      },
      {
        "title": "The Railcar Retreat",
        "subtype": "Unusual Adaptive Reuse",
        "description": "A familiar railcar shell becomes a starting point for rest, reading and a different kind of escape.",
        "visual": {
          "drawing": "railcar"
        },
        "key": "beyond-3",
        "category": "beyond",
        "narrative": "Let the length and window rhythm of an old railcar suggest a quiet retreat. A place to read, share a meal and rest could sit within the familiar shell, with new craft clearly distinguishable from the original fabric.",
        "conceptGoals": [
          "Retain the railcar’s identity",
          "Create places to rest and gather",
          "Work with the narrow proportions"
        ],
        "possibleTrades": [
          "Carpentry",
          "Electrical",
          "Plumbing",
          "Exterior Restoration"
        ],
        "buildStages": [
          {
            "label": "Idea",
            "description": "Imagine the experience first: a reading retreat, guest space or creative escape."
          },
          {
            "label": "Structure",
            "description": "Assess the shell, siting and support before developing the interior."
          },
          {
            "label": "Systems",
            "description": "Coordinate insulation, ventilation, electrical and any water needs."
          },
          {
            "label": "Finish",
            "description": "Introduce fitted seating and warm lining without hiding every trace of the original car."
          }
        ],
        "designNotes": [
          "The condition and history of the shell are an early part of the conversation.",
          "New fittings can echo the window rhythm without imitating old details."
        ],
        "materialDirection": "Retained metalwork, warm interior lining and fitted upholstered seating.",
        "systemsConsiderations": "Site access, enclosure condition, indoor comfort and utility connections shape feasibility."
      }
    ],
    "trades": [
      "Carpentry",
      "Electrical",
      "Plumbing",
      "Exterior Restoration"
    ],
    "exploreTitle": "A place for the idea without a category.",
    "exploreIntroduction": "Follow the experience you want to create. The shape of the project can come next.",
    "storyTitle": "Let two uses share one light-filled space.",
    "materials": "A glazed frame, durable planting surfaces and a timber making bench.",
    "systems": "Ventilation, shading, drainage and electrical locations shape how comfortably the two uses coexist."
  }
];
