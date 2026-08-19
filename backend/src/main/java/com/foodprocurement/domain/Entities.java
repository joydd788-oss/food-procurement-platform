package com.foodprocurement.domain;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.*; import java.util.UUID;
public final class Entities { private Entities(){}
 @Entity @Table(name="food_standard") public static class FoodStandard { @Id public UUID id=UUID.randomUUID(); public String code,name,category,specification,safetyRequirement; public boolean active=true; public Instant createdAt=Instant.now(); }
 @Entity public static class Supplier { @Id public UUID id=UUID.randomUUID(); public String name,creditCode,contactName,contactPhone,qualificationStatus; public BigDecimal rating=BigDecimal.ZERO; public boolean active=true; public Instant createdAt=Instant.now(); }
 @Entity @Table(name="procurement_plan") public static class ProcurementPlan { @Id public UUID id=UUID.randomUUID(); public String planNo,title,organization,status; public BigDecimal budget; public LocalDate requiredDate; public Instant createdAt=Instant.now(); }
 @Entity public static class Inquiry { @Id public UUID id=UUID.randomUUID(); public String inquiryNo,title,status; public UUID planId; public Instant deadline,createdAt=Instant.now(); }
 @Entity public static class Bid { @Id public UUID id=UUID.randomUUID(); public UUID inquiryId,supplierId; public BigDecimal amount; public String remark; public Instant submittedAt=Instant.now(); }
 @Entity @Table(name="purchase_order") public static class PurchaseOrder { @Id public UUID id=UUID.randomUUID(); public String orderNo,status; public UUID supplierId,planId; public BigDecimal totalAmount; public LocalDate expectedDelivery; public Instant createdAt=Instant.now(); }
 @Entity public static class Delivery { @Id public UUID id=UUID.randomUUID(); public String deliveryNo,vehicleNo,status; public UUID orderId; public Instant deliveredAt,createdAt=Instant.now(); }
 @Entity public static class Inspection { @Id public UUID id=UUID.randomUUID(); public UUID deliveryId; public String inspector,result,note; public BigDecimal temperature; public Instant inspectedAt=Instant.now(); }
 @Entity @Table(name="inventory_lot") public static class InventoryLot { @Id public UUID id=UUID.randomUUID(); public String lotNo,unit,status; public UUID standardId,supplierId; public BigDecimal quantity; public LocalDate receivedDate,expiryDate; }
 @Entity public static class Settlement { @Id public UUID id=UUID.randomUUID(); public String settlementNo,status; public UUID orderId; public BigDecimal amount; public LocalDate dueDate; public Instant createdAt=Instant.now(); }
}
