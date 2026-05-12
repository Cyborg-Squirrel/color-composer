# Color Composer REST API

**Framework:** Micronaut (Kotlin)
**Default base URL:** `http://localhost:8080`
**Content-Type:** `application/json`
**CORS:** Enabled globally

---

## Error Responses

All endpoints return consistent error shapes:

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Business logic error (e.g. invalid state) |
| `404 Not Found` | Resource with given UUID does not exist |
| `500 Internal Server Error` | Uncaught exception |

---

## Enums

### `ClientType`
`Pi` | `NightDriver`

### `ClientStatus`
| Value | Meaning |
|-------|---------|
| `SetupIncomplete` | Created but no strips configured |
| `Idle` | Connected, at least one strip configured, no active effects |
| `Active` | Connected with one or more effects playing or paused |
| `Offline` | Disconnected |
| `Error` | In an error state |

### `ColorOrder`
`RGB` | `RBG` | `GRB` | `GBR` | `BRG` | `BGR`

### `BlendMode`
| Value | Behavior |
|-------|----------|
| `Additive` | Adds RGB values together |
| `Average` | Averages RGB values when effects overlap |
| `Layer` | Higher-priority effect takes precedence |
| `UseHighest` | Uses the highest R, G, and B values independently |

### `PoolType`
| Value | Behavior |
|-------|----------|
| `Sync` | All strips receive the same effect data in sync |
| `Unified` | Strips are combined and treated as one strip |

### `LightEffectStatus`
| Value | Meaning |
|-------|---------|
| `Idle` | New or inactive (not stopped/paused) |
| `Playing` | Currently being rendered |
| `Paused` | Suspended — resumes from where it left off |
| `Stopped` | Suspended — restarts from beginning when reactivated |

### `LightEffectStatusCommand`
`Play` | `Pause` | `Stop`

### `EffectCategory`
| Value | Meaning |
|-------|---------|
| `Static` | Effect produces a fixed, non-animated output |
| `Ambient` | Effect animates without directional movement |
| `Motion` | Effect features directional or positional movement |

### `DiscoveryJobStatus`
`idle` | `inProgress` | `complete` | `error`

---

## Home

### `GET /home`
Returns counts of the core entities configured in the application, a list of currently active effects, all strips, and all clients. Useful for a dashboard overview.

**Response `200`**
```json
{
  "totalClients": 0,
  "totalStrips": 0,
  "totalEffects": 0,
  "totalPalettes": 0,
  "activeEffects": [
    {
      "uuid": "string",
      "name": "string",
      "type": "string",
      "category": "Static | Ambient | Motion",
      "stripUuid": "string",
      "poolUuid": "string",
      "paletteUuid": "string",
      "settings": {},
      "status": "Playing | Paused"
    }
  ],
  "strips": [
    {
      "uuid": "string",
      "clientUuid": "string",
      "name": "string",
      "pin": "string",
      "length": 0,
      "height": 0,
      "brightness": 0,
      "blendMode": "Additive | Average | Layer | UseHighest",
      "activeEffects": 0
    }
  ],
  "clients": [
    {
      "uuid": "string",
      "name": "string",
      "address": "string",
      "clientType": "Pi | NightDriver",
      "colorOrder": "RGB | RBG | GRB | GBR | BRG | BGR",
      "apiPort": 0,
      "wsPort": 0,
      "lastSeenAt": 0,
      "status": "ClientStatus",
      "activeEffects": 0,
      "powerLimit": 0,
      "firmwareVersion": "string",
      "fps": 0,
      "fadeTimeoutMillis": 0
    }
  ]
}
```

| Field | Description |
|-------|-------------|
| `totalClients` | Total number of LED clients |
| `totalStrips` | Total number of LED strips |
| `totalEffects` | Total number of lighting effects |
| `totalPalettes` | Total number of color palettes |
| `activeEffects` | Effects with status `Playing` or `Paused`; same shape as items from `GET /effect` |
| `strips` | All configured strips; same shape as items from `GET /strip` |
| `clients` | All configured clients; same shape as items from `GET /client` |

---

## Server Status

### `GET /setup-status`
Returns the application's overall setup state.

**Response `200`**
```json
{
  "status": "NoClients | NoStrips | NoEffects | SetupComplete"
}
```

| Status value | Meaning |
|---|---|
| `NoClients` | No LED clients configured |
| `NoStrips` | At least one client, but no strips |
| `NoEffects` | Client and strip exist, but no effects |
| `SetupComplete` | At least one client, strip, and effect configured |

---

### `GET /version`
Returns the application version string.

**Response `200`** — plain string (e.g. `"1.0.0"`)

---

## LED Clients

### `GET /client`
Returns all LED clients.

**Response `200`**
```json
{
  "clients": [
    {
      "uuid": "string",
      "name": "string",
      "address": "string",
      "clientType": "Pi | NightDriver",
      "colorOrder": "RGB | RBG | GRB | GBR | BRG | BGR",
      "apiPort": 0,
      "wsPort": 0,
      "lastSeenAt": 0,
      "status": "ClientStatus",
      "activeEffects": 0,
      "powerLimit": 0,
      "firmwareVersion": "string",
      "fps": 0,
      "fadeTimeoutMillis": 0
    }
  ]
}
```
> `powerLimit` is nullable.
> `lastSeenAt` is a Unix timestamp (milliseconds).
> `fps` defaults to `35`.
> `fadeTimeoutMillis` defaults to `0` (disabled).

---

### `GET /client/{uuid}`
Returns a single client by UUID.

**Path params:** `uuid` — client UUID

**Response `200`** — same shape as an item from `GET /client`

---

### `POST /client`
Creates a new LED client.

**Request body**
```json
{
  "name": "string",
  "address": "string",
  "clientType": "Pi | NightDriver",
  "colorOrder": "RGB",
  "apiPort": 0,
  "wsPort": 0,
  "powerLimit": 0,
  "fps": 0,
  "fadeTimeoutMillis": 0
}
```
> `colorOrder` and `powerLimit` are optional.
> `fps` defaults to `35` if omitted.
> `fadeTimeoutMillis` defaults to `0` (disabled) if omitted.

**Response `201`** — created client entity

---

### `PATCH /client/{uuid}`
Updates an existing client. All fields are optional — only provided fields are changed.

**Path params:** `uuid`

**Request body**
```json
{
  "name": "string",
  "address": "string",
  "colorOrder": "RGB",
  "apiPort": 0,
  "wsPort": 0,
  "powerLimit": 0,
  "fps": 0,
  "fadeTimeoutMillis": 0
}
```

**Response `204 No Content`**

---

### `DELETE /client/{uuid}`
Deletes a client.

**Path params:** `uuid`

**Response `204 No Content`**

---

## Client Discovery

### `POST /discover-clients`
Starts the client discovery process.

**Response `200`**

---

### `POST /cancel-discovery`
Cancels an in-progress discovery job.

**Response `200`**

---

### `GET /discovered-clients`
Returns clients found during a discovery run.

**Response `200`**
```json
{
  "clients": [
    {
      "name": "string",
      "address": "string"
    }
  ]
}
```
> Response shape varies based on discovery status (`inProgress`, `complete`, `error`).

---

### `GET /discovery-status`
Returns the current status of the discovery job.

**Response `200`**
```json
{
  "status": "idle | inProgress | complete | error"
}
```

---

### `POST /confirm-client`
Registers a discovered client as a saved LED client.

**Request body**
```json
{
  "name": "string",
  "address": "string"
}
```

**Response `201`** — created client entity

---

## LED Strips

### `GET /strip`
Returns all LED strips, optionally filtered by client.

**Query params**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `clientUuid` | string | No | Filter strips belonging to this client |

**Response `200`**
```json
{
  "strips": [
    {
      "uuid": "string",
      "clientUuid": "string",
      "name": "string",
      "pin": "string",
      "length": 0,
      "height": 0,
      "brightness": 0,
      "blendMode": "Additive | Average | Layer | UseHighest",
      "activeEffects": 0
    }
  ]
}
```

---

### `GET /strip/{uuid}`
Returns a single strip by UUID.

**Path params:** `uuid`

**Response `200`** — same shape as an item from `GET /strip`

---

### `POST /strip`
Creates a new LED strip.

**Request body**
```json
{
  "clientUuid": "string",
  "name": "string",
  "pin": "string",
  "length": 0,
  "height": 0,
  "powerLimit": 0,
  "brightness": 0,
  "blendMode": "Additive | Average | Layer | UseHighest"
}
```
> `height`, `powerLimit`, `brightness`, and `blendMode` are optional.

**Response `201`** — `{ "uuid": "string" }`

---

### `PATCH /strip/{uuid}`
Updates a strip. All fields are optional.

**Path params:** `uuid`

**Request body**
```json
{
  "name": "string",
  "pin": "string",
  "length": 0,
  "height": 0,
  "brightness": 0,
  "blendMode": "Additive | Average | Layer | UseHighest",
  "clientUuid": "string"
}
```

**Response `204 No Content`**

---

### `DELETE /strip/{uuid}`
Deletes a strip.

**Path params:** `uuid`

**Response `204 No Content`**

---

## Strip Pools

Strip pools group multiple LED strips together. See `PoolType` for behavior.

### `GET /pool`
Returns all strip pools.

**Response `200`**
```json
{
  "pools": [
    {
      "uuid": "string",
      "name": "string",
      "poolType": "Sync | Unified",
      "blendMode": "Additive | Average | Layer | UseHighest",
      "members": [
        {
          "uuid": "string",
          "stripUuid": "string",
          "inverted": false,
          "poolIndex": 0
        }
      ]
    }
  ]
}
```

---

### `GET /pool/{uuid}`
Returns a single pool by UUID.

**Path params:** `uuid`

**Response `200`** — same shape as an item from `GET /pool`

---

### `POST /pool`
Creates a new strip pool.

**Request body**
```json
{
  "name": "string",
  "poolType": "Sync | Unified",
  "blendMode": "Additive | Average | Layer | UseHighest"
}
```

**Response `201`** — `{ "uuid": "string" }`

---

### `PATCH /pool/{uuid}`
Updates a pool. All fields are optional.

**Path params:** `uuid`

**Request body**
```json
{
  "name": "string",
  "poolType": "Sync | Unified",
  "blendMode": "Additive | Average | Layer | UseHighest"
}
```

**Response `204 No Content`**

---

### `PATCH /pool/{uuid}/members`
Replaces the full member list for a pool.

**Path params:** `uuid`

**Request body**
```json
{
  "members": [
    {
      "uuid": "string",
      "stripUuid": "string",
      "inverted": false,
      "poolIndex": 0
    }
  ]
}
```
> `uuid` in each member is optional (null for new members).
> `poolIndex` controls ordering within the pool.

**Response `204 No Content`**

---

### `DELETE /pool/{uuid}`
Deletes a pool.

**Path params:** `uuid`

**Response `204 No Content`**

---

## Lighting Effects

Effects are attached to either a strip (`stripUuid`) or a pool (`poolUuid`) — exactly one must be provided.

### `GET /effect`
Returns all effects, optionally filtered by strip or pool.

**Query params**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `stripUuid` | string | No | Filter by strip |
| `poolUuid` | string | No | Filter by pool |

**Response `200`**
```json
{
  "effects": [
    {
      "uuid": "string",
      "name": "string",
      "type": "string",
      "category": "Static | Ambient | Motion",
      "stripUuid": "string",
      "poolUuid": "string",
      "paletteUuid": "string",
      "settings": {},
      "status": "Idle | Playing | Paused | Stopped"
    }
  ]
}
```
> Strip effects include `stripUuid`; pool effects include `poolUuid`. `paletteUuid` is nullable. `category` is derived from `type` — see `EffectCategory`.

---

### `GET /effect/{uuid}`
Returns a single effect by UUID.

**Path params:** `uuid`

**Response `200`** — same shape as an item from `GET /effect`

---

### `POST /effect`
Creates a new effect.

**Request body**
```json
{
  "name": "string",
  "effectType": "string",
  "stripUuid": "string",
  "poolUuid": "string",
  "settings": {},
  "paletteUuid": "string"
}
```
> Provide exactly one of `stripUuid` or `poolUuid`.
> `paletteUuid` is optional.
> `settings` is an arbitrary key/value map specific to the effect type.

**Response `201`** — `{ "uuid": "string" }`

---

### `PATCH /effect/{uuid}`
Updates an effect. All fields are optional.

**Path params:** `uuid`

**Request body**
```json
{
  "name": "string",
  "stripUuid": "string",
  "poolUuid": "string",
  "paletteUuid": "string",
  "settings": {}
}
```

**Response `204 No Content`**

---

### `POST /effect/status`
Bulk-updates the playback status of one or more effects.

**Request body**
```json
{
  "uuids": ["string"],
  "command": "Play | Pause | Stop"
}
```

**Response `204 No Content`**

---

### `DELETE /effect/{uuid}`
Deletes an effect.

**Path params:** `uuid`

**Response `204 No Content`**

---

## Effect Filters

Filters are applied to effects to modify their output.

### `GET /filter`
Returns filters, optionally scoped to an effect.

**Query params**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `effectUuid` | string | No | Return only filters for this effect |

> `GET /filter` with no params is not yet implemented.

**Response `200`**
```json
{
  "filters": [
    {
      "uuid": "string",
      "name": "string",
      "type": "string",
      "effectUuids": ["string"],
      "settings": {}
    }
  ]
}
```

---

### `GET /filter/{uuid}`
Returns a single filter by UUID.

**Path params:** `uuid`

**Response `200`** — same shape as an item from `GET /filter`

---

### `POST /filter`
Creates a new filter.

**Request body**
```json
{
  "name": "string",
  "filterType": "string",
  "settings": {}
}
```

**Response `201`** — `{ "uuid": "string" }`

---

### `PATCH /filter/{uuid}`
Updates a filter. All fields are optional.

**Path params:** `uuid`

**Request body**
```json
{
  "name": "string",
  "effectUuids": ["string"],
  "settings": {}
}
```

**Response `204 No Content`**

---

### `DELETE /filter/{uuid}`
Deletes a filter.

**Path params:** `uuid`

**Response `204 No Content`**

---

## Color Palettes

Palettes define color sets that can be referenced by effects via `paletteUuid`.

### `GET /palette`
Returns all palettes.

**Response `200`**
```json
{
  "palettes": [
    {
      "uuid": "string",
      "name": "string",
      "type": "string",
      "settings": {}
    }
  ]
}
```

---

### `GET /palette/{uuid}`
Returns a single palette by UUID.

**Path params:** `uuid`

**Response `200`** — same shape as an item from `GET /palette`

---

### `POST /palette`
Creates a new palette.

**Request body**
```json
{
  "name": "string",
  "paletteType": "string",
  "settings": {}
}
```

**Response `201`** — `{ "uuid": "string" }`

---

### `PATCH /palette/{uuid}`
Updates a palette. All fields are optional.

**Path params:** `uuid`

**Request body**
```json
{
  "name": "string",
  "settings": {}
}
```

**Response `204 No Content`**

---

### `DELETE /palette/{uuid}`
Deletes a palette.

**Path params:** `uuid`

**Response `200`**

---

## Endpoint Summary

| Method | Path | Description |
|--------|------|-------------|
| GET | `/home` | Counts and lists of clients, strips, effects, and palettes; active effects |
| GET | `/setup-status` | App setup state |
| GET | `/version` | App version |
| GET | `/client` | List all clients |
| GET | `/client/{uuid}` | Get client |
| POST | `/client` | Create client |
| PATCH | `/client/{uuid}` | Update client |
| DELETE | `/client/{uuid}` | Delete client |
| POST | `/discover-clients` | Start discovery |
| POST | `/cancel-discovery` | Cancel discovery |
| GET | `/discovered-clients` | List discovered clients |
| GET | `/discovery-status` | Discovery job status |
| POST | `/confirm-client` | Register discovered client |
| GET | `/strip` | List strips (filter: `?clientUuid=`) |
| GET | `/strip/{uuid}` | Get strip |
| POST | `/strip` | Create strip |
| PATCH | `/strip/{uuid}` | Update strip |
| DELETE | `/strip/{uuid}` | Delete strip |
| GET | `/pool` | List pools |
| GET | `/pool/{uuid}` | Get pool |
| POST | `/pool` | Create pool |
| PATCH | `/pool/{uuid}` | Update pool |
| PATCH | `/pool/{uuid}/members` | Replace pool members |
| DELETE | `/pool/{uuid}` | Delete pool |
| GET | `/effect` | List effects (filter: `?stripUuid=` / `?poolUuid=`) |
| GET | `/effect/{uuid}` | Get effect |
| POST | `/effect` | Create effect |
| PATCH | `/effect/{uuid}` | Update effect |
| POST | `/effect/status` | Bulk update effect statuses |
| DELETE | `/effect/{uuid}` | Delete effect |
| GET | `/filter` | List filters (filter: `?effectUuid=`) |
| GET | `/filter/{uuid}` | Get filter |
| POST | `/filter` | Create filter |
| PATCH | `/filter/{uuid}` | Update filter |
| DELETE | `/filter/{uuid}` | Delete filter |
| GET | `/palette` | List palettes |
| GET | `/palette/{uuid}` | Get palette |
| POST | `/palette` | Create palette |
| PATCH | `/palette/{uuid}` | Update palette |
| DELETE | `/palette/{uuid}` | Delete palette |
