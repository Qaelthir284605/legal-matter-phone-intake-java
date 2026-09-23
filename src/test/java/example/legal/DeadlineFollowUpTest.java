package example.legal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DeadlineFollowUpTest {
    @Test
    void sendsOnlyForDeliveredDocumentsWithAnUpcomingDeadline() {
        LocalDate today = LocalDate.of(2026, 9, 20);
        assertTrue(DeadlineFollowUp.shouldSend(true, today.plusDays(2), today));
        assertFalse(DeadlineFollowUp.shouldSend(false, today.plusDays(2), today));
        assertFalse(DeadlineFollowUp.shouldSend(true, today.plusDays(4), today));
        assertFalse(DeadlineFollowUp.shouldSend(true, today.minusDays(1), today));
    }
}
