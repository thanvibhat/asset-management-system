CREATE TABLE IF NOT EXISTS asset_components (
    id               BIGSERIAL PRIMARY KEY,
    asset_id         BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    component_type   VARCHAR(100) NOT NULL,
    serial_number    VARCHAR(100),
    source           VARCHAR(20) NOT NULL
                       CHECK (source IN ('OEM', 'REPLACED', 'BACKFILLED')),
    installation_date DATE NOT NULL,
    removal_date      DATE,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                       CHECK (status IN ('ACTIVE', 'REPLACED', 'FAULTY')),
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_asset_components_asset_id
    ON asset_components(asset_id);

CREATE INDEX IF NOT EXISTS idx_asset_components_status
    ON asset_components(asset_id, status);
