package io.github.oudexin.srm.agent.business.srm.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SrmLifecycleTest {

    @Test
    void rfqFollowsTheControlledSourcingFlow() {
        assertTrue(RfqStatus.DRAFT.canTransitionTo(RfqStatus.PUBLISHED));
        assertTrue(RfqStatus.QUOTING.canTransitionTo(RfqStatus.QUOTE_CLOSED));
        assertTrue(RfqStatus.EVALUATING.canTransitionTo(RfqStatus.AWARDED));
        assertFalse(RfqStatus.PUBLISHED.canTransitionTo(RfqStatus.AWARDED));
        assertFalse(RfqStatus.AWARDED.canTransitionTo(RfqStatus.CANCELLED));
    }

    @Test
    void releasedOrderCannotBypassReceiptTracking() {
        assertTrue(PurchaseOrderStatus.DRAFT.canTransitionTo(PurchaseOrderStatus.PENDING_APPROVAL));
        assertTrue(PurchaseOrderStatus.APPROVED.canTransitionTo(PurchaseOrderStatus.RELEASED));
        assertTrue(PurchaseOrderStatus.RELEASED.canTransitionTo(PurchaseOrderStatus.PARTIALLY_RECEIVED));
        assertFalse(PurchaseOrderStatus.DRAFT.canTransitionTo(PurchaseOrderStatus.RELEASED));
        assertFalse(PurchaseOrderStatus.CLOSED.canTransitionTo(PurchaseOrderStatus.RECEIVED));
    }
}
