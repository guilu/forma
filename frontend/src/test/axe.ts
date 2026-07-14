// Shared accessibility-testing helper (FOR-114). Wraps jest-axe's `axe()` scan
// and registers its `toHaveNoViolations` matcher on Vitest's `expect`, so
// individual test files just do:
//
//   const { container } = render(<Screen />);
//   expect(await axe(container)).toHaveNoViolations();
//
// Package choice: `jest-axe` over `vitest-axe`. `jest-axe` targets Jest by
// name, but it only depends on `expect.extend` + `jest-matcher-utils` for
// message formatting -- it never touches Jest runner globals -- so it
// registers cleanly against Vitest's `expect` too (verified locally against
// this project's `vitest ^3.0.0`). `vitest-axe` would be redundant on top of
// that.
//
// `@types/jest-axe` is deliberately NOT installed: it starts with
// `/// <reference types="jest" />` (which fails to resolve without
// `@types/jest`, not present in this project) and augments Jest's
// `Matchers`/`@jest/expect` namespaces instead of Vitest's `Assertion`
// interface, so it would not type `toHaveNoViolations()` on Vitest's
// `expect` even if it did compile. The `declare module 'vitest'`
// augmentation below does that instead, mirroring the same pattern
// `@testing-library/jest-dom/vitest.d.ts` already uses in this project for
// `toBeInTheDocument()` and friends.
import { axe, toHaveNoViolations } from 'jest-axe';
import { expect } from 'vitest';

expect.extend(toHaveNoViolations);

export { axe };

interface AxeMatchers<R = unknown> {
  toHaveNoViolations(): R;
}

declare module 'vitest' {
  // Declaration merging into Vitest's own `Assertion<T = any>` interface
  // requires an identical type-parameter list (including the `any` default)
  // and, since the interface only adds a method, an "empty" body -- both
  // unavoidable with TS's interface-merging augmentation mechanism, and the
  // same shape `@testing-library/jest-dom/vitest.d.ts` uses for its own
  // matchers in this project.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/no-empty-object-type
  interface Assertion<T = any> extends AxeMatchers<T> {}
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  interface AsymmetricMatchersContaining extends AxeMatchers {}
}
