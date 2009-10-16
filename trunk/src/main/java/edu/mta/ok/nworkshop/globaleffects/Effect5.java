package edu.mta.ok.nworkshop.globaleffects;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implementation of the 5th global effect described in Bellkor abstract (User x Time(movie)^0.5)
 */
public class Effect5 extends EffectAbstract {

	private short[] minDates;  
	private double[] avgDatesDiff;
	private short[] trainingDates;
	
	public Effect5(String userModelFileName, String movieModelFileName,
			String movieResidualFileName, String userResidualFileName,
			String probeFileName) {
		
		super(userModelFileName, movieModelFileName, movieResidualFileName,
				userResidualFileName, probeFileName, 150);
		
		minDates = new short[Constants.NUM_MOVIES];
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
		
		int currUser = 0;
		avgDatesDiff = new double[Constants.NUM_USERS];
		Arrays.fill(avgDatesDiff, 0.0);		
		int userRatingInd = 0;
		
		// Find the minimum rate date for each movie by running on the user indexed model
		for (short[] dates : userIndexedDates){
			
			userRatingInd = 0;
			
			for (short date : dates){
				if (date < minDates[userIndexedMovieIDs[currUser][userRatingInd] - 1]){
					minDates[userIndexedMovieIDs[currUser][userRatingInd] - 1] = date;
				}
				
				userRatingInd++;
			}
			
			currUser++;
		}

		currUser = 0;
		
		// Calculate the sum of square differences between each user rate date and minimum rate date for each movie
		for (short[] dates : userIndexedDates){
			
			userRatingInd = 0;
			
			for (short date : dates){
				avgDatesDiff[currUser] += Math.sqrt(date - minDates[userIndexedMovieIDs[currUser][userRatingInd] - 1]);
				userRatingInd++;
			}
			
			avgDatesDiff[currUser] /= dates.length;
			
			currUser++;
		}
		
		currUser = 0;		
		
		double tetaHatDenominator = 0;
		double tetaHatNumerator = 0;
		double currXi = 0;
		double tetaHat;
		
		// Calculate teta hat
		for (double[] residuals : userIndexedResiduals){
			int currInd = 0;
			tetaHatDenominator = 0;
			tetaHatNumerator = 0;
			
			for (double residual : residuals){
				// Subtract the squared root of the difference between the user rating date and the movie minimum rating date, with
				// the saverage value of the difference dates between the users ratings dates and the current movie minimum rating date
				currXi = Math.sqrt(userIndexedDates[currUser][currInd] - minDates[userIndexedMovieIDs[currUser][currInd] - 1]) - avgDatesDiff[currUser];
				
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
		int userInd = userIndices.get(trainingUserIDs[trainingInd]);
		double retVal = super.getProbeTetaVal(trainingInd);
		int dateDiff = trainingDates[trainingInd] - minDates[trainingMoviesIDs[trainingInd] - 1];
		if (dateDiff < 0){
			dateDiff = 0;
		}
		
		retVal *= (Math.sqrt(dateDiff) - avgDatesDiff[userInd]);
		
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
		
		retVal *= (Math.sqrt(date - minDates[movieId - 1]) - avgDatesDiff[userId]);
		
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
		return true;
	}
	
	public static void main(String[] args) {
		Effect5 effect = new Effect5(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect4.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect4.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect4.data");
		effect.startEffectCalculation();
	}
}
