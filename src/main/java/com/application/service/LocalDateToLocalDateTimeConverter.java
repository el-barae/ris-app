package com.application.service;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LocalDateToLocalDateTimeConverter
        implements Converter<LocalDate, LocalDateTime> {

    @Override
    public Result<LocalDateTime> convertToModel(
            LocalDate value, ValueContext context) {

        return value == null
                ? Result.ok(null)
                : Result.ok(value.atStartOfDay());
    }

    @Override
    public LocalDate convertToPresentation(
            LocalDateTime value, ValueContext context) {

        return value == null ? null : value.toLocalDate();
    }
}

