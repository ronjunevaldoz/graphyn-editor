# RunPod Serverless Integration Plan

**Goal:** run `server-sd` on RunPod to escape the 12 GB VRAM wall — enabling higher models,
full-resolution Wan video (the VAE decode needs ~21 GB), and multiple stacked LoRAs — while
keeping the editor's generate contract and the artifact/metadata/history pipeline unchanged.

## Why this is mostly additive

The editor already talks a clean HTTP+JSON contract to `server-sd`
(`/api/sd/generate-ex`, `/api/sd/generate-video`, built by `argsToJson` / `videoArgsToJson`),
resolves the URL+key per-run from the credentials panel, and records every result via
`saveArtifact` + `history.record`. Moving the server to RunPod is a **URL + auth** change,
not a rewrite.

## Architecture (no Python)

```
Editor (HttpStableDiffusionBackend, unchanged contract)
   │  POST https://ENDPOINT_ID.api.runpod.ai/api/sd/generate-ex   (+ RunPod auth header)
   ▼
RunPod Load Balancing endpoint  ──routes HTTP directly──►  Worker (48 GB GPU)
                                                             └─ Ktor server-sd (as-is)
                                                                  └─ JNI stable-diffusion.cpp
                                                                       └─ /runpod-volume/models
```

**Load Balancing** endpoints route HTTP straight to a server inside the container — any
language, **no handler function, no Python**. Our Ktor server *is* that server.

### Worker container
- Reuse the existing `Dockerfile.dev` image (Ktor jar + native `.so` + CUDA).
- Listen on `$PORT` (RunPod default 80); add a **`/ping`** route → `200` healthy, `204` while
  the engine is still initializing (RunPod measures cold start by the 204→200 transition).
- Models/LoRAs live on a **RunPod Network Volume** mounted at `/runpod-volume` — keep the
  `/models/...` layout the workflows already reference (symlink or set the model root). Image
  stays small; add higher models + more LoRAs without rebuilding.
- Keep `SdEngineCache` — a warm worker holds the model resident across requests.

### GPU: 48 GB (L40S / A6000)
Holds the full Wan VAE decode (~21 GB) + diffusion model + several LoRAs with **no offload and
no tiling** — which is exactly what makes generation fast enough to fit the request window and
what unblocks the video tier. (24 GB is the floor that unblocks Wan 5B; 80 GB only needed for
A14B unoffloaded / long clips.)

### Output transport: base64
Worker returns image/video bytes base64 in the JSON result; editor decodes → existing
`saveArtifact`. Watch the **30 MB payload cap** — fine for images and short clips; long videos
would need a network-volume handoff (deferred until needed).

### Editor changes (small)
- Credentials/environments panel (already exists): add **RunPod endpoint URL** + **API key**,
  as another environment profile (dev = local `server-sd`, prod = RunPod). `resolveSdConnection`
  already picks per-run, so switching is a settings change.
- `HttpStableDiffusionBackend`: add the RunPod bearer/`Authorization` header when the base URL is
  a RunPod endpoint. The 30-min client timeout already added covers held connections.
- Likely **no new backend class** — same routes, same JSON, same artifact path.

## The 5.5-min request cap — and the fallback

Load Balancing caps a request at **5.5 min processing / 2 min no-worker**. Our multi-minute
times on 12 GB came entirely from RAM offload; on 48 GB (resident, no offload/tiling) a warm
worker generates in seconds-to-low-minutes, so jobs fit. Mitigations, in order:
1. Keep ≥1 min worker warm (no cold-load penalty) for latency-sensitive use.
2. Cap demo clip length so video stays under the window.
3. **Only if long video still exceeds it:** add a *queue-based* endpoint for the video tier
   (async `/run` + `/status` poll). That is the single place a ~15-line Python SDK shim — or a
   Kotlin reimplementation of RunPod's internal job-poll — would be justified. Design to avoid it.

## Phases
1. **Worker image** — add `/ping`, `$PORT` binding, model-root → `/runpod-volume`; push to a registry.
2. **Endpoint + volume** — create the Load Balancing endpoint (48 GB), attach the network volume,
   upload models/LoRAs once.
3. **Editor wiring** — RunPod profile (URL + key) in the panel; auth header; select prod.
4. **Verify** — run the same templates against RunPod; confirm Wan video decodes with no tiling;
   test multi-LoRA stacking. Reuse `SdTemplateApiRunTest` pointed at the RunPod URL.
5. **Tune** — FlashBoot / min-max workers / idle timeout for cost vs latency.

## References
- Load Balancing endpoints (own HTTP server, /ping, PORT, limits): https://docs.runpod.io/serverless/load-balancing/overview
- Wan VAE decode buffer sizes / VRAM: https://github.com/leejet/stable-diffusion.cpp/discussions/868
- Wan 2.2 VAE tiling bug (why 12 GB can't tile under the limit): https://github.com/leejet/stable-diffusion.cpp/issues/1284
