package edu.mta.ok.nworkshop.globaleffects;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implementation of the 8th global effect described in Bellkor abstract (User x Movie average)
 */
public class Effect8 extends EffectAbstract {

	private short[] minDates;  
	private double[] avgRatings;
	
	public Effect8(String userModelFileName, String movieModelFileName,
			String movieResidualFileName, String userResidualFileName,
			String probeFileName) {
		
		super(userModelFileName, movieModelFileName, movieResidualFileName,
				userResidualFileName, probeFileName, 90);
		
		minDates = new short[Constants.NUM_MOVIES];
		Arrays.fill(minDates, Short.MAX_VALUE);
	}

	@Override
	protected void calculateEffect() {
		
		int currMovie = 0;
		avgRatings = new double[Constants.NUM_MOVIES];
		Arrays.fill(avgRatings, 0.0);		
		
		Object[] retVal = ModelUtils.loadMovieIndexedModel(movieModelFileName, false, false);
		byte[][] movieIndexRatings = (byte[][])retVal[1];
		retVal = null;
		
		// Find the average ratings for every movie
		for (byte[] ratings : movieIndexRatings){
			
			for (byte rating : ratings){
				avgRatings[currMovie] += rating;
			}
			
			avgRatings[currMovie] /= ratings.length;
			currMovie++;
		}
		
		// Free up memory
		movieIndexRatings = null;

		currMovie = 0;
		
		double tetaHatDenominator = 0;
		double tetaHatNumerator = 0;
		double currXi = 0;
		double tetaHat;
		int currUser = 0;
		
		// Calculate teta hat
		for (double[] residuals : userIndexedResiduals){
			int currInd = 0;
			tetaHatDenominator = 0;
			tetaHatNumerator = 0;
			
			for (double residual : residuals){
				currXi = avgRatings[userIndexedMovieIDs[currUser][currInd] - 1] - 3.6033;
				
				tetaHatNumerator += (residual * currXi);
				tetaHatDenominator += Math.pow(currXi, 2);
				
				currInd++;
			}
			
			tetaHat = (tetaHatDenominator == 0 && tetaHatNumerator == 0) ? 0 : (tetaHatNumerator / tetaHatDenominator);
			calculateTetaI(residuals.length, tetaHat, currUser);
			
			currUser++;
			
		}
	}
	
	@Override
	protected double getProbeTetaVal(int trainingInd) {		
		double retVal = super.getProbeTetaVal(trainingInd);
		retVal *= (avgRatings[trainingMoviesIDs[trainingInd] - 1] - 3.6033);
		
		return retVal;
	}
	
	@Override
	protected double getRatingTetaVal(int userId, short movieId, WantedModel model, int modelInd) {
		double retVal = super.getRatingTetaVal(userId, movieId, model, modelInd);
		retVal *= (avgRatings[movieId - 1] - 3.6033);
		
		return retVal;
	}
	
	@Override
	protected void initTeta() {
		teta = new double[Constants.NUM_USERS];
	}

	@Override
	protected WantedModel getWantedModel() {
		return WantedModel.USER_INDEXED_MODEL;
	}

	@Override
	protected boolean loadModelDates() {
		return false;
	}
	
	public static void main(String[] args) {
		Effect8 effect = new Effect8(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect7.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect7.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect7.data");
		effect.startEffectCalculation();
	}
}
