# Project Guide

## Purpose

This repository is an early-stage personal developer blog/site for `sirnik.Dev`.
Build it as a small, maintainable, server-rendered application. Prefer simple
Spring MVC and FreeMarker solutions over adding client-side application
infrastructure.

This is also a learning project. Its primary learning goals are to understand
htmx and become more proficient with Spring. Optimize collaboration for those
goals, not only for finishing features as quickly as possible.

## Learning and Collaboration

- Explain both what is being changed and why, especially for Spring request
  flow, dependency injection, MVC boundaries, persistence and transactions,
  testing, and htmx request/target/swap behavior. Keep explanations tied to the
  current change rather than giving broad lectures.
- Prefer explicit, idiomatic code that makes the Spring and htmx mechanics easy
  to follow. Avoid abstractions that hide the behavior the project is intended
  to teach.
- When a task is a useful, reasonably bounded exercise in Spring or htmx, do not
  immediately implement it. Push back constructively and offer the user the
  first attempt. State why it is worth practicing, identify the relevant files,
  give a small starting hint and acceptance criteria, then offer to review the
  result or help if they get stuck.
- When pushing back, also give the user a concrete research starting point.
  Suggest the specific concept or search phrase to use and where to look first,
  favoring official Spring, htmx, FreeMarker, Java, or other primary
  documentation over generic tutorials. Do not simply tell the user to search;
  provide enough vocabulary for them to recognize the relevant documentation.
- Implement directly when the user asks you to after that handoff, when they are
  blocked, or when the work is mostly repetitive plumbing rather than a useful
  learning exercise. Never withhold context needed for them to make progress.
- Treat CSS, visual styling, and routine Bulma composition as implementation
  work Codex may handle autonomously. Do not turn styling into an exercise unless
  the user requests it; explain only decisions that materially affect layout,
  responsiveness, accessibility, or maintainability.

## Current Stack

- Java 21
- Spring Boot 4.1.x
- Spring Web MVC
- Spring Data JPA and Hibernate
- Flyway database migrations
- H2 for local development and tests; PostgreSQL for production
- FreeMarker templates (`.ftl`)
- Bulma CSS loaded from a CDN
- htmx loaded from a CDN for progressive, server-driven interactions
- Maven Wrapper; no separate Node or frontend build
- JUnit 5 and Spring MockMvc for tests

Treat `pom.xml` as the source of truth for exact dependency versions and enabled
features. Spring Security is currently commented out; do not design against it
or enable it unless the task requires it.

## Repository Map

- `src/main/java/dev/sirnik/blog/BlogApplication.java`: application entry point
- `src/main/java/dev/sirnik/blog/controllers/`: Spring MVC controllers
- `src/main/java/dev/sirnik/blog/models/`: JPA entities
- `src/main/java/dev/sirnik/blog/repositories/`: Spring Data repositories
- `src/main/java/dev/sirnik/blog/utils/`: small reusable utilities
- `src/main/resources/templates/`: FreeMarker page and fragment templates
- `src/main/resources/db/migration/`: Flyway schema migrations
- `src/main/resources/application.properties`: Spring configuration
- `src/main/resources/application-prod.properties`: PostgreSQL production profile
- `src/test/java/dev/sirnik/blog/`: application and MVC tests
- `pom.xml`: dependencies, Java version, and build configuration

Keep Java code under the `dev.sirnik.blog` package. Use the plural
`controllers` package for controllers.

## Architecture and Implementation

- Keep controllers thin: map requests, validate/prepare inputs, populate the
  model, and select a view.
- Put reusable business behavior in focused service/domain classes as the site
  grows rather than embedding it in controllers or templates.
- Render full pages with FreeMarker. For htmx requests, prefer small server-
  rendered fragment templates that can also degrade gracefully when practical.
- Use semantic HTML and accessible interactions. Preserve keyboard access,
  visible focus states, labels, heading order, and useful alternative text.
- Reuse Bulma conventions before introducing custom CSS. If custom styles or
  scripts grow beyond a few local rules, place them under
  `src/main/resources/static/` rather than expanding inline blocks.
- Keep browser JavaScript minimal. Do not introduce a SPA framework, Node build,
  or new production dependency without a clear requirement.
- Keep credentials, tokens, and machine-specific configuration out of the
  repository. Use environment-backed Spring properties for secrets if they are
  introduced later.
- Change the database schema only through additive Flyway migrations. Do not edit
  an already-applied migration or use Hibernate schema updates as a substitute.
- Keep migrations compatible with both PostgreSQL and H2's PostgreSQL mode unless
  a database-specific migration is deliberately introduced and tested.

## Working Conventions

- Inspect nearby code before changing structure or naming; this codebase is
  small and evolving.
- Make the narrowest coherent change that completes the task. Avoid unrelated
  rewrites or speculative abstractions.
- Preserve existing user work and do not revert unrelated working-tree changes.
- Keep templates readable. Extract repeated page sections into FreeMarker
  includes/macros once repetition appears.
- Keep user-facing copy and MVC assertions in sync. When intentionally changing
  rendered text, update tests that assert the old text.
- Add or update tests for new routes, response status, selected views, model
  attributes, and important rendered content.
- Use the Maven Wrapper (`./mvnw`) so builds use the repository's pinned Maven
  version.

## Common Commands

```sh
./mvnw spring-boot:run
./mvnw test
./mvnw clean verify
```

The local application is normally available at `http://localhost:8080`.

## Verification

For code or template changes:

1. Run `./mvnw test`.
2. For visible UI changes, start the app and inspect the affected page at desktop
   and narrow/mobile widths.
3. Exercise both normal navigation and any htmx-specific request path that was
   changed.
4. Report any pre-existing or environment-related failure separately; do not
   hide it by weakening unrelated assertions.

Documentation-only changes do not require a full Maven build unless they alter
commands or technical claims about the application.

## Definition of Done

A change is complete when it follows the server-rendered architecture, keeps
tests and rendered behavior aligned, passes the relevant Maven checks, and does
not introduce avoidable dependencies or regressions in accessibility and mobile
layout.
