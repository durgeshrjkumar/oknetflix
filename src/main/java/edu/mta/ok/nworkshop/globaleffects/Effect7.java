package edu.mta.ok.nworkshop.globaleffects;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implementation of the 7th global effect described in Bellkor abstract (Movie x Time(user)^0.5)
 */
public class Effect7 extends EffectAbstract {

	private short[] minDates;
	private double[] avgDatesDiff;
	private short[] trainingDates;
	
	public Effect7(String userModelFileName, String movieModelFileName,
			String movieResidualFileName, String userResidualFileName,
			String probeFileName) {
		
		super(userModelFileName, movieModelFileName, movieResidualFileName,
				userResidualFileName, probeFileName, 500);
		
		minDates = new short[Constants.NUM_USERS];
		Arrays.fill(minDates, Short.MAX_VALUE);
		
		loadProbeDates();
	}
	
	private void loadProbeDates() {
		Object[] probe = ModelUtils.loadProbeData(Constants.NETFLIX_OUTPUT_DIR + "probe.data");
		trainingDates = (short[])probe[3];
		probe = null;
	}

	@Override
	protected void calculateEffect() {
		
		int currMovie = 0;
		avgDatesDiff = new double[Constants.NUM_MOVIES];
		Arrays.fill(avgDatesDiff, 0.0);		
		int movieRatingInd = 0;
		
		// Find the minimum rate date for each user by running on the movie indexed model
		for (short[] dates : movieIndexedDates){
			
			movieRatingInd = 0;
			
			for (short date : dates){
				if (date < minDates[movieIndexedUserIDs[currMovie][movieRatingInd]]){
					minDates[movieIndexedUserIDs[currMovie][movieRatingInd]] = date;
				}
				
				movieRatingInd++;
			}
			
			currMovie++;
		}

		currMovie = 0;
		
		// Create an array that will hold the number of movies that were rated by each user (we need it for the average calculation
		// because we don't have the user indexed model)
		int[] userRatingsNum = new int[Constants.NUM_MOVIES];
		Arrays.fill(userRatingsNum, 0);
		
		// Calculate the sum of square differences between a movie rate date and minimum rate date for every user
		for (short[] dates : movieIndexedDates){
			
			movieRatingInd = 0;
			
			for (short date : dates){
				avgDatesDiff[currMovie] += Math.sqrt(date - minDates[movieIndexedUserIDs[currMovie][movieRatingInd]]);
				movieRatingInd++;
			}
			
			avgDatesDiff[currMovie] /= dates.length;
			currMovie++;
		}
		
		// Free up consumed space
		userRatingsNum = null;
		
		currMovie = 0;		
		
		double tetaHatDenominator = 0;
		double tetaHatNumerator = 0;
		double currXi = 0;
		double tetaHat;
		
		// Calculate teta hat
		for (double[] residuals : movieIndexedResiduals){
			int currInd = 0;
			tetaHatDenominator = 0;
			tetaHatNumerator = 0;
			
			for (double residual : residuals){
				// Subtract the squared root of the difference between the user rating date and the movie minimum rating date, with
				// the saverage value of the difference dates between the users ratings dates and the current movie minimum rating date
				currXi = Math.sqrt(movieIndexedDates[currMovie][currInd] - minDates[movieIndexedUserIDs[currMovie][currInd]]) - avgDatesDiff[currMovie];
				
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
		int userInd = userIndices.get(trainingUserIDs[trainingInd]);
		double retVal = super.getProbeTetaVal(trainingInd);
		int dateDiff = trainingDates[trainingInd] - minDates[userInd];
		if (dateDiff < 0){
			dateDiff = 0;
		}
		
		retVal *= (Math.sqrt(dateDiff) - avgDatesDiff[trainingMoviesIDs[trainingInd] - 1]);
		
		return retVal;
	}
	
	@Override
	protected double getRatingTetaVal(int userId, short movieId, WantedModel model, int modelInd) {
		double retVal = super.getRatingTetaVal(userId, movieId, model, modelInd);
		short date = 0;
		
		if (model.equals(WantedModel.MOVIE_INDEXED_MODEL)){
			date = movieIndexedDates[movieId - 1][modelInd];
		}
		else{
			date = userIndexedDates[userId][modelInd];			
		}
		
		retVal *= (Math.sqrt(date - minDates[userId]) - avgDatesDiff[movieId - 1]);
		
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
		return true;
	}
	
	public static void main(String[] args) {
		Effect7 effect = new Effect7(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect6.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect6.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect6.data");
		effect.startEffectCalculation();
	}
}
