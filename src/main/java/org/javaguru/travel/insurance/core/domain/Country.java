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
    // Низкий риск (1.0)
    SPAIN("ES", "Spain", "Испания", new BigDecimal("1.0")),
    GERMANY("DE", "Germany", "Германия", new BigDecimal("1.0")),
    FRANCE("FR", "France", "Франция", new BigDecimal("1.0")),
    ITALY("IT", "Italy", "Италия", new BigDecimal("1.0")),
    AUSTRIA("AT", "Austria", "Австрия", new BigDecimal("1.0")),
    NETHERLANDS("NL", "Netherlands", "Нидерланды", new BigDecimal("1.0")),
    BELGIUM("BE", "Belgium", "Бельгия", new BigDecimal("1.0")),
    SWITZERLAND("CH", "Switzerland", "Швейцария", new BigDecimal("1.0")),
    SWEDEN("SE", "Sweden", "Швеция", new BigDecimal("1.0")),
    NORWAY("NO", "Norway", "Норвегия", new BigDecimal("1.0")),
    DENMARK("DK", "Denmark", "Дания", new BigDecimal("1.0")),
    JAPAN("JP", "Japan", "Япония", new BigDecimal("1.0")),
    SOUTH_KOREA("KR", "South Korea", "Южная Корея", new BigDecimal("1.0")),
    AUSTRALIA("AU", "Australia", "Австралия", new BigDecimal("1.0")),
    NEW_ZEALAND("NZ", "New Zealand", "Новая Зеландия", new BigDecimal("1.0")),
    CANADA("CA", "Canada", "Канада", new BigDecimal("1.0")),

    // Средний риск (1.3)
    THAILAND("TH", "Thailand", "Таиланд", new BigDecimal("1.3")),
    VIETNAM("VN", "Vietnam", "Вьетнам", new BigDecimal("1.3")),
    TURKEY("TR", "Turkey", "Турция", new BigDecimal("1.3")),
    UAE("AE", "United Arab Emirates", "ОАЭ", new BigDecimal("1.3")),
    CHINA("CN", "China", "Китай", new BigDecimal("1.3")),
    MEXICO("MX", "Mexico", "Мексика", new BigDecimal("1.3")),
    BRAZIL("BR", "Brazil", "Бразилия", new BigDecimal("1.3")),
    MALAYSIA("MY", "Malaysia", "Малайзия", new BigDecimal("1.3")),
    INDONESIA("ID", "Indonesia", "Индонезия", new BigDecimal("1.3")),
    PHILIPPINES("PH", "Philippines", "Филиппины", new BigDecimal("1.3")),
    USA("US", "United States", "США", new BigDecimal("1.3")),

    // Высокий риск (1.8)
    INDIA("IN", "India", "Индия", new BigDecimal("1.8")),
    EGYPT("EG", "Egypt", "Египет", new BigDecimal("1.8")),
    KENYA("KE", "Kenya", "Кения", new BigDecimal("1.8")),
    SOUTH_AFRICA("ZA", "South Africa", "ЮАР", new BigDecimal("1.8")),
    MOROCCO("MA", "Morocco", "Марокко", new BigDecimal("1.8")),
    TUNISIA("TN", "Tunisia", "Тунис", new BigDecimal("1.8")),
    ARGENTINA("AR", "Argentina", "Аргентина", new BigDecimal("1.8")),
    COLOMBIA("CO", "Colombia", "Колумбия", new BigDecimal("1.8")),
    PERU("PE", "Peru", "Перу", new BigDecimal("1.8")),

    // Очень высокий риск (2.5)
    AFGHANISTAN("AF", "Afghanistan", "Афганистан", new BigDecimal("2.5")),
    IRAQ("IQ", "Iraq", "Ирак", new BigDecimal("2.5")),
    SYRIA("SY", "Syria", "Сирия", new BigDecimal("2.5")),
    YEMEN("YE", "Yemen", "Йемен", new BigDecimal("2.5")),
    SOMALIA("SO", "Somalia", "Сомали", new BigDecimal("2.5"));

    private final String isoCode;
    private final String nameEn;
    private final String nameRu;
    private final BigDecimal riskCoefficient;

    public static Country fromIsoCode(String isoCode) {
        for (Country country : values()) {
            if (country.isoCode.equalsIgnoreCase(isoCode)) {
                return country;
            }
        }
        throw new IllegalArgumentException("Unknown country ISO code: " + isoCode);
    }
}