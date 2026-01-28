package org.javaguru.travel.insurance.domain.port;

import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.entity.MedicalRiskLimitLevel;
import org.javaguru.travel.insurance.domain.model.entity.Risk;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;
import org.javaguru.travel.insurance.domain.model.valueobject.RiskCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port для получения справочных данных
 * Определяет интерфейс, который должна реализовать инфраструктура (адаптеры)
 */
public interface ReferenceDataPort {
    
    /**
     * Находит страну по ISO коду на указанную дату
     * 
     * @param code ISO код страны
     * @param date дата, на которую нужна страна
     * @return Optional с Country если найдена и активна
     */
    Optional<Country> findCountry(CountryCode code, LocalDate date);
    
    /**
     * Находит риск по коду на указанную дату
     * 
     * @param code код риска
     * @param date дата, на которую нужен риск
     * @return Optional с Risk если найден и активен
     */
    Optional<Risk> findRisk(RiskCode code, LocalDate date);
    
    /**
     * Находит уровень медицинского покрытия по коду на указанную дату
     * 
     * @param code код уровня
     * @param date дата, на которую нужен уровень
     * @return Optional с MedicalRiskLimitLevel если найден и активен
     */
    Optional<MedicalRiskLimitLevel> findMedicalLevel(String code, LocalDate date);
    
    /**
     * Находит все риски по списку кодов на указанную дату
     * 
     * @param codes список кодов рисков
     * @param date дата, на которую нужны риски
     * @return список найденных и активных рисков
     */
    List<Risk> findRisks(List<RiskCode> codes, LocalDate date);
    
    /**
     * Получает все активные риски на указанную дату
     * 
     * @param date дата
     * @return список всех активных рисков
     */
    List<Risk> findAllActiveRisks(LocalDate date);
}
