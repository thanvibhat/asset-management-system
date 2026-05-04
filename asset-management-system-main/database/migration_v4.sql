CREATE TABLE IF NOT EXISTS product_master (
    id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(200) NOT NULL,
    category_id BIGINT REFERENCES asset_categories(id),
    manufacturer VARCHAR(100),
    description TEXT,
    asset_prefix VARCHAR(10),
    additional_attributes JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(product_name)
);

ALTER TABLE assets ADD COLUMN IF NOT EXISTS product_id BIGINT REFERENCES product_master(id);

CREATE TABLE IF NOT EXISTS location_master (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manufacturer_master (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    website VARCHAR(200),
    created_at TIMESTAMP DEFAULT NOW()
);
