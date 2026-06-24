# Social Media Integration Plan: LinkedIn + Twitter/X

## Overview

Extend Graphyn with **LinkedIn** and **Twitter/X** plugins following the Gmail integration pattern established in v0.2.1.

- **Pattern reuse:** Same credential provider, ShapeCard UI, hybrid deployment
- **Scope:** Essential operations only (no edge cases)
- **Release:** v0.2.2 + v0.2.3 (separate plugins)
- **Status:** 📋 Planned

---

## LinkedIn Integration (`plugins/linkedin`)

### Credential Pattern

**Environment variables:**
```bash
LINKEDIN_MAIN="<OAuth access token>"
LINKEDIN_COMPANY="<Company access token for posts>"
```

**Workflow reference:**
```kotlin
"credential" to "linkedin:main"
"credential" to "linkedin:company"
```

### Node Specs (7 nodes)

| Node | Purpose | Inputs | Outputs |
|------|---------|--------|---------|
| `fetch_profile` | Get current user profile | credential | id, name, headline, picture_url |
| `post_feed` | Share text/image to personal feed | credential, text, image_url (opt) | post_id, success, error |
| `get_feed` | Fetch recent feed items | credential, limit | posts[id, author, text, timestamp], count |
| `get_connections` | List 1st-degree connections | credential, limit | connections[id, name, headline], count |
| `send_message` | Send DM to a connection | credential, recipient_id, message | message_id, success, error |
| `like_post` | Like a published post | credential, post_id | success, error |
| `search_posts` | Full-text search on LinkedIn | credential, query, limit | posts[id, author, text, engagement_count], count |

### Card UI

All 7 nodes use **ShapeCard (64dp circle)** with LinkedIn blue (`#0A66C2`).

---

## Twitter/X Integration (`plugins/twitter`)

### Credential Pattern

**Environment variables:**
```bash
TWITTER_MAIN="<OAuth 2.0 access token>"
TWITTER_BOT="<Bot account token>"
```

**Workflow reference:**
```kotlin
"credential" to "twitter:main"
"credential" to "twitter:bot"
```

### Node Specs (8 nodes)

| Node | Purpose | Inputs | Outputs |
|------|---------|--------|---------|
| `post_tweet` | Publish a tweet | credential, text, media_ids (opt) | tweet_id, success, error |
| `get_timeline` | Fetch home timeline | credential, limit | tweets[id, author, text, likes, timestamp], count |
| `get_trending` | Get trending topics | credential, limit | trends[name, tweet_count], count |
| `search_tweets` | Search by keyword or #hashtag | credential, query, limit | tweets[id, author, text, engagement], count |
| `like_tweet` | Like a tweet | credential, tweet_id | success, error |
| `retweet` | Retweet a tweet | credential, tweet_id | success, error |
| `send_dm` | Send direct message | credential, recipient_id, text | message_id, success, error |
| `get_user_info` | Fetch user profile | credential, username | id, name, followers_count, verified, bio |

### Card UI

All 8 nodes use **ShapeCard (64dp circle)** with Twitter/X black (`#000000`) or dark gray.

---

## Implementation Order

### Phase 1: LinkedIn (`v0.2.2`)

**Week 1: Setup + Node Specs**
- [ ] Create `plugins/linkedin/` module (copy Gmail structure)
- [ ] Define 7 `NodeSpec` in `LinkedInNodes.kt`
- [ ] Implement `LinkedInPlugin` (register specs + stub executors)
- [ ] Implement `LinkedInEditorPlugin` (ShapeCard registration)

**Week 2: API Client + Execution**
- [ ] Implement `LinkedInApiClient(token)` with Ktor
- [ ] Wire up node handlers: `executeFetchProfile()`, `executePostFeed()`, etc.
- [ ] Add `LinkedInExecutors` with `NodeExecutor` adapters
- [ ] Unit tests for handlers

**Week 3: Testing + Polish**
- [ ] Manual test on canvas (drag nodes, connect)
- [ ] Demo: post to feed, fetch profile
- [ ] Update README + roadmap
- [ ] Tag `v0.2.2`, release

### Phase 2: Twitter/X (`v0.2.3`)

**Week 4: Setup + Node Specs**
- [ ] Create `plugins/twitter/` module
- [ ] Define 8 `NodeSpec` in `TwitterNodes.kt`
- [ ] Implement `TwitterPlugin` + `TwitterEditorPlugin`

**Week 5: API Client + Execution**
- [ ] Implement `TwitterApiClient(token)` with Ktor (Twitter API v2)
- [ ] Wire up all 8 node handlers
- [ ] Add `TwitterExecutors`
- [ ] Unit tests

**Week 6: Integration + Release**
- [ ] Canvas testing (post tweet, get timeline)
- [ ] Demo workflow: search → like → retweet
- [ ] Update README, roadmap
- [ ] Tag `v0.2.3`, release

---

## API Details

### LinkedIn API v2 (OAuth 2.0)

**Endpoints:**
- `GET /me` — current user profile
- `POST /ugcPosts` — share to feed
- `GET /feed` — home timeline
- `GET /relationships/connections` — 1st-degree connections
- `POST /messaging/conversations/{id}/events` — send DM
- `POST /likes` — like a post
- `GET /search/queries` — full-text search

**Token scope:**
```
openid profile email r_basicprofile r_organization_social w_organization_social
```

### Twitter API v2 (OAuth 2.0)

**Endpoints:**
- `POST /2/tweets` — create tweet
- `GET /2/tweets/search/recent` — search
- `GET /2/trends/search` — trending topics
- `POST /2/tweets/{id}/liking_by` — like tweet
- `POST /2/tweets/{id}/retweeted_by` — retweet
- `POST /2/dm_conversations/with/{participant_id}/dm_events` — send DM
- `GET /2/users/by/username/{username}` — user info

**Token scope:**
```
tweet.read tweet.write tweet.moderate.write users.read follows.read follows.write offline.access
```

---

## Credential Management

Both plugins reuse the **hybrid pattern from Gmail:**

| Deployment | Resolution | Source |
|---|---|---|
| **Server (Ktor)** | `ServerCredentialProvider` | `.env` or vault |
| **Client (Desktop/Mobile)** | `ClientCredentialProvider` | Keychain / SecurePrefs + OAuth flow |
| **Hybrid (recommended)** | Each node's executor resolves at runtime | Platform-specific |

**OAuth redirect:**
- **Server:** Token exchange at `/auth/linkedin/callback`, store in vault
- **Client:** System browser popup → app re-enters with code → exchange token locally

---

## Test Scenarios

### LinkedIn

1. ✅ Fetch profile → display user name
2. ✅ Post to feed → see post_id returned
3. ✅ Get connections → list 5 most recent
4. ✅ Send DM → success flag
5. ✅ Search posts → filter by keyword

### Twitter/X

1. ✅ Post tweet → get tweet_id
2. ✅ Get timeline → display 5 recent tweets
3. ✅ Get trending → show top 10 topics
4. ✅ Search tweets → keyword + hashtag queries
5. ✅ Like/retweet → success flag
6. ✅ Send DM → message_id
7. ✅ User info → followers_count, verified badge

---

## Risks & Mitigations

| Risk | Mitigation |
|---|---|
| **Rate limits** (Twitter: 450/15min, LinkedIn: varies) | Exponential backoff in executors, document limits in KDoc |
| **OAuth token refresh** | Executor handles refresh before expiry, stores new token via `CredentialProvider.setCredential()` |
| **Long API latencies** (15s+) | Use node-level timeout from `GraphynNodeSpec.timeoutMs` (default 30s) |
| **Account bans** (abuse) | Warn in plugin docs about posting limits; no automated spam features |

---

## Deliverables per Release

### v0.2.2 (LinkedIn)
- ✅ `plugins/linkedin/` module (15 KB)
- ✅ 7 node specs + executors
- ✅ KDoc on all public APIs
- ✅ Unit tests (handlers, API client mocks)
- ✅ README section + roadmap update

### v0.2.3 (Twitter/X)
- ✅ `plugins/twitter/` module (18 KB)
- ✅ 8 node specs + executors
- ✅ KDoc on all public APIs
- ✅ Unit tests
- ✅ README section + roadmap update
- ✅ Multi-service example workflow (search → like → DM)

---

## Next: Credential Management for Client Side

After LinkedIn + Twitter releases, implement:
- **OAuth 2.0 authorization code flow** in `ClientCredentialProvider`
- **Token refresh** without user interaction
- **Secure storage** (Keychain macOS/iOS, SecurePrefs Android, IndexedDB Web)
- **Fallback to server token exchange** if device storage unavailable

This unlocks fully **client-side workflows** without exposing credentials to a server.

---

## Success Metrics

- **Reduced boilerplate:** LinkedIn/Twitter plugins are <10% larger than Gmail (same patterns reused)
- **Reuse rate:** All 3 plugins use identical `CredentialProvider`, `ShapeCard`, `NodeExecutor` contracts
- **Node count:** 5 + 7 + 8 = **20 core service nodes**, enough for real automation workflows
- **Community adoption:** Track plugin imports in downstream projects

