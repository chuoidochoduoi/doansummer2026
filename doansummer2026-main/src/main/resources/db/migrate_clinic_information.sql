-- Run once on an existing production database before deploying this version.
-- Development uses ddl-auto=update; production uses ddl-auto=validate.
CREATE TABLE IF NOT EXISTS clinic_information (
    clinic_information_id UUID PRIMARY KEY,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    clinic_name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200) NOT NULL,
    tax_code VARCHAR(14) NOT NULL,
    operating_license VARCHAR(100),
    short_description VARCHAR(500),
    support_email VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    address VARCHAR(300) NOT NULL,
    website_url VARCHAR(500),
    facebook_url VARCHAR(500),
    youtube_url VARCHAR(500),
    zalo_url VARCHAR(500),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    CONSTRAINT clinic_information_tax_code_check
        CHECK (tax_code ~ '^[0-9]{10}(-[0-9]{3})?$'),
    CONSTRAINT clinic_information_latitude_check
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT clinic_information_longitude_check
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

INSERT INTO clinic_information (
    clinic_information_id, created_at, updated_at, deleted, clinic_name, legal_name,
    tax_code, operating_license, short_description, support_email, phone, address,
    website_url, facebook_url, youtube_url, zalo_url, latitude, longitude
) VALUES (
    '00000000-0000-0000-0000-000000000100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE,
    'Phòng khám CareS', 'Công ty TNHH Phòng khám CareS', '0101234567', '000123/HNO-GPHD',
    'Phòng khám đa khoa cung cấp dịch vụ chăm sóc sức khỏe chất lượng và thuận tiện.',
    'lienhe@caresclinic.vn', '1900 1234',
    'Khu Công nghệ cao Hòa Lạc, Thạch Thất, Hà Nội', NULL,
    'https://www.facebook.com/profile.php?id=61593125259676', NULL, NULL,
    21.0128000, 105.5259000
)
ON CONFLICT (clinic_information_id) DO NOTHING;
