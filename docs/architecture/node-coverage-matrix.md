# Node Coverage Matrix

Tracks test and implementation completeness across all registered node types.

**Legend**
- ✅ covered / implemented
- ❌ missing
- ➖ not applicable

| Node Type | Plugin | Executor | Unit Test | UI Test (Roborazzi) | Canvas Card | Inputs | Outputs | Notes |
|---|---|---|---|---|---|---|---|---|
| `math.add` | sample-math | ✅ | ✅ | ❌ | ❌ (default) | 2 | 1 | |
| `math.subtract` | sample-math | ✅ | ✅ | ❌ | ❌ (default) | 2 | 1 | |
| `math.multiply` | sample-math | ✅ | ✅ | ❌ | ❌ (default) | 2 | 1 | |
| `sample.logger` | sample-logger | ✅ | ✅ | ✅ | ❌ (default) | 1 | 1 | |
| `stylenodes.ksampler` | sample-style-nodes | ✅ | ❌ | ✅ | ✅ DarkHeaderCard | 4 | 1 | required inputs have no defaults — demo scenes show missing_required_input |
| `stylenodes.distribute_points` | sample-style-nodes | ✅ | ❌ | ✅ | ✅ FieldCard | 5 | 1 | |
| `stylenodes.webhook` | sample-style-nodes | ✅ | ❌ | ✅ | ✅ CircleCard | 0 | 1 | |
| `graphyn.sticky_note` | sticky-notes | ➖ | ❌ | ❌ | ✅ annotation | 0 | 0 | annotation-only, no executor needed |
| `control.branch` | control | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 2 | condition port is BooleanType |
| `control.merge` | control | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 1 | |
| `control.loop` | control | ✅ | ❌ | ❌ | ✅ FieldCard | 1 | 2 | item + index outputs; outputs NullValue with no input |
| `listops.zip` | list-ops | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 1 | output port is `result`, not `pairs` |
| `listops.map` | list-ops | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 1 | fn handle port has no executor-side impl yet — returns empty list |
| `listops.filter` | list-ops | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 1 | same as map — fn handle unimplemented |
| `listops.reduce` | list-ops | ✅ | ❌ | ❌ | ✅ FieldCard | 3 | 1 | fn handle unimplemented |
| `text.format` | text | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 1 | returns literal `{value}` when template port not wired |
| `text.split` | text | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 1 | |
| `text.regex` | text | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 2 | empty pattern always matches — misleading default |
| `types.schema` | types | ✅ | ❌ | ❌ | ✅ FieldCard | 1 | 1 | returns empty RecordValue with no config |
| `types.cast` | types | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 1 | returns NullValue if target type or value missing |
| `types.validate` | types | ✅ | ❌ | ❌ | ✅ FieldCard | 2 | 2 | |
| `io.http_request` | io | ✅ | ❌ | ❌ | ✅ FieldCard | 4 | 3 | fires real HTTP — no mock; blank URL → connection refused |
| `io.file_read` | io | ✅ | ❌ | ❌ | ✅ FieldCard | 1 | 2 | blank path → empty content, exists=false |
| `io.file_write` | io | ✅ | ❌ | ❌ | ✅ FieldCard | 3 | 1 | content port is OpaqueType → matches every output |
| `demo.subgraph` | app/demo (SubgraphRuntimePlugin) | ✅ | ❌ | ❌ | ✅ SubgraphCard | 1 | 1 | bug: inner workflow's last node output leaks as pipeline output instead of mapped `output` port |

## Summary

| Category | Count |
|---|---|
| Total node types | 24 |
| With executor | 23 (sticky_note is annotation-only) |
| With unit tests | 4 (math.×3, sample.logger) |
| With UI / Roborazzi tests | 4 (sample.logger, stylenodes.×3) |
| With custom canvas card | 20 |
| Without custom card (uses default) | 4 (math.×3, sample.logger) |

## Port Compatibility Notes

| Connection | From type | To type | Compatible? | Notes |
|---|---|---|---|---|
| `io.http_request body` → `listops.zip listA` | StringType | ListType | ❌ | Groups scene type mismatch — needs a parse/split adapter node |
| `io.http_request body` → `io.file_write content` | StringType | OpaqueType | ✅ | OpaqueType as *expected* accepts anything |
| `listops.*.result` → `listops.*.list` | ListType | ListType | ✅ | |
| `control.branch truePath/falsePath` → `control.merge a/b` | OpaqueType | OpaqueType | ✅ | |
| `demo.subgraph output` → `io.file_write content` | OpaqueType | OpaqueType | ✅ | but executor bug means output port never carries real value |

## Gaps to Address

1. **Unit tests**: every plugin executor needs at least one happy-path + one edge-case test in `commonTest`
2. **UI tests**: only style-nodes and logger have Roborazzi screenshots — all FieldCard-based nodes need a screenshot baseline
3. **`io.http_request`**: executor makes real network calls — needs a mock/stub strategy for tests
4. **`listops.map/filter/reduce`**: fn handle port is unimplemented in executors — currently passes through empty lists
5. **`demo.subgraph`**: output port mapping bug — inner workflow result leaks instead of mapping to declared `output` port
6. **`text.regex`**: empty pattern always returns `matched=true` — executor should validate pattern before running
7. **Groups scene**: needs a `text.split` or `types.cast` node wired between `http_request.body` and `listops.zip.listA`
