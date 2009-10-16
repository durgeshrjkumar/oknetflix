package edu.mta.ok.nworkshop;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Helper class that calculates RMSE according to the Netflix prize formula.
 * 
 * In order to calculate an algorithm predictions RMSE a call to {@link #addErrorElement(double, byte)} 
 * should be done for every prediction value terminated by a call for {@link #getFinalScore()} in order
 * to get the final RMSE value.
 */
public class RMSECalculator {

	private double error = 0.0;
	
	private int elementsNum = 0; 
	
	private static final int RMSE_PERCISION = 4;
	
	private MathContext context = new MathContext(RMSE_PERCISION);
	
	/**
	 * Erase the calculated values and starts a new calculation.
	 */
	public void reset(){
		error = 0.0;
		elementsNum = 0;
	}
	
	/**
	 * Adds a new error value to the errors sum.
	 * 
	 * @param prediction a predicted rating value for a certain item 
	 * @param rating the real rating value the item got
	 * @return the calculated error value equals to (prediction - rating)^2
	 */
	public double addErrorElement(double prediction, byte rating){				
				
		double retVal = Math.pow((prediction - rating), 2);
		
		error += retVal;				
		
		elementsNum++;
		
		return retVal;
	}
	
	/**
	 * Adds a new error value to the errors sum.
	 * 
	 * @param prediction a predicted rating value for a certain item 
	 * @param rating the real rating value the item got
	 * @return the calculated error value equals to (prediction - rating)^2
	 */
	public double addErrorElement(float prediction, byte rating){				
		
		double retVal = Math.pow((prediction - rating), 2);
		
		error += retVal;				
		
		elementsNum++;
		
		return retVal;
	}
	
	/**
	 * @return final RMSE value
	 */
	public double getFinalScore(){
		
		BigDecimal b = new BigDecimal(Math.sqrt(error / elementsNum), context);
		
		return b.doubleValue();
	}
}
