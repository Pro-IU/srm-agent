-- Step 3 additive migration. It is safe to re-run after the current 001 schema,
-- which already includes payment_terms_days for clean installations.

DROP PROCEDURE IF EXISTS srm_step3_payment_terms_migration;

DELIMITER $$
CREATE PROCEDURE srm_step3_payment_terms_migration()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'srm_supplier'
          AND column_name = 'payment_terms_days'
    ) THEN
        ALTER TABLE `srm_supplier`
            ADD COLUMN `payment_terms_days` INT DEFAULT NULL COMMENT '账期（天）' AFTER `delivery_score`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'srm_supplier'
          AND constraint_name = 'ck_srm_supplier_payment_terms'
    ) THEN
        ALTER TABLE `srm_supplier`
            ADD CONSTRAINT `ck_srm_supplier_payment_terms`
                CHECK (`payment_terms_days` IS NULL OR `payment_terms_days` >= 0);
    END IF;
END$$
DELIMITER ;

CALL srm_step3_payment_terms_migration();
DROP PROCEDURE srm_step3_payment_terms_migration;

-- Backfill only synthetic demo rows and never overwrite an existing local value.
UPDATE `srm_supplier`
SET `payment_terms_days` = CASE `supplier_id`
    WHEN 'sup_demo_alpha' THEN 45
    WHEN 'sup_demo_beta' THEN 30
    WHEN 'sup_demo_risk' THEN 15
    ELSE `payment_terms_days`
END
WHERE `supplier_id` IN ('sup_demo_alpha', 'sup_demo_beta', 'sup_demo_risk')
  AND `payment_terms_days` IS NULL;
