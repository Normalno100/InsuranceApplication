package org.javaguru.travel.insurance;

import org.javaguru.travel.insurance.BaseTestFixture;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Улучшенная версия MockSetupHelper с двумя подходами:
 * 1. Strict mocks - для точных тестов (вызывают ошибку если не используются)
 * 2. Lenient mocks - для setup в @BeforeEach (не вызывают ошибку)
 *
 * Рекомендация: используйте lenient только в @BeforeEach,
 * в самих тестах используйте обычные strict моки
 */
public class MockSetupHelper extends BaseTestFixture {

    // ========================================
    // СТРОГИЕ МОКИ (для использования в @Test)
    // ========================================

    /**
     * Строгий мок - будет ошибка если не используется
     * Используйте в самом тесте для конкретных проверок
     */
    public void mockAgeCalculatorStrict(AgeCalculator calculator, int age, BigDecimal coefficient) {
        when(calculator.calculateAge(any(), any())).thenReturn(age);
        when(calculator.getAgeCoefficient(age)).thenReturn(coefficient);
        when(calculator.calculateAgeAndCoefficient(any(), any()))
                .thenReturn(createAgeResult(age, coefficient));
    }

    public void mockCountryStrict(CountryRepository repository, CountryEntity country) {
        when(repository.findActiveByIsoCode(eq(country.getIsoCode()), any()))
                .thenReturn(Optional.of(country));
    }

    public void mockMedicalLevelStrict(MedicalRiskLimitLevelRepository repository,
                                       MedicalRiskLimitLevelEntity level) {
        when(repository.findActiveByCode(eq(level.getCode()), any()))
                .thenReturn(Optional.of(level));
    }

    public void mockRiskTypeStrict(RiskTypeRepository repository, RiskTypeEntity risk) {
        when(repository.findActiveByCode(eq(risk.getCode()), any()))
                .thenReturn(Optional.of(risk));
    }

    // ========================================
    // ЛЕНИВЫЕ МОКИ (для @BeforeEach)
    // ========================================

    /**
     * Ленивый мок - НЕ будет ошибки если не используется
     * Используйте в @BeforeEach для базовой настройки
     */
    public void mockAgeCalculatorLenient(AgeCalculator calculator, int age, BigDecimal coefficient) {
        lenient().when(calculator.calculateAge(any(), any())).thenReturn(age);
        lenient().when(calculator.getAgeCoefficient(age)).thenReturn(coefficient);
        lenient().when(calculator.calculateAgeAndCoefficient(any(), any()))
                .thenReturn(createAgeResult(age, coefficient));
    }

    public void mockCountryLenient(CountryRepository repository, CountryEntity country) {
        lenient().when(repository.findActiveByIsoCode(eq(country.getIsoCode()), any()))
                .thenReturn(Optional.of(country));
    }

    public void mockMedicalLevelLenient(MedicalRiskLimitLevelRepository repository,
                                        MedicalRiskLimitLevelEntity level) {
        lenient().when(repository.findActiveByCode(eq(level.getCode()), any()))
                .thenReturn(Optional.of(level));
    }

    public void mockRiskTypeLenient(RiskTypeRepository repository, RiskTypeEntity risk) {
        lenient().when(repository.findActiveByCode(eq(risk.getCode()), any()))
                .thenReturn(Optional.of(risk));
    }

    // ========================================
    // КОМПЛЕКСНАЯ НАСТРОЙКА
    // ========================================

    /**
     * Настройка всех репозиториев (LENIENT для @BeforeEach)
     * Эти моки не будут вызывать ошибку если не используются
     */
    public void setupDefaultMocksLenient(
            AgeCalculator ageCalculator,
            CountryRepository countryRepo,
            MedicalRiskLimitLevelRepository medicalRepo,
            RiskTypeRepository riskRepo) {

        mockAgeCalculatorLenient(ageCalculator, 35, new BigDecimal("1.1"));
        mockCountryLenient(countryRepo, defaultCountry());
        mockMedicalLevelLenient(medicalRepo, defaultMedicalLevel());
        mockRiskTypeLenient(riskRepo, mandatoryRisk());
    }

    /**
     * Настройка всех репозиториев (STRICT для @Test)
     * Эти моки ДОЛЖНЫ быть использованы в тесте
     */
    public void setupDefaultMocksStrict(
            AgeCalculator ageCalculator,
            CountryRepository countryRepo,
            MedicalRiskLimitLevelRepository medicalRepo,
            RiskTypeRepository riskRepo) {

        mockAgeCalculatorStrict(ageCalculator, 35, new BigDecimal("1.1"));
        mockCountryStrict(countryRepo, defaultCountry());
        mockMedicalLevelStrict(medicalRepo, defaultMedicalLevel());
        mockRiskTypeStrict(riskRepo, mandatoryRisk());
    }

    // ========================================
    // СПЕЦИАЛЬНЫЕ СЛУЧАИ
    // ========================================

    public void mockCountryNotFound(CountryRepository repository, String isoCode) {
        lenient().when(repository.findActiveByIsoCode(eq(isoCode), any()))
                .thenReturn(Optional.empty());
    }

    public void mockMedicalLevelNotFound(MedicalRiskLimitLevelRepository repository, String code) {
        lenient().when(repository.findActiveByCode(eq(code), any()))
                .thenReturn(Optional.empty());
    }

    public void mockRiskNotFound(RiskTypeRepository repository, String code) {
        lenient().when(repository.findActiveByCode(eq(code), any()))
                .thenReturn(Optional.empty());
    }
}