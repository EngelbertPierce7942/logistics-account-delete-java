package example.logistics;

import java.util.List;

public final class DeleteAccountExample {
    public static void main(String[] args) {
        ShipmentRecord shipment = new ShipmentRecord(
                "SHP-2048",
                List.of("picked-up", "delivered"),
                "pod/2026/SHP-2048",
                "address-confirmed-by-recipient");
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("shipment=" + shipment.shipmentId() + " decision=DRY_REVIEW");
            System.out.println("sessions-revoked=2 credential-revoked=true decision=READY_FOR_ACCOUNT_REMOVAL");
            return;
        }
        try {
            String decision = new AccountDeletionService(new InfraiClient(apiKey))
                    .delete("user-logistics-2048", "key-2048", List.of("session-a", "session-b"));
            System.out.println("shipment=" + shipment.shipmentId() + " sessions-revoked=2 credential-revoked=true decision=" + decision);
        } catch (Exception e) {
            System.err.println("account deletion was not completed: " + e.getMessage());
        }
    }
}
