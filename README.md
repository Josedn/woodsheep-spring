# Woodsheep (Spring Boot)

A lightweight Spring Boot WebSocket server that demonstrates a simple multiplayer flow with users, rooms, and chat.

- WebSocket endpoint: `ws://localhost:4567/ws/game`
- Demo page: `http://localhost:4567/` (static under `src/main/resources/static`)
- Java: 21, Build: Maven, Spring Boot: 4.0.1

## Run

- Dev: `./mvnw spring-boot:run`
- Tests: `./mvnw test`

## Message Framing

All client messages are JSON objects with two fields:

- `requestType`: string opcode
- `payload`: JSON object (shape depends on opcode)

Server messages follow the same structure:

- `requestType`: string
- `payload`: JSON object

Example client message:

```json
{ "requestType": "login", "payload": { "sso": "token-123" } }
```

Example server message:

```json
{ "requestType": "loginOk", "payload": { "userId": "...", "username": "..." } }
```

## Opcodes

- `login`
  - Payload: `{ "sso": string }`
  - Effect: Logs in (or reuses) a user for this WebSocket session. Responds with `loginOk`.

- `createRoom`
  - Payload: `{}`
  - Effect: Creates a room and joins the caller. Responds with:
    - `prepareRoom` (room settings)
    - `addUserToRoom` (roster for the room or just the new user)

- `joinRoom`
  - Payload: `{ "roomId": string }`
  - Effect: If valid, moves user to target room and responds with:
    - `prepareRoom`
    - `addUserToRoom`
    - If invalid, responds with `roomRejected` `{ reason: "invalid" }`.

- `roomList`
  - Payload: `{}`
  - Effect: Sends `roomList` with current rooms and removes the user from any active room.

- `chatMessage`
  - Payload: `{ "message": string }`
  - Effect: Broadcasts to all users in the same room as `chatMessage` with sender’s virtual id and message. No-op if the user is not in a room.

## Outgoing Messages

- `loginOk` `{ userId, username }`
- `prepareRoom` `{ roomId, map, hideBankCards, privateGame, maxPlayers, turnTimer, cardDiscardLimit, pointsToWin }`
- `roomList` as an array of `{ roomId, name, maxPlayers, currentPlayers }`
- `addUserToRoom` as an array of `{ virtualId, username }` (can be a single-user list)
- `removeUserFromRoom` `{ virtualId }`
- `chatMessage` `{ virtualId, message }`
- `roomRejected` `{ reason }`
- `error` `{ code, message }` (see Errors)

## Errors

Server may send a structured `error` response for protocol issues. The connection stays open for recoverable errors.

- Unknown opcode
  - Incoming: `{ requestType: "???", payload: { ... } }`
  - Outgoing: `{ requestType: "error", payload: { code: "unknown_opcode", message: "Unknown requestType: ???" } }`

- Bad/invalid JSON payload
  - Incoming: malformed JSON or payload not matching the handler’s expected shape
  - Outgoing: `{ requestType: "error", payload: { code: "bad_request", message: "Invalid message format" } }`

Room access errors use a domain-specific response instead of `error`:

- Invalid room id on `joinRoom`
  - Outgoing: `{ requestType: "roomRejected", payload: { reason: "invalid" } }`

## Session Lifecycle

- Connect: a `GameClient` is registered for the WebSocket session.
- `login`: binds a `User` to the session and returns `loginOk`.
- Creating/joining rooms updates the user’s current room and emits room state messages.
- `roomList`: returns list and removes the user from current room.
- Disconnect: session is closed, user leaves room (if any), and resources are cleaned up.

## Notes & Extensibility

- The server uses an enum-backed opcode registry for safer routing.
- A shared, Spring-configured `ObjectMapper` handles JSON parsing; unknown properties are ignored.
- `Room` uses an `AtomicInteger` to assign virtual ids safely.
- CORS is open (`*`) for `/ws/game` by default; tighten for production.
- Authentication is illustrative only; integrate real SSO or token checks as needed.

## Example Flow

1. Login
   ```json
   { "requestType": "login", "payload": { "sso": "token-123" } }
   ```
   → `{ "requestType": "loginOk", ... }`

2. Create room
   ```json
   { "requestType": "createRoom", "payload": {} }
   ```
   → `prepareRoom`, `addUserToRoom`

3. Send chat
   ```json
   { "requestType": "chatMessage", "payload": { "message": "hi" } }
   ```
   → `chatMessage` broadcast to room

4. Get room list (and leave current room)
   ```json
   { "requestType": "roomList", "payload": {} }
   ```
   → `roomList`

## Development

- Main: `io.bobba.woodsheep.WoodsheepApplication`
- WS config/handler: `io.bobba.woodsheep.net.*`
- Core domains: `core/users`, `core/rooms`, `core/gameclients`
- Protocol: `core/communication/protocol`
- Handlers: `core/communication/incoming/*`
- Composers: `core/communication/outgoing/*`

