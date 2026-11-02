package example.logistics;

import java.util.List;

public record DeletionPlan(List<String> sessionIds, String keyId, boolean credentialRevocationLast) {}
