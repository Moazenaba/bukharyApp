# «الواجهات 2»: one plan from four reviews

Sources:
- `frontend-design` (Anthropic)
- `impeccable` (critique + audit, native)
- `web-design-guidelines` (translated to Compose)
- `ui-ux-pro-max` (UX data + Compose stack)
- `taste-skill`, used for its system rules only (one accent, 90–95% neutral, one radius family, one filled button per view)

The owner's verdict on 1.1.21: it works, but the loud contrasting colours look cheap. The target is soft, rounded and smooth, a premium phone app in the iOS style.

## Direction: «هادئ وفاخر»

- **Colour:** the interface is 90–95% neutral, from warm or cool off-white paper, white sheets and near-black warm ink. Each theme has **one** refined accent colour. Because the accent is the only colour on screen, the main button is still the one spot you can find with a 90% blur, without shouting.
- **Shape:** one radius family:
  - 24dp for sheets and groups
  - 28dp for the hero
  - capsule (fully round) for buttons, chips, tags and the tab bar
  - circles for marks and avatars
- **Depth:** no hairline borders on cards. Separation comes from a soft, low shadow on the sheets (light themes) or a lighter surface (dark themes).
- **Buttons:**
  - Exactly one filled accent button per view.
  - The secondary button is *tonal*: ink at about 7% as the fill, with ink text. It is never a white slab on dark themes.
  - Buttons press with a small spring scale (0.97).
- **Type:**
  - large title 30sp
  - hero 22sp
  - heading 18sp
  - **body 16sp**
  - small 14sp
  - nothing smaller than 13sp
- **Tab bar:** a floating capsule over the content with a soft shadow. The current tab shows its icon and label in the accent colour.
- **Hero («الآن»):** in light themes, a soft accent-tinted card with ink text and the filled accent button. In dark themes, a lifted surface with an accent tint.

## Behaviour (P0/P1, all four reviews agree)

1. The Now card asks before a one-tap stage step, the same as the committee screen.
2. The hero picks what matters: an action I can do today, then today's exam, then a ceremony, then a task (late items first).
3. Marking a task done offers «تراجع» in a message.
4. Dialogs take the theme's colours (`surfaceContainer*` set).
5. The theme tests change for the new rule:
   - one accent
   - the hero is calm
   - secondary buttons are tonal and not loud
   - the accent stands at 3:1 or more on paper and on the hero
6. Arabic counts use a plural helper («مهمة واحدة / مهمتين / 3 مهام»).
7. Stage counts are no longer drawn like committee numbers.
8. The avatar initial skips «الشيخ / ال».

## Later rounds (impeccable command sweep on the new screenshots)

Commands, in order:
1. critique
2. audit
3. layout
4. typeset
5. colorize
6. clarify
7. adapt
8. harden
9. animate
10. delight
11. distill
12. quieter
13. bolder
14. optimize
15. onboard
16. overdrive
17. craft-floor
18. polish

Each runs against the CI screenshots of the new build. Its findings are applied, and the next one runs.

Known items for those rounds:
- one lazy item per row
- load-error states with retry
- busy states on buttons
- a date picker for the observer end date
- search in the people pickers
- TalkBack state on stages and pickers
- text clipping at 200% font size
