package com.sierradorada.sdbrewpi.plant;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plant")
public class PlantController {
    private final PlantService service;

    public PlantController(PlantService service) { this.service = service; }

    @GetMapping("/overview")
    ResponseEntity<PlantOverview> overview() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.overview());
    }

    @PutMapping("/sites/{id}")
    PlantProfileView update(@PathVariable String id, @Valid @RequestBody PlantProfileRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.updateProfile(id, request, actor);
    }

    @PostMapping("/sites/{siteId}/assets")
    ResponseEntity<PlantAssetView> createAsset(@PathVariable String siteId,
            @Valid @RequestBody PlantAssetCreateRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        PlantAssetView created = service.createAsset(siteId, request, actor);
        return ResponseEntity.created(URI.create("/api/v1/plant/assets/" + created.id())).body(created);
    }

    @PutMapping("/assets/{id}")
    PlantAssetView updateAsset(@PathVariable String id, @Valid @RequestBody PlantAssetUpdateRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.updateAsset(id, request, actor);
    }

    @PutMapping("/assets/{id}/retire")
    PlantAssetView retireAsset(@PathVariable String id, @Valid @RequestBody PlantAssetRetireRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.retireAsset(id, request, actor);
    }
}
