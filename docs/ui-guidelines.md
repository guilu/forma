# Forma UI guidelines

## Visual direction

The first visual concept uses a dark, modern dashboard style with neon green accents. The UI should feel closer to a technical observability dashboard than a generic fitness app.

Keywords:

```txt
clean
technical
focused
dark
high-contrast
data-driven
calm
```

## Brand

Working name:

```txt
FORMA
```

Product description:

```txt
Your personal fitness operating system.
```

Possible tagline:

```txt
Train, eat and adapt from real data.
```

## Palette

Initial palette suggestion:

```txt
Background: #050807
Surface:    #101613
Card:       #161f1a
Border:     #24342b
Text main:  #f4fff7
Text muted: #98a89d
Accent:     #9dff57
Warning:    #ffd166
Danger:     #ff5f56
```

Do not overuse the accent. It should highlight actions, trends and active states.

## Typography

Recommended approach:

- Use a clean sans-serif font.
- Large numeric metrics should be very readable.
- Dashboard labels should be compact and muted.

## Layout principles

1. The dashboard should answer the weekly status in less than 10 seconds.
2. Use cards for metrics.
3. Show trends, not only current values.
4. Avoid noisy charts.
5. Prefer short recommendations with explanation.

## Main navigation

Suggested sections:

```txt
Dashboard
Body
Training
Nutrition
Shopping
Insights
Settings
```

## Dashboard widgets

Initial dashboard should include:

- Current weight
- Body fat %
- Lean mass
- BMI
- Running progress
- Weekly training completion
- Calories target
- Protein target
- Shopping budget
- Weekly insight

## Controls

Three families, told apart by what they express — not by how they look. Two of
them look alike on purpose, which is exactly why the split has to be explicit.

### `Button` — actions

An emphasis ladder. Pick the rung by asking what the *screen* is for, not what
the button does:

| Variant | Use for | Looks like |
| --- | --- | --- |
| `primary` | The single action the screen exists for. One per screen. | Brand gradient + glow |
| `accent` | A strong action that is not that one. | Flat accent fill |
| `soft` | An action inside a card, accent-coloured but below the card's data. | Accent wash + accent border |
| `surface` | Chrome sitting on the page background, beside icon buttons. | Card fill + neutral border |
| `secondary` | Neutral alternative, e.g. the "Cancelar" beside a confirm. | Neutral outline, transparent |
| `ghost` | Lowest emphasis, in dense or repeated rows. | No border, no fill |
| `destructive` | The confirm of a destructive dialog. | Danger gradient + glow |

Two `primary` buttons on one screen means neither is primary. That is the rule
`accent` exists to relieve.

`tone="danger"` is a separate axis: it recolours the quiet variants (`soft`,
`surface`, `secondary`, `ghost`) into the danger ramp, for a delete that lives
in a table row or a detail panel. `destructive` stays a variant of its own — it
is the loud confirm, not a tone.

### `ButtonLink` — a navigation that looks like a button

Same classes, straight from `Button.module.css`, on a real `<Link>`. Use it
whenever the control navigates: a `<button>` that calls `navigate()` throws away
middle-click, open-in-new-tab and the browser's own link handling, and a screen
reader announces it as an action rather than a destination.

### Overriding a shared control (read this before you do it)

Any class a page hands to `Button`, `IconButton`, `Chip` or `ButtonLink` sits at
the **same (0,1,0) specificity** as the component's own rule. When both declare
the same property, the winner is whichever module lands later in the bundle —
which is not something to leave to chance.

**Double the selector.** That is the whole rule:

```css
/* Doubled: beats the shared control's own rule regardless of bundle order. */
.cta.cta {
  min-height: 0;
  padding: var(--space-2) var(--space-4);
}
```

**`composes` does not solve this**, despite looking like it should. It adds
`.button` *alongside* the composing class instead of ranking that class above
it, so the tie remains — the topbar's login action did this and silently lost
every override, rendering ten pixels taller than the toggle it pairs with,
while the identical arrangement elsewhere happened to win. `composes` is also
only valid on a simple selector, so a composing class cannot be doubled.

Use `composes` only where nothing needs to beat the base — typically an external
`<a href>`, which cannot be a `ButtonLink`. Put its overrides in a second,
doubled class applied alongside (see `.linkPillBase` / `.linkPillSize` in
`pages/admin/panel.module.css`).

Layout regressions of this kind are invisible to jsdom: it resolves neither the
cascade across modules nor media queries. The guards live in
`e2e/layout.spec.ts`.

### `IconButton` — actions with no label

The square glyph control: topbar controls, modal close, table row actions, date
and quantity steppers. `label` is required and becomes the accessible name,
because an icon-only control has no text to name it. `tone="danger"` tints a
row deletion — presentation only, it does not replace a confirmation.

Three independent axes, because the app genuinely needs the combinations:

| Prop | Values | Pick by |
| --- | --- | --- |
| `variant` | `surface` (default), `soft`, `ghost` | How much the control should stand off its background. `soft` is the same accent wash as `Button`'s. |
| `size` | `sm` (32), `md` (default, 40), `lg` (44) | `sm` in dense rows, `md` in chrome, `lg` for a page-level action needing the full touch target. |
| `tone` | `default`, `danger` | Whether the action destroys something. |

### `Chip` — selection

Reports whether an option is **chosen**. Category tabs, chart ranges, pickers.

A selected `Chip` and `Button variant="accent"` are nearly identical to the eye
and mean opposite things: one reports state, the other invites an action. Never
substitute one for the other.

The appearance is shared across groupings, the accessible state is not. Pass
`semantics` to match the wrapper the caller renders:

| Grouping | `semantics` | Publishes |
| --- | --- | --- |
| `role="tablist"` | `tab` | `aria-selected` |
| `role="radiogroup"` | `radio` | `aria-checked` |
| Standalone filter | `toggle` (default) | `aria-pressed` |

`size="md"` (default) is a page-level filter row and keeps the 44px touch
target; `size="sm"` is for a group inside a card, next to the data it filters.

### Not in these families

Text links, clickable cards (`choiceCard`, calendar days) and list rows are
their own thing. They are interactive but they are not buttons, and forcing
them into these components would misreport them.

Every one of these is a shared component under `frontend/src/components/`. A
new button-like control drawn in a page's own CSS module is a bug — that is how
the app ended up with the same pill hand-drawn in three places under three
names.

## Interaction style

Forma should feel direct and calm:

- No gamified confetti.
- No guilt language.
- No manipulative streaks.
- No fake precision.
- Always explain recommendations.

## Example insight copy

Good:

```txt
Body fat is down 0.3% over the last 2 weeks while weight is stable. Keep calories unchanged this week.
```

Bad:

```txt
Amazing! You are crushing it! Keep going!
```

## Late running nutrition UX

Running days need a special visual flow:

```txt
Breakfast -> Lunch -> Pre-run snack -> Run -> Light recovery -> Light dinner
```

The app should make it obvious that carbohydrates move earlier in the day and dinner stays lighter after late runs.

## Mobile considerations

Mobile must prioritize:

1. Today's plan.
2. Add measurement.
3. Mark training completed.
4. Shopping checklist.

Desktop can prioritize full dashboard and charts.
