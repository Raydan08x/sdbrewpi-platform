package com.sierradorada.sdbrewpi.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InventoryService {
    private static final Set<String> CATEGORIES=Set.of("RAW_MATERIAL","PACKAGING_MATERIAL","OPERATING_SUPPLY","WORK_IN_PROGRESS","FINISHED_GOOD","SPARE_PART");
    private static final Set<String> QUALITY=Set.of("QUARANTINE","APPROVED","REJECTED","BLOCKED");
    private static final Set<String> IN=Set.of("RECEIPT","ADJUSTMENT_IN","RETURN_IN");
    private static final Set<String> OUT=Set.of("ISSUE","ADJUSTMENT_OUT","CONSUMPTION","DISPOSAL");
    private final InventoryRepository repository;
    InventoryService(InventoryRepository repository){this.repository=repository;}

    InventoryOverview overview(){
        String site=repository.primarySiteId();
        List<InventoryItemView> items=repository.items(site); List<InventoryLotView> lots=repository.lots(site);
        List<InventoryBalanceView> balances=repository.balances(site);
        int low=(int)items.stream().filter(i->i.minimumStock()>0&&balances.stream().filter(b->b.itemId().equals(i.id())).mapToDouble(InventoryBalanceView::quantity).sum()<i.minimumStock()).count();
        int quarantine=(int)lots.stream().filter(l->"QUARANTINE".equals(l.qualityStatus())).count();
        return new InventoryOverview(Instant.now(),items,lots,balances,repository.movements(site),low,quarantine);
    }
    @Transactional InventoryItemView createItem(String siteId,InventoryItemRequest request){
        String category=upper(request.category()); if(!CATEGORIES.contains(category))throw new IllegalArgumentException("La categoría de inventario no es válida");
        String code=code(request.code()); if(repository.codeExists(siteId,code))throw new IllegalStateException("Ya existe un artículo con ese código");
        String id=UUID.randomUUID().toString(); repository.insertItem(id,siteId,code,request,Instant.now()); return repository.item(id).orElseThrow();
    }
    @Transactional InventoryLotView createLot(String itemId,InventoryLotRequest request){
        InventoryItemView item=repository.item(itemId).orElseThrow(()->new IllegalArgumentException("El artículo no existe"));
        if(!item.trackLots())throw new IllegalStateException("El artículo no está configurado para seguimiento por lote");
        if(!QUALITY.contains(upper(request.qualityStatus())))throw new IllegalArgumentException("El estado de calidad no es válido");
        String id=UUID.randomUUID().toString(); repository.insertLot(id,itemId,request,Instant.now()); return repository.lot(id).orElseThrow();
    }
    @Transactional InventoryMovementView createMovement(InventoryMovementRequest request,String actor){
        InventoryItemView item=repository.item(request.itemId()).orElseThrow(()->new IllegalArgumentException("El artículo no existe"));
        String type=upper(request.movementType()); if(!IN.contains(type)&&!OUT.contains(type))throw new IllegalArgumentException("El tipo de movimiento no es válido");
        if(!repository.warehouseAllows(request.warehouseId(),item.category()))throw new IllegalStateException("La bodega no admite la categoría del artículo");
        if(request.locationId()!=null&&!request.locationId().isBlank()&&!repository.locationBelongs(request.locationId(),request.warehouseId()))throw new IllegalArgumentException("La ubicación no pertenece a la bodega");
        if(item.trackLots()){
            if(request.lotId()==null||request.lotId().isBlank())throw new IllegalArgumentException("El lote es obligatorio para este artículo");
            InventoryLotView lot=repository.lot(request.lotId()).orElseThrow(()->new IllegalArgumentException("El lote no existe"));
            if(!lot.itemId().equals(item.id()))throw new IllegalArgumentException("El lote no pertenece al artículo");
            if(IN.contains(type)&&"REJECTED".equals(lot.qualityStatus()))throw new IllegalStateException("No se puede ingresar existencia a un lote rechazado");
        } else if(request.lotId()!=null&&!request.lotId().isBlank()) throw new IllegalArgumentException("Este artículo no maneja lotes");
        double signed=IN.contains(type)?request.quantity():-request.quantity();
        if(signed<0&&repository.balance(item.id(),request.lotId(),request.warehouseId(),request.locationId())+signed<0)throw new IllegalStateException("El movimiento dejaría existencias negativas");
        String id=UUID.randomUUID().toString(); repository.insertMovement(id,request,signed,safeActor(actor),Instant.now());
        return repository.movements(item.siteId()).stream().filter(m->m.id().equals(id)).findFirst().orElseThrow();
    }
    private String upper(String s){return s.trim().toUpperCase(Locale.ROOT);} private String code(String s){String v=upper(s).replaceAll("[^A-Z0-9_-]","-").replaceAll("-+","-");if(v.isBlank())throw new IllegalArgumentException("El código no es válido");return v;}
    private String safeActor(String a){if(a==null||a.isBlank())return "local-webapp";return a.trim().substring(0,Math.min(80,a.trim().length()));}
}
