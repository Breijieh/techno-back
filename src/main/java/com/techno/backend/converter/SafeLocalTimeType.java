package com.techno.backend.converter;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.TimeJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalTime;

/**
 * Safe LocalTime Hibernate Type
 * Handles invalid time values in the database gracefully by returning null
 * instead of throwing DateTimeException
 */
public class SafeLocalTimeType extends TimeJdbcType {

    private static final Logger log = LoggerFactory.getLogger(SafeLocalTimeType.class);

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return new BasicExtractor<>(javaType, this) {
            @Override
            protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
                return getValue(rs, paramIndex, options);
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                return getValue(statement, index, options);
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                return getValue(statement, name, options);
            }

            @SuppressWarnings("unchecked")
            private <T> T getValue(ResultSet rs, Object index, WrapperOptions options) throws SQLException {
                Time time;
                if (index instanceof String) {
                    time = rs.getTime((String) index);
                } else {
                    time = rs.getTime((Integer) index);
                }
                if (time == null) {
                    return null;
                }
                try {
                    return (T) javaType.wrap(time.toLocalTime(), options);
                } catch (Exception e) {
                    log.warn("Invalid time value in database: {}. Returning null. Error: {}", time, e.getMessage());
                    return null;
                }
            }

            @SuppressWarnings("unchecked")
            private <T> T getValue(CallableStatement statement, Object index, WrapperOptions options) throws SQLException {
                Time time = index instanceof String 
                    ? statement.getTime((String) index) 
                    : statement.getTime((Integer) index);
                if (time == null) {
                    return null;
                }
                try {
                    return (T) javaType.wrap(time.toLocalTime(), options);
                } catch (Exception e) {
                    log.warn("Invalid time value in database: {}. Returning null. Error: {}", time, e.getMessage());
                    return null;
                }
            }
        };
    }
}
