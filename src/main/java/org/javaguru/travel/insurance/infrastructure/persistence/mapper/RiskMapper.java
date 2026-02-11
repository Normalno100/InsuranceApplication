package org.javaguru.travel.insurance.infrastructure.persistence.mapper;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.domain.model.entity.Risk;
import org.javaguru.travel.insurance.domain.model.valueobject.Coefficient;
import org.javaguru.travel.insurance.domain.model.valueobject.RiskCode;
import org.springframework.stereotype.Component;

/**
 * Mapper для преобразования JPA Entity → Domain Entity
 */
@Component
public class RiskMapper {
    
    /**
     * Преобразует JPA Entity в Domain Entity
     */
    public Risk toDomain(RiskTypeEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Risk(
            new RiskCode(entity.getCode()),
            entity.getNameEn(),
            entity.getNameRu(),
            new Coefficient(entity.getCoefficient()),
            entity.getIsMandatory(),
            entity.getDescription(),
            entity.getValidFrom(),
            entity.getValidTo()
        );
    }
}
