package edu.mta.ok.nworkshop.globaleffects;

import edu.mta.ok.nworkshop.Constants;

/**
 * Implementation of the third global effect described in Bellkor abstract (User effect)
 */
public class Effect3 extends EffectAbstract {

	public Effect3(String userFileName, String movieFileName, String movieResidualFileName, String userResidualFileName, String probeFileName){
		super(userFileName, movieFileName, movieResidualFileName, userResidualFileName, probeFileName, 7);
	}
	
	protected void calculateEffect(){
		
		float tetaHatI = 0;		
				
		for (int currUser = 0; currUser < userIndexedResiduals.length; currUser++){
			
			tetaHatI = 0;			
			
			// Calculate the tetaI for the current User
			for (double rating : userIndexedResiduals[currUser]){	
				tetaHatI += rating;
			}
			
			calculateTetaI(userIndexedResiduals[currUser].length, (tetaHatI / userIndexedResiduals[currUser].length), currUser);
		}
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
		Effect3 effect = new Effect3(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidualEffect2.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidualEffect2.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect2.data");
		effect.startEffectCalculation();
	}
}