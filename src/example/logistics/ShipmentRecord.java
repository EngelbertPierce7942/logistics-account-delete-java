package example.logistics;

import java.util.List;

public record ShipmentRecord(
        String shipmentId,
        List<String> events,
        String proofOfDeliveryReference,
        String exception
) {}
