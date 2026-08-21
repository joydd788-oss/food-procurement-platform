package com.foodprocurement.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public final class Entities {
  private Entities() {}

  @Entity
  @Table(name = "food_standard")
  public static class FoodStandard {
    @Id public UUID id = UUID.randomUUID();
    @NotBlank @Size(max = 40) public String code;
    @NotBlank @Size(max = 120) public String name;
    @NotBlank @Size(max = 60) public String category;
    @Size(max = 255) public String specification;
    public String safetyRequirement;
    public boolean active = true;
    public Instant createdAt = Instant.now();
  }

  @Entity
  @Table(name = "supplier")
  public static class Supplier {
    @Id public UUID id = UUID.randomUUID();
    @NotBlank @Size(max = 160) public String name;
    @NotBlank @Size(max = 30) public String creditCode;
    @Size(max = 60) public String accountUsername;
    @Size(max = 60) public String contactName;
    @Size(max = 30) public String contactPhone;
    @NotBlank @Size(max = 30) public String qualificationStatus;
    @NotNull @DecimalMin("0") @DecimalMax("9.99") public BigDecimal rating = BigDecimal.ZERO;
    public boolean active = true;
    public Instant createdAt = Instant.now();
  }

  @Entity
  @Table(name = "procurement_plan")
  public static class ProcurementPlan {
    @Id public UUID id = UUID.randomUUID();
    @NotBlank @Size(max = 40) public String planNo;
    @NotBlank @Size(max = 160) public String title;
    @NotBlank @Size(max = 120) public String organization;
    @NotNull @DecimalMin("0.01") public BigDecimal budget;
    @NotBlank @Size(max = 30) public String status;
    public LocalDate requiredDate;
    public Instant createdAt = Instant.now();
  }

  @Entity
  @Table(name = "inquiry")
  public static class Inquiry {
    @Id public UUID id = UUID.randomUUID();
    @NotBlank @Size(max = 40) public String inquiryNo;
    @NotBlank @Size(max = 160) public String title;
    @NotBlank @Size(max = 30) public String status;
    public UUID planId;
    public Instant deadline, createdAt = Instant.now();
  }

  @Entity
  @Table(name = "bid")
  public static class Bid {
    @Id public UUID id = UUID.randomUUID();
    public UUID inquiryId;
    public UUID supplierId;
    @NotNull @DecimalMin("0.01") public BigDecimal amount;
    @Size(max = 500) public String remark;
    public Instant submittedAt = Instant.now();
  }

  @Entity
  @Table(name = "purchase_order")
  public static class PurchaseOrder {
    @Id public UUID id = UUID.randomUUID();
    @NotBlank @Size(max = 40) public String orderNo;
    @NotBlank @Size(max = 30) public String status;
    public UUID supplierId, planId;
    @NotNull @DecimalMin("0") public BigDecimal totalAmount;
    public LocalDate expectedDelivery;
    public Instant createdAt = Instant.now();
  }

  @Entity
  @Table(name = "delivery")
  public static class Delivery {
    @Id public UUID id = UUID.randomUUID();
    @NotBlank @Size(max = 40) public String deliveryNo;
    @Size(max = 30) public String vehicleNo;
    @NotBlank @Size(max = 30) public String status;
    public UUID orderId;
    public Instant deliveredAt, createdAt = Instant.now();
  }

  @Entity
  @Table(name = "inspection")
  public static class Inspection {
    @Id public UUID id = UUID.randomUUID();
    public UUID deliveryId;
    @NotBlank @Size(max = 80) public String inspector;
    @NotBlank @Size(max = 30) public String result;
    @Size(max = 500) public String note;
    @DecimalMin("-999.99") @DecimalMax("999.99") public BigDecimal temperature;
    public Instant inspectedAt = Instant.now();
  }

  @Entity
  @Table(name = "inventory_lot")
  public static class InventoryLot {
    @Id public UUID id = UUID.randomUUID();
    @NotBlank @Size(max = 60) public String lotNo;
    public UUID standardId, supplierId;
    @NotNull @DecimalMin("0") public BigDecimal quantity;
    @NotBlank @Size(max = 20) public String unit;
    @NotNull public LocalDate receivedDate;
    public LocalDate expiryDate;
    @NotBlank @Size(max = 30) public String status;
  }

  @Entity
  @Table(name = "settlement")
  public static class Settlement {
    @Id public UUID id = UUID.randomUUID();
    @NotBlank @Size(max = 40) public String settlementNo;
    @NotBlank @Size(max = 30) public String status;
    public UUID orderId;
    @NotNull @DecimalMin("0") public BigDecimal amount;
    public LocalDate dueDate;
    public Instant createdAt = Instant.now();
  }
}
