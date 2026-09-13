package com.sierradorada.sdbrewpi.production;

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
import java.util.List;

@RestController
@RequestMapping("/api/v1/production")
public class ProductionController {
    private final ProductionService service;

    public ProductionController(ProductionService service) { this.service = service; }

    @GetMapping("/overview")
    ResponseEntity<ProductionOverview> overview() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.overview());
    }

    @PostMapping("/recipes")
    ResponseEntity<RecipeView> createRecipe(@Valid @RequestBody RecipeRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        RecipeView created = service.createRecipe(request, actor);
        return ResponseEntity.created(URI.create("/api/v1/production/recipes/" + created.id())).body(created);
    }

    @PostMapping("/batches")
    ResponseEntity<BatchView> createBatch(@Valid @RequestBody BatchRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        BatchView created = service.createBatch(request, actor);
        return ResponseEntity.created(URI.create("/api/v1/production/batches/" + created.id())).body(created);
    }

    @PutMapping("/batches/{id}/complete")
    BatchView completeBatch(@PathVariable String id, @Valid @RequestBody BatchCompletionRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.completeBatch(id, request, actor);
    }

    @PutMapping("/batches/{id}/profile/start")
    BatchView startProfile(@PathVariable String id, @Valid @RequestBody ProfileCommandRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.startProfile(id, request, actor);
    }

    @PutMapping("/batches/{id}/profile/pause")
    BatchView pauseProfile(@PathVariable String id, @Valid @RequestBody ProfileCommandRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.pauseProfile(id, request, actor);
    }

    @PutMapping("/batches/{id}/profile/resume")
    BatchView resumeProfile(@PathVariable String id, @Valid @RequestBody ProfileCommandRequest request,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return service.resumeProfile(id, request, actor);
    }

    @GetMapping("/batches/{id}/events")
    List<BatchEventView> events(@PathVariable String id) { return service.events(id); }
}
