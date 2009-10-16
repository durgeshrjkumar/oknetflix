package edu.mta.ok.nworkshop.globaleffects;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implementation of the 4th global effect described in Bellkor abstract (User x Time(user)^0.5)
 */
public class Effect4 extends EffectAbstract {

	private short[] minDates;  
	private double[] avgDatesDiff;
	private short[] trainingDates;
	
	public Effect4(String userModelFileName, String movieModelFileName,
			String movieResidualFileName, String userResidualFileName,
			String probeFileName) {
		
		super(userModelFileName, movieModelFileName, movieResidualFileName,
				userResidualFileName, probeFileName, 550);
		
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
	
		int currUser = 0;
		avgDatesDiff = new double[Constants.NUM_USERS];
		Arrays.fill(avgDatesDiff, 0.0);
		
		// Find the minimum rate date for each user
		for (short[] dates : userIndexedDates){
			for (short date : dates){
				if (date < minDates[currUser]){
					minDates[currUser] = date;
				}
			}
			
			currUser++;
		}

		currUser = 0;
		
		// Calculate the average of square differences between rate date and minimum for each user
		for (short[] dates : userIndexedDates){						
			
			int counter2 = 0;
			
			for (short date : dates){
				
				avgDatesDiff[currUser] += Math.sqrt(date - minDates[currUser]);
				counter2++;
			}
			
			avgDatesDiff[currUser] /= dates.length;
			
			currUser++;
		}
		
		currUser = 0;		
		
		double tetaHatDenominator = 0;
		double tetaHatNumerator = 0;
		double currXi = 0;
		double tetaHat;
		
		for (double[] residuals : userIndexedResiduals){
			int currInd = 0;
			tetaHatDenominator = 0;
			tetaHatNumerator = 0;
			
			for (double residual : residuals){
				currXi = Math.sqrt(userIndexedDates[currUser][currInd] - minDates[currUser]) - avgDatesDiff[currUser];
				
				tetaHatNumerator += (residual * currXi);
				tetaHatDenominator += (currXi * currXi);
				
				currInd++;
			}
			
			tetaHat = (tetaHatDenominator == 0 && tetaHatNumerator == 0) ? 0 : (tetaHatNumerator / tetaHatDenominator);
			calculateTetaI(residuals.length, tetaHat, currUser);
			
			currUser++;
			
		}
	}

	/**
	 * Return the effect's teta value for the current user, multiplied by (sqrt(trainigRateDate - minUserRateDate) - userAvg). 
	 */
	
	@Override
	protected double getProbeTetaVal(int trainingInd) {
		double retVal = super.getProbeTetaVal(trainingInd);
		retVal *= (Math.sqrt(trainingDates[trainingInd] - minDates[userIndices.get(trainingUserIDs[trainingInd])]) - avgDatesDiff[userIndices.get(trainingUserIDs[trainingInd])]);
		
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
		
		retVal *= (Math.sqrt(date - minDates[userId]) - avgDatesDiff[userId]);
		
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
		Effect4 effect = new Effect4(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect3.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect3.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect3.data");
		effect.startEffectCalculation();
		effect.exportPredictions(Constants.NETFLIX_OUTPUT_DIR + "Predictions\\4GlobalEffect.txt");
	}
}
