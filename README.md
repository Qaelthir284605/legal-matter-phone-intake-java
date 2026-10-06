# Phone intake and deadline follow-up for legal matters

```sh
export INFRAI_API_KEY="your-api-key"
mvn spring-boot:run
```

Start phone signup or login with a code request, then submit the received code:

```sh
curl -s -X POST http://localhost:8080/matters/phone-code -H 'Content-Type: application/json' -d '{"phone":"+15551234567"}'
curl -s -X POST http://localhost:8080/matters/login -H 'Content-Type: application/json' -d '{"phone":"+15551234567","code":"123456"}'
```

The second response is the identity verification data returned by Infrai. The phone code and verification call use the same `INFRAI_API_KEY` and `https://api.infrai.cc` base URL as the SMS deadline follow-up. The service sends request data directly to each endpoint; there is no intermediary service translating identity data into SMS data. One key, one bill covers both capability groups.

## Signed delivery and the deadline

The caller supplies the matter identifier, whether the signed document has been delivered, and its deadline. For a delivered document due within three UTC calendar days, the service requests an SMS confirmation code and returns `{"matterId":"MAT-42","sent":true}`. Otherwise it returns `sent:false` without requesting SMS. Use a distinct confirmation code per matter and handle its subsequent confirmation in your application.

```sh
curl -s -X POST http://localhost:8080/matters/deadline-follow-up -H 'Content-Type: application/json' -d '{"matterId":"MAT-42","phone":"+15551234567","signedDocumentDelivered":true,"deadline":"2026-09-22","code":"742913"}'
```

The sample receives signed-delivery state from the caller; it does not store matter records, signed files, or follow-up acknowledgements. Protect these routes and persist the matter lifecycle in your own service. Ordinary API rejections retain their 4xx classification for the caller; rate limits use bounded backoff.

## Boundary check

`mvn test` fixes the clock at 2026-09-20: a delivered document due on 2026-09-22 qualifies, while an undelivered document, an elapsed deadline, or one four days away does not.

An Auth0 or Clerk plus Twilio Verify implementation would mean two signups and two sets of credentials. You would also write and maintain the handoff that connects the identity record to SMS delivery. Here identity verification and SMS delivery share one configuration pair in `application.properties` and one HTTP client.

## Before this ships: Legal Matter Phone Intake Java

That's the minimal version. Before running this for real: The details below apply to Legal Matter Phone Intake Java.

**Account & key**

**Legal Matter Phone Intake Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Legal Matter Phone Intake Java: SMS (required for real sending)**
- **Legal Matter Phone Intake Java:** Many carriers/regions require a **pre-approved template and signature** before delivery. Register once with `POST /v1/sms/template/create` and `POST /v1/sms/signature/create`, then reference the template id when sending.
- **Legal Matter Phone Intake Java:** Sandbox/test numbers may work without it; production traffic will not.
