-- Kizárólag a Spring "demo" profiljával töltődik be; éles adatok mellé ne aktiváld.
INSERT INTO employee_account (id, display_name, pin_hash, role, enabled, created_at, updated_at) VALUES
('20000000-0000-0000-0000-000000000001', 'Buha Milán', '$2a$10$z3UzbzzCCmqKGl8QrsmZRO.ZUfuSkCyC1/CFGDySkoYL99QxUuPaq', 'ADMIN', TRUE, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-180 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
('20000000-0000-0000-0000-000000000002', 'Kovács Anna', '$2a$10$z3UzbzzCCmqKGl8QrsmZRO.ZUfuSkCyC1/CFGDySkoYL99QxUuPaq', 'EMPLOYEE', TRUE, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-120 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
('20000000-0000-0000-0000-000000000003', 'Kokos Peti', '$2a$10$z3UzbzzCCmqKGl8QrsmZRO.ZUfuSkCyC1/CFGDySkoYL99QxUuPaq', 'EMPLOYEE', TRUE, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-90 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
('20000000-0000-0000-0000-000000000004', 'Nagy Dóra', '$2a$10$z3UzbzzCCmqKGl8QrsmZRO.ZUfuSkCyC1/CFGDySkoYL99QxUuPaq', 'EMPLOYEE', TRUE, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-45 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));

UPDATE product_definition SET default_price = CASE code
  WHEN 'GYM_DAY' THEN 2500
  WHEN 'GYM_8_ENTRY' THEN 12000
  WHEN 'GYM_12_ENTRY' THEN 16000
  WHEN 'GYM_MONTHLY_UNLIMITED' THEN 15000
  WHEN 'GYM_WOMEN_UNLIMITED' THEN 13500
  WHEN 'GYM_STUDENT_PENSIONER' THEN 11000
  WHEN 'GYM_ANNUAL' THEN 125000
  WHEN 'GYM_3_MONTH' THEN 40000
  WHEN 'GYM_6_MONTH' THEN 76000
  WHEN 'GYM_COMBINED_MONTHLY' THEN 19000
  ELSE default_price END;

INSERT INTO guest (id, full_name, full_name_search, registration_type, created_at, created_by_employee_id) VALUES
('30000000-0000-0000-0000-000000000001', 'Buha Nikolett', 'buha nikolett', 'IMPORTED', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-150 days'), '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000002', 'Püspöki Boldizsár', 'püspöki boldizsár', 'IMPORTED', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-145 days'), '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000003', 'Szabó Zsófia', 'szabó zsófia', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-35 days'), '20000000-0000-0000-0000-000000000002'),
('30000000-0000-0000-0000-000000000004', 'Világbajnok Zsombi', 'világbajnok zsombi', 'IMPORTED', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-210 days'), '20000000-0000-0000-0000-000000000003'),
('30000000-0000-0000-0000-000000000005', 'Tóth Bence', 'tóth bence', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-20 days'), '20000000-0000-0000-0000-000000000002'),
('30000000-0000-0000-0000-000000000006', 'Németh Júlia', 'németh júlia', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-16 days'), '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000007', 'Varga Márk', 'varga márk', 'IMPORTED', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-240 days'), '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000008', 'Kiss Emese', 'kiss emese', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-11 days'), '20000000-0000-0000-0000-000000000003'),
('30000000-0000-0000-0000-000000000009', 'Farkas Dávid', 'farkas dávid', 'IMPORTED', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-300 days'), '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000010', 'Horváth Petra', 'horváth petra', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-8 days'), '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000011', 'Molnár Levente', 'molnár levente', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-6 days'), '20000000-0000-0000-0000-000000000002'),
('30000000-0000-0000-0000-000000000012', 'Kovács Réka', 'kovács réka', 'IMPORTED', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-180 days'), '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000013', 'Balogh Ádám', 'balogh ádám', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-4 days'), '20000000-0000-0000-0000-000000000003'),
('30000000-0000-0000-0000-000000000014', 'Lakatos Lili', 'lakatos lili', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days'), '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000015', 'Takács Máté', 'takács máté', 'IMPORTED', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-400 days'), '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000016', 'Sipos Dorina', 'sipos dorina', 'NEW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'), '20000000-0000-0000-0000-000000000002');

INSERT INTO guest_pass (id, guest_id, product_definition_id, product_name_snapshot, purchased_at, valid_from, valid_until, total_entries, remaining_entries, price_paid, payment_method, issued_by_employee_id) VALUES
('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', 'Korlátlan havi bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-8 days'), date('now', '-8 days'), date('now', '+21 days'), NULL, NULL, 15000, 'CASH', '20000000-0000-0000-0000-000000000002'),
('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '8 alkalmas bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-55 days'), date('now', '-55 days'), date('now', '+4 days'), 8, 2, 12000, 'BANK_CARD', '20000000-0000-0000-0000-000000000003'),
('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000005', 'Női korlátlan bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-27 days'), date('now', '-27 days'), date('now', '+2 days'), NULL, NULL, 13500, 'CASH', '20000000-0000-0000-0000-000000000002'),
('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000008', '3 havi bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-20 days'), date('now', '-20 days'), date('now', '+69 days'), NULL, NULL, 40000, 'BANK_CARD', '20000000-0000-0000-0000-000000000001'),
('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', '12 alkalmas bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-12 days'), date('now', '-12 days'), date('now', '+47 days'), 12, 8, 16000, 'CASH', '20000000-0000-0000-0000-000000000003'),
('40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000006', 'Diák / nyugdíjas havi bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-25 days'), date('now', '-25 days'), date('now', '+4 days'), NULL, NULL, 11000, 'BANK_CARD', '20000000-0000-0000-0000-000000000004'),
('40000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000007', 'Éves bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days'), date('now', '-70 days'), date('now', '+294 days'), NULL, NULL, 125000, 'BANK_CARD', '20000000-0000-0000-0000-000000000001'),
('40000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000009', '6 havi bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days'), date('now', '-40 days'), date('now', '+139 days'), NULL, NULL, 76000, 'CASH', '20000000-0000-0000-0000-000000000002'),
('40000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000010', 'Kombinált havi bérlet (+ 4 szauna)', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-29 days'), date('now', '-29 days'), date('now'), NULL, NULL, 19000, 'BANK_CARD', '20000000-0000-0000-0000-000000000003'),
('40000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', 'Napijegy', strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), date('now'), date('now'), 1, 1, 2500, 'CASH', '20000000-0000-0000-0000-000000000004'),
('40000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000004', 'Korlátlan havi bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-38 days'), date('now', '-38 days'), date('now', '-9 days'), NULL, NULL, 15000, 'CASH', '20000000-0000-0000-0000-000000000002'),
('40000000-0000-0000-0000-000000000012', '30000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000002', '8 alkalmas bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-64 days'), date('now', '-64 days'), date('now', '-5 days'), 8, 0, 12000, 'BANK_CARD', '20000000-0000-0000-0000-000000000001'),
('40000000-0000-0000-0000-000000000013', '30000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000003', '12 alkalmas bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-3 days'), date('now', '-3 days'), date('now', '+56 days'), 12, 11, 16000, 'BANK_CARD', '20000000-0000-0000-0000-000000000003'),
('40000000-0000-0000-0000-000000000014', '30000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000005', 'Női korlátlan bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'), date('now', '-1 day'), date('now', '+28 days'), NULL, NULL, 13500, 'CASH', '20000000-0000-0000-0000-000000000004'),
('40000000-0000-0000-0000-000000000015', '30000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000006', 'Diák / nyugdíjas havi bérlet', strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), date('now'), date('now', '+29 days'), NULL, NULL, 11000, 'BANK_CARD', '20000000-0000-0000-0000-000000000002');

INSERT INTO guest_check_in (id, guest_id, guest_pass_id, checked_in_at, checked_in_by_employee_id, consumed_entry) VALUES
('50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', strftime('%Y-%m-%dT08:12:00Z', 'now'), '20000000-0000-0000-0000-000000000002', FALSE),
('50000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', strftime('%Y-%m-%dT09:35:00Z', 'now'), '20000000-0000-0000-0000-000000000003', TRUE),
('50000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000005', strftime('%Y-%m-%dT11:20:00Z', 'now', '-1 day'), '20000000-0000-0000-0000-000000000004', TRUE),
('50000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000013', '40000000-0000-0000-0000-000000000013', strftime('%Y-%m-%dT16:05:00Z', 'now'), '20000000-0000-0000-0000-000000000003', TRUE);

INSERT INTO audit_log (id, employee_id, employee_name, action, entity_type, entity_id, summary, occurred_at) VALUES
('60000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Kovács Anna', 'GUEST_CHECKED_IN', 'CHECK_IN', '50000000-0000-0000-0000-000000000001', 'Vendég beléptetve: Buha Nikolett', strftime('%Y-%m-%dT08:12:00Z', 'now')),
('60000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'Kokos Peti', 'GUEST_CHECKED_IN', 'CHECK_IN', '50000000-0000-0000-0000-000000000002', 'Vendég beléptetve: Püspöki Boldizsár', strftime('%Y-%m-%dT09:35:00Z', 'now')),
('60000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', 'Nagy Dóra', 'PASS_SOLD', 'GUEST_PASS', '40000000-0000-0000-0000-000000000010', 'Bérlet kiállítva: Napijegy – Horváth Petra', strftime('%Y-%m-%dT10:10:00Z', 'now')),
('60000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000003', 'Kokos Peti', 'GUEST_CHECKED_IN', 'CHECK_IN', '50000000-0000-0000-0000-000000000004', 'Vendég beléptetve: Balogh Ádám', strftime('%Y-%m-%dT16:05:00Z', 'now'));
