CREATE TABLE IF NOT EXISTS asset_status_history (
    id             BIGSERIAL PRIMARY KEY,
    asset_id       BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    event_type     VARCHAR(50) NOT NULL,
    from_status    VARCHAR(30),
    to_status      VARCHAR(30),
    notes          TEXT,
    performed_by   VARCHAR(100),
    event_date     TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata       JSONB DEFAULT '{}'
);

-- event_type values used in code:
-- PURCHASED, ALLOCATED, RETURNED, REASSIGNED,
-- MAINTENANCE_STARTED, MAINTENANCE_COMPLETED,
-- STATUS_CHANGED, COMPONENT_REPLACED, WARRANTY_UPDATED, RETIRED

CREATE INDEX IF NOT EXISTS idx_status_history_asset
    ON asset_status_history(asset_id, event_date DESC);
