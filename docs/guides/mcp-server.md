# MCP Server

The `mcp` module is a [Model Context Protocol](https://modelcontextprotocol.io) server over stdio — it exposes generic workflow CRUD + execute tools to an agent (Claude Desktop, Claude Code, etc.), with no template- or node-type-specific shortcuts. Every tool operates on an arbitrary `WorkflowDefinition` by id or raw JSON.

It embeds the engine directly (`createGraphynServerRuntime()` + `FileWorkflowStore`, the same `~/.graphyn/workflows` store the desktop editor uses) — no running `:server` process required.

---

## Quick start

```bash
./gradlew :mcp:installDist
```

This produces a runnable binary at `mcp/build/install/mcp/bin/mcp`. Add it to your MCP client config — for a project-level `.mcp.json`:

```json
{
  "mcpServers": {
    "graphyn-workflows": {
      "command": "./mcp/build/install/mcp/bin/mcp",
      "args": []
    }
  }
}
```

For Claude Desktop, add the equivalent entry to its own config with an absolute path to the binary.

---

## Tools

| Tool | Description |
|---|---|
| `workflow_list` | List all stored workflows (id, name, timestamps, version count) |
| `workflow_get` | Fetch a workflow's full definition by id |
| `workflow_publish` | Save/update a workflow from raw `WorkflowDefinition` JSON — validates before saving |
| `workflow_delete` | Delete a workflow and its version history |
| `workflow_execute` | Run a stored workflow by id, with optional `overrides` (per-node config) and `async` |
| `workflow_execution_status` | Poll buffered progress/result frames for an `async` run |
| `workflow_list_node_types` | List every registered node type — for authoring `workflow_publish` payloads |

`workflow_publish` and `workflow_execute`/`workflow_delete` are annotated `destructiveHint`/`openWorldHint` in their `ToolSchema` — the protocol's own mechanism for flagging that they run against the real engine (see [Unsandboxed execution](#unsandboxed-execution) below), not a custom permission layer on top.

---

## Discriminator note

`workflow_publish`'s `workflow` JSON uses a `"kind"` discriminator for `WorkflowValue` fields (it goes through the same `DefaultWorkflowJsonCodec` the `:server` HTTP routes use):

```json
{"kind": "string", "value": "hello"}
```

`workflow_execute`'s `overrides` argument uses a different, plain `Json` instance with the default `"type"` discriminator instead:

```json
{"type": "string", "value": "hello"}
```

This split mirrors a real difference already in `:server` itself (`GraphynWorkflowJson` vs. the plugin's own default `Json {}`) — not something introduced by the MCP layer.

---

## Configuring which plugins load

By default `:mcp` installs Shorts, MediaCore, MediaAi, and StableDiffusion on top of the base runtime plugins (Control, ListOps, Types, Text, Io, Json, Preview). Trim or reorder with `GRAPHYN_MCP_PLUGINS` — comma-separated plugin names, or the literal `all`:

```json
{
  "mcpServers": {
    "graphyn-workflows": {
      "command": "./mcp/build/install/mcp/bin/mcp",
      "args": [],
      "env": { "GRAPHYN_MCP_PLUGINS": "shorts,media-core" }
    }
  }
}
```

An unknown plugin name fails fast at startup with the available list rather than silently being ignored. `StableDiffusionPlugin()`'s default backend shells out to a local `sd-cli` binary (`SD_CLI_PATH` env var) — swap in `com.ronjunevaldoz.graphyn.plugins.stablesd.http.HttpStableDiffusionBackend` in `Main.kt` if your SD generation runs on a remote server instead.

---

## Unsandboxed execution

`workflow_publish` and `workflow_execute` run against the same production node executors as `:server` and the editor — including `script.eval` (arbitrary Kotlin) and `io.file_write`/`io.http_request` (filesystem/network access). There is no sandbox and no per-call confirmation gate; this is the same trust boundary as running the `:mcp` process itself. If you need to restrict what an agent can publish or run, that would need an allow-list built on top — not something `:mcp` enforces today.

---

## Node authoring

Since there's no node-catalog UI available over MCP, `workflow_list_node_types` is the way an agent discovers valid node types and ports before hand-authoring a `workflow_publish` payload from scratch — or start from `workflow_get` on an existing workflow and edit that JSON instead.
