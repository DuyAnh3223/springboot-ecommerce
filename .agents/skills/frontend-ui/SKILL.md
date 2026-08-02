---
name: frontend-ui
description: Implement or review Next.js UI components, forms, hooks, client state, Server/Client Component boundaries, async actions, or Vietnamese customer-facing copy in ABTechZone.
---

# Frontend UI Skill

Use this skill only for UI/component work. Keep changes focused and follow the nearest `client/AGENTS.md`.

## Component design

- Keep each component focused and readable. Split files when a component becomes difficult to reason about; do not split mechanically just to reduce line count.
- Prefer a small prop object over many independent primitive props.
- Props passed through a component without being used there indicate prop drilling; use a use-case store only when non-adjacent components truly share the state.
- Reuse existing shared hooks and utilities before creating new ones.

## State placement

1. Pure transformation with no React state/effects → plain function in `utils/`.
2. Validated form fields → React Hook Form; do not mirror them in local state or Zustand.
3. Logic used by one component subtree → local state or a focused custom hook.
4. Generic reusable client logic → `client/shared/hooks/`.
5. State shared by non-adjacent components in one audience use-case → audience-scoped Zustand store.
6. Server-owned lists and entities → fetch from the server; do not use Zustand as an API cache.

## Async actions

Use the canonical `useAsyncAction` from `client/shared/hooks/` for handlers that invoke server actions and need loading/error state. Do not duplicate hand-written loading/error/finally wrappers unless the behavior genuinely differs.

## Server, client and actions

- Server Components are the default for pages, layouts and data display.
- Add `"use client"` only for state, effects, refs, event handlers, browser APIs or client-only animation.
- Server Actions are for mutations and server-only operations: create/update/delete, authentication, cookies, redirects, revalidation and uploads.
- GET data should normally be read through a service from a Server Component, not through a pass-through Server Action.
- Services are the only layer that calls the backend API.
- Search/filter/sort/pagination should use URL state or an interactive client flow, not a Server Action merely to fetch GET data.

## UI language and reuse

- New UI copy and displayed data must be Vietnamese.
- Use `client/shared/hooks/` and `client/shared/utils/` for logic shared by multiple features.
- Keep Admin UI and Customer UI boundaries intact.

## Review checklist

- Is the component scope appropriate and behavior unchanged outside the request?
- Are validated fields owned by React Hook Form only?
- Is Zustand state truly shared beyond a direct parent/child relationship?
- Is `useAsyncAction` reused where appropriate?
- Are Server/Client boundaries minimal?
- Are imports using the canonical shared hook path and correct audience boundary?
