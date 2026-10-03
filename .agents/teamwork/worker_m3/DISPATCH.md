## 2026-10-03T15:33:21Z
You are Worker 3 (teamwork_preview_worker) for Milestone M3 (R2: Editorial UI/UX Refactor & Micro-interactions in `src/main/resources/static/index.html`).

Working directory: e:\News\News-Aggregator-Tracker\.agents\teamwork\worker_m3
Authoritative user request: e:\News\News-Aggregator-Tracker\.agents\teamwork\ORIGINAL_REQUEST.md
Project plan: e:\News\News-Aggregator-Tracker\.agents\teamwork\PROJECT.md
Audit Report: e:\News\News-Aggregator-Tracker\DEEP_UX_AUDIT_REPORT.md
Relevant skills:
- e:\News\News-Aggregator-Tracker\.agents\skills\taste-skill\SKILL.md
- e:\News\News-Aggregator-Tracker\.agents\skills\redesign-skill\SKILL.md
- e:\News\News-Aggregator-Tracker\.agents\skills\minimalist-skill\SKILL.md

MANDATORY FIRST STEP: Read e:\News\News-Aggregator-Tracker\.agents\teamwork\ORIGINAL_REQUEST.md, PROJECT.md, and DEEP_UX_AUDIT_REPORT.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your mission:
Directly refactor and elevate `src/main/resources/static/index.html` to fulfill all R2 requirements with zero AI-slop, elite editorial journalistic aesthetics, fluid gestures (<100ms), and WCAG AAA compliance:

1. WCAG AAA HIGH CONTRAST TOKENS (Light Mode & Dark Mode):
   - Replace brittle `!important` style overrides with structured CSS Custom Properties `:root` (Dark) and `html.light` (Light):
     * Canvas, Surface, Card, Border, Primary Text, Secondary Text, Muted Text, Accent.
     * Ensure text contrast ratios strictly achieve WCAG AAA (>= 7.0:1 for body and meta text; >= 3.0:1 for borders and UI elements) across BOTH themes.
     * Search input, category pills, side navigation, card text must all be crisp and legible without muddy grays.

2. ASYMMETRIC EDITORIAL BENTO GRID:
   - Upgrade the Bento Grid view (`renderBentoGrid` / `createBentoGridCard`):
     * Abandon the uniform 3-col repetitive cards.
     * Implement editorial visual pacing: Lead Story Card (2x2 span with hero image, prominent headline, core takeaway), Spotlight Cards (1x2 / 2x1 span), Quick Bites (1x1 span), and Storyline ribbons.
     * Wire up "Mạch tin" (Timeline) buttons and `✨ Khám phá` serendipity badges on Bento cards.

3. FLUID GESTURE & SCROLL PERFORMANCE (<100ms response):
   - Remove `{ passive: false }` from `touchmove` and `wheel` listeners so the browser's Compositor Thread is never blocked.
   - Replace linear `setInterval(100ms)` progress bar width manipulations with a GPU-accelerated Achievement Horizon glow and CSS transforms.
   - Smooth card transitions using `transform: translate3d` and CSS transitions with `will-change`.

4. DELIGHTFUL TACTILE MICRO-INTERACTIONS:
   - Kinetic Double-Tap to Like with bursting heart particles (CSS keyframe explosion or micro-canvas burst).
   - Reading Achievement Indicator: soft ambient glow/badge when reading completion threshold is reached ($D \ge 0.65 T_{standard}$).
   - Dedicated Bookmark / Save functionality with instant visual confirmation.
   - Seamless Story Arc Timeline Drawer (`#timelineModal`): smooth slide-in displaying chronological event milestones (Genesis, Progression, Latest) with interactive article links.
   - Seamless crossfade transitions when switching between One-Card Reels and Bento Grid views.

5. PRESERVE & ENHANCE M2 ALGORITHMIC FEATURES:
   - Retain `ThompsonSamplingBandit`, `calculateSerendipityScore`, dynamic slotting (70% MAB, 20% Serendipity with `✨ Khám phá`, 10% Trending), and `normalizeArticle`.

6. VERIFICATION:
   - Run `.\mvnw.cmd test-compile` to ensure 0 errors.
   - Verify JS syntax and runtime integrity.
   - Write comprehensive handoff report to `e:\News\News-Aggregator-Tracker\.agents\teamwork\worker_m3\handoff.md`.
   - Notify orchestrator via send_message when complete.
