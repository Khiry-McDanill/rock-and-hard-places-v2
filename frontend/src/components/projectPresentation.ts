// Compatibility entry point; all contextual mapping lives in the removable seed layer.
import {
  seedProjectMedia,
  type ProjectDisplayContext,
} from "./seedProjectMedia";
export type { ProjectDisplayContext } from "./seedProjectMedia";
export function projectPresentation(context: ProjectDisplayContext = {}) {
  return seedProjectMedia(context).cover;
}
