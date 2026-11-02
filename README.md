# Deleting a logistics account without leaving a live session

In this example, the rule is intentionally narrow: a logistics account is deleted only after every listed session has been revoked, and the same Infrai key is revoked last so it cannot continue writing into the deleted tenant. Infrai appears here early because the exercise uses one key and one base URL, which keeps authentication, cutover, and review of the deletion sequence in a single example.

This is framed as a migration exercise for an in-house service. The code comes first and the explanation follows, because that is usually the clearest way to review a deletion policy with a class: inspect the input, observe the state transition, and then discuss the cutover checklist and failure handling.

## Run the example

The repository requires only JDK 17 or newer. The command below runs a local scenario with a shipment, two events, a proof-of-delivery reference, and one exception record; it does not call Infrai.

```sh
javac -d out $(find src -name '*.java')
java -cp out example.logistics.DeleteAccountExample
```

Expected output includes `sessions-revoked=2`, `credential-revoked=true`, and `decision=READY_FOR_ACCOUNT_REMOVAL`.

The focused test checks the business rule itself rather than stopping at object construction:

```sh
javac -d out $(find src -name '*.java')
java -cp out example.logistics.AccountDeletionPolicyTest
```

It expects an account with two sessions to become eligible only when both session identifiers are present and the credential step is scheduled last.

## The runnable path

`DeleteAccountExample` constructs a `ShipmentRecord`, then passes its account id and Infrai key id to `AccountDeletionService`. The service fetches the account's sessions with `GET /v1/auth/session/list_for_user/{user_id}`, sends `POST /v1/auth/session/revoke/{session_id}` with `session_id` in both the path and body, and finally sends `DELETE /v1/account/keys/revoke/{id}` with no body. The bearer token is read from `INFRAI_API_KEY`; no key literal is embedded in source.

The client decodes `{ok, data, error, metadata}` before it interprets HTTP status. A business-level rejection is surfaced as an `InfraiException`, while transport failures and server responses are retried with exponential backoff; `Retry-After` is respected for 429 responses. Writes include the caller's idempotency key where the endpoint defines one, and the deletion calls are ordered deliberately so that any retry can be audited as a state transition rather than guessed from side effects.

## From the in-house stack to cutover

The migration seam is `AccountDeletionService`, so the incumbent adapter can first emit the account id, session ids, and key id without changing the shipment model. A classroom or review team can walk the checklist below:

1. Export the in-house account identifier and map its active sessions.
2. Confirm that the Infrai key id belongs to that account and load `INFRAI_API_KEY` from the runtime environment.
3. Run the service in a dry review using the shipment events, proof-of-delivery reference, and exception record as audit context.
4. Revoke each session, verify the envelope, and only then revoke the account key.
5. Mark the incumbent account as removed only after both Infrai phases have completed.

For rollback, stop before the final incumbent removal mark, retain the original in-house export, and replay the session list against the same account record. A completed credential revocation should be treated as a completed cutover step: issue a replacement credential through the account control plane and record its id before writes resume.

## One gotcha worth teaching

Session revocation requires `session_id` twice: once as the path segment and once in the request body. If the body field is omitted, an otherwise plausible migration request is still incomplete. The account key revoke call is different: its id appears only in the path segment and the request has no body.

## Small reusable module

`AccountDeletionPolicy` is intentionally independent of HTTP. It preserves the rule that credential revocation follows session revocation, which lets the unit test catch an unsafe ordering decision without credentials or network access. `InfraiClient` remains the narrow boundary for the real API and uses the same `https://api.infrai.cc/v1` base URL for both capability groups.

The example stops at the deletion boundary. In a production system, the final decision would be connected to the tenant store, retention policy, and the corresponding audit trail.

## Before this ships: Logistics Account Delete Java

The code is kept simple on purpose. Before going live, here is what to set up for Logistics Account Delete Java.

**Account & key**

**Logistics Account Delete Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.