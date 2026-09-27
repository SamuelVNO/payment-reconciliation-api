package io.github.samuelvno.paymentreconciliation.store;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.samuelvno.paymentreconciliation.shared.error.BusinessConflictException;
import io.github.samuelvno.paymentreconciliation.shared.error.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    public StoreResponse create(CreateStoreRequest request) {
        if (storeRepository.existsByDocument(request.document())) {
            throw new BusinessConflictException("A store with document %s already exists".formatted(request.document()));
        }
        Store store = storeRepository.save(new Store(request.name(), request.document(), request.feeRate()));
        return StoreResponse.from(store);
    }

    public StoreResponse findById(UUID id) {
        return StoreResponse.from(getStore(id));
    }

    public Page<StoreResponse> findAll(Pageable pageable) {
        return storeRepository.findAll(pageable).map(StoreResponse::from);
    }

    // Sem save(): a entidade está "managed" e o dirty checking gera o UPDATE no commit.
    @Transactional
    public StoreResponse update(UUID id, UpdateStoreRequest request) {
        Store store = getStore(id);
        store.update(request.name(), request.feeRate());
        return StoreResponse.from(store);
    }

    public Store getStore(UUID id) {
        return storeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Store", id));
    }
}
