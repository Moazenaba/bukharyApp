# Design skills used on this app (Android / Jetpack Compose) only

These four skills live in this repository so they load for any session that works on **bukharyApp** and **not** for the web app (a separate repository). They are third-party instructions: they were read in full before being copied, and nothing was installed or run from their repositories. No network access, no hooks, no executables.

| Skill | Source | Commit | License |
|---|---|---|---|
| `ui-ux-pro-max` | https://github.com/nextlevelbuilder/ui-ux-pro-max-skill (`.claude/skills/ui-ux-pro-max`) | `6ae86f61f520d6ca229d2f40e070410638c908c5` | MIT |
| `frontend-design` | https://github.com/anthropics/skills (`skills/frontend-design`) | `2c5958ecac7cee2b583967a2c4981d0f4a75c579` | Apache 2.0 (`LICENSE.txt`) |
| `impeccable` | https://github.com/pbakaus/impeccable (`.claude/skills/impeccable`) | `c4cd1924c389f68d37c687d9c2bbf13f12547c41` | Apache 2.0 (`LICENSE`, `NOTICE.md`) |
| `web-design-guidelines` | https://github.com/vercel-labs/agent-skills (`skills/web-design-guidelines`) + https://github.com/vercel-labs/web-interface-guidelines | `1a2c79d4e6c4a2283dc2c614b6538cbe1ed5ca62` + rules at `434b7f91364665f2f733b310ec54809bf8f37937` | MIT |

## Changes made to the originals

- **`ui-ux-pro-max`**
  - The script path in `SKILL.md` is project-relative (`.claude/skills/ui-ux-pro-max/scripts/search.py`); upstream used `${CLAUDE_PLUGIN_ROOT}`, which only exists when installed as a plugin.
  - Only what the search needs is included: `SKILL.md`, `references/`, the 4 runtime scripts (`search.py`, `core.py`, `design_system.py`, `reasoning_contract.py`; Python standard library only, they write files only when `--persist` is passed) and the data CSVs. Left out: tests, `validate_data.py`, and the catalog-maintenance JSON files.
  - Has a `jetpack-compose` stack and RTL search (`--rtl`; it cannot be combined with `--design-system`).
- **`frontend-design`**: unchanged.
- **`impeccable`**: trimmed to the text playbooks for evaluating and refining a UI (`critique`, `audit` and its native variant, `polish`, `layout`, `typeset`, `colorize`, `clarify`, `adapt`, `harden`, ...) plus `android.md`. **Not included**: the `impeccable` launcher/engine (it downloads a native binary), `settings.json` hooks, the live browser server, the sub-agents, and the new-work / build-phase flow. `SKILL.md` was rewritten for that and says to skip any step that runs `scripts/impeccable ...`. Two links in `critique.md` (`live.md`, `new-work.md`) therefore point to files that are not here.
- **`web-design-guidelines`**: upstream fetches its rules from the network on every run. Here the rules are a pinned copy (`references/guidelines.md`, from the commit above) and `SKILL.md` reads that file; it also says which rules are web-only and what the Android equivalents are.

## Fit for this app

All four were written for web UIs. The principles apply to this app (contrast, hierarchy, spacing, touch targets, empty and error states, forms, long content, RTL); web-only rules (CSS, ARIA, browsers, hydration) do not, and must be translated to Compose (`semantics`/`contentDescription`, 48dp targets, `MaterialTheme` tokens, `LayoutDirection.Rtl`).

## Suggested order for a review

1. **Visual:** `impeccable` (critique, layout, typeset) + `frontend-design`.
2. **UX:** `ui-ux-pro-max` (`--domain ux`, `--stack jetpack-compose`, `--domain style --rtl`) + `web-design-guidelines`.
3. **Code:** a separate engineering review of the Compose code, kept apart from the design review.

## Updating

Re-copy from the commits above (or newer ones, after reading the diff), and update this file. Never add a skill that fetches instructions or binaries at run time without pinning and reviewing them.
