package org.javaguru.travel.insurance.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Справочник стран с коэффициентами риска
 */
@Getter
@RequiredArgsConstructor
public enum Country {

    // ========== ГРУППА 1: НИЗКИЙ РИСК (1.0) ==========
    SPAIN("ES", "Spain", "Испания", CountryRiskGroup.LOW),
    GERMANY("DE", "Germany", "Германия", CountryRiskGroup.LOW),
    FRANCE("FR", "France", "Франция", CountryRiskGroup.LOW),
    ITALY("IT", "Italy", "Италия", CountryRiskGroup.LOW),
    AUSTRIA("AT", "Austria", "Австрия", CountryRiskGroup.LOW),
    NETHERLANDS("NL", "Netherlands", "Нидерланды", CountryRiskGroup.LOW),
    BELGIUM("BE", "Belgium", "Бельгия", CountryRiskGroup.LOW),
    SWITZERLAND("CH", "Switzerland", "Швейцария", CountryRiskGroup.LOW),
    SWEDEN("SE", "Sweden", "Швеция", CountryRiskGroup.LOW),
    NORWAY("NO", "Norway", "Норвегия", CountryRiskGroup.LOW),
    DENMARK("DK", "Denmark", "Дания", CountryRiskGroup.LOW),
    JAPAN("JP", "Japan", "Япония", CountryRiskGroup.LOW),
    SOUTH_KOREA("KR", "South Korea", "Южная Корея", CountryRiskGroup.LOW),
    AUSTRALIA("AU", "Australia", "Австралия", CountryRiskGroup.LOW),
    NEW_ZEALAND("NZ", "New Zealand", "Новая Зеландия", CountryRiskGroup.LOW),
    CANADA("CA", "Canada", "Канада", CountryRiskGroup.LOW),

    // ========== ГРУППА 2: СРЕДНИЙ РИСК (1.3) ==========
    THAILAND("TH", "Thailand", "Таиланд", CountryRiskGroup.MEDIUM),
    VIETNAM("VN", "Vietnam", "Вьетнам", CountryRiskGroup.MEDIUM),
    TURKEY("TR", "Turkey", "Турция", CountryRiskGroup.MEDIUM),
    UAE("AE", "United Arab Emirates", "ОАЭ", CountryRiskGroup.MEDIUM),
    CHINA("CN", "China", "Китай", CountryRiskGroup.MEDIUM),
    MEXICO("MX", "Mexico", "Мексика", CountryRiskGroup.MEDIUM),
    BRAZIL("BR", "Brazil", "Бразилия", CountryRiskGroup.MEDIUM),
    MALAYSIA("MY", "Malaysia", "Малайзия", CountryRiskGroup.MEDIUM),
    INDONESIA("ID", "Indonesia", "Индонезия", CountryRiskGroup.MEDIUM),
    PHILIPPINES("PH", "Philippines", "Филиппины", CountryRiskGroup.MEDIUM),
    USA("US", "United States", "США", CountryRiskGroup.MEDIUM),

    // ========== ГРУППА 3: ВЫСОКИЙ РИСК (1.8) ==========
    INDIA("IN", "India", "Индия", CountryRiskGroup.HIGH),
    EGYPT("EG", "Egypt", "Египет", CountryRiskGroup.HIGH),
    KENYA("KE", "Kenya", "Кения", CountryRiskGroup.HIGH),
    SOUTH_AFRICA("ZA", "South Africa", "ЮАР", CountryRiskGroup.HIGH),
    MOROCCO("MA", "Morocco", "Марокко", CountryRiskGroup.HIGH),
    TUNISIA("TN", "Tunisia", "Тунис", CountryRiskGroup.HIGH),
    ARGENTINA("AR", "Argentina", "Аргентина", CountryRiskGroup.HIGH),
    COLOMBIA("CO", "Colombia", "Колумбия", CountryRiskGroup.HIGH),
    PERU("PE", "Peru", "Перу", CountryRiskGroup.HIGH),

    // ========== ГРУППА 4: ОЧЕНЬ ВЫСОКИЙ РИСК (2.5) ==========
    AFGHANISTAN("AF", "Afghanistan", "Афганистан", CountryRiskGroup.VERY_HIGH),
    IRAQ("IQ", "Iraq", "Ирак", CountryRiskGroup.VERY_HIGH),
    SYRIA("SY", "Syria", "Сирия", CountryRiskGroup.VERY_HIGH),
    YEMEN("YE", "Yemen", "Йемен", CountryRiskGroup.VERY_HIGH),
    SOMALIA("SO", "Somalia", "Сомали", CountryRiskGroup.VERY_HIGH);

    private final String isoCode;
    private final String nameEn;
    private final String nameRu;
    private final CountryRiskGroup riskGroup;

    public BigDecimal getRiskCoefficient() {
        return riskGroup.getCoefficient();
    }

    /**
     * Поиск страны по ISO коду
     */
    public static Country fromIsoCode(String isoCode) {
        for (Country country : values()) {
            if (country.isoCode.equalsIgnoreCase(isoCode)) {
                return country;
            }
        }
        throw new IllegalArgumentException("Unknown country ISO code: " + isoCode);
    }

    /**
     * Поиск страны по английскому названию
     */
    public static Country fromNameEn(String name) {
        for (Country country : values()) {
            if (country.nameEn.equalsIgnoreCase(name)) {
                return country;
            }
        }
        throw new IllegalArgumentException("Unknown country name: " + name);
    }
}

/**
 * Группы риска стран
 */
@Getter
@RequiredArgsConstructor
enum CountryRiskGroup {
    LOW(new BigDecimal("1.0")),
    MEDIUM(new BigDecimal("1.3")),
    HIGH(new BigDecimal("1.8")),
    VERY_HIGH(new BigDecimal("2.5"));

    private final BigDecimal coefficient;
}