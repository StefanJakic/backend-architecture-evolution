ALTER TABLE project
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE project
ADD CONSTRAINT project_version_non_negative
CHECK (version >= 0);
