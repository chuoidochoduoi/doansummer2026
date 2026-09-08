-- Run after data2.sql. Every query in the ERROR section must return zero rows.
SET TIME ZONE 'Asia/Ho_Chi_Minh';

-- Summary for the presenter.
SELECT 'customers' metric,count(*) value FROM account WHERE role='CUSTOMER'
UNION ALL SELECT 'doctors',count(*) FROM staff_info WHERE system_role='DOCTOR' AND NOT deleted
UNION ALL SELECT 'visits',count(*) FROM customer_visit WHERE NOT deleted
UNION ALL SELECT 'completed_records',count(*) FROM medical_record WHERE status='COMPLETED' AND NOT deleted
UNION ALL SELECT 'signed_results',count(*) FROM test_result_revision WHERE status='SIGNED' AND NOT deleted
UNION ALL SELECT 'paid_invoices',count(*) FROM invoice WHERE status='PAID' AND NOT deleted;

-- Customer -> VIS -> medical record -> invoice -> payment -> feedback.
SELECT p.phone AS customer_account,p.full_name,
       'VIS-'||upper(left(history.visit_id::text,8)) AS visit_code,
       history.record_code,history.invoice_code,history.transaction_code,history.rating_score
FROM account a JOIN profile p ON p.account_id=a.account_id
JOIN LATERAL(SELECT cv.visit_id,r.record_code,i.invoice_code,t.transaction_code,r.rating_score
  FROM customer_visit cv JOIN medical_record r ON r.visit_id=cv.visit_id
  JOIN invoice i ON i.visit_id=cv.visit_id
  JOIN payment_transaction t ON t.invoice_id=i.invoice_id
  WHERE cv.customer_id=p.profile_id AND cv.status='COMPLETED' AND NOT cv.deleted
    AND r.status='COMPLETED' AND r.rating_score IS NOT NULL AND NOT r.deleted
    AND i.status='PAID' AND NOT i.deleted AND t.status='SUCCESS' AND NOT t.deleted
  ORDER BY cv.check_in_time DESC LIMIT 1) history ON true
WHERE a.role='CUSTOMER'
ORDER BY p.phone;

-- Room-doctor and schedule overview.
SELECT d.room_code,d.name,p.full_name,a.username,
 count(DISTINCT ss.work_date) scheduled_days,count(DISTINCT ss.shift_id) shifts
FROM department d JOIN staff_info s ON s.department_id=d.department_id AND s.system_role='DOCTOR'
JOIN profile p ON p.profile_id=s.profile_id JOIN account a ON a.account_id=p.account_id
LEFT JOIN staff_schedule ss ON ss.staff_id=s.staff_id AND NOT ss.deleted
GROUP BY d.room_code,d.name,p.full_name,a.username ORDER BY d.room_code;

-- ERROR: room does not have exactly one doctor.
SELECT d.room_code,count(s.staff_id) doctor_count FROM department d
LEFT JOIN staff_info s ON s.department_id=d.department_id AND s.system_role='DOCTOR' AND NOT s.deleted
WHERE NOT d.deleted AND d.status='AVAILABLE' GROUP BY d.room_code HAVING count(s.staff_id)<>1;

-- ERROR: Sunday activity.
SELECT 'schedule' source,schedule_id::text id,work_date FROM staff_schedule WHERE EXTRACT(ISODOW FROM work_date)=7 AND NOT deleted
UNION ALL SELECT 'appointment',appointment_id::text,scheduled_at::date FROM appointment
WHERE EXTRACT(ISODOW FROM scheduled_at)=7 AND status<>'CANCELLED' AND NOT deleted;

-- ERROR: invoice reconciliation.
SELECT i.invoice_code,i.subtotal,i.discount,i.tax,i.total_amount,i.paid_amount,
 (SELECT COALESCE(sum(t.amount),0) FROM payment_transaction t WHERE t.invoice_id=i.invoice_id AND t.status='SUCCESS') successful
FROM invoice i WHERE i.subtotal<>(SELECT COALESCE(sum(ii.unit_price*ii.quantity),0) FROM invoice_item ii WHERE ii.invoice_id=i.invoice_id)
 OR i.total_amount<>i.subtotal-i.discount+i.tax
 OR (i.status='PAID' AND i.paid_amount<>(SELECT COALESCE(sum(t.amount),0) FROM payment_transaction t WHERE t.invoice_id=i.invoice_id AND t.status='SUCCESS'));

-- Report coverage.
SELECT date_trunc('month',check_in_time)::date AS report_month,count(*) AS visits
FROM customer_visit GROUP BY 1 ORDER BY 1;
SELECT d.room_code,count(q.ticket_id) tickets FROM department d
LEFT JOIN queue_ticket q ON q.department_id=d.department_id AND NOT q.deleted
GROUP BY d.room_code ORDER BY d.room_code;
SELECT ii.service_code_snapshot,ii.service_snapshot,count(*) quantity,sum(ii.line_total) gross
FROM invoice_item ii JOIN invoice i ON i.invoice_id=ii.invoice_id
WHERE i.issue_date::date >= (date_trunc('month',current_date)-interval '2 months')::date
GROUP BY ii.service_code_snapshot,ii.service_snapshot ORDER BY quantity DESC;
