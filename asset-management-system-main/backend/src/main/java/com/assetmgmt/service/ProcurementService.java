package com.assetmgmt.service;

import com.assetmgmt.entity.Procurement;
import com.assetmgmt.entity.User;
import com.assetmgmt.repository.ProcurementRepository;
import com.assetmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final ProcurementRepository procurementRepository;
    private final UserRepository userRepository;

    public List<Procurement> getAllProcurements() {
        return procurementRepository.findAll();
    }

    public Optional<Procurement> getProcurementById(Long id) {
        return procurementRepository.findById(id);
    }

    public Procurement createProcurement(Procurement procurement, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        procurement.setCreatedBy(user);
        return procurementRepository.save(procurement);
    }

    public Procurement updateProcurement(Long id, Procurement updatedProcurement) {
        return procurementRepository.findById(id).map(procurement -> {
            procurement.setPoNumber(updatedProcurement.getPoNumber());
            procurement.setVendor(updatedProcurement.getVendor());
            procurement.setOrderDate(updatedProcurement.getOrderDate());
            procurement.setTotalCost(updatedProcurement.getTotalCost());
            procurement.setStatus(updatedProcurement.getStatus());
            return procurementRepository.save(procurement);
        }).orElseThrow(() -> new RuntimeException("Procurement not found with id " + id));
    }

    public void deleteProcurement(Long id) {
        procurementRepository.deleteById(id);
    }
}
