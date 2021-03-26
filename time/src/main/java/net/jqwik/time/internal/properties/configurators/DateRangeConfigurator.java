package net.jqwik.time.internal.properties.configurators;

import java.time.*;
import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.configurators.*;
import net.jqwik.api.providers.*;
import net.jqwik.time.api.arbitraries.*;
import net.jqwik.time.api.constraints.*;
import net.jqwik.time.internal.properties.arbitraries.*;

public class DateRangeConfigurator {

	public static class ForLocalDate extends ArbitraryConfiguratorBase {

		@Override
		protected boolean acceptTargetType(TypeUsage targetType) {
			return targetType.isAssignableFrom(LocalDate.class);
		}

		public Arbitrary<LocalDate> configure(Arbitrary<LocalDate> arbitrary, DateRange range) {
			LocalDate min = isoDateToLocalDate(range.min());
			LocalDate max = isoDateToLocalDate(range.max());
			if (arbitrary instanceof LocalDateArbitrary) {
				LocalDateArbitrary localDateArbitrary = (LocalDateArbitrary) arbitrary;
				return localDateArbitrary.between(min, max);
			} else {
				return arbitrary.filter(v -> filter(v, min, max));
			}
		}

	}

	public static class ForCalendar extends ArbitraryConfiguratorBase {

		@Override
		protected boolean acceptTargetType(TypeUsage targetType) {
			return targetType.isAssignableFrom(Calendar.class);
		}

		public Arbitrary<Calendar> configure(Arbitrary<Calendar> arbitrary, DateRange range) {
			Calendar min = isoDateToCalendar(range.min(), false);
			Calendar max = isoDateToCalendar(range.max(), true);
			if (arbitrary instanceof CalendarArbitrary) {
				CalendarArbitrary calendarArbitrary = (CalendarArbitrary) arbitrary;
				return calendarArbitrary.between(min, max);
			} else {
				return arbitrary.filter(v -> filter(v, min, max));
			}
		}

	}

	public static class ForDate extends ArbitraryConfiguratorBase {

		@Override
		protected boolean acceptTargetType(TypeUsage targetType) {
			return targetType.isAssignableFrom(Date.class);
		}

		public Arbitrary<Date> configure(Arbitrary<Date> arbitrary, DateRange range) {
			Date min = isoDateToDate(range.min(), false);
			Date max = isoDateToDate(range.max(), true);
			if (arbitrary instanceof DateArbitrary) {
				DateArbitrary dateArbitrary = (DateArbitrary) arbitrary;
				return dateArbitrary.between(min, max);
			} else {
				return arbitrary.filter(v -> filter(v, min, max));
			}
		}

	}

	private static boolean filter(LocalDate date, LocalDate min, LocalDate max) {
		return !date.isBefore(min) && !date.isAfter(max);
	}

	private static boolean filter(Calendar date, Calendar min, Calendar max) {
		return date.compareTo(min) >= 0 && date.compareTo(max) <= 0;
	}

	private static boolean filter(Date date, Date min, Date max) {
		return date.compareTo(min) >= 0 && date.compareTo(max) <= 0;
	}

	private static LocalDate isoDateToLocalDate(String iso) {
		return LocalDate.parse(iso);
	}

	private static Calendar isoDateToCalendar(String iso, boolean max) {
		LocalDate localDate = isoDateToLocalDate(iso);
		Calendar calendar = DefaultCalendarArbitrary.localDateToCalendar(localDate);
		if (max) {
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MILLISECOND, 999);
		}
		return calendar;
	}

	private static Date isoDateToDate(String iso, boolean max) {
		Calendar calendar = isoDateToCalendar(iso, max);
		return calendar.getTime();
	}
}