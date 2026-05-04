-- Asset Management System - PostgreSQL Schema
-- Run this before starting the application

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER' CHECK (role IN ('ADMIN','MANAGER','VIEWER')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Vendors
CREATE TABLE vendors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Procurement
CREATE TABLE procurement (
    id BIGSERIAL PRIMARY KEY,
    po_number VARCHAR(100) UNIQUE NOT NULL,
    vendor_id BIGINT REFERENCES vendors(id),
    order_date DATE,
    total_cost DECIMAL(12,2),
    status VARCHAR(50) DEFAULT 'ORDERED' CHECK (status IN ('ORDERED','SHIPPED','RECEIVED','CANCELLED')),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Asset Categories
CREATE TABLE asset_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    attribute_schema JSONB DEFAULT '[]', -- JSON array defining schema for dynamic fields
    created_at TIMESTAMP DEFAULT NOW()
);

-- Assets
CREATE TABLE assets (
    id BIGSERIAL PRIMARY KEY,
    asset_tag VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES asset_categories(id),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','ALLOCATED','UNDER_MAINTENANCE','RETIRED','LOST')),
    purchase_date DATE,
    purchase_cost DECIMAL(12,2),
    current_value DECIMAL(12,2),
    location VARCHAR(200),
    serial_number VARCHAR(100),
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    warranty_expiry DATE,
    procurement_id BIGINT REFERENCES procurement(id),
    dynamic_attributes JSONB DEFAULT '{}', -- Key-Value pairs based on attribute_schema
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Allocations
CREATE TABLE allocations (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL REFERENCES assets(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    allocated_by BIGINT NOT NULL REFERENCES users(id),
    allocated_at TIMESTAMP DEFAULT NOW(),
    returned_at TIMESTAMP,
    expected_return_date DATE,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','RETURNED','OVERDUE'))
);

-- Maintenance Records
CREATE TABLE maintenance_records (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL REFERENCES assets(id),
    vendor_id BIGINT REFERENCES vendors(id),
    maintenance_type VARCHAR(50) NOT NULL CHECK (maintenance_type IN ('PREVENTIVE','CORRECTIVE','INSPECTION')),
    description TEXT NOT NULL,
    cost DECIMAL(10,2),
    performed_by VARCHAR(100),
    scheduled_date DATE,
    completed_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Audit Log
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(50), -- username
    details JSONB,
    performed_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_category ON assets(category_id);
CREATE INDEX idx_allocations_asset ON allocations(asset_id);
CREATE INDEX idx_allocations_user ON allocations(user_id);
CREATE INDEX idx_allocations_status ON allocations(status);
CREATE INDEX idx_maintenance_asset ON maintenance_records(asset_id);
CREATE INDEX idx_maintenance_status ON maintenance_records(status);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);

-- Seed Data
-- All passwords: Admin@1234  (BCrypt cost=12, verified)
INSERT INTO users (username, password, email, full_name, role) VALUES
('admin',    '$2b$12$uhVdI.7mqB5UW9sLrBioLeWE0NRyZi.KHUEVis0ZSSKsQ3TD.oahW', 'admin@assetmgmt.com',   'System Administrator', 'ADMIN'),
('manager1', '$2b$12$mFMbyqUyj1hRYuY.IkrCmO91SvR49JBoPhwHpHbAnfMkewwNLDMV6', 'manager@assetmgmt.com', 'Asset Manager',        'MANAGER'),
('viewer1',  '$2b$12$21.lXfGCdgIXwoUJIjxMpuMKrkMwSJAeVmdzSFsf3rajeIayYtmcW', 'viewer@assetmgmt.com',  'John Viewer',          'VIEWER');

INSERT INTO vendors (name, contact_email, contact_phone, address) VALUES
('Dell Tech Solutions', 'enterprise@dell.com', '1-800-555-0101', 'Round Rock, TX'),
('Apple Enterprise', 'business@apple.com', '1-800-555-0102', 'Cupertino, CA'),
('TechFix Repairs', 'service@techfix.local', '555-0199', 'Local Store 4B');

INSERT INTO procurement (po_number, vendor_id, order_date, total_cost, status, created_by) VALUES
('PO-2023-001', 1, '2023-01-05', 4500.00, 'RECEIVED', 1),
('PO-2023-002', 2, '2023-03-10', 3199.00, 'RECEIVED', 1);

INSERT INTO asset_categories (name, description, attribute_schema) VALUES
('Laptops', 'Portable computing devices', '[{"name": "RAM", "type": "string", "required": true}, {"name": "Storage", "type": "string", "required": true}, {"name": "CPU", "type": "string", "required": true}]'),
('Desktops', 'Desktop workstations', '[{"name": "RAM", "type": "string", "required": true}, {"name": "Storage", "type": "string", "required": true}, {"name": "GPU", "type": "string", "required": false}]'),
('Mobile Phones', 'Smartphones and mobile devices', '[{"name": "OS", "type": "string", "required": true}, {"name": "Storage", "type": "string", "required": true}, {"name": "IMEI", "type": "string", "required": false}]'),
('Networking', 'Switches, routers, access points', '[{"name": "Ports", "type": "number", "required": true}, {"name": "PoE Support", "type": "boolean", "required": false}]'),
('Furniture', 'Office furniture and fixtures', '[]'),
('Printers', 'Printers and scanners', '[{"name": "Color", "type": "boolean", "required": true}, {"name": "Networked", "type": "boolean", "required": true}]'),
('Servers', 'Physical and virtual servers', '[{"name": "CPU Cores", "type": "number", "required": true}, {"name": "RAM (GB)", "type": "number", "required": true}, {"name": "Storage (TB)", "type": "number", "required": true}]'),
('Peripherals', 'Keyboards, mice, monitors', '[]');

INSERT INTO assets (asset_tag, name, description, category_id, status, purchase_date, purchase_cost, current_value, location, serial_number, manufacturer, model, warranty_expiry, procurement_id, dynamic_attributes, created_by) VALUES
('LAP-001', 'Dell XPS 15', '15-inch developer laptop', 1, 'AVAILABLE', '2023-01-15', 1500.00, 1200.00, 'IT Dept', 'SN-DELL-001', 'Dell', 'XPS 15 9520', '2026-01-15', 1, '{"RAM": "32GB", "CPU": "Intel i9", "Storage": "1TB SSD"}', 1),
('LAP-002', 'MacBook Pro 14', 'Apple M3 Pro', 1, 'ALLOCATED', '2023-03-20', 2200.00, 1900.00, 'Engineering', 'SN-APPL-002', 'Apple', 'MacBook Pro 14 M3', '2026-03-20', 2, '{"RAM": "18GB", "CPU": "Apple M3 Pro", "Storage": "512GB SSD"}', 1),
('DES-001', 'HP Workstation Z4', 'High-performance workstation', 2, 'AVAILABLE', '2022-06-10', 3000.00, 2200.00, 'Design Lab', 'SN-HP-003', 'HP', 'Z4 G4', '2025-06-10', NULL, '{"RAM": "64GB", "GPU": "NVIDIA RTX A4000", "Storage": "2TB NVMe"}', 1),
('MOB-001', 'iPhone 15 Pro', 'Corporate mobile device', 3, 'ALLOCATED', '2023-09-01', 999.00, 850.00, 'Sales Dept', 'SN-APPL-004', 'Apple', 'iPhone 15 Pro', '2025-09-01', NULL, '{"OS": "iOS 17", "IMEI": "354012345678901", "Storage": "256GB"}', 1),
('NET-001', 'Cisco Switch 24-Port', 'Layer 2 managed switch', 4, 'AVAILABLE', '2021-11-05', 800.00, 600.00, 'Server Room', 'SN-CSCO-005', 'Cisco', 'Catalyst 2960', '2024-11-05', NULL, '{"Ports": 24, "PoE Support": true}', 1),
('SRV-001', 'Dell PowerEdge R740', 'Database server', 7, 'AVAILABLE', '2022-02-14', 5500.00, 4200.00, 'Data Center', 'SN-DELL-006', 'Dell', 'PowerEdge R740', '2027-02-14', 1, '{"CPU Cores": 32, "RAM (GB)": 256, "Storage (TB)": 20}', 1),
('LAP-003', 'Lenovo ThinkPad X1', 'Executive laptop', 1, 'UNDER_MAINTENANCE', '2022-08-22', 1800.00, 1400.00, 'Maintenance', 'SN-LNVO-007', 'Lenovo', 'ThinkPad X1 Carbon', '2025-08-22', NULL, '{"RAM": "16GB", "CPU": "Intel i7", "Storage": "512GB SSD"}', 1),
('PRN-001', 'HP LaserJet Pro', 'Office laser printer', 6, 'AVAILABLE', '2022-12-01', 450.00, 350.00, 'Office Floor 2', 'SN-HP-008', 'HP', 'LaserJet Pro M404n', '2025-12-01', NULL, '{"Color": false, "Networked": true}', 1);

INSERT INTO allocations (asset_id, user_id, allocated_by, allocated_at, expected_return_date, notes, status) VALUES
(2, 2, 1, '2024-01-10 09:00:00', '2025-01-10', 'Permanent allocation for development work', 'ACTIVE'),
(4, 2, 1, '2024-02-01 10:00:00', '2025-02-01', 'Sales team mobile device', 'ACTIVE');

INSERT INTO maintenance_records (asset_id, vendor_id, maintenance_type, description, cost, performed_by, scheduled_date, completed_date, status, created_by) VALUES
(7, 3, 'CORRECTIVE', 'Keyboard replacement and battery check', 120.00, 'TechFix Services', '2024-12-01', NULL, 'IN_PROGRESS', 1),
(5, NULL, 'PREVENTIVE', 'Annual firmware update and port cleaning', 0.00, 'Internal IT', '2024-11-15', '2024-11-15', 'COMPLETED', 1);
