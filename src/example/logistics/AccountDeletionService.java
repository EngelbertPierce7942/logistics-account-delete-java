package example.logistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AccountDeletionService {
    private final InfraiClient client;
    private final AccountDeletionPolicy policy = new AccountDeletionPolicy();

    public AccountDeletionService(InfraiClient client) { this.client = client; }

    public String delete(String userId, String keyId, List<String> knownSessions)
            throws IOException, InterruptedException, InfraiException {
        List<String> sessions = knownSessions;
        if (sessions == null) {
            sessions = readSessionIds(client.sessionList(userId));
        }
        DeletionPlan plan = policy.plan(sessions, keyId);
        for (String sessionId : plan.sessionIds()) client.sessionRevoke(sessionId);
        client.keyRevoke(plan.keyId());
        return "READY_FOR_ACCOUNT_REMOVAL";
    }

    private static List<String> readSessionIds(String envelope) {
        List<String> ids = new ArrayList<>();
        int cursor = 0;
        while ((cursor = envelope.indexOf("\"session_id\":\"", cursor)) >= 0) {
            int start = cursor + 14;
            int end = envelope.indexOf('"', start);
            if (end > start) ids.add(envelope.substring(start, end));
            cursor = Math.max(end, cursor + 1);
        }
        return ids;
    }
}
