--
-- PostgreSQL database dump
--

\restrict qUPdSDLPQbT0xGjdF8FtcrvnCa4BCcjpYaMXZwbkjMaD506urkWBcHbcvQbadnx

-- Dumped from database version 16.13 (Debian 16.13-1.pgdg13+1)
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account (
    account_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    is_active boolean NOT NULL,
    password_hash character varying(100) NOT NULL,
    role character varying(20) NOT NULL,
    username character varying(50) NOT NULL,
    CONSTRAINT account_role_check CHECK (((role)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'STAFF'::character varying])::text[])))
);


--
-- Name: appointment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.appointment (
    appointment_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    cancel_reason character varying(500),
    guest_address character varying(255),
    guest_age integer,
    guest_email character varying(255),
    guest_full_name character varying(100),
    guest_gender character varying(10),
    guest_phone character varying(15),
    is_guest boolean,
    scheduled_at timestamp(6) without time zone NOT NULL,
    shift_name character varying(100),
    shift_time character varying(50),
    status character varying(20) NOT NULL,
    customer_id uuid,
    CONSTRAINT appointment_guest_gender_check CHECK (((guest_gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT appointment_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CHECKED_IN'::character varying, 'CANCELLED'::character varying, 'RESCHEDULED'::character varying])::text[])))
);


--
-- Name: appointment_services; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.appointment_services (
    appointment_id uuid NOT NULL,
    service_id uuid NOT NULL
);


--
-- Name: attendance_adjustment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attendance_adjustment (
    adjustment_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    reason character varying(1000) NOT NULL,
    requested_check_in timestamp(6) without time zone,
    requested_check_out timestamp(6) without time zone,
    review_note character varying(1000),
    reviewed_at timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    attendance_id uuid NOT NULL,
    reviewed_by uuid,
    CONSTRAINT attendance_adjustment_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: attendance_qr_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attendance_qr_token (
    token_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    active boolean NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    token_hash character varying(64) NOT NULL,
    created_by uuid NOT NULL
);


--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    audit_id uuid NOT NULL,
    action character varying(40) NOT NULL,
    actor_account_id uuid,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    description text,
    entity_id character varying(50),
    entity_name character varying(100) NOT NULL,
    ip_address character varying(50),
    new_value text,
    old_value text,
    user_agent character varying(500),
    CONSTRAINT audit_log_action_check CHECK (((action)::text = ANY ((ARRAY['CREATE'::character varying, 'UPDATE'::character varying, 'DELETE'::character varying, 'LOGIN'::character varying, 'LOGOUT'::character varying, 'LOGIN_FAILED'::character varying, 'EXPORT'::character varying, 'IMPORT'::character varying, 'VIEW'::character varying, 'STATUS_CHANGE'::character varying, 'PAYMENT_CONFIRMED'::character varying, 'PATIENT_CALLED'::character varying, 'QUEUE_SKIPPED'::character varying, 'EXAM_STARTED'::character varying, 'DRAFT_SAVED'::character varying, 'RECORD_COMPLETED'::character varying, 'RESULT_UPLOADED'::character varying, 'RESULT_SIGNED'::character varying, 'COMPLETED_RECORD_EDITED'::character varying])::text[])))
);


--
-- Name: chat_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chat_messages (
    message_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    content text,
    sender_id uuid,
    sender_type character varying(255) NOT NULL,
    session_id uuid NOT NULL,
    CONSTRAINT chat_messages_sender_type_check CHECK (((sender_type)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'BOT'::character varying, 'RECEPTIONIST'::character varying])::text[])))
);


--
-- Name: chat_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chat_sessions (
    session_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    assigned_receptionist_id uuid,
    status character varying(255) NOT NULL,
    customer_id uuid NOT NULL,
    CONSTRAINT chat_sessions_status_check CHECK (((status)::text = ANY ((ARRAY['BOT_HANDLING'::character varying, 'WAITING_FOR_AGENT'::character varying, 'IN_PROGRESS'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: customer_visit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_visit (
    visit_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    check_in_time timestamp(6) without time zone NOT NULL,
    check_out_time timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    appointment_id uuid,
    checked_in_by uuid,
    customer_id uuid,
    CONSTRAINT customer_visit_status_check CHECK (((status)::text = ANY ((ARRAY['CHECKED_IN'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: department; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.department (
    department_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    department_type character varying(20) NOT NULL,
    description character varying(500),
    name character varying(150) NOT NULL,
    room_code character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    head_doctor_id uuid,
    specialization_id uuid,
    CONSTRAINT department_department_type_check CHECK (((department_type)::text = ANY ((ARRAY['EXAMINATION'::character varying, 'PARACLINICAL'::character varying, 'LABORATORY'::character varying, 'IMAGING'::character varying])::text[]))),
    CONSTRAINT department_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'IN_SESSION'::character varying, 'MAINTENANCE'::character varying])::text[])))
);


--
-- Name: department_capability; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.department_capability (
    department_id uuid NOT NULL,
    capability_id uuid NOT NULL
);


--
-- Name: feedback_target; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feedback_target (
    feedback_target_id uuid NOT NULL,
    comment character varying(500),
    rating integer NOT NULL,
    source_record_id uuid,
    staff_explanation character varying(1000),
    target_key character varying(100) NOT NULL,
    target_name character varying(200) NOT NULL,
    target_type character varying(30) NOT NULL,
    medical_record_id uuid NOT NULL,
    staff_id uuid
);


--
-- Name: icd_10_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.icd_10_codes (
    code character varying(10) NOT NULL,
    category character varying(100),
    deleted boolean NOT NULL,
    description text,
    name character varying(255) NOT NULL
);


--
-- Name: icd_10_selections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.icd_10_selections (
    selection_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    code character varying(10) NOT NULL,
    code_name character varying(255),
    note text,
    record_id uuid NOT NULL
);


--
-- Name: insurance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insurance (
    insurance_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    code character varying(50) NOT NULL,
    description character varying(1000),
    name character varying(200) NOT NULL
);


--
-- Name: insurance_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insurance_rule (
    rule_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    department_type character varying(30) NOT NULL,
    discount_percent numeric(5,2) NOT NULL,
    insurance_id uuid NOT NULL,
    CONSTRAINT insurance_rule_department_type_check CHECK (((department_type)::text = ANY ((ARRAY['EXAMINATION'::character varying, 'PARACLINICAL'::character varying, 'LABORATORY'::character varying, 'IMAGING'::character varying])::text[])))
);


--
-- Name: invoice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice (
    invoice_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    discount numeric(18,2) NOT NULL,
    due_date date,
    invoice_code character varying(30) NOT NULL,
    issue_date date NOT NULL,
    note text,
    paid_amount numeric(18,2) NOT NULL,
    status character varying(20) NOT NULL,
    subtotal numeric(18,2) NOT NULL,
    tax numeric(18,2) NOT NULL,
    total_amount numeric(18,2) NOT NULL,
    customer_id uuid NOT NULL,
    issued_by uuid,
    medical_record_id uuid,
    visit_id uuid,
    CONSTRAINT invoice_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: invoice_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_item (
    item_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    bhyt_fund numeric(18,2),
    discount_amount numeric(18,2),
    discount_percent numeric(5,2),
    final_price numeric(18,2),
    line_total numeric(18,2) NOT NULL,
    note text,
    quantity integer NOT NULL,
    service_code_snapshot character varying(50),
    service_snapshot character varying(200) NOT NULL,
    unit_price numeric(18,2) NOT NULL,
    invoice_id uuid NOT NULL,
    service_id uuid
);


--
-- Name: medical_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medical_record (
    record_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    chief_complaint text,
    clinical_findings text,
    completed_at timestamp(6) without time zone,
    conclusion text,
    contact_requested boolean,
    diagnosis text,
    doctor_confirmed_at timestamp(6) without time zone,
    doctor_explanation character varying(1000),
    doctor_rating integer,
    feedback_status character varying(20),
    follow_up_date date,
    follow_up_note text,
    internal_note character varying(1000),
    manager_response character varying(1000),
    nursing_updated_at timestamp(6) without time zone,
    patient_instruction text,
    prescription_note text,
    rated_at timestamp(6) without time zone,
    rating_comment character varying(500),
    rating_score integer,
    record_code character varying(50),
    responded_at timestamp(6) without time zone,
    staff_rating integer,
    status character varying(20) NOT NULL,
    record_version bigint DEFAULT 0 NOT NULL,
    waiting_rating integer,
    doctor_id uuid NOT NULL,
    doctor_confirmed_by uuid,
    follow_up_appointment_id uuid,
    nursing_updated_by uuid,
    queue_ticket_id uuid,
    responded_by uuid,
    visit_id uuid NOT NULL,
    vital_signs_id uuid,
    CONSTRAINT medical_record_status_check CHECK (((status)::text = ANY ((ARRAY['IN_PROGRESS'::character varying, 'DRAFT'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: medical_service; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medical_service (
    service_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    allow_customer_booking boolean,
    allowed_gender character varying(10),
    department_type character varying(30) NOT NULL,
    description character varying(1000),
    duration_minutes integer,
    is_point_of_care boolean NOT NULL,
    maximum_age integer,
    minimum_age integer,
    name character varying(200) NOT NULL,
    price numeric(18,2) NOT NULL,
    requires_doctor_order boolean,
    requires_return_to_doctor boolean,
    result_wait_minutes integer,
    service_code character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    workflow_priority integer,
    department_id uuid,
    required_capability_id uuid,
    required_specialization_id uuid,
    requires_specimen boolean DEFAULT false NOT NULL,
    CONSTRAINT medical_service_allowed_gender_check CHECK (((allowed_gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT medical_service_department_type_check CHECK (((department_type)::text = ANY ((ARRAY['EXAMINATION'::character varying, 'PARACLINICAL'::character varying, 'LABORATORY'::character varying, 'IMAGING'::character varying])::text[]))),
    CONSTRAINT medical_service_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: medicine_catalog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medicine_catalog (
    medicine_id uuid NOT NULL,
    active boolean NOT NULL,
    active_ingredient character varying(255),
    default_frequency_per_day integer,
    default_unit character varying(30),
    default_usage character varying(500),
    deleted boolean NOT NULL,
    medicine_code character varying(50) NOT NULL,
    name character varying(255) NOT NULL
);


--
-- Name: notification; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification (
    notification_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    channel character varying(20) NOT NULL,
    content text NOT NULL,
    failure_reason character varying(500),
    notification_type character varying(40) NOT NULL,
    read_at timestamp(6) without time zone,
    related_entity character varying(50),
    related_entity_id uuid,
    sent_at timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    title character varying(200) NOT NULL,
    recipient_id uuid NOT NULL,
    CONSTRAINT notification_channel_check CHECK (((channel)::text = ANY ((ARRAY['IN_APP'::character varying, 'EMAIL'::character varying, 'SMS'::character varying, 'PUSH'::character varying])::text[]))),
    CONSTRAINT notification_notification_type_check CHECK (((notification_type)::text = ANY ((ARRAY['APPOINTMENT_REMINDER'::character varying, 'APPOINTMENT_CONFIRMED'::character varying, 'APPOINTMENT_CANCELLED'::character varying, 'TEST_RESULT_READY'::character varying, 'PAYMENT_DUE'::character varying, 'PAYMENT_SUCCESS'::character varying, 'REFUND'::character varying, 'GENERAL'::character varying])::text[]))),
    CONSTRAINT notification_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying, 'READ'::character varying])::text[])))
);


--
-- Name: payment_transaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_transaction (
    transaction_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    amount numeric(18,2) NOT NULL,
    gateway_reference character varying(100),
    note text,
    paid_at timestamp(6) without time zone,
    payment_method character varying(30) NOT NULL,
    status character varying(20) NOT NULL,
    transaction_code character varying(100) NOT NULL,
    invoice_id uuid NOT NULL,
    received_by uuid,
    CONSTRAINT payment_transaction_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'CARD'::character varying, 'BANK_TRANSFER'::character varying, 'MOMO'::character varying, 'VNPAY'::character varying, 'ZALOPAY'::character varying, 'INSURANCE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT payment_transaction_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: prescription_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prescription_item (
    prescription_item_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    frequency_per_day integer,
    medicine_name character varying(200) NOT NULL,
    note text,
    quantity integer NOT NULL,
    unit character varying(50),
    record_id uuid NOT NULL
);


--
-- Name: profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.profile (
    profile_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    address character varying(255),
    allergies text,
    blood_type character varying(15),
    date_of_birth date,
    email character varying(255),
    full_name character varying(100),
    gender character varying(10),
    height integer,
    insurance_id character varying(50),
    patient_code character varying(20),
    phone character varying(15),
    weight integer,
    account_id uuid,
    CONSTRAINT profile_blood_type_check CHECK (((blood_type)::text = ANY ((ARRAY['A_POSITIVE'::character varying, 'A_NEGATIVE'::character varying, 'B_POSITIVE'::character varying, 'B_NEGATIVE'::character varying, 'AB_POSITIVE'::character varying, 'AB_NEGATIVE'::character varying, 'O_POSITIVE'::character varying, 'O_NEGATIVE'::character varying])::text[]))),
    CONSTRAINT profile_gender_check CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: queue_ticket; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.queue_ticket (
    ticket_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    called_at timestamp(6) without time zone,
    completed_at timestamp(6) without time zone,
    queue_number integer NOT NULL,
    status character varying(20) NOT NULL,
    work_date date NOT NULL,
    department_id uuid NOT NULL,
    service_id uuid,
    visit_id uuid NOT NULL,
    CONSTRAINT queue_ticket_status_check CHECK (((status)::text = ANY ((ARRAY['BLOCKED'::character varying, 'WAITING'::character varying, 'CALLED'::character varying, 'IN_PROGRESS'::character varying, 'DONE'::character varying, 'SKIPPED'::character varying, 'WAITING_FOR_TEST'::character varying, 'TEST_DONE'::character varying])::text[])))
);


--
-- Name: service_capability; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_capability (
    capability_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    active boolean NOT NULL,
    code character varying(30) NOT NULL,
    description character varying(500),
    name character varying(150) NOT NULL
);


--
-- Name: service_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_category (
    category_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    description character varying(500),
    name character varying(150) NOT NULL,
    parent_category_id uuid
);


--
-- Name: shift_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shift_config (
    shift_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    end_time character varying(10) NOT NULL,
    is_active boolean NOT NULL,
    name character varying(100) NOT NULL,
    start_time character varying(10) NOT NULL
);


--
-- Name: specialization; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.specialization (
    specialization_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    description character varying(500),
    name character varying(150) NOT NULL
);


--
-- Name: staff_attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff_attendance (
    attendance_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    check_in_at timestamp(6) without time zone,
    check_in_ip character varying(64),
    check_out_at timestamp(6) without time zone,
    check_out_ip character varying(64),
    device_info character varying(500),
    status character varying(30) NOT NULL,
    schedule_id uuid NOT NULL,
    staff_id uuid NOT NULL,
    CONSTRAINT staff_attendance_status_check CHECK (((status)::text = ANY ((ARRAY['ON_TIME'::character varying, 'LATE'::character varying, 'WORKING'::character varying, 'COMPLETED'::character varying, 'LEFT_EARLY'::character varying, 'ABSENT'::character varying, 'MISSING_CHECKOUT'::character varying, 'ADJUSTMENT_PENDING'::character varying])::text[])))
);


--
-- Name: staff_capability; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff_capability (
    staff_capability_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    certificate_number character varying(100),
    expiry_date date,
    issued_date date,
    issuing_organization character varying(200),
    status character varying(20) NOT NULL,
    capability_id uuid NOT NULL,
    staff_id uuid NOT NULL,
    CONSTRAINT staff_capability_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: staff_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff_info (
    staff_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    bank_account character varying(30),
    highest_degree character varying(100),
    license_number character varying(50),
    national_id character varying(20),
    staff_code character varying(30),
    system_role character varying(30) NOT NULL,
    university character varying(200),
    department_id uuid,
    profile_id uuid NOT NULL,
    specialization_id uuid,
    CONSTRAINT staff_info_system_role_check CHECK (((system_role)::text = ANY ((ARRAY['DOCTOR'::character varying, 'GENERAL_DOCTOR'::character varying, 'SPECIALIST_DOCTOR'::character varying, 'NURSE'::character varying, 'RECEPTIONIST'::character varying, 'CASHIER'::character varying, 'CLINIC_MANAGER'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: staff_schedule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff_schedule (
    schedule_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    is_custom boolean NOT NULL,
    note text,
    status character varying(20) NOT NULL,
    work_date date NOT NULL,
    shift_id uuid,
    staff_id uuid NOT NULL,
    template_id uuid,
    CONSTRAINT staff_schedule_status_check CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'ABSENT'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: staff_schedule_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff_schedule_template (
    template_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    day_of_week character varying(10) NOT NULL,
    is_active boolean NOT NULL,
    shift_id uuid,
    staff_id uuid NOT NULL,
    CONSTRAINT staff_schedule_template_day_of_week_check CHECK (((day_of_week)::text = ANY ((ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying])::text[])))
);


--
-- Name: test_request; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.test_request (
    test_request_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    cancel_reason character varying(500),
    completed_at timestamp(6) without time zone,
    description text,
    performed_at timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    invoice_item_id uuid,
    medical_record_id uuid NOT NULL,
    performing_department uuid NOT NULL,
    queue_ticket_id uuid,
    requested_by uuid NOT NULL,
    service_id uuid NOT NULL,
    CONSTRAINT test_request_status_check CHECK (((status)::text = ANY ((ARRAY['BLOCKED'::character varying, 'PENDING'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: test_result; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.test_result (
    result_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    conclusion text,
    image_url character varying(500),
    performed_at timestamp(6) without time zone NOT NULL,
    sample_id character varying(100),
    verified_at timestamp(6) without time zone,
    performed_by uuid NOT NULL,
    test_request_id uuid NOT NULL,
    verified_by uuid,
    collected_at timestamp(6) without time zone,
    sample_status character varying(30),
    sample_type character varying(30),
    collected_by uuid,
    CONSTRAINT test_result_sample_status_check CHECK (((sample_status)::text = ANY ((ARRAY['ACCEPTED'::character varying, 'REJECTED'::character varying, 'RECOLLECT'::character varying])::text[]))),
    CONSTRAINT test_result_sample_type_check CHECK (((sample_type)::text = ANY ((ARRAY['BLOOD'::character varying, 'URINE'::character varying, 'STOOL'::character varying, 'SPUTUM'::character varying, 'SWAB'::character varying, 'BODY_FLUID'::character varying, 'TISSUE'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: vital_signs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vital_signs (
    vital_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    blood_pressure character varying(30),
    heart_rate integer,
    height numeric(5,2),
    recorded_at timestamp(6) without time zone,
    temperature numeric(4,1),
    weight numeric(5,2),
    medical_record_id uuid NOT NULL,
    recorded_by uuid
);


--
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (account_id);


--
-- Name: appointment appointment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointment
    ADD CONSTRAINT appointment_pkey PRIMARY KEY (appointment_id);


--
-- Name: appointment_services appointment_services_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointment_services
    ADD CONSTRAINT appointment_services_pkey PRIMARY KEY (appointment_id, service_id);


--
-- Name: attendance_adjustment attendance_adjustment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_adjustment
    ADD CONSTRAINT attendance_adjustment_pkey PRIMARY KEY (adjustment_id);


--
-- Name: attendance_qr_token attendance_qr_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_qr_token
    ADD CONSTRAINT attendance_qr_token_pkey PRIMARY KEY (token_id);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (audit_id);


--
-- Name: chat_messages chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (message_id);


--
-- Name: chat_sessions chat_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_sessions
    ADD CONSTRAINT chat_sessions_pkey PRIMARY KEY (session_id);


--
-- Name: customer_visit customer_visit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_visit
    ADD CONSTRAINT customer_visit_pkey PRIMARY KEY (visit_id);


--
-- Name: department_capability department_capability_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.department_capability
    ADD CONSTRAINT department_capability_pkey PRIMARY KEY (department_id, capability_id);


--
-- Name: department department_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT department_pkey PRIMARY KEY (department_id);


--
-- Name: feedback_target feedback_target_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedback_target
    ADD CONSTRAINT feedback_target_pkey PRIMARY KEY (feedback_target_id);


--
-- Name: icd_10_codes icd_10_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.icd_10_codes
    ADD CONSTRAINT icd_10_codes_pkey PRIMARY KEY (code);


--
-- Name: icd_10_selections icd_10_selections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.icd_10_selections
    ADD CONSTRAINT icd_10_selections_pkey PRIMARY KEY (selection_id);


--
-- Name: insurance insurance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance
    ADD CONSTRAINT insurance_pkey PRIMARY KEY (insurance_id);


--
-- Name: insurance_rule insurance_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_rule
    ADD CONSTRAINT insurance_rule_pkey PRIMARY KEY (rule_id);


--
-- Name: invoice_item invoice_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_item
    ADD CONSTRAINT invoice_item_pkey PRIMARY KEY (item_id);


--
-- Name: invoice invoice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT invoice_pkey PRIMARY KEY (invoice_id);


--
-- Name: medical_record medical_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT medical_record_pkey PRIMARY KEY (record_id);


--
-- Name: medical_service medical_service_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_service
    ADD CONSTRAINT medical_service_pkey PRIMARY KEY (service_id);


--
-- Name: medicine_catalog medicine_catalog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine_catalog
    ADD CONSTRAINT medicine_catalog_pkey PRIMARY KEY (medicine_id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (notification_id);


--
-- Name: payment_transaction payment_transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transaction
    ADD CONSTRAINT payment_transaction_pkey PRIMARY KEY (transaction_id);


--
-- Name: prescription_item prescription_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescription_item
    ADD CONSTRAINT prescription_item_pkey PRIMARY KEY (prescription_item_id);


--
-- Name: profile profile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT profile_pkey PRIMARY KEY (profile_id);


--
-- Name: queue_ticket queue_ticket_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.queue_ticket
    ADD CONSTRAINT queue_ticket_pkey PRIMARY KEY (ticket_id);


--
-- Name: service_capability service_capability_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_capability
    ADD CONSTRAINT service_capability_pkey PRIMARY KEY (capability_id);


--
-- Name: service_category service_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_category
    ADD CONSTRAINT service_category_pkey PRIMARY KEY (category_id);


--
-- Name: shift_config shift_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shift_config
    ADD CONSTRAINT shift_config_pkey PRIMARY KEY (shift_id);


--
-- Name: specialization specialization_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.specialization
    ADD CONSTRAINT specialization_pkey PRIMARY KEY (specialization_id);


--
-- Name: staff_attendance staff_attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT staff_attendance_pkey PRIMARY KEY (attendance_id);


--
-- Name: staff_capability staff_capability_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_capability
    ADD CONSTRAINT staff_capability_pkey PRIMARY KEY (staff_capability_id);


--
-- Name: staff_info staff_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_info
    ADD CONSTRAINT staff_info_pkey PRIMARY KEY (staff_id);


--
-- Name: staff_schedule staff_schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_schedule
    ADD CONSTRAINT staff_schedule_pkey PRIMARY KEY (schedule_id);


--
-- Name: staff_schedule_template staff_schedule_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_schedule_template
    ADD CONSTRAINT staff_schedule_template_pkey PRIMARY KEY (template_id);


--
-- Name: test_request test_request_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_request
    ADD CONSTRAINT test_request_pkey PRIMARY KEY (test_request_id);


--
-- Name: test_result test_result_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_result
    ADD CONSTRAINT test_result_pkey PRIMARY KEY (result_id);


--
-- Name: invoice uk1j8r34shs3su7lcr5c94ddshd; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT uk1j8r34shs3su7lcr5c94ddshd UNIQUE (invoice_code);


--
-- Name: department uk1t68827l97cwyxo9r1u6t4p7d; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT uk1t68827l97cwyxo9r1u6t4p7d UNIQUE (name);


--
-- Name: service_capability uk1vhirvt7tep3yrg5aea54p6pl; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_capability
    ADD CONSTRAINT uk1vhirvt7tep3yrg5aea54p6pl UNIQUE (name);


--
-- Name: medical_record uk2w91cu4cgok2cs6kc20dwhgb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT uk2w91cu4cgok2cs6kc20dwhgb7 UNIQUE (record_code);


--
-- Name: service_capability uk3enlnvs8f89nb2pdfsuwtnxs2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_capability
    ADD CONSTRAINT uk3enlnvs8f89nb2pdfsuwtnxs2 UNIQUE (code);


--
-- Name: attendance_qr_token uk6dt5o9qgesj6siqgaq73gum3r; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_qr_token
    ADD CONSTRAINT uk6dt5o9qgesj6siqgaq73gum3r UNIQUE (token_hash);


--
-- Name: profile uk7qvxqcla2uuov4rederes3mbu; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT uk7qvxqcla2uuov4rederes3mbu UNIQUE (patient_code);


--
-- Name: profile uk8601nsl7q0424f5tt4sw9ye7h; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT uk8601nsl7q0424f5tt4sw9ye7h UNIQUE (phone);


--
-- Name: profile uk9d5dpsf2ufa6rjbi3y0elkdcd; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT uk9d5dpsf2ufa6rjbi3y0elkdcd UNIQUE (email);


--
-- Name: staff_attendance uk_attendance_schedule; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT uk_attendance_schedule UNIQUE (schedule_id);


--
-- Name: feedback_target uk_feedback_target; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedback_target
    ADD CONSTRAINT uk_feedback_target UNIQUE (medical_record_id, target_key);


--
-- Name: queue_ticket uk_queue_dept_date_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.queue_ticket
    ADD CONSTRAINT uk_queue_dept_date_number UNIQUE (department_id, work_date, queue_number);


--
-- Name: staff_schedule_template uk_template_staff_dow; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_schedule_template
    ADD CONSTRAINT uk_template_staff_dow UNIQUE (staff_id, day_of_week);


--
-- Name: payment_transaction ukbg8ovyfv79ge0uifmqvo8oig1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transaction
    ADD CONSTRAINT ukbg8ovyfv79ge0uifmqvo8oig1 UNIQUE (transaction_code);


--
-- Name: vital_signs ukdssuhof1u8x9y6bi6glbg479i; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vital_signs
    ADD CONSTRAINT ukdssuhof1u8x9y6bi6glbg479i UNIQUE (medical_record_id);


--
-- Name: specialization uke17ai04xje55nwnjss2st2fyh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.specialization
    ADD CONSTRAINT uke17ai04xje55nwnjss2st2fyh UNIQUE (name);


--
-- Name: medical_service ukegi0oxagdj7jjyj43xnkabilm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_service
    ADD CONSTRAINT ukegi0oxagdj7jjyj43xnkabilm UNIQUE (service_code);


--
-- Name: insurance ukf59c9xis0nsd4yt3kws1kgf57; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance
    ADD CONSTRAINT ukf59c9xis0nsd4yt3kws1kgf57 UNIQUE (code);


--
-- Name: medical_record ukg2uwho7lkgu2gtu7kn69967sy; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT ukg2uwho7lkgu2gtu7kn69967sy UNIQUE (queue_ticket_id);


--
-- Name: department ukg3g5epewcgj7605hmds81a0w2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT ukg3g5epewcgj7605hmds81a0w2 UNIQUE (room_code);


--
-- Name: account ukgex1lmaqpg0ir5g1f5eftyaa1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT ukgex1lmaqpg0ir5g1f5eftyaa1 UNIQUE (username);


--
-- Name: staff_info uki7wlfnr14j9gyuben15xkyp4q; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_info
    ADD CONSTRAINT uki7wlfnr14j9gyuben15xkyp4q UNIQUE (national_id);


--
-- Name: customer_visit ukiaedu5qk7a42tamghgj65c4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_visit
    ADD CONSTRAINT ukiaedu5qk7a42tamghgj65c4 UNIQUE (appointment_id);


--
-- Name: staff_info ukirvcx58qtjy2xw4dc0g4mjeim; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_info
    ADD CONSTRAINT ukirvcx58qtjy2xw4dc0g4mjeim UNIQUE (staff_code);


--
-- Name: medical_record ukjhu53ra6kq25m2g8esv1xo3fk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT ukjhu53ra6kq25m2g8esv1xo3fk UNIQUE (vital_signs_id);


--
-- Name: profile ukk8qk4j2lbffv7x78ydpugc6tg; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT ukk8qk4j2lbffv7x78ydpugc6tg UNIQUE (account_id);


--
-- Name: staff_info ukkl4bll5s3mckctgdde9tjp8no; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_info
    ADD CONSTRAINT ukkl4bll5s3mckctgdde9tjp8no UNIQUE (license_number);


--
-- Name: medicine_catalog ukl7ovv7ss36u6laolxblyamiss; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine_catalog
    ADD CONSTRAINT ukl7ovv7ss36u6laolxblyamiss UNIQUE (medicine_code);


--
-- Name: staff_capability uklb345g19xnjpow7i2patlkbxj; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_capability
    ADD CONSTRAINT uklb345g19xnjpow7i2patlkbxj UNIQUE (staff_id, capability_id);


--
-- Name: test_result ukojp12la6mkseqby788j43cgbi; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_result
    ADD CONSTRAINT ukojp12la6mkseqby788j43cgbi UNIQUE (test_request_id);


--
-- Name: staff_info ukphlkjxbvuqfep0ksurialspfh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_info
    ADD CONSTRAINT ukphlkjxbvuqfep0ksurialspfh UNIQUE (profile_id);


--
-- Name: service_category ukrq4iui706ylaju1tyhc8j6wo4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_category
    ADD CONSTRAINT ukrq4iui706ylaju1tyhc8j6wo4 UNIQUE (name);


--
-- Name: medical_record uksd2oatnmt0lxfldlr4rs30gdb; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT uksd2oatnmt0lxfldlr4rs30gdb UNIQUE (follow_up_appointment_id);


--
-- Name: vital_signs vital_signs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vital_signs
    ADD CONSTRAINT vital_signs_pkey PRIMARY KEY (vital_id);


--
-- Name: idx_audit_actor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_actor ON public.audit_log USING btree (actor_account_id);


--
-- Name: idx_audit_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_created_at ON public.audit_log USING btree (created_at);


--
-- Name: idx_audit_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_entity ON public.audit_log USING btree (entity_name, entity_id);


--
-- Name: idx_icd_selection_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icd_selection_code ON public.icd_10_selections USING btree (code);


--
-- Name: idx_icd_selection_record; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_icd_selection_record ON public.icd_10_selections USING btree (record_id);


--
-- Name: idx_medicine_catalog_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_medicine_catalog_name ON public.medicine_catalog USING btree (name);


--
-- Name: department fk10134l4w1kglxh0292b3hr2s5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT fk10134l4w1kglxh0292b3hr2s5 FOREIGN KEY (specialization_id) REFERENCES public.specialization(specialization_id);


--
-- Name: staff_schedule_template fk15wl6p6lrft7y0qshoh1imyh3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_schedule_template
    ADD CONSTRAINT fk15wl6p6lrft7y0qshoh1imyh3 FOREIGN KEY (shift_id) REFERENCES public.shift_config(shift_id);


--
-- Name: prescription_item fk1htxnrx5qq3f3un08k8ugcqbx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescription_item
    ADD CONSTRAINT fk1htxnrx5qq3f3un08k8ugcqbx FOREIGN KEY (record_id) REFERENCES public.medical_record(record_id);


--
-- Name: department_capability fk1jegyu1jkf4a6m5vrvtbrmxrv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.department_capability
    ADD CONSTRAINT fk1jegyu1jkf4a6m5vrvtbrmxrv FOREIGN KEY (department_id) REFERENCES public.department(department_id);


--
-- Name: chat_messages fk3cpkdtwdxndrjhrx3gt9q5ux9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT fk3cpkdtwdxndrjhrx3gt9q5ux9 FOREIGN KEY (session_id) REFERENCES public.chat_sessions(session_id);


--
-- Name: vital_signs fk3q5y5bg80deqa15nudbdqrprk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vital_signs
    ADD CONSTRAINT fk3q5y5bg80deqa15nudbdqrprk FOREIGN KEY (recorded_by) REFERENCES public.staff_info(staff_id);


--
-- Name: customer_visit fk468aiqqurc1kntbwdrkatsn2v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_visit
    ADD CONSTRAINT fk468aiqqurc1kntbwdrkatsn2v FOREIGN KEY (checked_in_by) REFERENCES public.staff_info(staff_id);


--
-- Name: invoice fk5o5kmvra9sfhyayvev27i0yl4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT fk5o5kmvra9sfhyayvev27i0yl4 FOREIGN KEY (issued_by) REFERENCES public.staff_info(staff_id);


--
-- Name: test_request fk5ohqhrel292hapswyanp7wnl9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_request
    ADD CONSTRAINT fk5ohqhrel292hapswyanp7wnl9 FOREIGN KEY (requested_by) REFERENCES public.staff_info(staff_id);


--
-- Name: test_request fk5vpysbppkw19lix5kb5cl7b3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_request
    ADD CONSTRAINT fk5vpysbppkw19lix5kb5cl7b3 FOREIGN KEY (medical_record_id) REFERENCES public.medical_record(record_id);


--
-- Name: staff_info fk6hsxke7kfoqa28pfir684r1cs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_info
    ADD CONSTRAINT fk6hsxke7kfoqa28pfir684r1cs FOREIGN KEY (profile_id) REFERENCES public.profile(profile_id);


--
-- Name: customer_visit fk6inc5qoub8dqffl4b7o5fik4t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_visit
    ADD CONSTRAINT fk6inc5qoub8dqffl4b7o5fik4t FOREIGN KEY (customer_id) REFERENCES public.profile(profile_id);


--
-- Name: service_category fk6mf53vfwp4wxjtvv43sumwmx6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_category
    ADD CONSTRAINT fk6mf53vfwp4wxjtvv43sumwmx6 FOREIGN KEY (parent_category_id) REFERENCES public.service_category(category_id);


--
-- Name: queue_ticket fk6nq4g8gp0muymp711rikhfsew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.queue_ticket
    ADD CONSTRAINT fk6nq4g8gp0muymp711rikhfsew FOREIGN KEY (department_id) REFERENCES public.department(department_id);


--
-- Name: test_result fk6wg2w2igw251ew0uponmttl9l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_result
    ADD CONSTRAINT fk6wg2w2igw251ew0uponmttl9l FOREIGN KEY (verified_by) REFERENCES public.staff_info(staff_id);


--
-- Name: attendance_adjustment fk746xm0v5gykh2umdbew2ibcyk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_adjustment
    ADD CONSTRAINT fk746xm0v5gykh2umdbew2ibcyk FOREIGN KEY (attendance_id) REFERENCES public.staff_attendance(attendance_id);


--
-- Name: payment_transaction fk7okdbd50ppclniwq0iimt98px; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transaction
    ADD CONSTRAINT fk7okdbd50ppclniwq0iimt98px FOREIGN KEY (received_by) REFERENCES public.staff_info(staff_id);


--
-- Name: invoice fk7veo8o0cj1db0km4k2v9gje1w; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT fk7veo8o0cj1db0km4k2v9gje1w FOREIGN KEY (customer_id) REFERENCES public.profile(profile_id);


--
-- Name: medical_record fk94srxevyjv5j6mj2rsuvijpef; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT fk94srxevyjv5j6mj2rsuvijpef FOREIGN KEY (visit_id) REFERENCES public.customer_visit(visit_id);


--
-- Name: invoice_item fk9m4bpfjsp5ykix6fueyaf6if0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_item
    ADD CONSTRAINT fk9m4bpfjsp5ykix6fueyaf6if0 FOREIGN KEY (service_id) REFERENCES public.medical_service(service_id);


--
-- Name: invoice fk9vk4grlg5i2tt7u3ecrl5dmhd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT fk9vk4grlg5i2tt7u3ecrl5dmhd FOREIGN KEY (medical_record_id) REFERENCES public.medical_record(record_id);


--
-- Name: staff_schedule fkausiysmnq8v1wwxto70dfyj74; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_schedule
    ADD CONSTRAINT fkausiysmnq8v1wwxto70dfyj74 FOREIGN KEY (template_id) REFERENCES public.staff_schedule_template(template_id);


--
-- Name: staff_capability fkav53tyepoik9rlsu907wxi1jk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_capability
    ADD CONSTRAINT fkav53tyepoik9rlsu907wxi1jk FOREIGN KEY (capability_id) REFERENCES public.service_capability(capability_id);


--
-- Name: test_request fkb4d7opx1j0tqrog7xcbor41we; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_request
    ADD CONSTRAINT fkb4d7opx1j0tqrog7xcbor41we FOREIGN KEY (performing_department) REFERENCES public.department(department_id);


--
-- Name: icd_10_selections fkb5f38s1c38q0prw8jxkf19uj6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.icd_10_selections
    ADD CONSTRAINT fkb5f38s1c38q0prw8jxkf19uj6 FOREIGN KEY (record_id) REFERENCES public.medical_record(record_id);


--
-- Name: medical_record fkbhd3hq3n8rafsbwj03wdocvgx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT fkbhd3hq3n8rafsbwj03wdocvgx FOREIGN KEY (responded_by) REFERENCES public.staff_info(staff_id);


--
-- Name: department_capability fkboijeou7p28u5jyakti5dbvvu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.department_capability
    ADD CONSTRAINT fkboijeou7p28u5jyakti5dbvvu FOREIGN KEY (capability_id) REFERENCES public.service_capability(capability_id);


--
-- Name: invoice_item fkbu6tmpd0mtgu9wrw5bj5uv09v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_item
    ADD CONSTRAINT fkbu6tmpd0mtgu9wrw5bj5uv09v FOREIGN KEY (invoice_id) REFERENCES public.invoice(invoice_id);


--
-- Name: notification fkcokmaba2mnr1an1tc3ajitrd7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT fkcokmaba2mnr1an1tc3ajitrd7 FOREIGN KEY (recipient_id) REFERENCES public.profile(profile_id);


--
-- Name: medical_service fkd5g7uxr78uk1lxkk9dmwaqs1r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_service
    ADD CONSTRAINT fkd5g7uxr78uk1lxkk9dmwaqs1r FOREIGN KEY (department_id) REFERENCES public.department(department_id);


--
-- Name: staff_schedule fkd87u3pwst06bor2nb808x8w0l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_schedule
    ADD CONSTRAINT fkd87u3pwst06bor2nb808x8w0l FOREIGN KEY (shift_id) REFERENCES public.shift_config(shift_id);


--
-- Name: attendance_adjustment fkdk4d70iuu63umaaudpbgy4sdy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_adjustment
    ADD CONSTRAINT fkdk4d70iuu63umaaudpbgy4sdy FOREIGN KEY (reviewed_by) REFERENCES public.staff_info(staff_id);


--
-- Name: invoice fkeiqttpmb4vfv7go8g9lw0l93; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT fkeiqttpmb4vfv7go8g9lw0l93 FOREIGN KEY (visit_id) REFERENCES public.customer_visit(visit_id);


--
-- Name: staff_schedule fketh01bteno910e2w1vxk84elp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_schedule
    ADD CONSTRAINT fketh01bteno910e2w1vxk84elp FOREIGN KEY (staff_id) REFERENCES public.staff_info(staff_id);


--
-- Name: payment_transaction fkfdqxbup9flmh4er9jpx2pvdl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transaction
    ADD CONSTRAINT fkfdqxbup9flmh4er9jpx2pvdl FOREIGN KEY (invoice_id) REFERENCES public.invoice(invoice_id);


--
-- Name: test_result fki90suffidtk9t1w9n32ge1i8l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_result
    ADD CONSTRAINT fki90suffidtk9t1w9n32ge1i8l FOREIGN KEY (collected_by) REFERENCES public.staff_info(staff_id);


--
-- Name: insurance_rule fkiksat1bloiv913ur2ngby5uj9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_rule
    ADD CONSTRAINT fkiksat1bloiv913ur2ngby5uj9 FOREIGN KEY (insurance_id) REFERENCES public.insurance(insurance_id);


--
-- Name: test_result fkj88pnqmxaxnvxc87818aq48ju; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_result
    ADD CONSTRAINT fkj88pnqmxaxnvxc87818aq48ju FOREIGN KEY (performed_by) REFERENCES public.staff_info(staff_id);


--
-- Name: staff_info fkjk38sjdxxkevop4t75gwp6kb0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_info
    ADD CONSTRAINT fkjk38sjdxxkevop4t75gwp6kb0 FOREIGN KEY (department_id) REFERENCES public.department(department_id);


--
-- Name: staff_schedule_template fkjpgg0oack4oc6fy1cv7ywryqt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_schedule_template
    ADD CONSTRAINT fkjpgg0oack4oc6fy1cv7ywryqt FOREIGN KEY (staff_id) REFERENCES public.staff_info(staff_id);


--
-- Name: feedback_target fkk7w1td7a0migvu5ddtkqbxthc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedback_target
    ADD CONSTRAINT fkk7w1td7a0migvu5ddtkqbxthc FOREIGN KEY (staff_id) REFERENCES public.staff_info(staff_id);


--
-- Name: appointment_services fkkv6gwfscv4td54g96ra0p0gn0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointment_services
    ADD CONSTRAINT fkkv6gwfscv4td54g96ra0p0gn0 FOREIGN KEY (appointment_id) REFERENCES public.appointment(appointment_id);


--
-- Name: medical_record fkkyv3fws42623oo1irm0om6hms; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT fkkyv3fws42623oo1irm0om6hms FOREIGN KEY (follow_up_appointment_id) REFERENCES public.appointment(appointment_id);


--
-- Name: medical_record fkl929ixs1ll8d6qkn1g0jhx03; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT fkl929ixs1ll8d6qkn1g0jhx03 FOREIGN KEY (queue_ticket_id) REFERENCES public.queue_ticket(ticket_id);


--
-- Name: medical_record fklatqa6paclst5rcgtkv2wfxey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT fklatqa6paclst5rcgtkv2wfxey FOREIGN KEY (vital_signs_id) REFERENCES public.vital_signs(vital_id);


--
-- Name: profile fklc4oipegt3vyph78q31itt3pf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT fklc4oipegt3vyph78q31itt3pf FOREIGN KEY (account_id) REFERENCES public.account(account_id);


--
-- Name: feedback_target fkly49830wxcmp4xq1hkx7jhs6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedback_target
    ADD CONSTRAINT fkly49830wxcmp4xq1hkx7jhs6 FOREIGN KEY (medical_record_id) REFERENCES public.medical_record(record_id);


--
-- Name: customer_visit fkm51ifxgfp2lk84n03h5scg47h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_visit
    ADD CONSTRAINT fkm51ifxgfp2lk84n03h5scg47h FOREIGN KEY (appointment_id) REFERENCES public.appointment(appointment_id);


--
-- Name: test_request fkmed4mql4u95sa4tifcodwikwr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_request
    ADD CONSTRAINT fkmed4mql4u95sa4tifcodwikwr FOREIGN KEY (queue_ticket_id) REFERENCES public.queue_ticket(ticket_id);


--
-- Name: appointment fkmq61jq13p7bc8qc2l960920d8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointment
    ADD CONSTRAINT fkmq61jq13p7bc8qc2l960920d8 FOREIGN KEY (customer_id) REFERENCES public.profile(profile_id);


--
-- Name: department fkn9ca3o7wdxpgd4q3ap78gg7kt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT fkn9ca3o7wdxpgd4q3ap78gg7kt FOREIGN KEY (head_doctor_id) REFERENCES public.staff_info(staff_id);


--
-- Name: test_request fknomhqmlh06o5hb3g98308dmif; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_request
    ADD CONSTRAINT fknomhqmlh06o5hb3g98308dmif FOREIGN KEY (invoice_item_id) REFERENCES public.invoice_item(item_id);


--
-- Name: staff_capability fknqdko61dckgq9q2ss41p9ucw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_capability
    ADD CONSTRAINT fknqdko61dckgq9q2ss41p9ucw FOREIGN KEY (staff_id) REFERENCES public.staff_info(staff_id);


--
-- Name: attendance_qr_token fknudwb480a0rprn9b4jpp7i31j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_qr_token
    ADD CONSTRAINT fknudwb480a0rprn9b4jpp7i31j FOREIGN KEY (created_by) REFERENCES public.staff_info(staff_id);


--
-- Name: vital_signs fkoo271kblgxr02vcah5f4y1y4d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vital_signs
    ADD CONSTRAINT fkoo271kblgxr02vcah5f4y1y4d FOREIGN KEY (medical_record_id) REFERENCES public.medical_record(record_id);


--
-- Name: queue_ticket fkpe6lv5jyjgic1ggicc0mobwc1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.queue_ticket
    ADD CONSTRAINT fkpe6lv5jyjgic1ggicc0mobwc1 FOREIGN KEY (visit_id) REFERENCES public.customer_visit(visit_id);


--
-- Name: chat_sessions fkphkymcb9bsgda43s5ox3l0woy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_sessions
    ADD CONSTRAINT fkphkymcb9bsgda43s5ox3l0woy FOREIGN KEY (customer_id) REFERENCES public.profile(profile_id);


--
-- Name: staff_info fkpkvrbnwil9pvgml4oeqjs21pq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_info
    ADD CONSTRAINT fkpkvrbnwil9pvgml4oeqjs21pq FOREIGN KEY (specialization_id) REFERENCES public.specialization(specialization_id);


--
-- Name: test_request fkpo5q96jrv455r98lyx50cjr1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_request
    ADD CONSTRAINT fkpo5q96jrv455r98lyx50cjr1 FOREIGN KEY (service_id) REFERENCES public.medical_service(service_id);


--
-- Name: medical_service fkpqs9bqxx6balyn2a4ma1q3yt1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_service
    ADD CONSTRAINT fkpqs9bqxx6balyn2a4ma1q3yt1 FOREIGN KEY (required_capability_id) REFERENCES public.service_capability(capability_id);


--
-- Name: queue_ticket fkq0lhmdslaqjrkaky7rkajh5v4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.queue_ticket
    ADD CONSTRAINT fkq0lhmdslaqjrkaky7rkajh5v4 FOREIGN KEY (service_id) REFERENCES public.medical_service(service_id);


--
-- Name: staff_attendance fkq9vywcl1oatrmwprrrtf5n5vt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT fkq9vywcl1oatrmwprrrtf5n5vt FOREIGN KEY (staff_id) REFERENCES public.staff_info(staff_id);


--
-- Name: medical_service fks4e4uk27e8p9tccun9uh7x6n0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_service
    ADD CONSTRAINT fks4e4uk27e8p9tccun9uh7x6n0 FOREIGN KEY (required_specialization_id) REFERENCES public.specialization(specialization_id);


--
-- Name: staff_attendance fks8anvc6qbch3g8vhuwrvu9qj4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT fks8anvc6qbch3g8vhuwrvu9qj4 FOREIGN KEY (schedule_id) REFERENCES public.staff_schedule(schedule_id);


--
-- Name: medical_record fksrratp15tqyqnvjacqm1k1vdq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT fksrratp15tqyqnvjacqm1k1vdq FOREIGN KEY (doctor_confirmed_by) REFERENCES public.staff_info(staff_id);


--
-- Name: medical_record fkt1yt19xtkp14yue4f5ag22dcf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT fkt1yt19xtkp14yue4f5ag22dcf FOREIGN KEY (nursing_updated_by) REFERENCES public.staff_info(staff_id);


--
-- Name: appointment_services fktb7pnt1rylh1ih0adgfrj80oa; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointment_services
    ADD CONSTRAINT fktb7pnt1rylh1ih0adgfrj80oa FOREIGN KEY (service_id) REFERENCES public.medical_service(service_id);


--
-- Name: medical_record fktgncbgxv1hq93mm6hkto0w8s0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_record
    ADD CONSTRAINT fktgncbgxv1hq93mm6hkto0w8s0 FOREIGN KEY (doctor_id) REFERENCES public.staff_info(staff_id);


--
-- Name: test_result fktnlg1lqeaj5k05d01jxtth7c8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_result
    ADD CONSTRAINT fktnlg1lqeaj5k05d01jxtth7c8 FOREIGN KEY (test_request_id) REFERENCES public.test_request(test_request_id);


--
-- PostgreSQL database dump complete
--

\unrestrict qUPdSDLPQbT0xGjdF8FtcrvnCa4BCcjpYaMXZwbkjMaD506urkWBcHbcvQbadnx

