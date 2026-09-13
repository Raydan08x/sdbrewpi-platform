package com.sierradorada.sdbrewpi.plant;

import com.sierradorada.sdbrewpi.shared.RevisionConflictException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlantService {
    private static final Set<String> ASSET_TYPES = Set.of(
        "CHILLER", "PUMP", "VFD", "POWER", "FERMENTER", "BREWHOUSE", "MILL",
        "CONTROLLER", "HMI", "RELAY_MODULE", "SENSOR_GATEWAY", "SENSOR", "PACKAGING", "FILTER", "OTHER");
    private static final Set<String> ASSET_STATUSES = Set.of(
        "DOCUMENTED", "AVAILABLE", "NEEDS_DATA", "OFFLINE", "WAITING_BATTERY", "MAINTENANCE", "OUT_OF_SERVICE");
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
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id, "UPDATE_PLANT_PROFILE",
            request.name().trim() + "; fermentadores=" + request.plannedFermenters(),
            "Configuración de planta actualizada");
        return repository.findSite(id).orElseThrow();
    }

    @Transactional
    public PlantAssetView createAsset(String siteId, PlantAssetCreateRequest request, String actor) {
        repository.findSite(siteId).orElseThrow(() -> new IllegalArgumentException("La planta no existe"));
        String code = normalizeCode(request.code());
        validateAsset(request.assetType(), request.status());
        if (repository.assetCodeExists(siteId, code, null)) {
            throw new IllegalStateException("Ya existe un equipo con ese código en la planta");
        }
        String id = UUID.randomUUID().toString();
        repository.insertAsset(id, siteId, code, request, Instant.now());
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id,
            "CREATE_PLANT_ASSET", code, "Equipo registrado en la planta");
        return repository.findAsset(id).orElseThrow();
    }

    @Transactional
    public PlantAssetView updateAsset(String id, PlantAssetUpdateRequest request, String actor) {
        PlantAssetView current = repository.findAsset(id)
            .orElseThrow(() -> new IllegalArgumentException("El equipo no existe"));
        if (!current.active()) throw new IllegalStateException("El equipo está retirado");
        String code = normalizeCode(request.code());
        validateAsset(request.assetType(), request.status());
        if (repository.assetCodeExists(current.siteId(), code, id)) {
            throw new IllegalStateException("Ya existe un equipo con ese código en la planta");
        }
        if (repository.updateAsset(id, code, request, Instant.now()) == 0) throw new RevisionConflictException();
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id,
            "UPDATE_PLANT_ASSET", code, "Equipo actualizado");
        return repository.findAsset(id).orElseThrow();
    }

    @Transactional
    public PlantAssetView retireAsset(String id, PlantAssetRetireRequest request, String actor) {
        PlantAssetView current = repository.findAsset(id)
            .orElseThrow(() -> new IllegalArgumentException("El equipo no existe"));
        if (!current.active()) throw new IllegalStateException("El equipo ya está retirado");
        if (repository.retireAsset(id, request.expectedRevision(), Instant.now()) == 0) {
            throw new RevisionConflictException();
        }
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id,
            "RETIRE_PLANT_ASSET", current.code(), "Equipo retirado del inventario activo");
        return repository.findAsset(id).orElseThrow();
    }

    private void validateAsset(String typeValue, String statusValue) {
        String type = typeValue.trim().toUpperCase(Locale.ROOT);
        String status = statusValue.trim().toUpperCase(Locale.ROOT);
        if (!ASSET_TYPES.contains(type)) throw new IllegalArgumentException("El tipo de equipo no es válido");
        if (!ASSET_STATUSES.contains(status)) throw new IllegalArgumentException("El estado del equipo no es válido");
    }

    private String normalizeCode(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "-")
            .replaceAll("-+", "-");
        if (normalized.isBlank() || normalized.length() > 50) {
            throw new IllegalArgumentException("El código debe contener letras o números y tener máximo 50 caracteres");
        }
        return normalized;
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
