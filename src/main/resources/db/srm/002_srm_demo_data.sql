-- Repeatable synthetic Step-2 SRM demo data. Every name, code, address, contact and scenario is fictional.
-- This script is intentionally idempotent: it preserves existing rows with the same synthetic primary keys.

INSERT IGNORE INTO `srm_supplier`
(`supplier_id`,`supplier_code`,`supplier_name`,`supply_category`,`status`,`risk_level`,`quality_score`,`delivery_score`,`payment_terms_days`,`country_region`,`contact_name`,`contact_email`,`risk_note`)
VALUES
('sup_demo_alpha','SYN-ALPHA-001','华东精密制造（合成）','机加工件','QUALIFIED','LOW',96.50,94.00,45,'中国-华东','林澈','lin.che@example.test','质量与交付表现稳定（合成评分）'),
('sup_demo_beta','SYN-BETA-002','远航电子元件（合成）','电子连接件','POTENTIAL','MEDIUM',88.00,86.50,30,'中国-华南','周遥','zhou.yao@example.test','首次导入，需完成现场审核（合成场景）'),
('sup_demo_risk','SYN-RISK-003','晨星表面处理（合成）','表面处理','SUSPENDED','HIGH',72.00,68.00,15,'中国-华中','陈岚','chen.lan@example.test','交付波动与整改未关闭，暂停新增业务（合成风险场景）');

INSERT IGNORE INTO `srm_rfq_project`
(`rfq_id`,`rfq_no`,`project_name`,`material_code`,`material_name`,`required_quantity`,`unit`,`required_delivery_date`,`quotation_deadline`,`budget_amount`,`currency`,`status`,`owner_id`,`award_supplier_id`,`award_quote_id`)
VALUES
('rfq_demo_001','RFQ-DEMO-2026-001','阀体机加工件年度补货（合成）','MAT-DEMO-VALVE-01','阀体机加工件（合成）',100.000,'件','2026-10-15','2026-09-10 18:00:00',13000.00,'CNY','AWARDED','buyer_demo_01','sup_demo_alpha','quote_demo_001_a'),
('rfq_demo_002','RFQ-DEMO-2026-002','连接器试制批寻源（合成）','MAT-DEMO-CONN-02','工业连接器（合成）',500.000,'只','2026-11-20','2026-10-05 18:00:00',18000.00,'CNY','QUOTING','buyer_demo_02',NULL,NULL),
('rfq_demo_003','RFQ-DEMO-2026-003','表面处理外协复审（合成风险）','MAT-DEMO-COAT-03','耐蚀表面处理服务（合成）',200.000,'批','2026-10-30','2026-09-18 18:00:00',26000.00,'CNY','EVALUATING','buyer_demo_01',NULL,NULL);

INSERT IGNORE INTO `srm_supplier_quote`
(`quote_id`,`quote_no`,`rfq_id`,`supplier_id`,`quote_version`,`status`,`unit_price`,`quoted_quantity`,`tax_rate`,`total_amount`,`currency`,`lead_time_days`,`valid_until`,`submitted_at`,`risk_note`)
VALUES
('quote_demo_001_a','SQ-DEMO-001-A','rfq_demo_001','sup_demo_alpha',1,'ACCEPTED',112.5000,100.000,0.1300,12712.50,'CNY',20,'2026-10-31','2026-09-08 10:30:00',NULL),
('quote_demo_001_b','SQ-DEMO-001-B','rfq_demo_001','sup_demo_beta',1,'REJECTED',118.0000,100.000,0.1300,13334.00,'CNY',28,'2026-10-31','2026-09-08 15:20:00','价格与交期均不占优（合成评审结果）'),
('quote_demo_002_b','SQ-DEMO-002-B','rfq_demo_002','sup_demo_beta',1,'SUBMITTED',31.8000,500.000,0.1300,17967.00,'CNY',35,'2026-10-20','2026-09-28 09:15:00','潜在供应商，需资质审核后方可定点'),
('quote_demo_003_r','SQ-DEMO-003-R','rfq_demo_003','sup_demo_risk',1,'SUBMITTED',108.0000,200.000,0.1300,24408.00,'CNY',15,'2026-10-10','2026-09-16 11:45:00','高风险供应商报价，仅用于异常识别与人工复核演示');

INSERT IGNORE INTO `srm_price_library_record`
(`price_record_id`,`supplier_id`,`material_code`,`material_name`,`unit`,`unit_price`,`currency`,`minimum_order_quantity`,`effective_start_date`,`effective_end_date`,`status`,`source_quote_id`,`remark`)
VALUES
('price_demo_001','sup_demo_alpha','MAT-DEMO-VALVE-01','阀体机加工件（合成）','件',112.5000,'CNY',50.000,'2026-09-15','2027-03-31','VALID','quote_demo_001_a','来自中选报价的合成价格库记录'),
('price_demo_002','sup_demo_beta','MAT-DEMO-CONN-02','工业连接器（合成）','只',31.8000,'CNY',500.000,'2026-09-28','2026-10-20','EXPIRING','quote_demo_002_b','即将到期，供价格风险提醒演示'),
('price_demo_003','sup_demo_risk','MAT-DEMO-COAT-03','耐蚀表面处理服务（合成）','批',108.0000,'CNY',100.000,'2026-09-16','2026-12-31','SUSPENDED','quote_demo_003_r','供应商暂停合作，价格记录不可用于下单');

INSERT IGNORE INTO `srm_purchase_order`
(`purchase_order_id`,`po_no`,`supplier_id`,`source_rfq_id`,`selected_quote_id`,`status`,`purchaser_id`,`order_date`,`expected_delivery_date`,`currency`,`total_amount`,`high_risk_review_required`,`remark`)
VALUES
('po_demo_001','PO-DEMO-2026-001','sup_demo_alpha','rfq_demo_001','quote_demo_001_a','PARTIALLY_RECEIVED','buyer_demo_01','2026-09-15','2026-10-15','CNY',12712.50,0,'正常流程：已定点、已下达、部分收货（合成）'),
('po_demo_approve','PO-DEMO-2026-002','sup_demo_alpha','rfq_demo_001','quote_demo_001_a','PENDING_APPROVAL','buyer_demo_01','2026-09-20','2026-11-01','CNY',12712.50,0,'待人工审批的正常合成订单，用于确认流程演示'),
('po_demo_risk','PO-DEMO-2026-R01','sup_demo_risk','rfq_demo_003',NULL,'PENDING_APPROVAL','buyer_demo_01','2026-09-19','2026-10-30','CNY',24408.00,1,'风险场景：暂停供应商且RFQ未定点，必须人工复核，不得自动下达');

INSERT IGNORE INTO `srm_purchase_order_line`
(`purchase_order_id`,`line_no`,`material_code`,`material_name`,`unit`,`ordered_quantity`,`received_quantity`,`unit_price`,`tax_rate`,`status`,`remark`)
VALUES
('po_demo_001',1,'MAT-DEMO-VALVE-01','阀体机加工件（合成）','件',100.000,40.000,112.5000,0.1300,'PARTIALLY_RECEIVED','已收40件，待跟催余量（合成）'),
('po_demo_approve',1,'MAT-DEMO-VALVE-01','阀体机加工件（合成）','件',100.000,0.000,112.5000,0.1300,'OPEN','待人工审批的合成明细'),
('po_demo_risk',1,'MAT-DEMO-COAT-03','耐蚀表面处理服务（合成）','批',200.000,0.000,108.0000,0.1300,'OPEN','高风险演示订单，审批前禁止释放');
