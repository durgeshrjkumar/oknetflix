package edu.mta.ok.nworkshop.utils;

/**
 * Helper class that provide static methods for calculating correlation value between two objects.
 */
public class CorrelationUtils {

	/**
	 * Calculate the pearson correlation coefficient betwen two items using the given values.
	 * 
	 * @param xSum sum of the first item ratings.
	 * @param ySum sum of the second item ratings.
	 * @param xPowSum sum of the first item ratings when every rating value is power by 2.
	 * @param yPowSum sum of the second item ratings when every rating value is power by 2.
	 * @param xySum sum of the first item ratings multiplied with the second item ratings (every item 1 rating is multiplied with the matching item 2 rating that 
	 * the same user gave).
	 * @param ratersNum the number of raters the two items have in common.
	 * @param alpha an arbitrary value used to shrink the correlation based on the number of raters the two items have thus making the result more accurate.
	 * in case the value is null no shrinkage will be done. 
	 * @return the correlation value between two items.
	 */
	public static double getPearsonCorrelation(double xSum,double ySum, double xPowSum, double yPowSum, double xySum, int ratersNum, Integer alpha){
		double retVal = 0;
		
		double denominator = Math.sqrt((xPowSum - (Math.pow(xSum, 2) / ratersNum)) * (yPowSum - (Math.pow(ySum, 2) / ratersNum)));
		double numerator = xySum - ((xSum*ySum) / ratersNum);
		
		if (denominator == 0){
			retVal = 0;
		}
		else{		
			retVal = numerator / denominator;
			
			if (alpha != null){			
				retVal *= ((double)ratersNum / (double)(ratersNum + alpha));
			}
		}
		
		return retVal;
	}
	
	/**
	 * Call {@link #getPearsonCorrelation(double, double, double, double, double, int, Integer)} without shrinking the correlation (set alpha parameter to null)
	 * @see #getPearsonCorrelation(double, double, double, double, double, int)
	 */
	public static double getPearsonCorrelation(double xSum,double ySum, double xPowSum, double yPowSum, double xySum, int ratersNum){
		
		return CorrelationUtils.getPearsonCorrelation(xSum, ySum, xPowSum, yPowSum, xySum, ratersNum, null);
	}
	
	/**
	 * Calculate the pearson correlation coefficient betwen two items using the given values.
	 * The correlation is calculated according to the formula stated in {@link http://www.grouplens.org/papers/pdf/www10_sarwar.pdf} (section 3.1.2).
	 * 
	 * @param xPowSum sum of the first item ratings when every rating value is power by 2. 
	 * the value is used for the counter calculation as appear in the formula. 
	 * @param yPowSum sum of the second item ratings when every rating value is power by 2.
	 * the value is used for the counter calculation as appear in the formula.
	 * @param xySum the value of the counter in the correlation formula. 
	 * @return the correlation value between two items.
	 */
	public static double getPearsonCorrelationCoefficient(double xySum, double xPowSum, double yPowSum){
		double denominator = Math.sqrt(xPowSum) * Math.sqrt(yPowSum);
		
		double retVal;
		
		if (denominator == 0){
			retVal = 0;
		}
		else{
			retVal = (xySum / denominator);
		}
		
		return retVal;
	}
}
