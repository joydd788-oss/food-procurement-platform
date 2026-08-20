package com.foodprocurement.repository;

import com.foodprocurement.domain.Entities.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public final class Repositories {
  private Repositories() {}
  public interface Standards extends JpaRepository<FoodStandard, UUID> {
    Page<FoodStandard> findByNameContainingIgnoreCase(String name, Pageable pageable);
  }
  public interface Suppliers extends JpaRepository<Supplier, UUID> {
    Page<Supplier> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<Supplier> findByAccountUsername(String accountUsername);
  }
  public interface Plans extends JpaRepository<ProcurementPlan, UUID> {
    Page<ProcurementPlan> findByTitleContainingIgnoreCase(String title, Pageable pageable);
  }
  public interface Inquiries extends JpaRepository<Inquiry, UUID> {
    Page<Inquiry> findByTitleContainingIgnoreCase(String title, Pageable pageable);
  }
  public interface Bids extends JpaRepository<Bid, UUID> {
    List<Bid> findByInquiryIdOrderByAmountAsc(UUID inquiryId);
    Optional<Bid> findByInquiryIdAndSupplierId(UUID inquiryId, UUID supplierId);
  }
  public interface Orders extends JpaRepository<PurchaseOrder, UUID> {
    Page<PurchaseOrder> findByOrderNoContainingIgnoreCase(String orderNo, Pageable pageable);
  }
  public interface Deliveries extends JpaRepository<Delivery, UUID> {
    Page<Delivery> findByDeliveryNoContainingIgnoreCase(String deliveryNo, Pageable pageable);
  }
  public interface Inspections extends JpaRepository<Inspection, UUID> {
    Page<Inspection> findByInspectorContainingIgnoreCase(String inspector, Pageable pageable);
  }
  public interface Lots extends JpaRepository<InventoryLot, UUID> {
    List<InventoryLot> findByLotNo(String lotNo);
    Page<InventoryLot> findByLotNoContainingIgnoreCase(String lotNo, Pageable pageable);
  }
  public interface Settlements extends JpaRepository<Settlement, UUID> {
    Page<Settlement> findBySettlementNoContainingIgnoreCase(String settlementNo, Pageable pageable);
  }
}
