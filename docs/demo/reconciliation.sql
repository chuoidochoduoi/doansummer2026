-- Chỉ đọc. Chạy trên database demo đã seed để đối chiếu mã thực tế.
-- Không sử dụng mã bệnh án ghi cố định theo ngày của tài liệu cũ.
SELECT p.full_name AS patient, a.username,
       'VIS-' || upper(left(v.visit_id::text,8)) AS visit_code,
       m.record_code, dp.full_name AS doctor,
       i.invoice_code, i.status AS invoice_status, i.total_amount, i.paid_amount,
       pt.transaction_code, pt.payment_method, pt.amount,
       m.rating_score, m.rating_comment, m.rated_at, m.feedback_status,
       m.manager_response, m.responded_at
FROM profile p
JOIN account a ON a.account_id=p.account_id AND a.role='CUSTOMER'
JOIN customer_visit v ON v.customer_id=p.profile_id
JOIN medical_record m ON m.visit_id=v.visit_id AND m.status='COMPLETED'
JOIN staff_info d ON d.staff_id=m.doctor_id
JOIN profile dp ON dp.profile_id=d.profile_id
JOIN invoice i ON i.visit_id=v.visit_id
LEFT JOIN payment_transaction pt ON pt.invoice_id=i.invoice_id
ORDER BY a.username,m.completed_at,i.invoice_code;

-- Phòng/nhân viên thực hiện, người ký và người lấy mẫu của từng kết quả.
SELECT m.record_code, p.full_name AS patient, ms.name AS service,
       dep.room_code, cp.full_name AS collector, pp.full_name AS performer,
       sp.full_name AS signer, r.collected_at,r.performed_at,rev.signed_at,
       rev.status,m.rating_comment
FROM test_result r
JOIN test_request t ON t.test_request_id=r.test_request_id
JOIN medical_record m ON m.record_id=t.medical_record_id
JOIN customer_visit v ON v.visit_id=m.visit_id
JOIN profile p ON p.profile_id=v.customer_id
JOIN medical_service ms ON ms.service_id=t.service_id
JOIN department dep ON dep.department_id=t.performing_department
JOIN test_result_revision rev ON rev.result_id=r.result_id AND rev.status='SIGNED'
LEFT JOIN staff_info c ON c.staff_id=r.collected_by LEFT JOIN profile cp ON cp.profile_id=c.profile_id
JOIN staff_info performer ON performer.staff_id=r.performed_by JOIN profile pp ON pp.profile_id=performer.profile_id
JOIN staff_info signer ON signer.staff_id=rev.signed_by JOIN profile sp ON sp.profile_id=signer.profile_id
ORDER BY m.record_code;

-- Không coi số phiếu là vị trí hàng chờ; UI tiếp tục dùng QueuePriorityService.
SELECT 'VIS-' || upper(left(v.visit_id::text,8)) AS visit_code,p.full_name,
       q.work_date,d.room_code,ms.name,q.status,q.queue_number,
       m.record_code,m.status AS record_status
FROM queue_ticket q JOIN customer_visit v ON v.visit_id=q.visit_id
JOIN profile p ON p.profile_id=v.customer_id
JOIN department d ON d.department_id=q.department_id
JOIN medical_service ms ON ms.service_id=q.service_id
LEFT JOIN medical_record m ON m.queue_ticket_id=q.ticket_id
ORDER BY q.work_date DESC,d.room_code,q.queue_number;
