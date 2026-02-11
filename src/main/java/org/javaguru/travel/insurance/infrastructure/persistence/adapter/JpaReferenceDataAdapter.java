package org.javaguru.travel.insurance.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.entity.MedicalRiskLimitLevel;
import org.javaguru.travel.insurance.domain.model.entity.Risk;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;
import org.javaguru.travel.insurance.domain.model.valueobject.RiskCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.javaguru.travel.insurance.infrastructure.persistence.mapper.CountryMapper;
import org.javaguru.travel.insurance.infrastructure.persistence.mapper.MedicalLevelMapper;
import org.javaguru.travel.insurance.infrastructure.persistence.mapper.RiskMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA адаптер для ReferenceDataPort
 * Реализует интерфейс домена используя JPA репозитории
 * 
 * Это слой Infrastructure - он знает о JPA, но домен не знает о нем
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaReferenceDataAdapter implements ReferenceDataPort {
    
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    
    private final CountryMapper countryMapper;
    private final RiskMapper riskMapper;
    private final MedicalLevelMapper medicalLevelMapper;
    
    @Override
    public Optional<Country> findCountry(CountryCode code, LocalDate date) {
        log.debug("Finding country: code={}, date={}", code, date);
        
        return countryRepository
            .findActiveByIsoCode(code.value(), date)
            .map(countryMapper::toDomain);
    }
    
    @Override
    public Optional<Risk> findRisk(RiskCode code, LocalDate date) {
        log.debug("Finding risk: code={}, date={}", code, date);
        
        return riskTypeRepository
            .findActiveByCode(code.value(), date)
            .map(riskMapper::toDomain);
    }
    
    @Override
    public Optional<MedicalRiskLimitLevel> findMedicalLevel(String code, LocalDate date) {
        log.debug("Finding medical level: code={}, date={}", code, date);
        
        return medicalLevelRepository
            .findActiveByCode(code, date)
            .map(medicalLevelMapper::toDomain);
    }
    
    @Override
    public List<Risk> findRisks(List<RiskCode> codes, LocalDate date) {
        log.debug("Finding risks: codes={}, date={}", codes, date);
        
        return codes.stream()
            .map(code -> findRisk(code, date))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Risk> findAllActiveRisks(LocalDate date) {
        log.debug("Finding all active risks on date={}", date);
        
        return riskTypeRepository.findAllActive()
            .stream()
            .filter(entity -> entity.isActiveOn(date))
            .map(riskMapper::toDomain)
            .collect(Collectors.toList());
    }
}
