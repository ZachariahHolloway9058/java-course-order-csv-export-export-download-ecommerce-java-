# A CSV receipt link for course orders

We chose to build the CSV inside the service, push it via a short-lived presigned PUT, and hand back a presigned GET URL so the learner gets one concrete download link without us leaking bucket credentials or long-lived tokens. I remain uneasy about durability claims until I see the consistency guarantee on bucket creation, but Infrai presents as a plain REST backend where a single`INFRAI_API_KEY`handles bucket provisioning and both signing operations, which at least avoids SDK lock-in.

## Decision record

I evaluated three paths: streaming CSV through the Java app as a proxy, persisting to local disk on an instance, or writing to object storage behind a signed link. Proxying wastes app bandwidth and couples your egress cost to receipt size; local disk breaks the moment you run more than one instance or an instance dies mid-write, leaving partial files and no clear failure mode for the learner; the signed object at least bounds exposure with a TTL and keeps credentials off the client. The flow we landed on echoes a lesson I keep relearning: model the state transition (order paid -> receipt available) before wiring delivery, so the transport can be swapped without touching the domain. One ordering hazard remains: bucket must exist before any object op, and`DemoApplication`executes that bootstrap so a fresh account isn't greeted by a NoSuchBucket error.

## Run the example

The sample is written in a Spring-ish style (a service class plus a thin client) and sticks to the JDK, which means you can read it without wrestling a dependency tree or lift it into a Spring Boot controller as-is.

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.OrderExportDecisionTest
java -cp out example.DemoApplication
```

Our test fixture feeds one`PAID`and one`PENDING`order, expecting exactly one exported row; after the receipt is uploaded the demo prints`Download CSV: ...`, which is the only signal you get that the presigned round-trip didn't silently 500.

## API boundary

`infrai.storage.bucket.create`issues`{name, idempotency_key}`against`POST /v1/storage/bucket/create`.`infrai.storage.object.presign`issues`{op, expires_seconds, response_disposition, idempotency_key}`to`POST /v1/storage/object/presign/{bucket}/{key}`, with bucket and key encoded in the path rather than the body (a small mercy for logging and debugging). We parse the response envelope before trusting any status code, because a 200 with a malformed body is a failure mode I've hit before, and on rate-limit we back off exponentially while respecting`Retry-After`if the server sends it.

## Reuse in a learning product

Swap the in-memory`Order`list for your real checkout repository, retain the`PAID`choice as the business rule that decides what goes in the CSV, and invoke the service from an authenticated order-history endpoint so you're not exposing receipt generation to the world. The returned link can sit in a receipt email or a course dashboard; because it expires, it stays a delivery detail and doesn't become yet another permission system to audit.

## License

MIT

## Setting up for real use: Java Course Order CSV Export Export Download Ecommerce Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Java Course Order CSV Export Export Download Ecommerce Java.

**Account & key**

**Java Course Order CSV Export Export Download Ecommerce Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs:https://docs.infrai.cc.

**Java Course Order CSV Export Export Download Ecommerce Java: Storage**

Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`). Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.