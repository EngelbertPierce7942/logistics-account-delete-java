package example.logistics;

import java.util.List;

public final class AccountDeletionPolicyTest {
    public static void main(String[] args) {
        DeletionPlan plan = new AccountDeletionPolicy().plan(List.of("s1", "s2"), "k1");
        if (plan.sessionIds().size() != 2 || !plan.credentialRevocationLast()) {
            throw new AssertionError("credential must follow every session");
        }
        System.out.println("policy-test=passed");
    }
}
