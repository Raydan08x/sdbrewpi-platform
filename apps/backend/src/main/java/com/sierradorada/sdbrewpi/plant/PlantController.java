package com.sierradorada.sdbrewpi.plant;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
