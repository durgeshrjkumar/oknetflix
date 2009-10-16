package edu.mta.ok.nworkshop.globaleffects;

import edu.mta.ok.nworkshop.Constants;

/**
 * Implementation of the second global effect described in Bellkor abstract (Movie effect)
 */
public class Effect2 extends EffectAbstract {

	public Effect2(String userFileName, String movieFileName, String movieResidualFileName, String userResidualFileName, String probeFileName){
		super(userFileName, movieFileName, movieResidualFileName, userResidualFileName, probeFileName, 25);
	}
	
	@Override
	protected void calculateEffect(){
		
		float tetaHatI = 0;		
				
		for (int currMovie = 0; currMovie < movieIndexedResiduals.length; currMovie++){
			
			tetaHatI = 0;			
			
			// Calculate the tetaI for the current movie
			for (double rating : movieIndexedResiduals[currMovie]){
				tetaHatI += rating;
			}
			
			calculateTetaI(movieIndexedResiduals[currMovie].length, (tetaHatI / movieIndexedResiduals[currMovie].length), currMovie);
		}
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
		Effect2 effect = new Effect2(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect1.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect1.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect1.data");
		effect.startEffectCalculation();
	}
}
