package example.logistics;

import java.util.List;

public final class AccountDeletionPolicy {
    public DeletionPlan plan(List<String> sessionIds, String keyId) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            throw new IllegalArgumentException("an account deletion needs its session list");
        }
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("an account deletion needs its credential id");
        }
        return new DeletionPlan(List.copyOf(sessionIds), keyId, true);
    }
}
