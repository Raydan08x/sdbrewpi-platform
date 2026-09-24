package com.sierradorada.sdbrewpi.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

record InventoryItemView(String id, String siteId, String code, String name, String category, String baseUnit,
    boolean trackLots, double minimumStock, String notes, long revision, boolean active, Instant updatedAt) {}
record InventoryLotView(String id, String itemId, String internalCode, String supplierLot, LocalDate expiryDate,
    String qualityStatus, String notes, Instant createdAt) {}
record InventoryBalanceView(String itemId, String itemCode, String itemName, String category, String baseUnit,
    String warehouseId, String warehouseCode, String warehouseName, String locationId, String locationCode,
    String lotId, String lotCode, String qualityStatus, double quantity, double minimumStock) {}
record InventoryMovementView(String id, String itemId, String itemCode, String lotId, String lotCode,
    String warehouseId, String warehouseCode, String locationId, String locationCode, String movementType,
    double quantity, String reference, String notes, String actor, Instant occurredAt) {}
record InventoryOverview(Instant generatedAt, List<InventoryItemView> items, List<InventoryLotView> lots,
    List<InventoryBalanceView> balances, List<InventoryMovementView> recentMovements, int lowStockItems,
    int quarantinedLots) {}

record InventoryItemRequest(@NotBlank @Size(max=50) String code, @NotBlank @Size(max=160) String name,
    @NotBlank String category, @NotBlank @Size(max=20) String baseUnit, boolean trackLots,
    @DecimalMin("0") double minimumStock, @Size(max=1000) String notes) {}
record InventoryLotRequest(@NotBlank @Size(max=80) String internalCode, @Size(max=100) String supplierLot,
    LocalDate expiryDate, @NotBlank String qualityStatus, @Size(max=500) String notes) {}
record InventoryMovementRequest(@NotBlank String itemId, String lotId, @NotBlank String warehouseId,
    String locationId, @NotBlank String movementType, @NotNull @DecimalMin("0.001") Double quantity,
    @Size(max=120) String reference, @Size(max=500) String notes) {}
