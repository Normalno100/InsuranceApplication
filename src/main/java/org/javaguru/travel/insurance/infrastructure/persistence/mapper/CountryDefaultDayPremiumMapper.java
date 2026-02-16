package org.javaguru.travel.insurance.infrastructure.persistence.mapper;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CountryDefaultDayPremiumEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mapper для преобразования между JPA Entity и Domain моделями
 * 
 * НАЗНАЧЕНИЕ:
 * Изолирует Domain layer от Infrastructure layer.
 * Domain не знает о JPA, Hibernate и деталях хранения.
 * 
 * ПРИНЦИПЫ:
 * - Entity → Domain: toDomain()
 * - Domain → Entity: toEntity()
 * - Immutable domain objects
 * - Mutable JPA entities
 */
@Component
public class CountryDefaultDayPremiumMapper {

    /**
     * Преобразует JPA Entity в DTO для использования в сервисах
     * 
     * Возвращаем простой DTO вместо полноценного Domain объекта,
     * т.к. CountryDefaultDayPremium - это справочные данные без сложной бизнес-логики.
     * 
     * @param entity JPA entity
     * @return DTO с данными премии
     */
    public CountryDefaultDayPremiumDto toDomain(CountryDefaultDayPremiumEntity entity) {
        if (entity == null) {
            return null;
        }

        return new CountryDefaultDayPremiumDto(
                entity.getId(),
                entity.getCountryIsoCode(),
                entity.getDefaultDayPremium(),
                entity.getCurrency(),
                entity.getDescription(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.isActive()
        );
    }

    /**
     * Преобразует DTO в JPA Entity
     * 
     * Используется редко, т.к. обычно создание/обновление делается напрямую через Entity.
     * 
     * @param dto DTO с данными
     * @return JPA entity
     */
    public CountryDefaultDayPremiumEntity toEntity(CountryDefaultDayPremiumDto dto) {
        if (dto == null) {
            return null;
        }

        CountryDefaultDayPremiumEntity entity = new CountryDefaultDayPremiumEntity();
        entity.setId(dto.id());
        entity.setCountryIsoCode(dto.countryIsoCode());
        entity.setDefaultDayPremium(dto.defaultDayPremium());
        entity.setCurrency(dto.currency());
        entity.setDescription(dto.description());
        entity.setValidFrom(dto.validFrom());
        entity.setValidTo(dto.validTo());

        return entity;
    }

    /**
     * DTO для передачи данных дефолтной премии
     * 
     * Immutable record для безопасной передачи данных между слоями.
     * 
     * @param id ID записи
     * @param countryIsoCode ISO код страны
     * @param defaultDayPremium дефолтная дневная премия
     * @param currency валюта
     * @param description описание
     * @param validFrom дата начала действия
     * @param validTo дата окончания действия (может быть null)
     * @param isActive активна ли премия сейчас
     */
    public record CountryDefaultDayPremiumDto(
            Long id,
            String countryIsoCode,
            BigDecimal defaultDayPremium,
            String currency,
            String description,
            LocalDate validFrom,
            LocalDate validTo,
            boolean isActive
    ) {
        /**
         * Проверяет, действует ли премия на указанную дату
         */
        public boolean isActiveOn(LocalDate date) {
            if (date.isBefore(validFrom)) {
                return false;
            }
            return validTo == null || !date.isAfter(validTo);
        }

        /**
         * Создает builder для удобного создания DTO
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder для CountryDefaultDayPremiumDto
         */
        public static class Builder {
            private Long id;
            private String countryIsoCode;
            private BigDecimal defaultDayPremium;
            private String currency = "EUR";
            private String description;
            private LocalDate validFrom = LocalDate.now();
            private LocalDate validTo;
            private boolean isActive = true;

            public Builder id(Long id) {
                this.id = id;
                return this;
            }

            public Builder countryIsoCode(String countryIsoCode) {
                this.countryIsoCode = countryIsoCode;
                return this;
            }

            public Builder defaultDayPremium(BigDecimal defaultDayPremium) {
                this.defaultDayPremium = defaultDayPremium;
                return this;
            }

            public Builder currency(String currency) {
                this.currency = currency;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder validFrom(LocalDate validFrom) {
                this.validFrom = validFrom;
                return this;
            }

            public Builder validTo(LocalDate validTo) {
                this.validTo = validTo;
                return this;
            }

            public Builder isActive(boolean isActive) {
                this.isActive = isActive;
                return this;
            }

            public CountryDefaultDayPremiumDto build() {
                return new CountryDefaultDayPremiumDto(
                        id,
                        countryIsoCode,
                        defaultDayPremium,
                        currency,
                        description,
                        validFrom,
                        validTo,
                        isActive
                );
            }
        }
    }
}
