package edu.mta.ok.nworkshop.globaleffects;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;

/**
 * Implementation of the 10th global effect described in Bellkor abstract (Movie x User average)
 */
public class Effect10 extends EffectAbstract {

	private double[] avgMovieRatings;
	private double[] avgUserRatings;
	
	public Effect10(String userModelFileName, String movieModelFileName,
			String movieResidualFileName, String userResidualFileName,
			String probeFileName) {
		
		super(userModelFileName, movieModelFileName, movieResidualFileName,
				userResidualFileName, probeFileName, 90);
	}

	@Override
	protected void calculateEffect() {
		
		int currMovie = 0;
		avgMovieRatings = new double[Constants.NUM_MOVIES];
		avgUserRatings = new double[Constants.NUM_USERS];
		Arrays.fill(avgMovieRatings, 0.0);		
		Arrays.fill(avgUserRatings, 0.0);
		
		int[] userRatingsNum = new int[Constants.NUM_USERS];
		Arrays.fill(userRatingsNum, 0);
		
		// Find the average ratings for every movie
		for (byte[] ratings : movieIndexedRatings){

			int currInd = 0;
			
			for (byte rating : ratings){
				avgMovieRatings[currMovie] += rating;
				userRatingsNum[movieIndexedUserIDs[currMovie][currInd]]++;
				avgUserRatings[movieIndexedUserIDs[currMovie][currInd]] += rating;
				
				currInd++;
			}
			
			avgMovieRatings[currMovie] /= ratings.length;
			currMovie++;
		}
		
		int currUser = 0;
		
		// Calculate the average user ratings
		for (double ratingsNum : userRatingsNum){
			avgUserRatings[currUser] /= ratingsNum;
			currUser++;
		}
		
		currMovie = 0;
		
		double tetaHatDenominator = 0;
		double tetaHatNumerator = 0;
		double currXi = 0;
		double tetaHat;
		currUser = 0;
		
		// Calculate teta hat
		for (double[] residuals : movieIndexedResiduals){
			int currInd = 0;
			tetaHatDenominator = 0;
			tetaHatNumerator = 0;
			
			for (double residual : residuals){
				currXi = avgUserRatings[movieIndexedUserIDs[currMovie][currInd]] - avgMovieRatings[currMovie];
				
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
		retVal *= (avgUserRatings[userInd] - avgMovieRatings[trainingMoviesIDs[trainingInd] - 1]);
		
		return retVal;
	}
	
	@Override
	protected double getRatingTetaVal(int userId, short movieId, WantedModel model, int modelInd) {
		double retVal = super.getRatingTetaVal(userId, movieId, model, modelInd);
		retVal *= (avgUserRatings[userId] - avgMovieRatings[movieId - 1]);
		
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
	
	@Override
	protected boolean loadModelRatings() {
		return true;
	}
	
	public static void main(String[] args) {
		Effect10 effect = new Effect10(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect9.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect9.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect9.data");
		effect.startEffectCalculation();
	}
}
