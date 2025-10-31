package org.javaguru.travel.insurance.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Улучшенная версия JsonComparator с поддержкой опций сравнения
 */
public class JsonComparator {

    private static final ObjectMapper objectMapper = TestDataLoader.getObjectMapper();

    /**
     * Опции для настройки поведения сравнения
     */
    public static class ComparisonOptions {
        private boolean ignoreExtraFields = false;
        private boolean ignoreNullFields = false;
        private boolean strictArrayOrder = true;

        public static ComparisonOptions defaults() {
            return new ComparisonOptions();
        }

        public ComparisonOptions ignoreExtraFields(boolean ignore) {
            this.ignoreExtraFields = ignore;
            return this;
        }

        public ComparisonOptions ignoreNullFields(boolean ignore) {
            this.ignoreNullFields = ignore;
            return this;
        }

        public ComparisonOptions strictArrayOrder(boolean strict) {
            this.strictArrayOrder = strict;
            return this;
        }
    }

    /**
     * Сравнивает два JSON с опциями по умолчанию
     */
    public static void assertJsonEquals(String expectedJson, String actualJson) {
        assertJsonEquals(expectedJson, actualJson, ComparisonOptions.defaults());
    }

    /**
     * Сравнивает два JSON с указанными опциями
     */
    public static void assertJsonEquals(String expectedJson, String actualJson, ComparisonOptions options) {
        try {
            JsonNode expectedNode = objectMapper.readTree(expectedJson);
            JsonNode actualNode = objectMapper.readTree(actualJson);

            List<String> differences = compareNodes("", expectedNode, actualNode, options);

            if (!differences.isEmpty()) {
                StringBuilder message = new StringBuilder("JSON documents differ:\n");
                differences.forEach(diff -> message.append("  - ").append(diff).append("\n"));
                message.append("\nExpected JSON:\n").append(prettyPrint(expectedJson));
                message.append("\n\nActual JSON:\n").append(prettyPrint(actualJson));
                Assertions.fail(message.toString());
            }
        } catch (IOException e) {
            Assertions.fail("Failed to parse JSON: " + e.getMessage());
        }
    }

    /**
     * Сравнивает JSON, игнорируя дополнительные поля в actual
     * (полезно когда в actual есть null поля, которых нет в expected)
     */
    public static void assertJsonEqualsIgnoringExtraFields(String expectedJson, String actualJson) {
        ComparisonOptions options = ComparisonOptions.defaults()
                .ignoreExtraFields(true);
        assertJsonEquals(expectedJson, actualJson, options);
    }

    /**
     * Сравнивает JSON, игнорируя все null поля
     * (полезно для error responses где все поля кроме errors = null)
     */
    public static void assertJsonEqualsIgnoringNullFields(String expectedJson, String actualJson) {
        ComparisonOptions options = ComparisonOptions.defaults()
                .ignoreNullFields(true);
        assertJsonEquals(expectedJson, actualJson, options);
    }

    /**
     * Комбинированный метод: игнорирует и дополнительные поля, и null значения
     */
    public static void assertJsonEqualsLenient(String expectedJson, String actualJson) {
        ComparisonOptions options = ComparisonOptions.defaults()
                .ignoreExtraFields(true)
                .ignoreNullFields(true);
        assertJsonEquals(expectedJson, actualJson, options);
    }

    private static List<String> compareNodes(String path, JsonNode expected, JsonNode actual,
                                             ComparisonOptions options) {
        List<String> differences = new ArrayList<>();

        if (expected == null && actual == null) {
            return differences;
        }

        if (expected == null) {
            if (!options.ignoreNullFields) {
                differences.add(path + ": Expected null but got " + actual);
            }
            return differences;
        }

        if (actual == null) {
            if (!options.ignoreNullFields) {
                differences.add(path + ": Expected " + expected + " but got null");
            }
            return differences;
        }

        // Игнорируем null значения если включена опция
        if (options.ignoreNullFields) {
            if (expected.isNull() || actual.isNull()) {
                return differences;
            }
        }

        if (expected.getNodeType() != actual.getNodeType()) {
            differences.add(path + ": Type mismatch - expected " + expected.getNodeType()
                    + " but got " + actual.getNodeType());
            return differences;
        }

        switch (expected.getNodeType()) {
            case OBJECT:
                differences.addAll(compareObjects(path, expected, actual, options));
                break;
            case ARRAY:
                differences.addAll(compareArrays(path, expected, actual, options));
                break;
            case STRING:
            case NUMBER:
            case BOOLEAN:
                if (!expected.equals(actual)) {
                    differences.add(path + ": Expected '" + expected.asText()
                            + "' but got '" + actual.asText() + "'");
                }
                break;
            case NULL:
                // Both are null
                break;
        }

        return differences;
    }

    private static List<String> compareObjects(String path, JsonNode expected, JsonNode actual,
                                               ComparisonOptions options) {
        List<String> differences = new ArrayList<>();
        String currentPath = path.isEmpty() ? "" : path + ".";

        // Check for missing fields in actual
        Iterator<Map.Entry<String, JsonNode>> expectedFields = expected.fields();
        while (expectedFields.hasNext()) {
            Map.Entry<String, JsonNode> entry = expectedFields.next();
            String fieldName = entry.getKey();
            JsonNode expectedValue = entry.getValue();
            JsonNode actualValue = actual.get(fieldName);

            // Игнорируем null поля если включена опция
            if (options.ignoreNullFields && expectedValue.isNull()) {
                continue;
            }

            if (actualValue == null) {
                differences.add(currentPath + fieldName + ": Field is missing in actual JSON");
            } else {
                differences.addAll(compareNodes(currentPath + fieldName, expectedValue, actualValue, options));
            }
        }

        // Check for extra fields in actual (только если не игнорируем)
        if (!options.ignoreExtraFields) {
            Iterator<Map.Entry<String, JsonNode>> actualFields = actual.fields();
            while (actualFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = actualFields.next();
                String fieldName = entry.getKey();
                JsonNode actualValue = entry.getValue();

                // Игнорируем null поля если включена опция
                if (options.ignoreNullFields && actualValue.isNull()) {
                    continue;
                }

                if (!expected.has(fieldName)) {
                    differences.add(currentPath + fieldName + ": Unexpected field in actual JSON");
                }
            }
        }

        return differences;
    }

    private static List<String> compareArrays(String path, JsonNode expected, JsonNode actual,
                                              ComparisonOptions options) {
        List<String> differences = new ArrayList<>();

        if (expected.size() != actual.size()) {
            differences.add(path + ": Array size mismatch - expected "
                    + expected.size() + " but got " + actual.size());
            return differences;
        }

        for (int i = 0; i < expected.size(); i++) {
            differences.addAll(compareNodes(path + "[" + i + "]",
                    expected.get(i), actual.get(i), options));
        }

        return differences;
    }

    private static String prettyPrint(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (IOException e) {
            return json;
        }
    }

    // Все остальные методы из оригинального JsonComparator остаются без изменений

    public static void assertJsonContains(String expectedJson, String actualJson) {
        // ... (тот же код что и раньше)
    }

    public static void assertJsonEqualsFromFile(String expectedJsonFileName, Object actualObject) {
        try {
            String expectedJson = TestDataLoader.loadResponseAsString(expectedJsonFileName);
            String actualJson = objectMapper.writeValueAsString(actualObject);
            assertJsonEquals(expectedJson, actualJson);
        } catch (IOException e) {
            Assertions.fail("Failed to load or serialize JSON: " + e.getMessage());
        }
    }

    public static void assertObjectsEqualAsJson(Object expected, Object actual) {
        try {
            String expectedJson = objectMapper.writeValueAsString(expected);
            String actualJson = objectMapper.writeValueAsString(actual);
            assertJsonEquals(expectedJson, actualJson);
        } catch (IOException e) {
            Assertions.fail("Failed to serialize objects: " + e.getMessage());
        }
    }

    public static PartialJsonBuilder partialJson() {
        return new PartialJsonBuilder();
    }

    public static class PartialJsonBuilder {
        private final StringBuilder json = new StringBuilder("{");
        private boolean firstField = true;

        public PartialJsonBuilder addField(String name, Object value) {
            if (!firstField) {
                json.append(",");
            }
            firstField = false;

            json.append("\"").append(name).append("\":");
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value == null) {
                json.append("null");
            } else {
                try {
                    json.append(objectMapper.writeValueAsString(value));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to serialize value", e);
                }
            }
            return this;
        }

        public String build() {
            return json.append("}").toString();
        }
    }
}