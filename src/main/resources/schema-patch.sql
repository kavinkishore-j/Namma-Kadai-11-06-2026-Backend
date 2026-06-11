-- ============================================================
-- schema-patch.sql
-- Run once to seed permissions, roles, and role_permission mappings
-- ============================================================

-- 1. Permissions
INSERT IGNORE INTO permissions (permission_name) VALUES
('VIEW_MENU'),
('CREATE_MENU'),
('UPDATE_MENU'),
('PLACE_ORDER'),
('VIEW_ALL_ORDERS'),
('UPDATE_ORDER_STATUS');

-- 2. Roles (in case DataInitializer hasn't run yet)
INSERT IGNORE INTO roles (role_name) VALUES
('ADMIN'),
('STAFF'),
('CUSTOMER');

-- 3. Role → Permission mappings

-- ADMIN gets everything
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r, permissions p
WHERE r.role_name = 'ADMIN';

-- STAFF: view menu, update menu, view & update orders
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r
JOIN permissions p ON p.permission_name IN ('VIEW_MENU', 'UPDATE_MENU', 'VIEW_ALL_ORDERS', 'UPDATE_ORDER_STATUS')
WHERE r.role_name = 'STAFF';

-- CUSTOMER: view menu, place orders
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM roles r
JOIN permissions p ON p.permission_name IN ('VIEW_MENU', 'PLACE_ORDER')
WHERE r.role_name = 'CUSTOMER';
