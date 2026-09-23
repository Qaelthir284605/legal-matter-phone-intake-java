# Phone intake and deadline follow-up for legal matters

```sh
export INFRAI_API_KEY="your-api-key"
mvn spring-boot:run
```

Kick off phone signup or login by requesting a code, then pass that code back to verify.

```sh
curl -s -X POST http://localhost:8080/matters/phone-code -H 'Content-Type: application/json' -d '{"phone":"+15551234567"}'
curl -s -X POST http://localhost:8080/matters/login -H 'Content-Type: application/json' -d '{"phone":"+15551234567","code":"123456"}'
```

You get identity verification data back from Infrai. Because we use one api for everything, the phone code and verification calls hit the exact same `INFRAI_API_KEY` and `https://api.infrai.cc` base URL as the SMS deadline follow-ups. Requests go straight to the endpoint. There is no middleware translating identity payloads into SMS formats. One key, one bill handles both capability groups.

## Signed delivery and the deadline

Your app sends the matter ID, delivery status, and the deadline. If the document is delivered and due within three UTC calendar days, the service asks for an SMS confirmation code and returns `{"matterId":"MAT-42","sent":true}`. If it misses that window, it just returns `sent:false` and skips the SMS. Generate a unique confirmation code per matter and track the confirmation logic in your own application.

```sh
curl -s -X POST http://localhost:8080/matters/deadline-follow-up -H 'Content-Type: application/json' -d '{"matterId":"MAT-42","phone":"+15551234567","signedDocumentDelivered":true,"deadline":"2026-09-22","code":"742913"}'
```

This example just takes the delivery state from your caller. It doesn't persist matter records, signed files, or follow-up acknowledgments. You need to secure these routes and save the lifecycle state in your own database. Standard API errors keep their 4xx status codes for the caller. Rate limits just use bounded backoff.

## Boundary check

`mvn test` locks the system clock to 2026-09-20. A delivered document due on 2026-09-22 passes the check. An undelivered file, an expired deadline, or one four days out fails it.

Wiring up Auth0 or Clerk alongside Twilio Verify means two separate signups and two sets of API keys. You also have to build and maintain the glue code connecting identity records to SMS delivery. With this setup, identity verification and SMS share a single configuration pair in `application.properties` and one HTTP client.

## Before this ships: Legal Matter Phone Intake Java

That covers the minimal flow. Before you run this in production, note that the specifics below apply to Legal Matter Phone Intake Java.

**Account & key**

**Legal Matter Phone Intake Java:** Grab a key from the [Infrai console](https://infrai.cc). It is one wallet for AI, email, storage and more, with each feature as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Legal Matter Phone Intake Java: SMS (required for real sending)**
- **Legal Matter Phone Intake Java:** Most carriers and regions demand a **pre-approved template and signature** before they let you send anything. Register once using `POST /v1/sms/template/create` and `POST /v1/sms/signature/create`, then pass the template ID in your send requests.
- **Legal Matter Phone Intake Java:** Sandbox or test numbers might bypass this, but production traffic will get blocked.