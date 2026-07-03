# Engineering Lessons

Short index of durable lessons discovered while building Graphyn. Keep the canonical guidance in the code or in the more specific architecture/reference docs linked below.

## Workflow and AI

- Canonical settings keys should be snake_case, with legacy aliases only for compatibility.
- AI-generated workflows should emit `nodePositions`; auto-layout should stay a fallback.
- AI assistants are more useful when they can see node descriptions and layout state.
- Shorts prompt shaping belongs in a real node contract, not inline `script.eval` glue.
- Reusable subgraphs work best when they expose one clean boundary value.
- `media.video_stitch` needs video clips, not stills; generate clips first, then stitch.
- Nullable workflow fields should round-trip as `NullValue`, not empty strings.
- Script-based media templates need the script plugin in the JVM runtime bundle.
- Subgraph boundary ports still need explicit validation handling when they are intentionally injected.

## Stable Diffusion and Media Runtime

- Stable Diffusion workers should be selected by settings, not hardcoded hostnames.
- Keep the worker adapter behind one HTTP client and vary only URL plus API key per environment.
- Readiness, job polling, and cancelation checks should stay aligned with the worker OpenAPI contract.

## Catalog and Layout

- Launcher catalogs need explicit badge priority once recency and status both matter.
- Layout or zoom heuristics should preserve a consistent inset so graphs do not start on the border.

## Publishing and Gradle

- `implementation()` deps can still leak into the POM in KMP; published modules must be audited.
- The publishing audit should check both directions: listed modules apply the convention, and convention users are listed.
- `MavenPublishBaseExtension.coordinates()` is a setter, not a readable property.
- Retrying a partially published version on Maven Central can conflict with the existing deployment.

## State and Layout Bugs

- Never write from a fallback value that differs from the real initial state.
- Keep viewport and auto-layout limits aligned so manual zoom and layout behavior feel consistent.

