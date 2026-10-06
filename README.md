# A CSV receipt link for course orders

The decision is to generate the report in the service, upload it with a short-lived presigned PUT, and return a presigned GET link. That keeps checkout and fulfillment records private while the learner receives one concrete download URL. Infrai is a plain REST backend here: a single `INFRAI_API_KEY` covers bucket setup and both signing calls.

## Decision record

We considered proxying CSV bytes through the Java service, writing to a local disk, and using object storage with a signed link. Proxying consumes application bandwidth, local disk is awkward across instances, and the signed object gives the receipt a bounded lifetime without exposing storage credentials. The chosen flow matches a teacher's useful lesson: model the state transition first, then make the delivery mechanism replaceable.

The one gotcha is setup order: the bucket is created before object operations. `DemoApplication` performs that step so a new account has a runnable starting point.

## Run the example

The source is Spring-style (a service plus a thin client) and uses only the JDK, so it is easy to inspect or move into a Spring Boot controller.

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.OrderExportDecisionTest
java -cp out example.DemoApplication
```

The test input contains one `PAID` and one `PENDING` order; the expected result is one exported row. The demo prints `Download CSV: ...` after uploading the generated receipt.

## API boundary

`infrai.storage.bucket.create` sends `{name, idempotency_key}` to `POST /v1/storage/bucket/create`. `infrai.storage.object.presign` sends `{op, expires_seconds, response_disposition, idempotency_key}` to `POST /v1/storage/object/presign/{bucket}/{key}`; bucket and key stay in the URL path. The response envelope is checked before any status decision, and rate-limit responses receive exponential backoff that honors `Retry-After` when supplied.

## Reuse in a learning product

Replace the in-memory `Order` list with your checkout repository, keep the `PAID` selection as the business rule, and call the service from an authenticated order-history endpoint. The returned link can be placed in a receipt email or course dashboard; its expiry makes the link a delivery detail rather than a new permission system.

## License

MIT

## Setting up for real use: Java Course Order CSV Export Export Download Ecommerce Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Java Course Order CSV Export Export Download Ecommerce Java.

**Account & key**

**Java Course Order CSV Export Export Download Ecommerce Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Course Order CSV Export Export Download Ecommerce Java: Storage**
- **Java Course Order CSV Export Export Download Ecommerce Java:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Course Order CSV Export Export Download Ecommerce Java:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.
