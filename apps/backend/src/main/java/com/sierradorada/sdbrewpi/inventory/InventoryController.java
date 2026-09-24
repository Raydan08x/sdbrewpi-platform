package com.sierradorada.sdbrewpi.inventory;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/inventory")
class InventoryController {
    private final InventoryService service; InventoryController(InventoryService service){this.service=service;}
    @GetMapping("/overview") ResponseEntity<InventoryOverview> overview(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.overview());}
    @PostMapping("/sites/{siteId}/items") ResponseEntity<InventoryItemView> createItem(@PathVariable String siteId,@Valid @RequestBody InventoryItemRequest request){InventoryItemView v=service.createItem(siteId,request);return ResponseEntity.created(URI.create("/api/v1/inventory/items/"+v.id())).body(v);}
    @PostMapping("/items/{itemId}/lots") ResponseEntity<InventoryLotView> createLot(@PathVariable String itemId,@Valid @RequestBody InventoryLotRequest request){InventoryLotView v=service.createLot(itemId,request);return ResponseEntity.created(URI.create("/api/v1/inventory/lots/"+v.id())).body(v);}
    @PostMapping("/movements") ResponseEntity<InventoryMovementView> movement(@Valid @RequestBody InventoryMovementRequest request,@RequestHeader(value="X-Actor",required=false)String actor){InventoryMovementView v=service.createMovement(request,actor);return ResponseEntity.created(URI.create("/api/v1/inventory/movements/"+v.id())).body(v);}
}
