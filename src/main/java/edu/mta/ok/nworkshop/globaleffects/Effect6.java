package edu.mta.ok.nworkshop.globaleffects;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implementation of the 6th global effect described in Bellkor abstract (Movie x Time(movie)^0.5)
 */
public class Effect6 extends EffectAbstract {

	private short[] minDates;
	private double[] avgDatesDiff;
	private short[] trainingDates;
	
	public Effect6(String userModelFileName, String movieModelFileName,
			String movieResidualFileName, String userResidualFileName,
			String probeFileName) {
		
		super(userModelFileName, movieModelFileName, movieResidualFileName,
				userResidualFileName, probeFileName, 4000);
		
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
		
		int currMovie = 0;
		avgDatesDiff = new double[Constants.NUM_MOVIES];
		Arrays.fill(avgDatesDiff, 0.0);
		
		// Find the minimum rate date for each movie
		for (short[] dates : movieIndexedDates){
			for (short date : dates){
				if (date < minDates[currMovie]){
					minDates[currMovie] = date;
				}
			}
			
			currMovie++;
		}

		currMovie = 0;
		
		// Calculate the average of square differences between rate date and minimum for each movie
		for (short[] dates : movieIndexedDates){
			
			for (short date : dates){
				avgDatesDiff[currMovie] += Math.sqrt(date - minDates[currMovie]); 
			}
			
			avgDatesDiff[currMovie] /= dates.length;
			
			currMovie++;
		}
		
		currMovie = 0;		
		
		double tetaHatDenominator = 0;
		double tetaHatNumerator = 0;
		double currXi = 0;
		double tetaHat;
		
		for (double[] residuals : movieIndexedResiduals){
			int currInd = 0;
			tetaHatDenominator = 0;
			tetaHatNumerator = 0;
			
			for (double residual : residuals){								
				
				currXi = Math.sqrt(movieIndexedDates[currMovie][currInd] - minDates[currMovie]) - avgDatesDiff[currMovie];
				
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
		int dateDiff = trainingDates[trainingInd] - minDates[trainingMoviesIDs[trainingInd] - 1];
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
		
		retVal *= (Math.sqrt(date - minDates[movieId - 1]) - avgDatesDiff[movieId - 1]);
		
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
		Effect6 effect = new Effect6(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect5.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect5.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect5.data");
		
		effect.startEffectCalculation();
	}
}
