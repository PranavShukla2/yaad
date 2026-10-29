INSERT INTO reminder (id, title, notes, type, category_id, start_local, rule, time_zone_id, all_day,
    early_alert_seconds, priority, nag_seconds, amount_minor, amount_currency, amount_minor_digits, auto_pay,
    linked_loan_id, archived, updated_at)
VALUES ('emi-1', 'Home loan EMI', 'HDFC', 'Emi', 'money', '2026-11-05T09:00',
    'FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=5;COUNT=240', 'Asia/Kolkata', 1, '259200,3600', 'High', 1800, 867823, 'INR',
    2, 1, 'loan-1', 0, 1793181600000);
INSERT INTO reminder (id, title, type, start_local, priority, updated_at)
VALUES ('call-1', 'Call mom', 'General', '2026-10-29T19:00', 'Normal', 1793181600000);
INSERT INTO checklist_item VALUES ('emi-1', 0, 'Check balance', 0);
INSERT INTO checklist_item VALUES ('emi-1', 1, 'Pay', 1);
INSERT INTO occurrence VALUES ('emi-1', 1793856600000, 'done', 1793860000000, 867823, 'INR', 2, 'paid by UPI');
INSERT INTO category VALUES ('gym', 'Gym', 4279200371);
INSERT INTO setting VALUES ('currency', 'INR');
INSERT INTO loan VALUES ('loan-1', 'HDFC', 'Home', 100000000, 'INR', 2, 8500, 240, '2026-11-05', NULL, 1793181600000);
INSERT INTO loan_event VALUES ('loan-1', 0, 12, 'prepay_tenure', 10000000, NULL);
INSERT INTO loan_event VALUES ('loan-1', 1, 24, 'rate_keep_emi', NULL, 9000);
