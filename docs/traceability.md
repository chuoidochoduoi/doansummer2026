# Bảng truy vết chức năng

| Chức năng | Route frontend tiêu biểu | API/controller | Service chính | Domain chính | Kiểm thử trọng tâm |
|---|---|---|---|---|---|
| Đặt lịch | `/customer/appointment`, `/appointment` | AppointmentController | AppointmentService | Appointment, ShiftVersion | đặt nhóm, dịch vụ, ca |
| Tiếp nhận | `/receptionist/check-in` | AppointmentController, CustomerVisitController | AppointmentService, CustomerVisitService | Appointment, CustomerVisit | quyền, idempotency |
| Tạo phiếu | `/receptionist/create-ticket` | CustomerVisitController | CustomerVisitService | Visit, Invoice, QueueTicket | nhiều dịch vụ, thứ tự |
| Hàng chờ | `/doctor/departments/:departmentId` | QueueTicket endpoints | QueueTicketService, QueuePriorityService | QueueTicket | ưu tiên, khóa phòng |
| Khám bệnh | `/doctor/examinations/department/:departmentId` | DoctorExaminationController | MedicalRecordService | MedicalRecord, VitalSigns | lưu nháp, hoàn thành |
| CLS | `/doctor/lab/:departmentId`, `/lab/:id` | TestRequestController | TestRequestService | TestRequest, TestResult | nhóm gói, ký kết quả |
| Thanh toán | `/cashier/invoices` | Invoice/Transaction endpoints | InvoiceService, TransactionService | Invoice, Transaction | BHYT, CareS, hoàn tác |
| Hành trình | `/customer/waiting-room`, `/staff/patient-journeys` | Journey endpoints | PatientJourneyService | Visit, Ticket, Invoice | thứ tự chu kỳ |
| Lịch trực | `/admin/schedule`, `/staff/schedule` | schedule controllers | StaffScheduleService | StaffSchedule, Department | độ phủ bác sĩ |
| Tái khám | `/receptionist/follow-ups` | follow-up endpoints | MedicalRecordService | MedicalRecord, Appointment | thiết lập, quyền |

Bảng này mô tả điểm vào, không thay thế Swagger hoặc test source. Khi route hay service đổi, cập nhật cùng commit.
