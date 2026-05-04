-- Migration V2: RBAC and Notifications

-- Permissions Table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- Role-Permissions Join Table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- User-Roles Join Table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Seed Initial RBAC Data
INSERT INTO permissions (name, description) VALUES
('ASSET_READ', 'View assets'),
('ASSET_CREATE', 'Add new assets'),
('ASSET_UPDATE', 'Modify assets'),
('ASSET_DELETE', 'Delete assets'),
('USER_MANAGE', 'Manage users and roles'),
('ALLOCATION_MANAGE', 'Allocate and return assets'),
('MAINTENANCE_MANAGE', 'Log and update maintenance'),
('REPORT_VIEW', 'View dashboard and reports');

INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Full system access'),
('ROLE_MANAGER', 'Asset and allocation management'),
('ROLE_VIEWER', 'Read-only access');

-- Link Permissions to Roles
-- Admin gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN';

-- Manager gets most things
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ROLE_MANAGER' AND p.name IN ('ASSET_READ', 'ASSET_CREATE', 'ASSET_UPDATE', 'ALLOCATION_MANAGE', 'MAINTENANCE_MANAGE', 'REPORT_VIEW');

-- Viewer gets read access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ROLE_VIEWER' AND p.name IN ('ASSET_READ', 'REPORT_VIEW');

-- Migrate existing users to new role system
-- Note: This assumes existing roles match the new role names (prefixed with ROLE_)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u 
JOIN roles r ON r.name = 'ROLE_' || u.role;

-- Optional: Drop the old role column if confirmed
-- ALTER TABLE users DROP COLUMN role;
