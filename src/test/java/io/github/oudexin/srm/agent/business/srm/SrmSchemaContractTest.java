package io.github.oudexin.srm.agent.business.srm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Lightweight contract checks that keep SQL constraints and the synthetic risk fixtures discoverable. */
class SrmSchemaContractTest {

    @Test
    void schemaDefinesRelationshipAndQuantityConstraints() throws IOException {
        String schema = resource("db/srm/001_srm_schema.sql");
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `srm_supplier`"));
        assertTrue(schema.contains("FOREIGN KEY (`rfq_id`) REFERENCES `srm_rfq_project`"));
        assertTrue(schema.contains("FOREIGN KEY (`purchase_order_id`) REFERENCES `srm_purchase_order`"));
        assertTrue(schema.contains("ck_srm_po_line_received_quantity"));
        assertTrue(schema.contains("uk_srm_quote_rfq_supplier_version"));
    }

    @Test
    void demoDataIncludesNormalAndRiskScenarios() throws IOException {
        String data = resource("db/srm/002_srm_demo_data.sql");
        assertTrue(data.contains("'PARTIALLY_RECEIVED'"));
        assertTrue(data.contains("'SUSPENDED','HIGH'"));
        assertTrue(data.contains("必须人工复核，不得自动下达"));
        assertTrue(data.contains("INSERT IGNORE"));
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
