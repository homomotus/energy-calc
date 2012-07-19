package org.kklenski;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

public class EnergyCalc {

	private static final int DAY = 0;
	private static final int NIGHT = 1;

	//TODO: throw exception if data not ordered or exceeds month
	public static int[] calc(int year, int month, Map<Integer, Integer> data) throws IllegalArgumentException {
		int day = 0;
		int night = 0;
		int prevHour = 0;
		TimeZone tz = TimeZone.getTimeZone("Europe/Tallinn");
		Calendar cal = Calendar.getInstance(tz);
		cal.clear();
		cal.set(year, month, 1);
		cal.add(Calendar.HOUR_OF_DAY, -1);
		
		//DateFormat format = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM, new Locale("ru"));

		for (int hour : data.keySet()) {
			cal.add(Calendar.HOUR_OF_DAY, hour-prevHour);
			
			int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			boolean weekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
			boolean dst = tz.inDaylightTime(cal.getTime());
			//System.out.println(String.format("%d - %s,%d,%b,%b",hour,format.format(cal.getTime()), data.get(hour), weekend, dst));
			
			if (!weekend && hourOfDay >= (dst ? 8 : 7) && hourOfDay < (dst ? 24 : 23)) {
				day += data.get(hour); 
			} else {
				night += data.get(hour);
			}
			
			prevHour = hour;
			if (cal.get(Calendar.MONTH) != month) {
				throw new IllegalArgumentException("Invalid range");
			}
		}
		
		int[] result = new int[2];
		result[DAY] = day;
		result[NIGHT] = night;
		
		return result;
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
	
	//FIXME
	@Test
	public void testCalcWrongOrder() {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		data.put(1, 2);
		data.put(2, 3);
		data.put(32, 6);
		data.put(744, 0);
		data.put(26, 3);
		
		int[] result = calc(2012, Calendar.JANUARY, data);
		assertEquals(6, result[DAY]);
		assertEquals(8, result[NIGHT]);
	}
	
	@Test
	public void testCalcBeforeDstStart() {
		int[] result = calc(2012, Calendar.MARCH, initData(23, 0));
		assertEquals(  1110, result[DAY]);
		assertEquals(110001, result[NIGHT]);
	}
	
	@Test
	public void testCalcAfterDstStart() {
		int[] result = calc(2012, Calendar.MARCH, initData(26, -1));
		assertEquals( 11100, result[DAY]);
		assertEquals(100011, result[NIGHT]);
	}
	
	@Test
	public void testCalcBeforeDstEnd() {
		int[] result = calc(2012, Calendar.OCTOBER, initData(26, 0));
		assertEquals( 11100, result[DAY]);
		assertEquals(100011, result[NIGHT]);
	}
	
	@Test
	public void testCalcAfterDstEnd() {
		int[] result = calc(2012, Calendar.OCTOBER, initData(29, 1));
		assertEquals(  1110, result[DAY]);
		assertEquals(110001, result[NIGHT]);
	}
	
	private Map<Integer, Integer> initData(int days, int dstCorrection) {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		int hours = 1 + (days-1)*24 + dstCorrection;
		data.put(hours+6,       1); //6:00
		data.put(hours+7,      10); //7:00
		data.put(hours+8,     100); //8:00
		
		data.put(hours+22,   1000); //22:00
		data.put(hours+23,  10000); //23:00
		data.put(hours+24, 100000); //00:00 day after
		
		return data;
	}
	
	private Map<Integer, Integer> initData2(int hours) {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		for (int i = 1; i <= hours; i++) {
			data.put(i, 1);
		}

		return data;
	}
	
	@Test
	public void testCalcFullJanuary() {
		int[] result = calc(2012, Calendar.JANUARY, initData2(31*24));
		assertEquals(352, result[DAY]);
		assertEquals(392, result[NIGHT]);
	}
	
	@Test
	public void testCalcFullMarch() {
		int[] result = calc(2012, Calendar.MARCH, initData2(31*24-1));
		assertEquals(352, result[DAY]);
		assertEquals(391, result[NIGHT]);
	}
	
	@Test
	public void testCalcFullOctober() {
		int[] result = calc(2012, Calendar.OCTOBER, initData2(31*24+1));
		assertEquals(368, result[DAY]);
		assertEquals(377, result[NIGHT]);
	}


	@Test
	public void energyCalcTestEmptyData() {
		Map<Integer, Integer> data = new LinkedHashMap<Integer, Integer>();
		
		int[] result = calc(2012, Calendar.JANUARY, data);
		assertEquals(0, result[DAY]);
		assertEquals(0, result[NIGHT]);
	}
	
	//@Test out of range 
	
}
