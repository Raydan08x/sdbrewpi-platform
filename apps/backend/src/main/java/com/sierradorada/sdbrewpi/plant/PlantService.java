package com.sierradorada.sdbrewpi.plant;

import com.sierradorada.sdbrewpi.shared.RevisionConflictException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlantService {
    private final PlantRepository repository;
    private final boolean hardwareEnabled;

    public PlantService(PlantRepository repository,
            @Value("${sdbrewpi.hardware.enabled:false}") boolean hardwareEnabled) {
        this.repository = repository;
        this.hardwareEnabled = hardwareEnabled;
    }

    public PlantOverview overview() {
        PlantProfileView site = repository.primarySite();
        List<PlantAssetView> assets = repository.findAssets(site.id());
        List<String> profiles = assets.stream().map(PlantAssetView::firmwareProfile)
            .filter(value -> !value.isBlank()).distinct().sorted().toList();
        int needingData = (int) assets.stream().filter(asset -> "NEEDS_DATA".equals(asset.status())).count();
        DeviceOnboardingView onboarding = new DeviceOnboardingView(
            "WAITING_FOR_CONNECTION", false, hardwareEnabled,
            "Conecta un controlador compatible por USB para iniciar la identificación; el escáner todavía no está habilitado.",
            profiles,
            List.of("Detectar puerto y hardware", "Leer identidad y versión", "Seleccionar firmware compatible",
                "Verificar firma y checksum", "Crear respaldo", "Instalar y validar", "Registrar el dispositivo"));
        return new PlantOverview(Instant.now(), site, assets, onboarding, needingData);
    }

    @Transactional
    public PlantProfileView updateProfile(String id, PlantProfileRequest request, String actor) {
        repository.findSite(id).orElseThrow(() -> new IllegalArgumentException("La planta no existe"));
        validateTimezone(request.timezone());
        validateCurrency(request.currency());
        validateLogoUrl(request.logoUrl());
        if (repository.updateProfile(id, request, Instant.now()) == 0) throw new RevisionConflictException();
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id,
            request.name().trim() + "; fermentadores=" + request.plannedFermenters());
        return repository.findSite(id).orElseThrow();
    }

    private void validateTimezone(String value) {
        try { ZoneId.of(value.trim()); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("La zona horaria no es válida"); }
    }

    private void validateCurrency(String value) {
        try { Currency.getInstance(value.trim()); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("La moneda no es válida"); }
    }

    private void validateLogoUrl(String value) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = URI.create(value.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("La URL del logo debe usar HTTP o HTTPS");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La URL del logo debe usar HTTP o HTTPS");
        }
    }

    private String safeActor(String actor) {
        if (actor == null || actor.isBlank()) return "local-webapp";
        String trimmed = actor.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }
}
