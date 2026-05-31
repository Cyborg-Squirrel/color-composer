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
| `Inactive` | New or inactive (not stopped/paused) |
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

### `EffectSettingsType`
`Boolean` | `Integer` | `Number` | `String` | `RgbColor`

### `FadeCurve`
`Linear` | `Logarithmic`

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
      "status": "Playing | Paused",
      "layer": 0
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
      "inUse": false
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
      "inUse": false
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
  "clientUuid": "string",
  "unassign": false
}
```
> A non-null `clientUuid` that differs from the strip's current client reassigns the strip; a `pin` must be supplied when assigning to a client. Omitting `clientUuid` (or sending `null`) leaves the current client assignment unchanged.
> `unassign` (default `false`) detaches the strip from its client. Combining `unassign: true` with a non-null `clientUuid` returns `400`.

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
      "status": "Inactive | Playing | Paused | Stopped",
      "layer": 0
    }
  ]
}
```
> Strip effects include `stripUuid`; pool effects include `poolUuid`; unassigned effects include neither. Both `stripUuid` and `poolUuid` are nullable. `paletteUuid` is nullable. `category` is derived from `type` — see `EffectCategory`. `layer` is the render layer (0 = base) and acts as an ordered position within its strip or pool — layers are kept contiguous, so creating, moving, or deleting an effect shifts neighboring effects to fill or make room.

---

### `GET /effect/{uuid}`
Returns a single effect by UUID.

**Path params:** `uuid`

**Response `200`** — same shape as an item from `GET /effect`

---

### `GET /effect/schemas`
Returns the settings schema for every available effect type. Schemas describe the expected keys, value types, and validation constraints for the `settings` map used when creating or updating an effect.

**Response `200`**
```json
[
  {
    "effectName": "string",
    "category": "Static | Ambient | Motion",
    "fields": [
      {
        "key": "string",
        "type": "Boolean | Integer | Number | String",
        "validators": [],
        "description": "string",
        "default": null
      }
    ]
  }
]
```
> `default` is the value applied when no explicit setting is provided. It is typed to match `type` (boolean, integer, number, or string), and is `null` if no default is defined for that field.

Each `validators` entry is a polymorphic object with a `type` discriminator:

| `type` | Additional fields | Meaning |
|--------|-------------------|---------|
| `min` | `value: number` | Field value must be ≥ `value` |
| `max` | `value: number` | Field value must be ≤ `value` |
| `options` | `values: string[]` | Field value must be one of the listed strings |

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
  "paletteUuid": "string",
  "layer": 0
}
```
> Provide exactly one of `stripUuid` or `poolUuid`.
> `paletteUuid` is optional.
> `settings` is an arbitrary key/value map specific to the effect type.
> `layer` is optional. If omitted, the new effect is appended on top (at `layer = count`, where `count` is the current number of effects on the same strip or pool). If provided, it inserts at that position and shifts existing effects at or above it up by one; the value must lie in `[0, count]`. Out-of-range values return `400`.

**Response `201`** — `{ "uuid": "string" }`

---

### `PATCH /effect/update/{uuid}`
Updates an effect's name, palette, settings, or layer. Strip/pool reassignment is **not** handled here — use `PATCH /effect/reassign/{uuid}`.

**Path params:** `uuid`

**Request body**
```json
{
  "name": "string",
  "paletteUuid": "string",
  "unassignPalette": false,
  "settingsUuid": "string",
  "layer": 0
}
```
> All fields are optional, but the request must change at least one thing — an empty request (every field null and `unassignPalette` false) returns `400`.
> `unassignPalette` (default `false`) clears the effect's palette. Setting it to `true` while also supplying a different `paletteUuid` returns `400`.
> `paletteUuid` / `settingsUuid` reassign the palette/settings preset by UUID; an unknown UUID returns `400`.
> `layer` is optional. If omitted, the effect's layer is unchanged. If provided (only meaningful for an assigned effect), the effect moves to that position within its current strip or pool and the other effects shift to keep layers contiguous; the value must lie in `[0, count-1]` for that strip/pool, otherwise `400`.
> A palette-only change keeps the running effect's animation state and just swaps the palette in place; any other change rebuilds the effect's runtime.
> Deleting an effect (`DELETE /effect/{uuid}`) compacts the layers above it.

**Response `204 No Content`**

---

### `PATCH /effect/reassign/{uuid}`
Reassigns an effect to a different strip or pool, or unassigns it entirely. Introduced on this branch — previously this was folded into `PATCH /effect/{uuid}`.

**Path params:** `uuid`

**Request body**
```json
{
  "unassign": false,
  "targetStripUuid": "string",
  "targetPoolUuid": "string"
}
```
> The request must specify something — `unassign: false` with both targets null returns `400`.
> Provide at most one of `targetStripUuid` / `targetPoolUuid`; supplying both returns `400`. An unknown target UUID returns `400`.
> `unassign: true` detaches the effect from its strip/pool and drops it from the active-effect registry (unassigned effects are not rendered). Combining `unassign: true` with a target that differs from the current owner returns `400`.
> Reassigning to the effect's current owner is a no-op (still emits a `LightEffectUpdated` event).
> On a move, the effect is appended as the top layer of the destination, the source's layers are compacted, and the destination's layers shift to make room — all owners stay contiguous.

**Response `204 No Content`**

---

### `POST /effect/command`
Bulk-updates the playback status of one or more effects. (Renamed from `POST /effect/status`; request body unchanged.)

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

## Effect Settings

Effect settings are named, reusable snapshots of a `settings` map for a given effect type. They can be marked as the default preset for their type.

### `GET /effect/settings`
Returns all effect settings presets.

**Response `200`**
```json
{
  "effectSettings": [
    {
      "uuid": "string",
      "type": "string",
      "name": "string",
      "settings": {},
      "isDefault": false,
      "skipFramesIfBlank": true
    }
  ]
}
```
> `skipFramesIfBlank` controls whether the renderer skips frames whose output is entirely blank. Defaults to `true` on creation.

---

### `GET /effect/settings/{uuid}`
Returns a single effect settings preset by UUID.

**Path params:** `uuid`

**Response `200`** — same shape as an item from `GET /effect/settings`

---

### `POST /effect/settings`
Creates a new effect settings preset.

**Request body**
```json
{
  "type": "string",
  "name": "string",
  "settings": {},
  "isDefault": false,
  "skipFramesIfBlank": true
}
```
> `isDefault` defaults to `false`. Setting it to `true` clears the existing default for that type.
> `skipFramesIfBlank` defaults to `true`.

**Response `201`** — `"uuid-string"`

---

### `PATCH /effect/settings/{uuid}`
Updates an effect settings preset. All fields are optional.

**Path params:** `uuid`

**Request body**
```json
{
  "name": "string",
  "settings": {},
  "isDefault": true,
  "skipFramesIfBlank": true
}
```
> Setting `isDefault` to `true` clears the existing default for that type.
> All fields are optional; `skipFramesIfBlank` is preserved if omitted.

**Response `204 No Content`**

---

### `DELETE /effect/settings/{uuid}`
Deletes an effect settings preset.

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

## Event Stream

### `GET /events`
Opens a Server-Sent Events (SSE) stream. The server pushes an event whenever a resource is created, updated, or deleted. Clients can use this to keep their state in sync without polling.

**Response** — `Content-Type: text/event-stream` (persistent connection)

Each SSE frame has its `event` field set to the event type name and its `data` field set to the JSON payload:

```
event: LightEffectCreated
data: {"uuid":"...","type":"LightEffectCreated","data":{ ...full effect object... }}
```

#### Event payload

Every event carries the affected resource's `uuid`, a `type` discriminator, and a `data` payload describing what changed:

```json
{
  "uuid": "string",
  "type": "string",
  "data": {}
}
```

| Field | Description |
|-------|-------------|
| `uuid` | UUID of the affected resource |
| `type` | Discriminator — see table below |
| `data` | Payload describing the change (see below). Omitted on `*Deleted` events. |

The shape of `data` depends on the event:

- **Created events** (`*Created`) — `data` is the full resource object, identical to what the resource's `GET` endpoint returns. For example, `LightEffectCreated.data` matches an item from `GET /effect`, `LedClientCreated.data` matches an item from `GET /client`, and so on.
- **Updated events** (`*Updated`) — `data` is a *delta* object containing **only the fields that changed**; unchanged fields are omitted. For example, an effect whose layer changed from `1` to `2` and whose name changed from `"MyEffect"` to `"My Effect"` produces:

  ```
  event: LightEffectUpdated
  data: {"uuid":"...","type":"LightEffectUpdated","data":{"layer":2,"name":"My Effect"}}
  ```

- **Deleted events** (`*Deleted`) — there is no `data` field; only `uuid` and `type` are present.

> Because delta objects omit null fields, *clearing* a value cannot be represented and is not reported in the delta — e.g. unassigning an effect's palette, or unassigning an effect from its strip/pool. Consumers should treat an omitted field as "unchanged".

#### Delta fields by type

All delta fields are optional and appear only when changed.

| Event | Delta fields |
|-------|--------------|
| `LedClientUpdated` | `name`, `address`, `colorOrder`, `apiPort`, `wsPort`, `powerLimit`, `fps`, `fadeTimeoutMillis` |
| `LedStripUpdated` | `name`, `pin`, `length`, `height`, `brightness`, `blendMode`, `clientUuid` |
| `StripPoolUpdated` | `name`, `poolType`, `blendMode`, `members` (full replacement member list) |
| `LightEffectUpdated` | `name`, `paletteUuid`, `settingsUuid`, `status`, `stripUuid`, `poolUuid`, `layer` |
| `EffectSettingsUpdated` | `name`, `settings`, `isDefault`, `skipFramesIfBlank` |
| `PaletteUpdated` | `name`, `settings` |

#### Event types

| `type` | Trigger |
|--------|---------|
| `LedClientCreated` | A new LED client was created |
| `LedClientUpdated` | An LED client was updated |
| `LedClientDeleted` | An LED client was deleted |
| `LedStripCreated` | A new LED strip was created |
| `LedStripUpdated` | An LED strip was updated |
| `LedStripDeleted` | An LED strip was deleted |
| `StripPoolCreated` | A new strip pool was created |
| `StripPoolUpdated` | A strip pool was updated |
| `StripPoolDeleted` | A strip pool was deleted |
| `LightEffectCreated` | A new lighting effect was created |
| `LightEffectUpdated` | A lighting effect was updated |
| `LightEffectDeleted` | A lighting effect was deleted |
| `EffectSettingsCreated` | A new effect settings preset was created |
| `EffectSettingsUpdated` | An effect settings preset was updated |
| `EffectSettingsDeleted` | An effect settings preset was deleted |
| `PaletteCreated` | A new color palette was created |
| `PaletteUpdated` | A color palette was updated |
| `PaletteDeleted` | A color palette was deleted |

---

## Endpoint Summary

| Method | Path | Description |
|--------|------|-------------|
| GET | `/events` | SSE stream — resource create/update/delete events |
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
| GET | `/effect/schemas` | List settings schemas for all effect types |
| GET | `/effect/{uuid}` | Get effect |
| POST | `/effect` | Create effect |
| PATCH | `/effect/update/{uuid}` | Update effect name/palette/settings/layer |
| PATCH | `/effect/reassign/{uuid}` | Reassign effect to a strip/pool or unassign |
| POST | `/effect/command` | Bulk update effect statuses |
| DELETE | `/effect/{uuid}` | Delete effect |
| GET | `/effect/settings` | List all effect settings presets |
| GET | `/effect/settings/{uuid}` | Get effect settings preset |
| POST | `/effect/settings` | Create effect settings preset |
| PATCH | `/effect/settings/{uuid}` | Update effect settings preset |
| DELETE | `/effect/settings/{uuid}` | Delete effect settings preset |
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
