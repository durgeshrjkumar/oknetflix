package edu.mta.ok.nworkshop.globaleffects;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implementation of the 9th global effect described in Bellkor abstract (User x Movie support)
 */
public class Effect9 extends EffectAbstract {

	private short[] minDates;  
	private double[] supportAvg;
	private byte[][] movieRatings;
	
	public Effect9(String userModelFileName, String movieModelFileName,
			String movieResidualFileName, String userResidualFileName,
			String probeFileName) {
		
		super(userModelFileName, movieModelFileName, movieResidualFileName,
				userResidualFileName, probeFileName, 90);
		
		minDates = new short[Constants.NUM_MOVIES];
		Arrays.fill(minDates, Short.MAX_VALUE);
	}

	@Override
	protected void calculateEffect() {
		
		int currUser = 0;
		supportAvg = new double[Constants.NUM_USERS];
		Arrays.fill(supportAvg, 0.0);		
		
		Object[] retVal = ModelUtils.loadMovieIndexedModel(movieModelFileName, false, false);
		movieRatings = (byte[][])retVal[1];
		retVal = null;
		
		// Find the average support for every user
		for (short[] ids : userIndexedMovieIDs){
			
			for(short id : ids){
				supportAvg[currUser] += Math.sqrt(movieRatings[id - 1].length);
			}
			
			supportAvg[currUser] /= ids.length;
			currUser++;
		}

		double tetaHatDenominator = 0;
		double tetaHatNumerator = 0;
		double currXi = 0;
		double tetaHat;
		currUser = 0;
		
		// Calculate teta hat
		for (double[] residuals : userIndexedResiduals){
			int currInd = 0;
			tetaHatDenominator = 0;
			tetaHatNumerator = 0;
			
			for (double residual : residuals){
				currXi = Math.sqrt(movieRatings[userIndexedMovieIDs[currUser][currInd] - 1].length) - supportAvg[currUser];
				
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
		int userInd = userIndices.get(trainingUserIDs[trainingInd]);
		retVal *= (Math.sqrt(this.movieRatings[trainingMoviesIDs[trainingInd] - 1].length) - supportAvg[userInd]);
		
		return retVal;
	}
	
	@Override
	protected double getRatingTetaVal(int userId, short movieId, WantedModel model, int modelInd) {
		double retVal = super.getRatingTetaVal(userId, movieId, model, modelInd);
		retVal *= (Math.sqrt(this.movieRatings[movieId - 1].length) - supportAvg[userId]);
		
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
		Effect9 effect = new Effect9(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect8.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect8.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect8.data");
		effect.startEffectCalculation();
	}
}
