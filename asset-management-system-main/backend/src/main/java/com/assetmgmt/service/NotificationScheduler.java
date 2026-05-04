package com.assetmgmt.service;

import com.assetmgmt.entity.Allocation;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.entity.MaintenanceRecord;
import com.assetmgmt.entity.User;
import com.assetmgmt.repository.AllocationRepository;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.repository.MaintenanceRepository;
import com.assetmgmt.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NotificationScheduler {

    private final AssetRepository assetRepository;
    private final AllocationRepository allocationRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public NotificationScheduler(AssetRepository assetRepository,
                                 AllocationRepository allocationRepository,
                                 MaintenanceRepository maintenanceRepository,
                                 UserRepository userRepository,
                                 NotificationService notificationService) {
        this.assetRepository = assetRepository;
        this.allocationRepository = allocationRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkWarrantyExpiry() {
        LocalDate now = LocalDate.now();
        LocalDate in30Days = now.plusDays(30);
        List<Asset> assets = assetRepository.findByWarrantyExpiryBetween(now, in30Days);

        List<User> admins = userRepository.findByEnabled(true).stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName())))
                .collect(Collectors.toList());

        for (Asset asset : assets) {
            String message = "Warranty expiring soon: " + asset.getName() + " (Tag: " + asset.getAssetTag() + ") expires on " + asset.getWarrantyExpiryDate();
            
            // Notify assigned user(s)
            List<Allocation> activeAllocations = allocationRepository.findByStatus(Allocation.AllocationStatus.ACTIVE);
            activeAllocations.stream()
                    .filter(a -> a.getAsset().getId().equals(asset.getId()))
                    .forEach(a -> notificationService.sendNotification(a.getUser(), message, "WARRANTY"));

            // Notify admins
            for (User admin : admins) {
                notificationService.sendNotification(admin, message, "WARRANTY");
            }
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkOverdueAllocations() {
        List<Allocation> activeAllocations = allocationRepository.findByStatus(Allocation.AllocationStatus.ACTIVE);
        LocalDate now = LocalDate.now();

        for (Allocation allocation : activeAllocations) {
            if (allocation.getExpectedReturnDate() != null && allocation.getExpectedReturnDate().isBefore(now)) {
                allocation.setStatus(Allocation.AllocationStatus.OVERDUE);
                allocationRepository.save(allocation);

                String message = "Your asset is overdue for return: " + allocation.getAsset().getName();
                notificationService.sendNotification(allocation.getUser(), message, "OVERDUE");
            }
        }
    }

    @Scheduled(cron = "0 30 8 * * *")
    @Transactional
    public void checkMaintenanceDue() {
        LocalDate now = LocalDate.now();
        LocalDate in3Days = now.plusDays(3);
        List<MaintenanceRecord> records = maintenanceRepository.findByStatusAndScheduledDateBetween(
                MaintenanceRecord.MaintenanceStatus.SCHEDULED, now, in3Days);

        List<User> admins = userRepository.findByEnabled(true).stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName())))
                .collect(Collectors.toList());

        for (MaintenanceRecord record : records) {
            String message = "Maintenance due soon for: " + record.getAsset().getName() + " on " + record.getScheduledDate();
            for (User admin : admins) {
                notificationService.sendNotification(admin, message, "MAINTENANCE");
            }
        }
    }
}
