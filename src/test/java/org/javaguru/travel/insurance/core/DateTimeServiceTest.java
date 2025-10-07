package org.javaguru.travel.insurance.core;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeServiceTest {

    private DateTimeService dateTimeService = new DateTimeService();

    @Test
    public void shouldDaysBetweenBeZero() {
        LocalDate date1 = LocalDate.of(2023,1,1);
        LocalDate date2 = LocalDate.of(2023,1,1);
        var daysBetween = dateTimeService.getDaysBetween(date1, date2);
        assertEquals(0L, daysBetween);
    }

    @Test
    public void shouldDaysBetweenBePositive() {
        LocalDate date1 = LocalDate.of(2023,1,1);
        LocalDate date2 = LocalDate.of(2023,1,10);
        var daysBetween = dateTimeService.getDaysBetween(date1, date2);
        assertEquals(9L,daysBetween);
    }

    @Test

    public void shouldDaysBetweenBeNegative() {
        LocalDate date1 = LocalDate.of(2023,1,10);
        LocalDate date2 = LocalDate.of(2023,1,1);
        var daysBetween = dateTimeService.getDaysBetween(date1, date2);
        assertEquals(-9L, daysBetween);
    }

    private Date createDate(String dateStr) {
        try {
            return new SimpleDateFormat("dd.MM.yyyy").parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}