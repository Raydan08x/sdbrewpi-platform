package com.sierradorada.sdbrewpi.fermentation;

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
@RequestMapping("/api/v1/fermentation")
public class FermentationController {
    private final FermentationService service;

    public FermentationController(FermentationService service) { this.service = service; }

    @GetMapping("/overview")
    ResponseEntity<FermentationOverview> overview() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.overview());
    }

    @PutMapping("/tanks/{id}/setpoint")
    CommandResult setpoint(@PathVariable String id, @Valid @RequestBody SetpointRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.setpoint(id, request, actor);
    }

    @PutMapping("/tanks/{id}/mode")
    CommandResult mode(@PathVariable String id, @Valid @RequestBody ModeRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.mode(id, request, actor);
    }
}

