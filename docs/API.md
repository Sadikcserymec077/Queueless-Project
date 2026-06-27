# QueueLess AI API Reference

All secured endpoints require:

```http
Authorization: Bearer <jwt>
```

Responses use a common wrapper:

```json
{
  "success": true,
  "message": "Operation completed",
  "data": {},
  "timestamp": "2026-06-27T12:00:00Z"
}
```

## Authentication

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | Register a user account |
| POST | `/api/auth/login` | Public | Login and receive JWT |

## User

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| GET | `/api/users/me` | USER, ADMIN | Current profile |
| GET | `/api/users/history` | USER, ADMIN | Booking history with pagination |

## Organizations

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| GET | `/api/organizations` | Public | Search organizations by name/type |
| GET | `/api/organizations/{id}` | Public | Organization details |
| POST | `/api/organizations` | ADMIN | Create organization |
| PUT | `/api/organizations/{id}` | ADMIN | Update organization |
| DELETE | `/api/organizations/{id}` | ADMIN | Disable organization |

## Counters

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| GET | `/api/counters/organization/{organizationId}` | Public | List counters for organization |
| GET | `/api/counters/{id}` | USER, ADMIN | Counter details |
| POST | `/api/counters` | ADMIN | Create counter |
| PUT | `/api/counters/{id}` | ADMIN | Update counter |
| PATCH | `/api/counters/{id}/disable` | ADMIN | Disable counter |

## Tokens

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| POST | `/api/tokens` | USER | Book virtual token |
| GET | `/api/tokens/me/active` | USER, ADMIN | Current active token |
| GET | `/api/tokens/{id}/status` | Owner, ADMIN | Live queue status |
| PATCH | `/api/tokens/{id}/cancel` | Owner, ADMIN | Cancel token |
| GET | `/api/tokens/admin/search` | ADMIN | Search tokens by status, date, user, organization, or token number |
| GET | `/api/tokens/counters/{counterId}/queue` | ADMIN | Current counter queue |
| POST | `/api/tokens/counters/{counterId}/call-next` | ADMIN | Call next waiting token |
| PATCH | `/api/tokens/{id}/complete` | ADMIN | Complete called token |
| PATCH | `/api/tokens/{id}/skip` | ADMIN | Skip waiting or called token |
| POST | `/api/tokens/verify-qr` | ADMIN | Verify QR payload |

## Notifications

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| GET | `/api/notifications` | USER, ADMIN | Retrieve notifications |
| PATCH | `/api/notifications/{id}/read` | Owner | Mark notification read |

## Analytics

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| GET | `/api/analytics/dashboard` | ADMIN | Dashboard stats and chart series |

## WebSocket

Connect to `/ws` with STOMP/SockJS.

| Topic | Payload |
| --- | --- |
| `/topic/counters/{counterId}` | Current counter queue snapshot |
| `/topic/queues` | Queue update event |
| `/topic/users/{userId}` | User token/status update |
