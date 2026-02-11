package org.javaguru.travel.insurance.infrastructure.persistence.mapper;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.valueobject.Coefficient;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;
import org.springframework.stereotype.Component;

/**
 * Mapper для преобразования JPA Entity → Domain Entity
 * Изолирует домен от инфраструктуры
 */
@Component
public class CountryMapper {
    
    /**
     * Преобразует JPA Entity в Domain Entity
     */
    public Country toDomain(CountryEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Country(
            new CountryCode(entity.getIsoCode()),
            entity.getNameEn(),
            entity.getNameRu(),
            mapRiskGroup(entity.getRiskGroup()),
            new Coefficient(entity.getRiskCoefficient()),
            entity.getValidFrom(),
            entity.getValidTo()
        );
    }
    
    /**
     * Преобразует строковую группу риска в enum
     */
    private Country.RiskGroup mapRiskGroup(String riskGroup) {
        if (riskGroup == null) {
            throw new IllegalArgumentException("Risk group cannot be null");
        }
        
        return switch (riskGroup.toUpperCase()) {
            case "LOW" -> Country.RiskGroup.LOW;
            case "MEDIUM" -> Country.RiskGroup.MEDIUM;
            case "HIGH" -> Country.RiskGroup.HIGH;
            case "VERY_HIGH" -> Country.RiskGroup.VERY_HIGH;
            default -> throw new IllegalArgumentException("Unknown risk group: " + riskGroup);
        };
    }
}
