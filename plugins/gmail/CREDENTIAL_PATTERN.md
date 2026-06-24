# Gmail Integration: Credential Management Pattern

This document describes the credential management pattern used in the Gmail plugin and how to replicate it for other service integrations (Discord, Telegram, Google Sheets, etc.).

## Overview

**Credentials are environment-resolved, not workflow-embedded.**

Workflows reference credentials by **scope:key** (e.g., `"gmail:main"`), and the execution environment provides the actual credential value at runtime. This allows:

- Same workflow to run on server (using .env) or client (using secure storage)
- Multiple accounts per service (gmail:work, gmail:personal)
- Secure credential isolation from workflow definitions
- Easy rotation and revocation without redefining workflows

## Architecture

### Layer 1: Workflow Definition

```kotlin
// Node inputs reference credentials by key, not value
val gmailFetchEmails = NodeSpec(
    name = "gmail_fetch_emails",
    inputs = listOf(
        PortSpec("credential", StringType, defaultValue = null),  // e.g., "gmail:main"
        PortSpec("label", StringType, defaultValue = "INBOX"),
        PortSpec("limit", IntType, defaultValue = 10),
    ),
    outputs = listOf(
        PortSpec("emails", ListType(...)),
        PortSpec("count", IntType),
    ),
)

// Workflow specifies credential scope:key
val myWorkflow = WorkflowDefinition(
    nodes = listOf(
        WorkflowNode(
            spec = gmailFetchEmails,
            inputs = mapOf(
                "credential" to "gmail:main",      // ← String reference, not secret
                "label" to "INBOX",
                "limit" to 10,
            ),
        ),
    ),
)
```

### Layer 2: CredentialProvider Interface

The executor asks for credentials at runtime via a pluggable interface:

```kotlin
interface CredentialProvider {
    suspend fun getCredential(scope: String, key: String): String?
}
```

Each deployment provides an implementation:

**Server** (`ServerCredentialProvider`):
```kotlin
class ServerCredentialProvider : CredentialProvider {
    override suspend fun getCredential(scope: String, key: String): String? {
        val envKey = "${scope.uppercase()}_${key.uppercase()}"
        return System.getenv(envKey)  // GMAIL_MAIN, DISCORD_PROD, etc.
    }
}
```

**Client** (`ClientCredentialProvider`):
```kotlin
class ClientCredentialProvider : CredentialProvider {
    override suspend fun getCredential(scope: String, key: String): String? {
        return secureStorage.get("$scope:$key")  // Keychain (iOS/Mac), SecurePrefs (Android)
    }
}
```

### Layer 3: Executor Resolution

The executor resolves credentials before executing nodes:

```kotlin
class GmailExecutor(private val credentialProvider: CredentialProvider) {
    suspend fun execute(workflow: WorkflowDefinition): Result {
        for (node in workflow.nodes) {
            // Parse "gmail:main" → scope="gmail", key="main"
            val credentialRef = node.inputs["credential"] as String
            val credential = credentialProvider.getCredential(scope, key)
            
            // Execute node with resolved credential
            val result = executeNode(node, credential, ...)
        }
    }
}
```

## Environment Variable Naming

**Pattern:** `{SCOPE}_{KEY}`

Examples:
```bash
# Gmail
export GMAIL_MAIN="ya29.a0AfH6..."      # OAuth token for personal account
export GMAIL_WORK="ya29.a0AfH6..."      # OAuth token for work account

# Discord
export DISCORD_PROD="NzI4NjIyODk..."     # Bot token for production
export DISCORD_DEV="NzI4NjIyODk..."      # Bot token for dev/testing

# Google Sheets
export SHEETS_PROJECT1="AIzaSyD..."      # API key for project 1
export SHEETS_PROJECT2="AIzaSyD..."      # API key for project 2

# Telegram
export TELEGRAM_PERSONAL="123456789..."  # Bot token for personal use
export TELEGRAM_ALERTS="987654321..."    # Bot token for alerts
```

## Deployment Patterns

### Server-Only

Single environment with shared credentials:

```bash
# .env or deployed to server
GMAIL_MAIN="..."
DISCORD_PROD="..."
SHEETS_PROJECT1="..."
```

All workflows on this server resolve from the same credential store.

### Client-Only

Each user authenticates individually:

```kotlin
// iOS app: uses Keychain
val provider = ClientCredentialProvider()

// User clicks "Connect Gmail"
// → OAuth flow
// → Token stored in Keychain as "gmail:main"
// → Workflow executes locally with Keychain-stored token
```

### Hybrid (Recommended)

Server orchestrator + embedded client execution:

```
┌──────────────────┐
│  Workflow UI     │
│  (Client App)    │
└────────┬─────────┘
         │
         ├─────────────────┐
         │ (local creds)   │ (server creds)
         │                 │
    ┌────▼──────┐   ┌──────▼────┐
    │  Fetch &  │   │ RAG Query  │
    │  Parse    │   │ LLM Call   │
    │  Emails   │   │ Image Gen  │
    │(OAuth)    │   │(API Keys)  │
    └───────────┘   └────────────┘
         │                 │
         └────────┬────────┘
              Results
```

- **Client side:** OAuth tokens for Gmail fetch/send, secure storage
- **Server side:** API keys for LLM, image generation, Telegram webhooks
- **Resolution:** Each node's `CredentialProvider` knows its environment

## Implementation for New Services

To add Discord, Telegram, Google Sheets, etc., follow this pattern:

### 1. Define NodeSpecs

```kotlin
// plugins/discord/src/commonMain/kotlin/DiscordNodes.kt
val discordSendMessage = NodeSpec(
    name = "discord_send_message",
    inputs = listOf(
        PortSpec("credential", StringType, defaultValue = null),  // "discord:prod"
        PortSpec("channel_id", StringType, defaultValue = null),
        PortSpec("message", StringType, defaultValue = null),
    ),
    outputs = listOf(
        PortSpec("success", BooleanType),
        PortSpec("message_id", StringType),
        PortSpec("error", StringType),
    ),
)
```

### 2. Create Executor

```kotlin
// plugins/discord/src/commonMain/kotlin/DiscordExecutor.kt
class DiscordExecutor(private val credentialProvider: CredentialProvider) {
    suspend fun execute(node: WorkflowNode): Map<String, Any?> {
        val credentialRef = node.inputs["credential"] as String
        val credential = credentialProvider.getCredential("discord", credentialRef.substringAfterLast(":"))
        
        // Execute with credential
        return discordClient.sendMessage(
            token = credential!!,
            channelId = node.inputs["channel_id"] as String,
            message = node.inputs["message"] as String,
        )
    }
}
```

### 3. Register in Plugin

```kotlin
// plugins/discord/src/commonMain/kotlin/DiscordPlugin.kt
object DiscordPlugin : GraphynPlugin {
    override val nodeSpecs = listOf(
        discordSendMessage,
        discordGetServers,
        discordCreateChannel,
        // ...
    )
    
    override val executorFactory: (CredentialProvider) -> NodeExecutor = { provider ->
        DiscordExecutor(provider)
    }
}
```

### 4. Deploy with Credentials

**Server (.env):**
```bash
DISCORD_PROD="NzI4NjIyODk..."
```

**Client (app startup):**
```kotlin
// iOS: User taps "Connect Discord"
// → OAuth flow to Discord
// → Token stored in Keychain under "discord:main"
```

## Security Considerations

- **Never serialize credentials into workflows** — only the reference (scope:key)
- **Credentials are resolved at execution time**, not serialization time
- **Each deployment provides its own CredentialProvider** — security boundary
- **Credentials don't flow between client and server** — server can't see client tokens, vice versa
- **Token refresh happens at executor level** — transparent to nodes

## Testing

Use test credential providers:

```kotlin
class TestCredentialProvider : CredentialProvider {
    private val testCredentials = mapOf(
        "gmail:test" to "test-token-123",
        "discord:test" to "test-bot-456",
    )
    
    override suspend fun getCredential(scope: String, key: String): String? {
        return testCredentials["$scope:$key"]
    }
}

@Test
fun testGmailFetchWithTestCredentials() {
    val executor = GmailExecutor(TestCredentialProvider())
    val result = executor.execute(testWorkflow)
    assert(result.success)
}
```

---

**Pattern Summary:**

| Layer | Responsibility | Example |
|-------|---|---|
| **Workflow** | Reference credentials by scope:key | `"gmail:main"` |
| **CredentialProvider** | Resolve reference to actual value | `getCredential("gmail", "main")` |
| **Executor** | Execute nodes with resolved credentials | `executeGmailNode(credential, ...)` |
| **Deployment** | Provide environment-specific CredentialProvider | `ServerCredentialProvider` or `ClientCredentialProvider` |

This pattern scales to any number of services and deployment models.
