// `jest-axe` ships no type declarations of its own, and the community
// `@types/jest-axe` package targets Jest (triple-slash-references
// `@types/jest`, which this project doesn't install, and augments Jest's
// matcher namespaces instead of Vitest's). This minimal ambient declaration
// covers only the two exports `frontend/src/test/axe.ts` actually uses,
// typed against `axe-core`'s own (bundled) declarations.
declare module 'jest-axe' {
  import type { AxeResults, ImpactValue, Result, RunOptions, Spec } from 'axe-core';

  export interface JestAxeConfigureOptions extends RunOptions {
    globalOptions?: Spec;
    impactLevels?: ImpactValue[];
  }

  export type JestAxe = (html: Element | string, options?: RunOptions) => Promise<AxeResults>;

  export const axe: JestAxe;

  export function configureAxe(options?: JestAxeConfigureOptions): JestAxe;

  export interface AxeMatcherResult {
    pass: boolean;
    message(): string;
    actual: Result[];
  }

  export const toHaveNoViolations: {
    toHaveNoViolations(results?: Partial<AxeResults>): AxeMatcherResult;
  };
}
