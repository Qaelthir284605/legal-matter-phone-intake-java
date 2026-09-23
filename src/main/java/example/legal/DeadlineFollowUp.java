package example.legal;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DeadlineFollowUp {
    private DeadlineFollowUp() {}

    public static boolean shouldSend(boolean signedDocumentDelivered, LocalDate deadline, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, deadline);
        return signedDocumentDelivered && days >= 0 && days <= 3;
    }
}
