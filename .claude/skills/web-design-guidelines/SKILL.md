---
name: web-design-guidelines
description: Review UI code for Web Interface Guidelines compliance. Use when asked to "review my UI", "check accessibility", "audit design", "review UX", or "check my site against best practices".
metadata:
  author: vercel
  version: "1.0.0"
  argument-hint: <file-or-pattern>
---

# Web Interface Guidelines

Review files for compliance with Web Interface Guidelines.

> **Local changes to the upstream skill (see ../SOURCES.md):**
> 1. The rules are read from the pinned copy `references/guidelines.md` (upstream commit `434b7f9`), not fetched from the network at review time. A rule file that changes by itself on every run is untrusted input.
> 2. These rules were written for web code (HTML/CSS/React). In this repository the UI is Kotlin / Jetpack Compose, so apply the **principles** (accessible names, visible focus and state, forms, empty states, long content, destructive-action confirmation, locale-aware dates and numbers, copy) and **skip web-only rules** (`aria-*`, CSS, `<a>`/`<button>` markup, hydration, `<meta>`). Android equivalents: `contentDescription` / `semantics`, 48dp minimum touch targets, `Modifier.clickable(role = ...)`, `KeyboardOptions`, system font scale and RTL layout direction.

## How It Works

1. Read `references/guidelines.md` (next to this file).
2. Read the specified files (or ask the user for files/pattern).
3. Check against the rules that apply to the platform (see the note above).
4. Output findings in the terse `file:line` format described in `references/guidelines.md`.

## Usage

When a user provides a file or pattern argument:
1. Read `references/guidelines.md`.
2. Read the specified files.
3. Apply the rules that apply to the platform.
4. Output findings using the format specified in the guidelines.

If no files specified, ask the user which files to review.
