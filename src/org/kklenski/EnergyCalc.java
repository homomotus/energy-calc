package org.kklenski;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

public class EnergyCalc {

	public static final int DAY = 0;
	public static final int NIGHT = 1;

	/**
	 * <p>
	 * Given client's per-hour electricity consumption for some month calculates
	 * total day- and night- consumption.
	 * </p>
	 * <p>
	 * Hours are enumerated from 1 starting from beginning of month. Day hours
	 * are 7-23 at working days (8-24 at daylight saving time or summer). All
	 * other hours are night hours.
	 * </p>
	 * 
	 * @param year
	 * @param month
	 * @param timezone
	 * @param data
	 *            {@link Map} where key represents hour (of month) and value
	 *            represents energy consumption. <code>data</code> should be
	 *            ordered using ascending ordering of its keys.
	 * 
	 * @return total day- and night- consumption in the form of array with
	 *         totals stored in the corresponding indexes {@link EnergyCalc#DAY}
	 *         and {@link EnergyCalc#NIGHT}
	 * @throws RuntimeException
	 *             in case data is not ordered, hour value is not positive or
	 *             hour falls out of current month's hour range
	 */
	public static int[] calc(int year, int month, TimeZone timezone, Map<Integer, Integer> data) throws RuntimeException {
		int day = 0;
		int night = 0;
		int prevHour = 0;
		Calendar cal = Calendar.getInstance(timezone);
		cal.clear();
		cal.set(year, month, 1);
		cal.add(Calendar.HOUR_OF_DAY, -1);
		
		for (int hour : data.keySet()) {
			if (hour < prevHour) {
				throw new RuntimeException("Data should be ordered by hour in ascending order");
			}
			
			if (hour < 1) {
				throw new RuntimeException(String.format("Hour should be positive, but %d encountered", hour));
			}
			
			cal.add(Calendar.HOUR_OF_DAY, hour-prevHour);
			
			if (cal.get(Calendar.MONTH) != month) {
				throw new RuntimeException(String.format("Hour %d does not belong to the specified month", hour));
			}
			
			int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			boolean weekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
			boolean dst = timezone.inDaylightTime(cal.getTime());
			
			if (!weekend && hourOfDay >= (dst ? 8 : 7) && hourOfDay < (dst ? 24 : 23)) {
				day += data.get(hour); 
			} else {
				night += data.get(hour);
			}
			
			prevHour = hour;
		}
		
		int[] result = new int[2];
		result[DAY] = day;
		result[NIGHT] = night;
		return result;
	}
	
	public static int[] calc(int year, int month, Map<Integer, Integer> data) throws RuntimeException {
		return calc(year, month, TimeZone.getTimeZone("Europe/Tallinn"), data);
	}
	
	@Test
	public void testCalcBasic() {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		data.put(1, 2);
		data.put(2, 3);
		data.put(26, 3);
		data.put(32, 6);
		data.put(744, 0);
		
		int[] result = calc(2012, Calendar.JANUARY, data);
		assertEquals(6, result[DAY]);
		assertEquals(8, result[NIGHT]);
	}
	
	@Test(expected=RuntimeException.class)
	public void testCalcWrongOrder() {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		data.put(1, 2);
		data.put(2, 3);
		data.put(32, 6);
		data.put(744, 0);
		data.put(26, 3);
		
		calc(2012, Calendar.JANUARY, data);
	}
	
	@Test(expected=RuntimeException.class)
	public void testCalcOutOfRange() {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		data.put(24*31+1, 1);
		calc(2012, Calendar.JANUARY, data);
	}
	
	@Test(expected=RuntimeException.class)
	public void testCalcNegativeHour() {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		data.put(-2, 1);
		calc(2012, Calendar.JANUARY, data);
	}
	
	@Test(expected=RuntimeException.class)
	public void testCalcZeroHour() {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		data.put(0, 1);
		calc(2012, Calendar.JANUARY, data);
	}
	
	@Test
	public void testCalcBeforeDstStart() {
		int[] result = calc(2012, Calendar.MARCH, fillData(23, 0));
		assertEquals(  1110, result[DAY]);
		assertEquals(110001, result[NIGHT]);
	}
	
	@Test
	public void testCalcAfterDstStart() {
		int[] result = calc(2012, Calendar.MARCH, fillData(26, -1));
		assertEquals( 11100, result[DAY]);
		assertEquals(100011, result[NIGHT]);
	}
	
	@Test
	public void testCalcBeforeDstEnd() {
		int[] result = calc(2012, Calendar.OCTOBER, fillData(26, 0));
		assertEquals( 11100, result[DAY]);
		assertEquals(100011, result[NIGHT]);
	}
	
	@Test
	public void testCalcAfterDstEnd() {
		int[] result = calc(2012, Calendar.OCTOBER, fillData(29, 1));
		assertEquals(  1110, result[DAY]);
		assertEquals(110001, result[NIGHT]);
	}
	
	private Map<Integer, Integer> fillData(int day, int dstCorrection) {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		int hours = 1 + (day-1)*24 + dstCorrection;
		data.put(hours+6,       1); //6:00
		data.put(hours+7,      10); //7:00
		data.put(hours+8,     100); //8:00
		
		data.put(hours+22,   1000); //22:00
		data.put(hours+23,  10000); //23:00
		data.put(hours+24, 100000); //00:00 day after
		
		return data;
	}
	
	private Map<Integer, Integer> fillData(int hours) {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		for (int i = 1; i <= hours; i++) {
			data.put(i, 1);
		}

		return data;
	}
	
	@Test
	public void testCalcFullJanuary() {
		int[] result = calc(2012, Calendar.JANUARY, fillData(31*24));
		assertEquals(352, result[DAY]);
		assertEquals(392, result[NIGHT]);
	}
	
	@Test
	public void testCalcFullMarch() {
		int[] result = calc(2012, Calendar.MARCH, fillData(31*24-1)); //-1 hour because DST is switched on
		assertEquals(352, result[DAY]);
		assertEquals(391, result[NIGHT]);
	}
	
	@Test
	public void testCalcFullOctober() {
		int[] result = calc(2012, Calendar.OCTOBER, fillData(31*24+1)); //+1 hour because DST is switched off
		assertEquals(368, result[DAY]);
		assertEquals(377, result[NIGHT]);
	}

	@Test
	public void testCalcEmptyData() {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		int[] result = calc(2012, Calendar.JANUARY, data);
		assertEquals(0, result[DAY]);
		assertEquals(0, result[NIGHT]);
	}
	
}
