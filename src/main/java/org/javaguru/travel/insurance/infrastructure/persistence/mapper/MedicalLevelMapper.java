package org.javaguru.travel.insurance.infrastructure.persistence.mapper;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.domain.model.entity.MedicalRiskLimitLevel;
import org.javaguru.travel.insurance.domain.model.valueobject.Currency;
import org.springframework.stereotype.Component;

/**
 * Mapper для преобразования JPA Entity → Domain Entity
 */
@Component
public class MedicalLevelMapper {
    
    /**
     * Преобразует JPA Entity в Domain Entity
     */
    public MedicalRiskLimitLevel toDomain(MedicalRiskLimitLevelEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new MedicalRiskLimitLevel(
            entity.getCode(),
            entity.getCoverageAmount(),
            entity.getDailyRate(),
            Currency.fromStringOrDefault(entity.getCurrency()),
            entity.getValidFrom(),
            entity.getValidTo()
        );
    }
}
