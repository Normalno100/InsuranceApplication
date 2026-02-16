package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CountryDefaultDayPremiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с дефолтными дневными премиями по странам
 * 
 * ОСНОВНЫЕ СЦЕНАРИИ ИСПОЛЬЗОВАНИЯ:
 * 1. Поиск активной премии для страны на дату
 * 2. Получение всех активных премий
 * 3. Получение истории изменений премий для страны
 * 4. Управление версионированием тарифов
 */
@Repository
public interface CountryDefaultDayPremiumRepository extends JpaRepository<CountryDefaultDayPremiumEntity, Long> {

    // ============================================
    // Основные запросы (Active records)
    // ============================================

    /**
     * Находит активную дефолтную премию для страны на указанную дату
     * 
     * ВАЖНО: Возвращает только одну запись - ту, которая действует на дату.
     * Учитывается temporal validity: valid_from <= date AND (valid_to IS NULL OR valid_to >= date)
     * 
     * @param countryIsoCode ISO код страны (например, "ES", "US")
     * @param date дата, на которую нужна премия
     * @return Optional с найденной премией или empty
     */
    @Query("SELECT c FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.countryIsoCode = :countryIsoCode " +
           "AND c.validFrom <= :date " +
           "AND (c.validTo IS NULL OR c.validTo >= :date)")
    Optional<CountryDefaultDayPremiumEntity> findActiveByCountryAndDate(
            @Param("countryIsoCode") String countryIsoCode,
            @Param("date") LocalDate date
    );

    /**
     * Находит активную дефолтную премию для страны на текущую дату
     * 
     * Convenience метод для частого случая использования.
     * 
     * @param countryIsoCode ISO код страны
     * @return Optional с найденной премией или empty
     */
    default Optional<CountryDefaultDayPremiumEntity> findActiveByCountry(String countryIsoCode) {
        return findActiveByCountryAndDate(countryIsoCode, LocalDate.now());
    }

    /**
     * Получает все активные дефолтные премии на указанную дату
     * 
     * Полезно для:
     * - Генерации отчетов
     * - Валидации данных
     * - Административных интерфейсов
     * 
     * @param date дата проверки
     * @return список всех активных премий
     */
    @Query("SELECT c FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.validFrom <= :date " +
           "AND (c.validTo IS NULL OR c.validTo >= :date) " +
           "ORDER BY c.countryIsoCode")
    List<CountryDefaultDayPremiumEntity> findAllActiveOnDate(@Param("date") LocalDate date);

    /**
     * Получает все активные дефолтные премии на текущую дату
     * 
     * @return список всех текущих премий
     */
    default List<CountryDefaultDayPremiumEntity> findAllActive() {
        return findAllActiveOnDate(LocalDate.now());
    }

    // ============================================
    // Запросы для версионирования и истории
    // ============================================

    /**
     * Получает всю историю изменений премий для страны
     * 
     * Возвращает все записи (текущие, прошлые, будущие) для страны.
     * Отсортировано по дате начала действия (от новых к старым).
     * 
     * @param countryIsoCode ISO код страны
     * @return список всех премий для страны
     */
    @Query("SELECT c FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.countryIsoCode = :countryIsoCode " +
           "ORDER BY c.validFrom DESC")
    List<CountryDefaultDayPremiumEntity> findAllByCountry(@Param("countryIsoCode") String countryIsoCode);

    /**
     * Получает текущую (активную сейчас) премию для страны
     * 
     * @param countryIsoCode ISO код страны
     * @return Optional с текущей премией или empty
     */
    @Query("SELECT c FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.countryIsoCode = :countryIsoCode " +
           "AND c.validFrom <= CURRENT_DATE " +
           "AND (c.validTo IS NULL OR c.validTo >= CURRENT_DATE)")
    Optional<CountryDefaultDayPremiumEntity> findCurrentByCountry(@Param("countryIsoCode") String countryIsoCode);

    /**
     * Получает будущие (еще не вступившие в силу) премии для страны
     * 
     * Полезно для планирования и административных задач.
     * 
     * @param countryIsoCode ISO код страны
     * @return список будущих премий
     */
    @Query("SELECT c FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.countryIsoCode = :countryIsoCode " +
           "AND c.validFrom > CURRENT_DATE " +
           "ORDER BY c.validFrom")
    List<CountryDefaultDayPremiumEntity> findFutureByCountry(@Param("countryIsoCode") String countryIsoCode);

    /**
     * Получает исторические (уже не действующие) премии для страны
     * 
     * @param countryIsoCode ISO код страны
     * @return список исторических премий
     */
    @Query("SELECT c FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.countryIsoCode = :countryIsoCode " +
           "AND c.validTo IS NOT NULL " +
           "AND c.validTo < CURRENT_DATE " +
           "ORDER BY c.validFrom DESC")
    List<CountryDefaultDayPremiumEntity> findHistoricalByCountry(@Param("countryIsoCode") String countryIsoCode);

    // ============================================
    // Проверки существования
    // ============================================

    /**
     * Проверяет наличие активной премии для страны
     * 
     * @param countryIsoCode ISO код страны
     * @return true если есть активная премия
     */
    default boolean hasActivePremium(String countryIsoCode) {
        return findActiveByCountry(countryIsoCode).isPresent();
    }

    /**
     * Проверяет наличие любой премии для страны (текущей, прошлой или будущей)
     * 
     * @param countryIsoCode ISO код страны
     * @return true если есть хотя бы одна запись
     */
    boolean existsByCountryIsoCode(String countryIsoCode);

    // ============================================
    // Запросы для валидации данных
    // ============================================

    /**
     * Проверяет наличие перекрывающихся периодов действия для страны
     * 
     * ВАЖНО: Этот запрос используется для валидации при создании/обновлении записей.
     * Нельзя создавать записи с пересекающимися периодами valid_from/valid_to.
     * 
     * @param countryIsoCode ISO код страны
     * @param validFrom начало периода
     * @param validTo конец периода (может быть NULL)
     * @param excludeId ID записи, которую нужно исключить из проверки (для UPDATE операций)
     * @return список конфликтующих записей
     */
    @Query("SELECT c FROM CountryDefaultDayPremiumEntity c\n" +
            "WHERE c.countryIsoCode = :countryIsoCode\n" +
            "AND (:excludeId IS NULL OR c.id <> :excludeId)\n" +
            "AND (\n" +
            "      (c.validFrom <= :validFrom AND (c.validTo IS NULL OR c.validTo >= :validFrom))\n" +
            "   OR (:validTo IS NULL OR \n" +
            "       (c.validFrom <= :validTo AND (c.validTo IS NULL OR c.validTo >= :validFrom))\n" +
            "      )\n" +
            ")\n")
    List<CountryDefaultDayPremiumEntity> findConflictingPeriods(
            @Param("countryIsoCode") String countryIsoCode,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo,
            @Param("excludeId") Long excludeId
    );

    // ============================================
    // Статистические запросы
    // ============================================

    /**
     * Подсчитывает количество стран с активными премиями
     * 
     * @return количество уникальных стран
     */
    @Query("SELECT COUNT(DISTINCT c.countryIsoCode) FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.validFrom <= CURRENT_DATE " +
           "AND (c.validTo IS NULL OR c.validTo >= CURRENT_DATE)")
    long countCountriesWithActivePremiums();

    /**
     * Получает минимальную активную премию
     * 
     * @return минимальная дневная премия среди всех активных
     */
    @Query("SELECT MIN(c.defaultDayPremium) FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.validFrom <= CURRENT_DATE " +
           "AND (c.validTo IS NULL OR c.validTo >= CURRENT_DATE)")
    Optional<java.math.BigDecimal> findMinActivePremium();

    /**
     * Получает максимальную активную премию
     * 
     * @return максимальная дневная премия среди всех активных
     */
    @Query("SELECT MAX(c.defaultDayPremium) FROM CountryDefaultDayPremiumEntity c " +
           "WHERE c.validFrom <= CURRENT_DATE " +
           "AND (c.validTo IS NULL OR c.validTo >= CURRENT_DATE)")
    Optional<java.math.BigDecimal> findMaxActivePremium();
}
