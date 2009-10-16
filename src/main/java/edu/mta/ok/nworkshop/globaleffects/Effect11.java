package edu.mta.ok.nworkshop.globaleffects;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implementation of the 11th global effect described in Bellkor abstract (Movie x User support)
 */
public class Effect11 extends EffectAbstract {

	private double[] supportAvg;
	private byte[][] userRatings;
	
	public Effect11(String userModelFileName, String movieModelFileName,
			String movieResidualFileName, String userResidualFileName,
			String probeFileName) {
		
		super(userModelFileName, movieModelFileName, movieResidualFileName,
				userResidualFileName, probeFileName, 90);
	}

	@Override
	protected void calculateEffect() {
		
		int currMovie = 0;
		supportAvg = new double[Constants.NUM_MOVIES];
		Arrays.fill(supportAvg, 0.0);		
		
		Object[] retVal = ModelUtils.loadUserIndexedModel(userModelFileName, false);
		userRatings = (byte[][])retVal[1];
		retVal = null;
		
		// Find the average support for every user
		for (int[] ids : movieIndexedUserIDs){
			
			for(int id : ids){
				supportAvg[currMovie] += Math.sqrt(userRatings[id].length);
			}
			
			supportAvg[currMovie] /= ids.length;
			currMovie++;
		}

		double tetaHatDenominator = 0;
		double tetaHatNumerator = 0;
		double currXi = 0;
		double tetaHat;
		currMovie = 0;
		
		// Calculate teta hat
		for (double[] residuals : movieIndexedResiduals){
			int currInd = 0;
			tetaHatDenominator = 0;
			tetaHatNumerator = 0;
			
			for (double residual : residuals){
				currXi = Math.sqrt(userRatings[movieIndexedUserIDs[currMovie][currInd]].length) - supportAvg[currMovie];
				
				tetaHatNumerator += (residual * currXi);
				tetaHatDenominator += Math.pow(currXi, 2);
				
				currInd++;
			}
			
			tetaHat = (tetaHatDenominator == 0 && tetaHatNumerator == 0) ? 0 : (tetaHatNumerator / tetaHatDenominator);
			calculateTetaI(residuals.length, tetaHat, currMovie);
			
			currMovie++;
		}
	}
	
	@Override
	protected double getProbeTetaVal(int trainingInd) {		
		double retVal = super.getProbeTetaVal(trainingInd);
		int userInd = userIndices.get(trainingUserIDs[trainingInd]);
		retVal *= (Math.sqrt(this.userRatings[userInd].length) - supportAvg[trainingMoviesIDs[trainingInd] - 1]);
		
		return retVal;
	}
	
	@Override
	protected double getRatingTetaVal(int userId, short movieId, WantedModel model, int modelInd) {
		double retVal = super.getRatingTetaVal(userId, movieId, model, modelInd);
		retVal *= (Math.sqrt(this.userRatings[userId].length) - supportAvg[movieId - 1]);
		
		return retVal;
	}
	
	@Override
	protected void initTeta() {
		teta = new double[Constants.NUM_MOVIES];
	}

	@Override
	protected WantedModel getWantedModel() {
		return WantedModel.MOVIE_INDEXED_MODEL;
	}

	@Override
	protected boolean loadModelDates() {
		return false;
	}
	
	public static void main(String[] args) {
		Effect11 effect = new Effect11(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect10.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect10.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect10.data");
		effect.startEffectCalculation();
		effect.exportPredictions(Constants.NETFLIX_OUTPUT_DIR + "Predictions\\fullGlobalEffects.txt");
	}
}
