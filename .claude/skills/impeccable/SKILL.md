---
name: impeccable
description: "Use to critique, audit, polish or refine an existing UI: app screens, components, native iOS/Android screens. Evaluates hierarchy, typography, spacing, color, clarity, accessibility and edge cases, then proposes and applies fixes."
version: 4.9.0
user-invocable: true
argument-hint: "[critique|audit · polish · layout|typeset|colorize|bolder|quieter|distill|animate|delight|overdrive · clarify|adapt|harden|onboard|optimize] [target]"
license: Apache 2.0
---

> **Local changes to the upstream skill (see ../SOURCES.md).** This is a trimmed copy of Impeccable 4.9.0 (Apache 2.0, https://github.com/pbakaus/impeccable, commit `c4cd192`).
> - **Only the text playbooks are included.** The `impeccable` launcher/engine (it downloads a native binary), the hooks, the live browser server, the sub-agents and the build-phase gates are **not** installed, on purpose. Whenever a reference says to run `scripts/impeccable <verb>` (`context`, `detect`, `critique-storage`, `audit-storage`, `live-server`, ...), **skip that step**: do not try to download, install or run an engine, and do the same check by reading the code and the screenshots the user provides. Say so in one line the first time you skip one.
> - The new-work flow (`shape`, `init`, `document`, `extract`, `live`, `generate`, `craft`) is not included. For new screens use the `frontend-design` and `ui-ux-pro-max` skills.
> - **This repository is a native Android app** (Kotlin, Jetpack Compose, Arabic / RTL). Prefer the `*.native.md` variants and `reference/android.md`. Web-only advice (CSS, ARIA, browsers) does not apply: translate the principle to Compose (`semantics`, `contentDescription`, 48dp touch targets, `MaterialTheme` tokens, `LayoutDirection.Rtl`). Use it for this app only, not for the web app (a separate repository).

This skill gives you the tools and permission to improve design the way an award-winning design director would: a clear point of view, a deep grasp of what the users need, and exceptional craft.

Core principles:
- Finish the job. No hedging, no shortcuts.
- Scale ambition to the surface's mode (see Modes). On operating tools (this app is one) ambition shows as precision and restraint, and patterns users already know earn their place.
- Verify by looking at the rendered result when screenshots are available; when they are not, say that the review is from code and what could not be judged (real contrast, real spacing, real text wrapping).

## Setup

1. Read the project's existing context directly if it exists (`PRODUCT.md`, `DESIGN.md`); never invent missing context. Do not run any launcher.
2. Load the request's playbook from the Commands table. Inspect the target and the incumbent look before editing.
3. Immediately before any UI edit, including small refinements, read [reference/craft-floor.md](reference/craft-floor.md): the quality floor, the absolute bans and the reflexes no detector catches. Do not load it for planning-only work.

## How to design

- **The brief wins.** Honor pinned aesthetics, fonts and palettes even when they conflict with a warning. Redirecting a clear brief toward your taste is failure.
- **Refinement preserves; redesign replaces.** Refinement keeps the incumbent identity, behavior, copy, and everything outside scope. Ask before replacing factual copy or adding claims.
- **Loaded symbols stay out of the decoration.** Never use emblems tied to militarism, supremacy or hate movements as motifs, badges or ornament.

## Modes

The mode names what the visitor's success looks like on this surface. This app is **Operate**: the user completes a task (committees, exams, stages, ceremonies). Scanability, consistency, native expectations and the real usage scene outrank expression. See [reference/operate.md](reference/operate.md), [reference/mode-operate.md](reference/mode-operate.md); the other modes are in [reference/mode-persuade.md](reference/mode-persuade.md) and [reference/mode-read.md](reference/mode-read.md).

## Commands

| Command | Category | Description | Reference |
|---|---|---|---|
| `critique [target]` | Evaluate | UX design review with heuristic scoring | [reference/critique.md](reference/critique.md) |
| `audit [target]` | Evaluate | Technical quality checks (a11y, perf, responsive) | [reference/audit.md](reference/audit.md) · native: [reference/audit.native.md](reference/audit.native.md) |
| `polish [target]` | Refine | Final quality pass before shipping | [reference/polish.md](reference/polish.md) |
| `bolder [target]` | Refine | Amplify safe or bland designs | [reference/bolder.md](reference/bolder.md) |
| `quieter [target]` | Refine | Tone down aggressive or overstimulating designs | [reference/quieter.md](reference/quieter.md) |
| `distill [target]` | Refine | Strip to essence, remove complexity | [reference/distill.md](reference/distill.md) |
| `harden [target]` | Refine | Production-ready: errors, i18n, edge cases | [reference/harden.md](reference/harden.md) |
| `onboard [target]` | Refine | First-run flows, empty states, activation | [reference/onboard.md](reference/onboard.md) |
| `animate [target]` | Enhance | Purposeful animation and motion | [reference/animate.md](reference/animate.md) |
| `colorize [target]` | Enhance | Strategic color for monochromatic UIs | [reference/colorize.md](reference/colorize.md) |
| `typeset [target]` | Enhance | Typography hierarchy and fonts | [reference/typeset.md](reference/typeset.md) |
| `layout [target]` | Enhance | Spacing, rhythm and visual hierarchy | [reference/layout.md](reference/layout.md) |
| `delight [target]` | Enhance | Personality and memorable touches | [reference/delight.md](reference/delight.md) |
| `overdrive [target]` | Enhance | Push past conventional limits | [reference/overdrive.md](reference/overdrive.md) |
| `clarify [target]` | Fix | UX copy, labels and error messages | [reference/clarify.md](reference/clarify.md) |
| `adapt [target]` | Fix | Different devices and screen sizes | [reference/adapt.md](reference/adapt.md) · native: [reference/adapt.native.md](reference/adapt.native.md) |
| `optimize [target]` | Fix | Diagnose and fix UI performance | [reference/optimize.md](reference/optimize.md) |

Platform references: [reference/android.md](reference/android.md) (Material 3), [reference/ios.md](reference/ios.md).

Routing: when the user explicitly or clearly asks to run a command, load its reference (the native variant on native platforms) and follow it. Ask once if two commands fit. Otherwise treat the request as general design work on the incumbent implementation.
