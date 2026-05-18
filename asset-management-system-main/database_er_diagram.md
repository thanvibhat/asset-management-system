# Asset Management System - Entity Relationship Diagram

The following diagram illustrates the core database schema for the Asset Management System. The architecture centers around the **Asset** entity, which connects to classification, acquisition, assignment, and maintenance modules.

```mermaid
erDiagram
    ASSET ||--o{ ASSET_COMPONENT : contains
    ASSET ||--o{ ALLOCATION : assigned_in
    ASSET ||--o{ MAINTENANCE_RECORD : undergoes
    ASSET ||--o{ ASSET_STATUS_HISTORY : tracks_status
    ASSET }|--|| ASSET_CATEGORY : classified_by
    ASSET }|--|| PRODUCT_MASTER : based_on
    ASSET }|--|| VENDOR : purchased_from
    ASSET }|--o| PROCUREMENT : part_of
    ASSET }|--|| USER : created_by
    ASSET ||--o{ ASSET : parent_of

    USER ||--o{ ALLOCATION : holds
    USER ||--o{ ALLOCATION : allocated_by
    USER ||--o{ NOTIFICATION : receives
    USER }|--o{ ROLE : assigned
    ROLE }|--o{ PERMISSION : grants

    VENDOR ||--o{ ASSET : supplies
    PRODUCT_MASTER ||--o{ ASSET : defines
    PROCUREMENT ||--o{ ASSET : includes

    ASSET {
        long id PK
        string asset_tag
        string name
        string status
        decimal purchase_cost
        date purchase_date
        json dynamic_attributes
    }

    ASSET_COMPONENT {
        long id PK
        string component_type
        string serial_number
        string status
        string old_component_disposition
    }

    ALLOCATION {
        long id PK
        long asset_id FK
        long user_id FK
        datetime allocated_at
        datetime returned_at
        string status
    }

    USER {
        long id PK
        string username
        string email
        string employee_id
        boolean enabled
    }

    MAINTENANCE_RECORD {
        long id PK
        long asset_id FK
        string type
        string status
        decimal cost
    }
```

## Key Relationships

### Asset Lifecycle
- **Self-Join**: Assets can have a parent-child relationship (e.g., a Laptop as a parent to a Monitor or Docking Station).
- **Components**: Tracks modular parts (RAM, Battery) that can be replaced or disposed of.
- **Status History**: Every change in an asset's availability is logged for audit purposes.

### Resource Allocation
- **Allocation**: Acts as a join table between `Asset` and `User`, tracking who currently holds the asset and the historical chain of custody.

### RBAC (Role-Based Access Control)
- **User-Role-Permission**: A standard many-to-many relationship that defines granular access to system features (e.g., an Admin can replace components, while a Viewer can only see details).
