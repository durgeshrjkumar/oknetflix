package edu.mta.ok.nworkshop.utils;

import java.util.Calendar;

/**
 * Helper class that contain static methods for dates calculations.
 */
public class DateUtils {

	/**
	 * Return the number of days between two dates 
	 * 
	 * @param firstDate The first date we want to check days difference
	 * @param secondDate The second date we want to check days difference 
	 * @return The number of days exists between the two given days
	 * @throws IllegalArgumentException In case the dates TimeZone doesn't match
	 */
	public static short calculateDiffBetweenDates(Calendar firstDate, Calendar secondDate) throws IllegalArgumentException{
		short retVal = 0;		
		
		if (!firstDate.getTimeZone().equals(secondDate.getTimeZone())){
			throw new IllegalArgumentException("Dates time zone doesn't match");
		}
		
		long endL;
        long startL;
		
		if (firstDate.before(secondDate)){
			endL =  secondDate.getTimeInMillis() + secondDate.getTimeZone().getOffset(secondDate.getTimeInMillis()); 
			startL = firstDate.getTimeInMillis() + firstDate.getTimeZone().getOffset(firstDate.getTimeInMillis());
		}
		else{
			startL =  secondDate.getTimeInMillis() + secondDate.getTimeZone().getOffset(secondDate.getTimeInMillis()); 
			endL = firstDate.getTimeInMillis() + firstDate.getTimeZone().getOffset(firstDate.getTimeInMillis());
			
		}
		
		retVal = (short) ((endL - startL) / (24 * 60 * 60 * 1000));
		
		return retVal;
	}
}
