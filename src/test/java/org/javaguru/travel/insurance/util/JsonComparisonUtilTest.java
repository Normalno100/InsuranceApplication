package org.javaguru.travel.insurance.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonComparisonUtil
 */
class JsonComparisonUtilTest {

    // ========== БАЗОВЫЕ ТЕСТЫ ==========

    @Test
    @DisplayName("Два идентичных простых JSON объекта равны")
    void testIdenticalSimpleObjects() {
        String json1 = "{\"name\":\"John\",\"age\":30}";
        String json2 = "{\"name\":\"John\",\"age\":30}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("JSON объекты с полями в разном порядке равны")
    void testObjectsWithDifferentFieldOrder() {
        String json1 = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
        String json2 = "{\"city\":\"New York\",\"name\":\"John\",\"age\":30}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("JSON объекты с разными значениями не равны")
    void testObjectsWithDifferentValues() {
        String json1 = "{\"name\":\"John\",\"age\":30}";
        String json2 = "{\"name\":\"John\",\"age\":31}";

        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("JSON объекты с разным количеством полей не равны")
    void testObjectsWithDifferentFieldCount() {
        String json1 = "{\"name\":\"John\",\"age\":30}";
        String json2 = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";

        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    // ========== ТЕСТЫ С МАССИВАМИ ==========

    @Test
    @DisplayName("Массивы с одинаковым порядком элементов равны")
    void testArraysWithSameOrder() {
        String json1 = "{\"items\":[1,2,3]}";
        String json2 = "{\"items\":[1,2,3]}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("Массивы с разным порядком элементов не равны (по умолчанию)")
    void testArraysWithDifferentOrderNotEqualByDefault() {
        String json1 = "{\"items\":[1,2,3]}";
        String json2 = "{\"items\":[3,2,1]}";

        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("Массивы с разным порядком элементов равны при ignoreArrayOrder=true")
    void testArraysWithDifferentOrderEqualWhenIgnored() {
        String json1 = "{\"items\":[1,2,3]}";
        String json2 = "{\"items\":[3,2,1]}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2, true));
    }

    @Test
    @DisplayName("Массивы объектов с разным порядком равны при ignoreArrayOrder=true")
    void testArraysOfObjectsWithDifferentOrder() {
        String json1 = "{\"users\":[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]}";
        String json2 = "{\"users\":[{\"name\":\"Jane\",\"age\":25},{\"name\":\"John\",\"age\":30}]}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2, true));
    }

    @Test
    @DisplayName("Массивы с разным количеством элементов не равны")
    void testArraysWithDifferentSizes() {
        String json1 = "{\"items\":[1,2,3]}";
        String json2 = "{\"items\":[1,2,3,4]}";

        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json2));
        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json2, true));
    }

    // ========== ТЕСТЫ С ВЛОЖЕННЫМИ СТРУКТУРАМИ ==========

    @Test
    @DisplayName("Вложенные объекты с разным порядком полей равны")
    void testNestedObjectsWithDifferentFieldOrder() {
        String json1 = "{\"user\":{\"name\":\"John\",\"address\":{\"city\":\"NY\",\"zip\":\"10001\"}}}";
        String json2 = "{\"user\":{\"address\":{\"zip\":\"10001\",\"city\":\"NY\"},\"name\":\"John\"}}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("Сложная вложенная структура сравнивается корректно")
    void testComplexNestedStructure() {
        String json1 = """
            {
                "person": {
                    "firstName": "John",
                    "lastName": "Doe",
                    "age": 30
                },
                "country": "ES",
                "risks": ["SPORT_ACTIVITIES", "LUGGAGE_LOSS"]
            }
            """;

        String json2 = """
            {
                "risks": ["SPORT_ACTIVITIES", "LUGGAGE_LOSS"],
                "person": {
                    "age": 30,
                    "lastName": "Doe",
                    "firstName": "John"
                },
                "country": "ES"
            }
            """;

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    // ========== ТЕСТЫ С ПРИМИТИВНЫМИ ТИПАМИ ==========

    @Test
    @DisplayName("Строковые значения сравниваются корректно")
    void testStringValues() {
        String json1 = "{\"name\":\"John Doe\"}";
        String json2 = "{\"name\":\"John Doe\"}";
        String json3 = "{\"name\":\"Jane Doe\"}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json3));
    }

    @Test
    @DisplayName("Числовые значения сравниваются корректно")
    void testNumericValues() {
        String json1 = "{\"age\":30,\"price\":99.99}";
        String json2 = "{\"price\":99.99,\"age\":30}";
        String json3 = "{\"age\":30,\"price\":100.00}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json3));
    }

    @Test
    @DisplayName("Boolean значения сравниваются корректно")
    void testBooleanValues() {
        String json1 = "{\"active\":true,\"verified\":false}";
        String json2 = "{\"verified\":false,\"active\":true}";
        String json3 = "{\"active\":false,\"verified\":false}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json3));
    }

    @Test
    @DisplayName("Null значения сравниваются корректно")
    void testNullValues() {
        String json1 = "{\"name\":\"John\",\"middleName\":null}";
        String json2 = "{\"middleName\":null,\"name\":\"John\"}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    // ========== ТЕСТЫ С NULL И ПУСТЫМИ ЗНАЧЕНИЯМИ ==========

    @Test
    @DisplayName("Оба JSON null - равны")
    void testBothJsonNull() {
        assertTrue(JsonComparisonUtil.areJsonEqual(null, null));
    }

    @Test
    @DisplayName("Один JSON null - не равны")
    void testOneJsonNull() {
        String json = "{\"name\":\"John\"}";

        assertFalse(JsonComparisonUtil.areJsonEqual(json, null));
        assertFalse(JsonComparisonUtil.areJsonEqual(null, json));
    }

    @Test
    @DisplayName("Пустые объекты равны")
    void testEmptyObjects() {
        String json1 = "{}";
        String json2 = "{}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("Пустые массивы равны")
    void testEmptyArrays() {
        String json1 = "{\"items\":[]}";
        String json2 = "{\"items\":[]}";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    // ========== ТЕСТЫ С ПРОБЕЛАМИ И ФОРМАТИРОВАНИЕМ ==========

    @Test
    @DisplayName("JSON с разным форматированием равны")
    void testDifferentFormatting() {
        String json1 = "{\"name\":\"John\",\"age\":30}";
        String json2 = """
            {
              "name": "John",
              "age": 30
            }
            """;

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("JSON с лишними пробелами равны")
    void testExtraWhitespace() {
        String json1 = "{\"name\":\"John\",\"age\":30}";
        String json2 = "{  \"name\"  :  \"John\"  ,  \"age\"  :  30  }";

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    // ========== ТЕСТЫ findDifferences ==========

    @Test
    @DisplayName("findDifferences возвращает пустой список для равных JSON")
    void testFindDifferencesForEqualJson() {
        String json1 = "{\"name\":\"John\",\"age\":30}";
        String json2 = "{\"age\":30,\"name\":\"John\"}";

        List<String> differences = JsonComparisonUtil.findDifferences(json1, json2);

        assertTrue(differences.isEmpty());
    }

    @Test
    @DisplayName("findDifferences находит отличия в значениях")
    void testFindDifferencesInValues() {
        String json1 = "{\"name\":\"John\",\"age\":30}";
        String json2 = "{\"name\":\"John\",\"age\":31}";

        List<String> differences = JsonComparisonUtil.findDifferences(json1, json2);

        assertEquals(1, differences.size());
        assertTrue(differences.get(0).contains("age"));
        assertTrue(differences.get(0).contains("30"));
        assertTrue(differences.get(0).contains("31"));
    }

    @Test
    @DisplayName("findDifferences находит отсутствующие поля")
    void testFindDifferencesMissingFields() {
        String json1 = "{\"name\":\"John\",\"age\":30}";
        String json2 = "{\"name\":\"John\"}";

        List<String> differences = JsonComparisonUtil.findDifferences(json1, json2);

        assertEquals(1, differences.size());
        assertTrue(differences.get(0).contains("age"));
    }

    @Test
    @DisplayName("findDifferences находит дополнительные поля")
    void testFindDifferencesExtraFields() {
        String json1 = "{\"name\":\"John\"}";
        String json2 = "{\"name\":\"John\",\"age\":30}";

        List<String> differences = JsonComparisonUtil.findDifferences(json1, json2);

        assertEquals(1, differences.size());
        assertTrue(differences.get(0).contains("age"));
    }

    @Test
    @DisplayName("findDifferences находит отличия в вложенных объектах")
    void testFindDifferencesInNestedObjects() {
        String json1 = "{\"user\":{\"name\":\"John\",\"age\":30}}";
        String json2 = "{\"user\":{\"name\":\"Jane\",\"age\":30}}";

        List<String> differences = JsonComparisonUtil.findDifferences(json1, json2);

        assertEquals(1, differences.size());
        assertTrue(differences.get(0).contains("user.name"));
    }

    @Test
    @DisplayName("findDifferences находит отличия в размере массивов")
    void testFindDifferencesInArraySizes() {
        String json1 = "{\"items\":[1,2,3]}";
        String json2 = "{\"items\":[1,2]}";

        List<String> differences = JsonComparisonUtil.findDifferences(json1, json2);

        assertFalse(differences.isEmpty());
        assertTrue(differences.stream().anyMatch(d -> d.contains("array sizes")));
    }

    // ========== ТЕСТЫ normalizeJson ==========

    @Test
    @DisplayName("normalizeJson убирает лишнее форматирование")
    void testNormalizeJson() {
        String json = """
            {
              "name": "John",
              "age": 30
            }
            """;

        String normalized = JsonComparisonUtil.normalizeJson(json);

        assertNotNull(normalized);
        assertFalse(normalized.contains("\n"));
        assertTrue(normalized.contains("name"));
        assertTrue(normalized.contains("John"));
    }

    @Test
    @DisplayName("normalizeJson обрабатывает null")
    void testNormalizeJsonWithNull() {
        String normalized = JsonComparisonUtil.normalizeJson(null);
        assertNull(normalized);
    }

    // ========== ТЕСТЫ prettyPrintJson ==========

    @Test
    @DisplayName("prettyPrintJson форматирует JSON")
    void testPrettyPrintJson() {
        String json = "{\"name\":\"John\",\"age\":30}";

        String pretty = JsonComparisonUtil.prettyPrintJson(json);

        assertNotNull(pretty);
        assertTrue(pretty.contains("\n"));
        assertTrue(pretty.length() > json.length());
    }

    @Test
    @DisplayName("prettyPrintJson обрабатывает null")
    void testPrettyPrintJsonWithNull() {
        String pretty = JsonComparisonUtil.prettyPrintJson(null);
        assertNull(pretty);
    }

    // ========== ТЕСТЫ С РЕАЛЬНЫМИ ДАННЫМИ ИЗ ПРИЛОЖЕНИЯ ==========

    @Test
    @DisplayName("Сравнение реального response из TravelCalculatePremiumResponseV2")
    void testRealTravelInsuranceResponse() {
        String json1 = """
            {
                "personFirstName": "John",
                "personLastName": "Doe",
                "agreementDateFrom": "2025-01-15",
                "agreementDateTo": "2025-01-25",
                "countryIsoCode": "ES",
                "medicalRiskLimitLevel": "50000",
                "selectedRisks": ["SPORT_ACTIVITIES", "LUGGAGE_LOSS"],
                "agreementPrice": 125.50,
                "currency": "EUR"
            }
            """;

        String json2 = """
            {
                "currency": "EUR",
                "agreementPrice": 125.50,
                "selectedRisks": ["SPORT_ACTIVITIES", "LUGGAGE_LOSS"],
                "medicalRiskLimitLevel": "50000",
                "countryIsoCode": "ES",
                "agreementDateTo": "2025-01-25",
                "agreementDateFrom": "2025-01-15",
                "personLastName": "Doe",
                "personFirstName": "John"
            }
            """;

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("Сравнение response с calculation details")
    void testResponseWithCalculationDetails() {
        String json1 = """
            {
                "personFirstName": "John",
                "calculation": {
                    "baseRate": 4.50,
                    "ageCoefficient": 1.0,
                    "countryCoefficient": 1.0,
                    "totalCoefficient": 1.0
                },
                "agreementPrice": 45.00
            }
            """;

        String json2 = """
            {
                "agreementPrice": 45.00,
                "personFirstName": "John",
                "calculation": {
                    "totalCoefficient": 1.0,
                    "countryCoefficient": 1.0,
                    "ageCoefficient": 1.0,
                    "baseRate": 4.50
                }
            }
            """;

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    // ========== ТЕСТЫ С НЕКОРРЕКТНЫМ JSON ==========

    @Test
    @DisplayName("Некорректный JSON возвращает false")
    void testInvalidJson() {
        String json1 = "{invalid json}";
        String json2 = "{\"name\":\"John\"}";

        assertFalse(JsonComparisonUtil.areJsonEqual(json1, json2));
    }

    @Test
    @DisplayName("findDifferences с некорректным JSON возвращает ошибку")
    void testFindDifferencesWithInvalidJson() {
        String json1 = "{invalid}";
        String json2 = "{\"name\":\"John\"}";

        List<String> differences = JsonComparisonUtil.findDifferences(json1, json2);

        assertFalse(differences.isEmpty());
        assertTrue(differences.stream().anyMatch(d -> d.contains("Error")));
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    @DisplayName("Сравнение больших JSON объектов работает корректно")
    void testLargeJsonComparison() {
        StringBuilder json1Builder = new StringBuilder("{\"items\":[");
        StringBuilder json2Builder = new StringBuilder("{\"items\":[");

        for (int i = 0; i < 100; i++) {
            String item = String.format("{\"id\":%d,\"name\":\"Item%d\"}", i, i);
            if (i > 0) {
                json1Builder.append(",");
                json2Builder.append(",");
            }
            json1Builder.append(item);
            json2Builder.append(item);
        }

        json1Builder.append("]}");
        json2Builder.append("]}");

        String json1 = json1Builder.toString();
        String json2 = json2Builder.toString();

        assertTrue(JsonComparisonUtil.areJsonEqual(json1, json2));
    }
}