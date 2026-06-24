-- EC2 / RDS 修复：users 表新增密码字段导致 502 时执行
-- 原因：已有用户数据时 Hibernate 无法直接 ADD COLUMN ... NOT NULL

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_not_set boolean DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_required boolean DEFAULT false;

UPDATE users SET password_not_set = false WHERE password_not_set IS NULL;
UPDATE users SET password_reset_required = false WHERE password_reset_required IS NULL;
