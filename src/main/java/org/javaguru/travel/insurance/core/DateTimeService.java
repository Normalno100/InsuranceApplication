package org.javaguru.travel.insurance.core;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class DateTimeService {

    public long getDaysBetween(LocalDate date1, LocalDate date2) {
        return ChronoUnit.DAYS.between(date1,date2);
    }
}
