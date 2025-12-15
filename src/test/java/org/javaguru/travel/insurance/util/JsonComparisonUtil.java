package org.javaguru.travel.insurance.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Утилита для сравнения JSON строк без учёта порядка элементов
 *
 * Особенности:
 * - Сравнивает содержимое JSON независимо от порядка полей в объектах
 * - Сравнивает массивы с учётом порядка элементов (по умолчанию)
 * - Поддерживает опцию игнорирования порядка в массивах
 * - Игнорирует различия в форматировании (пробелы, переводы строк)
 * - Предоставляет детальную информацию о различиях
 */
@Slf4j
public class JsonComparisonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Сравнивает две JSON строки без учёта порядка полей
     * Массивы сравниваются с учётом порядка элементов
     *
     * @param json1 первая JSON строка
     * @param json2 вторая JSON строка
     * @return true если JSON эквивалентны
     */
    public static boolean areJsonEqual(String json1, String json2) {
        return areJsonEqual(json1, json2, false);
    }

    /**
     * Сравнивает две JSON строки с опциональным игнорированием порядка в массивах
     *
     * @param json1 первая JSON строка
     * @param json2 вторая JSON строка
     * @param ignoreArrayOrder если true, порядок элементов в массивах игнорируется
     * @return true если JSON эквивалентны
     */
    public static boolean areJsonEqual(String json1, String json2, boolean ignoreArrayOrder) {
        try {
            if (json1 == null && json2 == null) {
                return true;
            }
            if (json1 == null || json2 == null) {
                return false;
            }

            JsonNode tree1 = objectMapper.readTree(json1);
            JsonNode tree2 = objectMapper.readTree(json2);

            return compareJsonNodes(tree1, tree2, ignoreArrayOrder);

        } catch (Exception e) {
            log.error("Error comparing JSON strings", e);
            return false;
        }
    }

    /**
     * Сравнивает два JsonNode
     *
     * @param node1 первый узел
     * @param node2 второй узел
     * @param ignoreArrayOrder игнорировать порядок в массивах
     * @return true если узлы эквивалентны
     */
    private static boolean compareJsonNodes(JsonNode node1, JsonNode node2, boolean ignoreArrayOrder) {
        // Проверка на null
        if (node1 == null && node2 == null) {
            return true;
        }
        if (node1 == null || node2 == null) {
            return false;
        }

        // Проверка типа узла
        if (node1.getNodeType() != node2.getNodeType()) {
            return false;
        }

        // Сравнение в зависимости от типа
        if (node1.isObject()) {
            return compareObjects(node1, node2, ignoreArrayOrder);
        } else if (node1.isArray()) {
            return compareArrays(node1, node2, ignoreArrayOrder);
        } else if (node1.isNumber() && node2.isNumber()) {
            return node1.decimalValue()
                    .compareTo(node2.decimalValue()) == 0;
        }
        else {
            // Примитивные типы (string, boolean, null)
            return node1.equals(node2);
        }
    }

    /**
     * Сравнивает два JSON объекта
     */
    private static boolean compareObjects(JsonNode obj1, JsonNode obj2, boolean ignoreArrayOrder) {
        // Проверка количества полей
        if (obj1.size() != obj2.size()) {
            return false;
        }

        // Получаем все имена полей из первого объекта
        Iterator<String> fieldNames = obj1.fieldNames();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();

            // Проверяем наличие поля во втором объекте
            if (!obj2.has(fieldName)) {
                return false;
            }

            // Рекурсивно сравниваем значения полей
            JsonNode value1 = obj1.get(fieldName);
            JsonNode value2 = obj2.get(fieldName);

            if (!compareJsonNodes(value1, value2, ignoreArrayOrder)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Сравнивает два JSON массива
     */
    private static boolean compareArrays(JsonNode arr1, JsonNode arr2, boolean ignoreArrayOrder) {
        // Проверка размера
        if (arr1.size() != arr2.size()) {
            return false;
        }

        if (ignoreArrayOrder) {
            // Сравнение без учёта порядка
            return compareArraysUnordered(arr1, arr2);
        } else {
            // Сравнение с учётом порядка
            for (int i = 0; i < arr1.size(); i++) {
                if (!compareJsonNodes(arr1.get(i), arr2.get(i), false)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Сравнивает массивы без учёта порядка элементов
     */
    private static boolean compareArraysUnordered(JsonNode arr1, JsonNode arr2) {
        List<JsonNode> list1 = new ArrayList<>();
        List<JsonNode> list2 = new ArrayList<>();

        arr1.forEach(list1::add);
        arr2.forEach(list2::add);

        // Для каждого элемента из первого массива ищем соответствие во втором
        List<JsonNode> remaining = new ArrayList<>(list2);

        for (JsonNode node1 : list1) {
            boolean found = false;

            for (int i = 0; i < remaining.size(); i++) {
                if (compareJsonNodes(node1, remaining.get(i), true)) {
                    remaining.remove(i);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return remaining.isEmpty();
    }

    /**
     * Возвращает детальное описание различий между двумя JSON строками
     *
     * @param json1 первая JSON строка
     * @param json2 вторая JSON строка
     * @return список различий
     */
    public static List<String> findDifferences(String json1, String json2) {
        return findDifferences(json1, json2, false);
    }

    /**
     * Возвращает детальное описание различий между двумя JSON строками
     *
     * @param json1 первая JSON строка
     * @param json2 вторая JSON строка
     * @param ignoreArrayOrder игнорировать порядок в массивах
     * @return список различий
     */
    public static List<String> findDifferences(String json1, String json2, boolean ignoreArrayOrder) {
        List<String> differences = new ArrayList<>();

        try {
            if (json1 == null && json2 == null) {
                return differences;
            }
            if (json1 == null) {
                differences.add("First JSON is null, second is not");
                return differences;
            }
            if (json2 == null) {
                differences.add("Second JSON is null, first is not");
                return differences;
            }

            JsonNode tree1 = objectMapper.readTree(json1);
            JsonNode tree2 = objectMapper.readTree(json2);

            findNodeDifferences(tree1, tree2, "", differences, ignoreArrayOrder);

        } catch (Exception e) {
            differences.add("Error parsing JSON: " + e.getMessage());
        }

        return differences;
    }

    /**
     * Рекурсивно находит различия между узлами
     */
    private static void findNodeDifferences(
            JsonNode node1,
            JsonNode node2,
            String path,
            List<String> differences,
            boolean ignoreArrayOrder) {

        if (node1 == null && node2 == null) {
            return;
        }

        if (node1 == null) {
            differences.add(path + ": present in second JSON but missing in first");
            return;
        }

        if (node2 == null) {
            differences.add(path + ": present in first JSON but missing in second");
            return;
        }

        if (node1.getNodeType() != node2.getNodeType()) {
            differences.add(path + ": different types - " + node1.getNodeType() + " vs " + node2.getNodeType());
            return;
        }

        if (node1.isObject()) {
            findObjectDifferences(node1, node2, path, differences, ignoreArrayOrder);
        } else if (node1.isArray()) {
            findArrayDifferences(node1, node2, path, differences, ignoreArrayOrder);
        } else {
            if (!node1.equals(node2)) {
                differences.add(path + ": different values - '" + node1.asText() + "' vs '" + node2.asText() + "'");
            }
        }
    }

    /**
     * Находит различия в объектах
     */
    private static void findObjectDifferences(
            JsonNode obj1,
            JsonNode obj2,
            String path,
            List<String> differences,
            boolean ignoreArrayOrder) {

        Set<String> allFields = new HashSet<>();
        obj1.fieldNames().forEachRemaining(allFields::add);
        obj2.fieldNames().forEachRemaining(allFields::add);

        for (String fieldName : allFields) {
            String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;

            JsonNode value1 = obj1.get(fieldName);
            JsonNode value2 = obj2.get(fieldName);

            if (value1 == null) {
                differences.add(fieldPath + ": present in second JSON but missing in first");
            } else if (value2 == null) {
                differences.add(fieldPath + ": present in first JSON but missing in second");
            } else {
                findNodeDifferences(value1, value2, fieldPath, differences, ignoreArrayOrder);
            }
        }
    }

    /**
     * Находит различия в массивах
     */
    private static void findArrayDifferences(
            JsonNode arr1,
            JsonNode arr2,
            String path,
            List<String> differences,
            boolean ignoreArrayOrder) {

        if (arr1.size() != arr2.size()) {
            differences.add(path + ": different array sizes - " + arr1.size() + " vs " + arr2.size());
        }

        if (!ignoreArrayOrder) {
            int minSize = Math.min(arr1.size(), arr2.size());
            for (int i = 0; i < minSize; i++) {
                findNodeDifferences(arr1.get(i), arr2.get(i), path + "[" + i + "]", differences, false);
            }
        } else {
            differences.add(path + ": array order comparison not implemented in detailed mode");
        }
    }

    /**
     * Нормализует JSON строку (убирает лишние пробелы и форматирование)
     *
     * @param json исходная JSON строка
     * @return нормализованная JSON строка
     */
    public static String normalizeJson(String json) {
        try {
            if (json == null) {
                return null;
            }
            JsonNode tree = objectMapper.readTree(json);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            log.error("Error normalizing JSON", e);
            return json;
        }
    }

    /**
     * Форматирует JSON строку для читаемого вывода
     *
     * @param json исходная JSON строка
     * @return отформатированная JSON строка
     */
    public static String prettyPrintJson(String json) {
        try {
            if (json == null) {
                return null;
            }
            JsonNode tree = objectMapper.readTree(json);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        } catch (Exception e) {
            log.error("Error pretty printing JSON", e);
            return json;
        }
    }
}