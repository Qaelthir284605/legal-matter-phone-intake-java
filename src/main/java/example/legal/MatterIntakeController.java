package example.legal;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matters")
public class MatterIntakeController {
    private final InfraiClient infrai;

    public MatterIntakeController(InfraiClient infrai) { this.infrai = infrai; }

    public record Phone(String phone) {}
    public record Confirm(String phone, String code) {}
    public record FollowUp(String matterId, String phone, boolean signedDocumentDelivered,
                           LocalDate deadline, String code) {}

    @PostMapping("/phone-code")
    public JsonNode sendCode(@RequestBody Phone input) {
        require(input.phone(), "phone");
        return infrai.post("/v1/auth/phone/send_code", Map.of("phone", input.phone(), "purpose", "login"));
    }

    @PostMapping("/login")
    public JsonNode login(@RequestBody Confirm input) {
        require(input.phone(), "phone");
        require(input.code(), "code");
        return infrai.post("/v1/auth/phone/verify", Map.of(
            "phone", input.phone(), "code", input.code(), "login", true));
    }

    @PostMapping("/deadline-follow-up")
    public ResponseEntity<Map<String, Object>> followUp(@RequestBody FollowUp input) {
        require(input.matterId(), "matterId");
        require(input.phone(), "phone");
        require(input.code(), "code");
        if (input.deadline() == null) throw new IllegalArgumentException("deadline is required");
        boolean due = DeadlineFollowUp.shouldSend(input.signedDocumentDelivered(), input.deadline(),
            LocalDate.now(ZoneOffset.UTC));
        if (!due) return ResponseEntity.ok(Map.of("matterId", input.matterId(), "sent", false));
        // The same credential and base URL serve identity and SMS; no relay service is needed.
        infrai.post("/v1/sms/otp", Map.of("to", input.phone()));
        return ResponseEntity.ok(Map.of("matterId", input.matterId(), "sent", true));
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(InfraiClient.Rejection.class)
    public ResponseEntity<JsonNode> rejected(InfraiClient.Rejection error) {
        int status = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(status).body(error.error());
    }
}
