package com.foodprocurement.web;

import com.foodprocurement.domain.Entities.*;
import com.foodprocurement.repository.Repositories.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1")
public class ApiController {

  private final Standards standards;
  private final Suppliers suppliers;
  private final Plans plans;
  private final Inquiries inquiries;
  private final Bids bids;
  private final Orders orders;
  private final Deliveries deliveries;
  private final Inspections inspections;
  private final Lots lots;
  private final Settlements settlements;

  ApiController(Standards standards, Suppliers suppliers, Plans plans, Inquiries inquiries, Bids bids,
      Orders orders, Deliveries deliveries, Inspections inspections, Lots lots, Settlements settlements) {
    this.standards = standards;
    this.suppliers = suppliers;
    this.plans = plans;
    this.inquiries = inquiries;
    this.bids = bids;
    this.orders = orders;
    this.deliveries = deliveries;
    this.inspections = inspections;
    this.lots = lots;
    this.settlements = settlements;
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication a) {
    return Map.of("username", a.getName(),
        "authorities", a.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
  }

  @GetMapping("/standards")
  public Map<String, Object> standards(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<FoodStandard> p = (q == null || q.isBlank())
        ? standards.findAll(pageable) : standards.findByNameContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/standards")
  @ResponseStatus(HttpStatus.CREATED)
  public FoodStandard createStandard(@Valid @RequestBody FoodStandard x) {
    x.id = UUID.randomUUID();
    x.createdAt = Instant.now();
    return standards.save(x);
  }

  @GetMapping("/suppliers")
  public Map<String, Object> suppliers(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<Supplier> p = (q == null || q.isBlank())
        ? suppliers.findAll(pageable) : suppliers.findByNameContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/suppliers")
  @ResponseStatus(HttpStatus.CREATED)
  public Supplier createSupplier(@Valid @RequestBody Supplier x) {
    x.id = UUID.randomUUID();
    x.createdAt = Instant.now();
    return suppliers.save(x);
  }

  @GetMapping("/plans")
  public Map<String, Object> plans(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<ProcurementPlan> p = (q == null || q.isBlank())
        ? plans.findAll(pageable) : plans.findByTitleContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/plans")
  @ResponseStatus(HttpStatus.CREATED)
  public ProcurementPlan createPlan(@Valid @RequestBody ProcurementPlan x) {
    x.id = UUID.randomUUID();
    x.createdAt = Instant.now();
    return plans.save(x);
  }

  @GetMapping("/inquiries")
  public Map<String, Object> inquiries(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<Inquiry> p = (q == null || q.isBlank())
        ? inquiries.findAll(pageable) : inquiries.findByTitleContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/inquiries")
  @ResponseStatus(HttpStatus.CREATED)
  public Inquiry createInquiry(@Valid @RequestBody Inquiry x) {
    x.id = UUID.randomUUID();
    x.createdAt = Instant.now();
    return inquiries.save(x);
  }

  @Operation(summary = "供应商报价；同一供应商可重复提交，以最新报价为准")
  @GetMapping("/inquiries/{id}/bids")
  public List<Bid> bids(@PathVariable UUID id) {
    if (hasOnlySupplierRole()) {
      Supplier own = suppliers.findByAccountUsername(currentUsername()).orElse(null);
      if (own == null) {
        return List.of();
      }
      Optional<Bid> mine = bids.findByInquiryIdAndSupplierId(id, own.id);
      return mine.isPresent() ? List.of(mine.get()) : List.of();
    }
    return bids.findByInquiryIdOrderByAmountAsc(id);
  }

  @PostMapping("/inquiries/{id}/bids")
  @ResponseStatus(HttpStatus.CREATED)
  public Bid bid(@PathVariable UUID id, @Valid @RequestBody Bid x) {
    Inquiry inq = inquiries.findById(id).orElseThrow(() -> new NoSuchElementException("询价不存在"));
    if (!"OPEN".equals(inq.status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "询价已关闭，无法报价");
    }
    if (inq.deadline != null && inq.deadline.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "报价已截止");
    }
    if (hasOnlySupplierRole()) {
      Supplier own = suppliers.findByAccountUsername(currentUsername())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账户未关联供应商档案"));
      x.supplierId = own.id;
    } else if (x.supplierId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少供应商");
    }
    x.inquiryId = id;
    x.id = UUID.randomUUID();
    x.submittedAt = Instant.now();
    return bids.findByInquiryIdAndSupplierId(id, x.supplierId)
        .map(existing -> {
          existing.amount = x.amount;
          existing.remark = x.remark;
          existing.submittedAt = x.submittedAt;
          return bids.save(existing);
        })
        .orElseGet(() -> bids.save(x));
  }

  @GetMapping("/orders")
  public Map<String, Object> orders(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<PurchaseOrder> p = (q == null || q.isBlank())
        ? orders.findAll(pageable) : orders.findByOrderNoContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public PurchaseOrder createOrder(@Valid @RequestBody PurchaseOrder x) {
    x.id = UUID.randomUUID();
    x.createdAt = Instant.now();
    return orders.save(x);
  }

  @GetMapping("/deliveries")
  public Map<String, Object> deliveries(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<Delivery> p = (q == null || q.isBlank())
        ? deliveries.findAll(pageable) : deliveries.findByDeliveryNoContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/deliveries")
  @ResponseStatus(HttpStatus.CREATED)
  public Delivery createDelivery(@Valid @RequestBody Delivery x) {
    x.id = UUID.randomUUID();
    x.createdAt = Instant.now();
    return deliveries.save(x);
  }

  @GetMapping("/inspections")
  public Map<String, Object> inspections(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "inspectedAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<Inspection> p = (q == null || q.isBlank())
        ? inspections.findAll(pageable) : inspections.findByInspectorContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/inspections")
  @ResponseStatus(HttpStatus.CREATED)
  public Inspection createInspection(@Valid @RequestBody Inspection x) {
    x.id = UUID.randomUUID();
    x.inspectedAt = Instant.now();
    return inspections.save(x);
  }

  @GetMapping("/inventory")
  public Map<String, Object> inventory(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "receivedDate", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<InventoryLot> p = (q == null || q.isBlank())
        ? lots.findAll(pageable) : lots.findByLotNoContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/inventory")
  @ResponseStatus(HttpStatus.CREATED)
  public InventoryLot createLot(@Valid @RequestBody InventoryLot x) {
    x.id = UUID.randomUUID();
    return lots.save(x);
  }

  @GetMapping("/settlements")
  public Map<String, Object> settlements(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<Settlement> p = (q == null || q.isBlank())
        ? settlements.findAll(pageable) : settlements.findBySettlementNoContainingIgnoreCase(q, pageable);
    return page(p);
  }

  @PostMapping("/settlements")
  @ResponseStatus(HttpStatus.CREATED)
  public Settlement createSettlement(@Valid @RequestBody Settlement x) {
    x.id = UUID.randomUUID();
    x.createdAt = Instant.now();
    return settlements.save(x);
  }

  @Operation(summary = "以库存批次号追溯食材来源")
  @GetMapping("/traceability/{lotNo}")
  public Map<String, Object> trace(@PathVariable String lotNo) {
    var lot = lots.findByLotNo(lotNo).stream().findFirst()
        .orElseThrow(() -> new NoSuchElementException("未找到批次"));
    Map<String, Object> out = new HashMap<>();
    out.put("lot", lot);
    out.put("standard", standards.findById(lot.standardId).orElse(null));
    out.put("supplier", suppliers.findById(lot.supplierId).orElse(null));
    return out;
  }

  @GetMapping("/dashboard")
  public Map<String, Object> dashboard() {
    return Map.of("standards", standards.count(),
        "suppliers", suppliers.count(),
        "plans", plans.count(),
        "openInquiries", inquiries.findAll().stream().filter(x -> "OPEN".equals(x.status)).count(),
        "orders", orders.count(),
        "inventoryLots", lots.count(),
        "pendingSettlements", settlements.findAll().stream().filter(x -> !"PAID".equals(x.status)).count());
  }

  private Map<String, Object> page(Page<?> p) {
    return Map.of("items", p.getContent(), "total", p.getTotalElements(), "page", p.getNumber(), "size", p.getSize());
  }

  private String currentUsername() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    return a == null ? null : a.getName();
  }

  private boolean hasOnlySupplierRole() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) {
      return false;
    }
    var roles = a.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    return roles.contains("ROLE_SUPPLIER") && !roles.contains("ROLE_ADMIN")
        && !roles.contains("ROLE_BUYER") && !roles.contains("ROLE_REGULATOR");
  }

  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<Map<String, String>> missing(NoSuchElementException e) {
    return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> badRequest(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + ": " + f.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(Map.of("message", msg));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Map<String, String>> constraint(ConstraintViolationException e) {
    return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException e) {
    return ResponseEntity.badRequest().body(Map.of("message", "数据违反唯一性或完整性约束，请检查后重试"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
    String reason = e.getReason() == null ? String.valueOf(e.getStatusCode().value()) : e.getReason();
    return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", reason));
  }
}
