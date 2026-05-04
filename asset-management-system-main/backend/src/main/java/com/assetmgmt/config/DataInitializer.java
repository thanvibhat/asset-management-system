package com.assetmgmt.config;

import com.assetmgmt.entity.*;
import com.assetmgmt.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_PASSWORD = "Admin@1234";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocationMasterRepository locationMasterRepository;
    private final ManufacturerMasterRepository manufacturerMasterRepository;
    private final ProductMasterRepository productMasterRepository;
    private final AssetCategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final VendorRepository vendorRepository;
    private final ProcurementRepository procurementRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AuditLogRepository auditLogRepository;

    public DataInitializer(UserRepository userRepository, 
                           RoleRepository roleRepository, 
                           PermissionRepository permissionRepository,
                           PasswordEncoder passwordEncoder,
                           LocationMasterRepository locationMasterRepository,
                           ManufacturerMasterRepository manufacturerMasterRepository,
                           ProductMasterRepository productMasterRepository,
                           AssetCategoryRepository categoryRepository,
                           AssetRepository assetRepository,
                           VendorRepository vendorRepository,
                           ProcurementRepository procurementRepository,
                           MaintenanceRepository maintenanceRepository,
                           AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.locationMasterRepository = locationMasterRepository;
        this.manufacturerMasterRepository = manufacturerMasterRepository;
        this.productMasterRepository = productMasterRepository;
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
        this.vendorRepository = vendorRepository;
        this.procurementRepository = procurementRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void run(String... args) {
        // Seed roles first
        seedRoles();

        // Seed users
        upsertUser("admin",    "admin@assetmgmt.com",   "System Administrator", "ROLE_ADMIN");
        upsertUser("manager1", "manager@assetmgmt.com", "Asset Manager",        "ROLE_MANAGER");
        upsertUser("viewer1",  "viewer@assetmgmt.com",  "John Viewer",          "ROLE_VIEWER");

        // Seed master data
        seedLocations();
        seedManufacturers();
        seedProductMaster();

        // Seed permissions
        upsertPermission("PRODUCT_MANAGE", "Create and manage product master");
        upsertPermission("MASTER_MANAGE",  "Manage location and manufacturer master data");
        upsertPermission("REPORT_VIEW",    "View dashboard and analytics reports");
        upsertPermission("USER_MANAGE",    "Manage users and roles");
        upsertPermission("ASSET_CREATE",   "Create new assets");
        upsertPermission("ASSET_UPDATE",   "Update existing assets");
        upsertPermission("ASSET_DELETE",   "Delete assets");

        // Seed Analytics Data
        seedAnalyticsData();

        // Sync asset statuses with active maintenance
        syncAssetStatuses();

        log.info("Data initialization completed successfully.");
    }

    private void seedRoles() {
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_VIEWER");
        for (String roleName : roles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).description(roleName + " role").build());
                log.info("Seeded role: '{}'", roleName);
            }
        }
    }

    private void seedLocations() {
        List<String[]> locations = List.of(
            new String[]{ "Head Office",         "Main corporate headquarters" },
            new String[]{ "Branch - Mumbai",     "Mumbai regional office" },
            new String[]{ "Branch - Delhi",      "Delhi regional office" },
            new String[]{ "Branch - Bangalore",  "Bangalore regional office" },
            new String[]{ "Branch - Chennai",    "Chennai regional office" },
            new String[]{ "Server Room",         "Primary data centre and server room" },
            new String[]{ "Warehouse",           "Asset storage and inventory warehouse" },
            new String[]{ "Remote / WFH",        "Work-from-home or remote employee location" }
        );

        for (String[] entry : locations) {
            String name = entry[0];
            String description = entry[1];
            if (!locationMasterRepository.existsByNameIgnoreCase(name)) {
                locationMasterRepository.save(
                    LocationMaster.builder()
                        .name(name)
                        .description(description)
                        .build()
                );
                log.info("Seeded location: '{}'", name);
            }
        }
    }

    private void seedManufacturers() {
        List<String[]> manufacturers = List.of(
            new String[]{ "Dell",          "https://www.dell.com" },
            new String[]{ "Apple",         "https://www.apple.com" },
            new String[]{ "HP",            "https://www.hp.com" },
            new String[]{ "Lenovo",        "https://www.lenovo.com" },
            new String[]{ "Samsung",       "https://www.samsung.com" },
            new String[]{ "LG",            "https://www.lg.com" },
            new String[]{ "Logitech",      "https://www.logitech.com" },
            new String[]{ "Sony",          "https://www.sony.com" },
            new String[]{ "Asus",          "https://www.asus.com" },
            new String[]{ "Acer",          "https://www.acer.com" },
            new String[]{ "Microsoft",     "https://www.microsoft.com" },
            new String[]{ "Cisco",         "https://www.cisco.com" }
        );

        for (String[] entry : manufacturers) {
            String name = entry[0];
            String website = entry[1];
            if (!manufacturerMasterRepository.existsByNameIgnoreCase(name)) {
                manufacturerMasterRepository.save(
                    ManufacturerMaster.builder()
                        .name(name)
                        .website(website)
                        .build()
                    );
                log.info("Seeded manufacturer: '{}'", name);
            }
        }
    }

    private void seedProductMaster() {
        seedProduct(
            "Laptop - Standard",
            "Laptops",           // category name
            "Dell",
            "Standard corporate laptop for employees",
            "LAP",
            "[" +
              "{\"name\":\"RAM (GB)\",\"dataType\":\"Number\",\"mandatory\":true}," +
              "{\"name\":\"Storage (GB)\",\"dataType\":\"Number\",\"mandatory\":true}," +
              "{\"name\":\"Processor\",\"dataType\":\"String\",\"mandatory\":true}," +
              "{\"name\":\"Display Size (inches)\",\"dataType\":\"Number\",\"mandatory\":false}," +
              "{\"name\":\"Operating System\",\"dataType\":\"String\",\"mandatory\":false}" +
            "]"
        );

        seedProduct(
            "Desktop Computer",
            "Laptop",           // reuse Laptop category or create Desktop category if exists
            "HP",
            "Office desktop workstation",
            "DKT",
            "[" +
              "{\"name\":\"RAM (GB)\",\"dataType\":\"Number\",\"mandatory\":true}," +
              "{\"name\":\"Storage (GB)\",\"dataType\":\"Number\",\"mandatory\":true}," +
              "{\"name\":\"Processor\",\"dataType\":\"String\",\"mandatory\":true}," +
              "{\"name\":\"Monitor Size (inches)\",\"dataType\":\"Number\",\"mandatory\":false}" +
            "]"
        );

        seedProduct(
            "Smartphone - Corporate",
            "Mobile Phones",
            "Samsung",
            "Corporate mobile phone for field staff",
            "MOB",
            "[" +
              "{\"name\":\"IMEI Number\",\"dataType\":\"String\",\"mandatory\":true}," +
              "{\"name\":\"OS Version\",\"dataType\":\"String\",\"mandatory\":true}," +
              "{\"name\":\"Storage (GB)\",\"dataType\":\"Number\",\"mandatory\":false}," +
              "{\"name\":\"SIM Type\",\"dataType\":\"String\",\"mandatory\":false}" +
            "]"
        );

        seedProduct(
            "Monitor",
            "Peripherals",
            "LG",
            "External display monitor",
            "MON",
            "[" +
              "{\"name\":\"Screen Size (inches)\",\"dataType\":\"Number\",\"mandatory\":true}," +
              "{\"name\":\"Resolution\",\"dataType\":\"String\",\"mandatory\":true}," +
              "{\"name\":\"Panel Type\",\"dataType\":\"String\",\"mandatory\":false}" +
            "]"
        );

        seedProduct(
            "Wireless Mouse",
            "Peripherals",
            "Logitech",
            "Standard wireless mouse",
            "MOU",
            "[" +
              "{\"name\":\"Connectivity\",\"dataType\":\"String\",\"mandatory\":false}," +
              "{\"name\":\"DPI\",\"dataType\":\"Number\",\"mandatory\":false}" +
            "]"
        );

        seedProduct(
            "Keyboard",
            "Peripherals",
            "Logitech",
            "Standard USB/wireless keyboard",
            "KEY",
            "[" +
              "{\"name\":\"Layout\",\"dataType\":\"String\",\"mandatory\":false}," +
              "{\"name\":\"Connectivity\",\"dataType\":\"String\",\"mandatory\":false}" +
            "]"
        );

        seedProduct(
            "Network Switch",
            "Peripherals",
            "Cisco",
            "Managed network switch for office infrastructure",
            "NSW",
            "[" +
              "{\"name\":\"Number of Ports\",\"dataType\":\"Number\",\"mandatory\":true}," +
              "{\"name\":\"Speed (Gbps)\",\"dataType\":\"Number\",\"mandatory\":true}," +
              "{\"name\":\"Managed\",\"dataType\":\"Boolean\",\"mandatory\":false}," +
              "{\"name\":\"PoE Supported\",\"dataType\":\"Boolean\",\"mandatory\":false}" +
            "]"
        );

        seedProduct(
            "UPS - Office",
            "Peripherals",
            "APC",
            "Uninterruptible power supply for workstations",
            "UPS",
            "[" +
              "{\"name\":\"Capacity (VA)\",\"dataType\":\"Number\",\"mandatory\":true}," +
              "{\"name\":\"Battery Backup (minutes)\",\"dataType\":\"Number\",\"mandatory\":false}" +
            "]"
        );
    }

    private void seedProduct(String productName, String categoryName,
                              String manufacturer, String description,
                              String assetPrefix, String additionalAttributes) {
        if (!productMasterRepository.existsByProductNameIgnoreCase(productName)) {
            AssetCategory category = categoryRepository.findByName(categoryName).orElse(null);
            ProductMaster product = ProductMaster.builder()
                .productName(productName)
                .category(category)
                .manufacturer(manufacturer)
                .description(description)
                .assetPrefix(assetPrefix)
                .additionalAttributes(additionalAttributes)
                .build();
            productMasterRepository.save(product);
            log.info("Seeded product: '{}' (prefix: {})", productName, assetPrefix);
        }
    }

    private void seedAnalyticsData() {
        if (assetRepository.count() > 0) return;

        log.info("Seeding analytics data...");

        // 1. Vendors
        Vendor dellVendor = vendorRepository.save(Vendor.builder().name("Dell India").contactEmail("sales@dell.in").contactPhone("+91 80 12345678").address("Electronic City, Bangalore").vendorType(Vendor.VendorType.BOTH).build());
        Vendor appleVendor = vendorRepository.save(Vendor.builder().name("Apple Premium Reseller").contactEmail("orders@apple.com").contactPhone("+91 22 87654321").address("Bandra Kurla Complex, Mumbai").vendorType(Vendor.VendorType.PROCUREMENT).build());
        Vendor localService = vendorRepository.save(Vendor.builder().name("Quick Repair Services").contactEmail("fix@it.com").contactPhone("+91 11 99998888").address("Nehru Place, Delhi").vendorType(Vendor.VendorType.MAINTENANCE).build());
        
        vendorRepository.save(Vendor.builder().name("HP World").contactEmail("support@hp.com").contactPhone("+91 80 44332211").address("Whitefield, Bangalore").vendorType(Vendor.VendorType.BOTH).build());
        vendorRepository.save(Vendor.builder().name("Lenovo Solutions").contactEmail("biz@lenovo.in").contactPhone("+91 44 55667788").address("Guindy, Chennai").vendorType(Vendor.VendorType.PROCUREMENT).build());
        vendorRepository.save(Vendor.builder().name("Samsung Electronics").contactEmail("corporate@samsung.com").contactPhone("+91 124 5550000").address("Gurgaon, Haryana").vendorType(Vendor.VendorType.BOTH).build());
        vendorRepository.save(Vendor.builder().name("Logitech Peripheral Shop").contactEmail("sales@logitech.com").contactPhone("+1 650 1234567").address("Newark, California, USA").vendorType(Vendor.VendorType.PROCUREMENT).build());
        vendorRepository.save(Vendor.builder().name("Global Network Fixers").contactEmail("help@networkfixers.com").contactPhone("+91 22 11223344").address("Andheri West, Mumbai").vendorType(Vendor.VendorType.MAINTENANCE).build());
        vendorRepository.save(Vendor.builder().name("Office Supplies Co.").contactEmail("info@officesupplies.in").contactPhone("+91 11 44332211").address("Connaught Place, Delhi").vendorType(Vendor.VendorType.BOTH).build());

        // 2. Categories (Already seeded in seedProductMaster via seedProduct)
        AssetCategory laptopCat = categoryRepository.findByName("Laptops").orElseThrow();
        AssetCategory mobileCat = categoryRepository.findByName("Mobile Phones").orElseThrow();

        // 3. Assets & Procurement
        java.time.LocalDate now = java.time.LocalDate.now();
        
        Procurement p1 = procurementRepository.save(Procurement.builder()
                .poNumber("PO-001").vendor(dellVendor).totalCost(new java.math.BigDecimal("500000")).quantity(10).orderDate(now.minusMonths(6)).build());
        
        for (int i = 1; i <= 5; i++) {
            Asset a = assetRepository.save(Asset.builder()
                    .assetTag("LAP-" + i).name("Dell Latitude 5420").category(laptopCat).status(Asset.AssetStatus.AVAILABLE)
                    .purchaseDate(now.minusMonths(6)).purchaseCost(new java.math.BigDecimal("50000")).currentValue(new java.math.BigDecimal("40000"))
                    .procurement(p1).build());
            
            // Add maintenance records
            maintenanceRepository.save(MaintenanceRecord.builder()
                    .asset(a).vendor(dellVendor).maintenanceType(MaintenanceRecord.MaintenanceType.PREVENTIVE)
                    .description("Routine checkup").cost(new java.math.BigDecimal("2000")).status(MaintenanceRecord.MaintenanceStatus.COMPLETED)
                    .scheduledDate(now.minusMonths(3)).completedDate(now.minusMonths(3)).build());
        }

        Asset a2 = assetRepository.save(Asset.builder()
                .assetTag("IPHONE-1").name("iPhone 13").category(mobileCat).status(Asset.AssetStatus.AVAILABLE)
                .purchaseDate(now.minusMonths(12)).purchaseCost(new java.math.BigDecimal("80000")).currentValue(new java.math.BigDecimal("60000"))
                .build());

        maintenanceRepository.save(MaintenanceRecord.builder()
                .asset(a2).vendor(localService).maintenanceType(MaintenanceRecord.MaintenanceType.CORRECTIVE)
                .description("Screen Replacement").cost(new java.math.BigDecimal("15000")).status(MaintenanceRecord.MaintenanceStatus.COMPLETED)
                .scheduledDate(now.minusMonths(1)).completedDate(now.minusMonths(1)).build());

        log.info("Analytics data seeded.");
        
        // 4. Audit Logs
        auditLogRepository.save(AuditLog.builder().entityType("Asset").entityId(1L).action("CREATE").performedBy("admin").details("{\"name\":\"Dell Latitude 5420\",\"assetTag\":\"LAP-1\"}").build());
        auditLogRepository.save(AuditLog.builder().entityType("Asset").entityId(2L).action("CREATE").performedBy("admin").details("{\"name\":\"Dell Latitude 5420\",\"assetTag\":\"LAP-2\"}").build());
        auditLogRepository.save(AuditLog.builder().entityType("User").entityId(2L).action("UPDATE").performedBy("admin").details("{\"username\":\"manager1\",\"enabled\":true}").build());
        auditLogRepository.save(AuditLog.builder().entityType("MaintenanceRecord").entityId(1L).action("CREATE").performedBy("manager1").details("{\"description\":\"Routine checkup\",\"cost\":2000}").build());
        auditLogRepository.save(AuditLog.builder().entityType("Allocation").entityId(1L).action("CREATE").performedBy("manager1").details("{\"assetId\":1,\"userId\":2}").build());
        auditLogRepository.save(AuditLog.builder().entityType("Vendor").entityId(1L).action("UPDATE").performedBy("admin").details("{\"name\":\"Dell India\",\"status\":\"ACTIVE\"}").build());
        
        log.info("Audit logs seeded.");
    }

    private void upsertPermission(String name, String description) {
        permissionRepository.findByName(name).ifPresentOrElse(
            existing -> {},  // already exists, skip
            () -> {
                Permission permission = new Permission();
                permission.setName(name);
                permission.setDescription(description);
                permissionRepository.save(permission);
                log.info("Seeded permission: '{}'", name);

                // Assign to ROLE_ADMIN automatically
                roleRepository.findByName("ROLE_ADMIN").ifPresent(adminRole -> {
                    adminRole.getPermissions().add(permission);
                    roleRepository.save(adminRole);
                    log.info("Assigned permission '{}' to ROLE_ADMIN", name);
                });
            }
        );
    }

    private void upsertUser(String username, String email, String fullName, String roleName) {
        roleRepository.findByName(roleName).ifPresent(role -> {
            userRepository.findByUsername(username).ifPresentOrElse(
                existing -> {
                    boolean needsSave = false;

                    if (!passwordEncoder.matches(DEFAULT_PASSWORD, existing.getPassword())) {
                        existing.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                        needsSave = true;
                        log.warn("Fixed incorrect password hash for user: '{}'", username);
                    }

                    boolean hasRole = existing.getRoles().stream()
                            .anyMatch(r -> r.getName().equals(roleName));
                    if (!hasRole) {
                        existing.getRoles().add(role);
                        needsSave = true;
                        log.warn("Re-linked missing role '{}' to existing user '{}'", roleName, username);
                    }

                    if (needsSave) userRepository.save(existing);
                },
                () -> {
                    User user = User.builder()
                            .username(username)
                            .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                            .email(email)
                            .fullName(fullName)
                            .roles(new java.util.HashSet<>(java.util.Collections.singletonList(role)))
                            .enabled(true)
                            .build();
                    userRepository.save(user);
                    log.info("Created default user: '{}' with role={}", username, roleName);
                }
            );
        });
    }
    private void syncAssetStatuses() {
        log.info("Synchronizing asset statuses with active maintenance records...");
        List<MaintenanceRecord.MaintenanceStatus> activeStatuses = List.of(
            MaintenanceRecord.MaintenanceStatus.SCHEDULED,
            MaintenanceRecord.MaintenanceStatus.IN_PROGRESS
        );
        
        List<MaintenanceRecord> activeRecords = maintenanceRepository.findByStatusIn(activeStatuses);
        log.info("Found {} active maintenance records to sync.", activeRecords.size());
        
        for (MaintenanceRecord record : activeRecords) {
            Asset asset = record.getAsset();
            if (asset != null) {
                log.info("Checking asset '{}' (Tag: {}) - Current status: {}", asset.getName(), asset.getAssetTag(), asset.getStatus());
                if (asset.getStatus() != Asset.AssetStatus.UNDER_MAINTENANCE) {
                    asset.setStatus(Asset.AssetStatus.UNDER_MAINTENANCE);
                    assetRepository.save(asset);
                    log.info("Fixed status for asset '{}' (Tag: {}) -> UNDER_MAINTENANCE", asset.getName(), asset.getAssetTag());
                }
            }
        }
    }
}
