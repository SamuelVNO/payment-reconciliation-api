package io.github.samuelvno.paymentreconciliation.store;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Lojas", description = "Cadastro das lojas (tenants). Somente ADMIN cria e lista.")
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @Operation(summary = "Cadastra uma loja com a taxa contratada")
    @PostMapping
    public ResponseEntity<StoreResponse> create(@Valid @RequestBody CreateStoreRequest request) {
        StoreResponse store = storeService.create(request);
        return ResponseEntity.created(URI.create("/api/stores/" + store.id())).body(store);
    }

    @Operation(summary = "Busca uma loja")
    @GetMapping("/{id}")
    public StoreResponse findById(@PathVariable UUID id) {
        return storeService.findById(id);
    }

    @Operation(summary = "Lista lojas (paginado)")
    @GetMapping
    public Page<StoreResponse> findAll(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return storeService.findAll(pageable);
    }

    @Operation(summary = "Atualiza nome e taxa")
    @PutMapping("/{id}")
    public StoreResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateStoreRequest request) {
        return storeService.update(id, request);
    }
}
