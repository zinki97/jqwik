package net.jqwik.time.internal.properties.arbitraries;

import java.time.*;
import java.time.temporal.*;

import org.apiguardian.api.*;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.*;
import net.jqwik.time.api.*;
import net.jqwik.time.api.arbitraries.*;

import static java.time.temporal.ChronoUnit.*;
import static org.apiguardian.api.API.Status.*;

@API(status = INTERNAL)
public class DefaultLocalDateTimeArbitrary extends ArbitraryDecorator<LocalDateTime> implements LocalDateTimeArbitrary {

	private static final LocalDateTime DEFAULT_MIN = LocalDateTime.of(DefaultLocalDateArbitrary.DEFAULT_MIN_DATE, LocalTime.MIN);
	private static final LocalDateTime DEFAULT_MAX = LocalDateTime.of(DefaultLocalDateArbitrary.DEFAULT_MAX_DATE, LocalTime.MAX);

	private LocalDateTime min = null;
	private LocalDateTime max = null;

	private ChronoUnit ofPrecision = DefaultLocalTimeArbitrary.DEFAULT_PRECISION;

	@Override
	protected Arbitrary<LocalDateTime> arbitrary() {

		LocalDateTime effectiveMin = calculateEffectiveMin();
		LocalDateTime effectiveMax = calculateEffectiveMax();
		if (effectiveMax.isBefore(effectiveMin)) {
			throw new IllegalArgumentException("The maximum date time is too soon after the minimum date time.");
		}

		LocalDateArbitrary dates = Dates.dates();
		LocalTimeArbitrary times = generateTimeArbitrary(effectiveMin, effectiveMax);

		dates = dates.atTheEarliest(effectiveMin.toLocalDate());
		dates = dates.atTheLatest(effectiveMax.toLocalDate());

		times = times.ofPrecision(ofPrecision);

		Arbitrary<LocalDateTime> dateTimes = Combinators.combine(dates, times).as(LocalDateTime::of);

		dateTimes = dateTimes.filter(v -> !v.isBefore(effectiveMin) && !v.isAfter(effectiveMax))
							 .edgeCases(edgeCases -> edgeCases.add(effectiveMin, effectiveMax));

		return dateTimes;

	}

	private LocalDateTime calculateEffectiveMin() {
		LocalDateTime effective = min != null ? min : DEFAULT_MIN;
		return calculateEffectiveMinWithPrecision(effective);
	}

	private LocalDateTime calculateEffectiveMinWithPrecision(LocalDateTime effective) {
		LocalDate date = effective.toLocalDate();
		LocalTime time = effective.toLocalTime();
		try {
			time = DefaultLocalTimeArbitrary.calculateEffectiveMinWithPrecision(time, ofPrecision);
		} catch (IllegalArgumentException e) {
			time = LocalTime.MIN;
			LocalDate effectiveDate = date.plusDays(1);
			if (effectiveDate.isBefore(date)) {
				throw new IllegalArgumentException("Date is LocalDate.MAX and must be increased by 1 day.");
			}
			date = effectiveDate;
		}
		return LocalDateTime.of(date, time);
	}

	private LocalDateTime calculateEffectiveMax() {
		LocalDateTime effective = max != null ? max : DEFAULT_MAX;
		return calculateEffectiveMaxWithPrecision(effective);
	}

	private LocalDateTime calculateEffectiveMaxWithPrecision(LocalDateTime effective) {
		LocalDate date = effective.toLocalDate();
		LocalTime time = effective.toLocalTime();
		time = DefaultLocalTimeArbitrary.calculateEffectiveMaxWithPrecision(time, ofPrecision);
		return LocalDateTime.of(date, time);
	}

	private LocalTimeArbitrary generateTimeArbitrary(LocalDateTime effectiveMin, LocalDateTime effectiveMax) {
		LocalTimeArbitrary times = Times.times();
		if (effectiveMin.toLocalDate().isEqual(effectiveMax.toLocalDate())) {
			times = times.between(effectiveMin.toLocalTime(), effectiveMax.toLocalTime());
		}
		return times;
	}

	@Override
	public LocalDateTimeArbitrary atTheEarliest(LocalDateTime min) {
		if (min.getYear() <= 0) {
			throw new IllegalArgumentException("Minimum year in a date time must be > 0");
		}
		if ((max != null) && min.isAfter(max)) {
			throw new IllegalArgumentException("Minimum date time must not be after maximum date time");
		}

		DefaultLocalDateTimeArbitrary clone = typedClone();
		clone.min = min;
		return clone;
	}

	@Override
	public LocalDateTimeArbitrary atTheLatest(LocalDateTime max) {
		if (max.getYear() <= 0) {
			throw new IllegalArgumentException("Minimum year in a date time must be > 0");
		}
		if ((min != null) && max.isBefore(min)) {
			throw new IllegalArgumentException("Maximum date time must not be before minimum date time");
		}

		DefaultLocalDateTimeArbitrary clone = typedClone();
		clone.max = max;
		return clone;
	}

	@Override
	public LocalDateTimeArbitrary ofPrecision(ChronoUnit ofPrecision) {
		if (!(ofPrecision.equals(HOURS)
					  || ofPrecision.equals(MINUTES)
					  || ofPrecision.equals(SECONDS)
					  || ofPrecision.equals(MILLIS)
					  || ofPrecision.equals(MICROS)
					  || ofPrecision.equals(NANOS))) {
			throw new IllegalArgumentException("Precision value must be one of these ChronoUnit values: HOURS, MINUTES, SECONDS, MILLIS, MICROS, NANOS");
		}

		DefaultLocalDateTimeArbitrary clone = typedClone();
		clone.ofPrecision = ofPrecision;
		return clone;
	}

}