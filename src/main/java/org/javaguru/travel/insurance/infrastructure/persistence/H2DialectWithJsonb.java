package org.javaguru.travel.insurance.infrastructure.persistence;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Расширенный H2 диалект с поддержкой типа jsonb.
 *
 * task_133: В H2 тип jsonb не существует нативно.
 * При ddl-auto=create-drop Hibernate пытается создать колонку с типом jsonb
 * (из @Column(columnDefinition = "jsonb")), что вызывает ошибку в H2.
 *
 * РЕШЕНИЕ (Hibernate 6 / Spring Boot 3):
 *   Переопределяем columnDefinitionToTypeCode() чтобы строка "jsonb"
 *   маппилась на SqlTypes.LONGVARCHAR, который H2 поддерживает нативно.
 *
 * ИСПОЛЬЗОВАНИЕ: в application-test.yml:
 *   spring.jpa.database-platform: org.javaguru.travel.insurance.infrastructure.persistence.H2DialectWithJsonb
 */
public class H2DialectWithJsonb extends H2Dialect {

    @Override
    public Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
        if ("jsonb".equalsIgnoreCase(columnTypeName) || "json".equalsIgnoreCase(columnTypeName)) {
            return SqlTypes.LONGVARCHAR;
        }
        return super.resolveSqlTypeCode(columnTypeName, typeConfiguration);
    }
}