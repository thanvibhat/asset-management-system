ALTER TABLE asset_components
ADD COLUMN IF NOT EXISTS old_component_disposition VARCHAR(30);
