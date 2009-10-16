package edu.mta.ok.nworkshop.globaleffects;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implements the first global effect stated in Bellkor abstract (global mean).
 * 
 * As stated in the abstract, the first effect simply return 3.6033 (global ratings mean) as 
 * prediction for every probe entry. 
 */
public class Effect1 extends EffectAbstract{

	public Effect1(String userFileName, String movieFileName){
		super(userFileName, movieFileName, null, null, null, 0);
		
		initProbe();
	}
	
	private void initProbe(){
		Object[] data = ModelUtils.loadProbeData(PredictorProperties.getInstance().getProbeFile());
		trainingUserIDs = (int[])data[0];
		trainingMoviesIDs = (short[])data[1];
		trainingRatings = (byte[])data[2];
		trainingPredictions = new double[Constants.PROBES_NUM];
	}
			
	@Override
	protected void calculateEffect() {

		// Not calculation is needed
		
	}

	@Override
	protected double getPrediction(int trainingInd) {
		return 3.6033;
	}

	@Override
	protected WantedModel getWantedModel() {
		return WantedModel.BOTH;
	}

	@Override
	protected void initTeta() {
		teta = new double[Constants.NUM_MOVIES];
		
	}

	@Override
	protected boolean loadModelDates() {
		return false;
	}
	
	@Override
	protected void saveResidualModels() {
		
		movieIndexedResiduals = new double[movieIndexedRatings.length][];
		
		int counter = 0;
		
		// Initialize the movie indexed residuals by subtracting the average prediction from the original movie indexed ratings matrix
		for (byte[] ratings : movieIndexedRatings){
			movieIndexedResiduals[counter] = new double[ratings.length];
			int ratingInd = 0;
			
			for (byte rating : ratings){
				movieIndexedResiduals[counter][ratingInd++] = rating - 3.6033f;
			}
			
			counter++;
		}
		
		saveMovieIndexedResiduals();
		
		userIndexedResiduals = new double[userIndexedRatings.length][];
		counter = 0;
		
		// Initialize the user indexed residuals by subtracting the average prediction from the original user indexed ratings matrix		
		for (byte[] ratings : userIndexedRatings){
			userIndexedResiduals[counter] = new double[ratings.length];
			int ratingInd = 0;
			
			for (byte rating : ratings){
				userIndexedResiduals[counter][ratingInd++] = rating - 3.6033;
			}
			
			counter++;
		}
		
		saveUserIndexedResiduals();
	}
	
	@Override
	protected boolean loadModelRatings(){
		return true;
	}
	
	public static void main(String[] args) {
		Effect1 effect = new Effect1(PredictorProperties.getInstance().getUserIndexedModelFile(), PredictorProperties.getInstance().getMovieIndexedModelFile());
		effect.startEffectCalculation();
	}
}
