package edu.mta.ok.nworkshop.predictor;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.PredictorProperties.Predictors;
import edu.mta.ok.nworkshop.PredictorProperties.PropertyKeys;
import edu.mta.ok.nworkshop.model.MovieIndexedModel;
import edu.mta.ok.nworkshop.model.MovieIndexedModelResiduals;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelResiduals;
import edu.mta.ok.nworkshop.similarity.PearsonCorrelationCoefifcientSimilarity;
import edu.mta.ok.nworkshop.similarity.SimilarityCalculator;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * KNN predictor class that uses residuals of global effects as ratings instead of the raw scores.
 * The class uses residulas models and similarity calculator in order to predict ratings.
 * 
 * @see UserIndexedModelResiduals
 * @see PearsonCorrelationCoefifcientSimilarity
 */
public class KNNGlobalEffectPredictor extends KNNPredictorAbstract {

	private static final String DEFAULT_EFFECT_DATA_FILENAME = Constants.NETFLIX_OUTPUT_DIR + "globalEffects/Effect11.data";
	private double[] residualPredictions;

	public KNNGlobalEffectPredictor() {
		this(PredictorProperties.getInstance().getPredictorIntProperty(Predictors.KNN, PropertyKeys.NEIGHBORS_NUM, DEFAULT_NEIGHBOARS_NUM));
	}
	
	public KNNGlobalEffectPredictor(int neighboarsNum) {
		super(neighboarsNum, DEFAULT_ALPHA);
		
		this.userModel = new UserIndexedModelResiduals();
		MovieIndexedModel movieModelTemp = new MovieIndexedModelResiduals();
		this.similarityModel = new PearsonCorrelationCoefifcientSimilarity(movieModelTemp,  this.userModel);
		
		System.out.println("Start calculating similarities");
		this.similarityModel.calculateSimilarities();
		System.out.println("Finish calculating similarities");
		
		initEffectData(DEFAULT_EFFECT_DATA_FILENAME);
		initCommonRatersData();
	}
	
	public KNNGlobalEffectPredictor(SimilarityCalculator simModel, UserIndexedModel userModel, int neighboarsNum, String effectDataFileName) {		
		this(simModel, userModel, neighboarsNum, DEFAULT_ALPHA, effectDataFileName);
	}
	
	public KNNGlobalEffectPredictor(SimilarityCalculator simModel, UserIndexedModel userModel, int neighboarsNum, int alpha, String effectDataFileName) {		
		super(simModel, userModel, neighboarsNum, alpha);
		
		initEffectData(effectDataFileName);
	}

	private void initEffectData(String effectDataFileName) {
		Object[] effect = ModelUtils.loadEffectProbeData(effectDataFileName);
		residualPredictions = (double[])effect[3];
	}
	
	@Override
	protected double getFinalPrediction(int userId, short movieId,
			double prediction, double totalCorrelation, int neighboarsNum, int probeIndex) {
		double retVal = super.getFinalPrediction(userId, movieId, prediction, totalCorrelation,
				neighboarsNum, probeIndex);
		
		// Adding the global effects score to the predicted rating
		return retVal + residualPredictions[probeIndex];
	}

	public static void main(String[] args) {
		
		UserIndexedModel uim = new UserIndexedModelResiduals(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data",
				Constants.NETFLIX_OUTPUT_DIR + "userIndexesMap.data",
				Constants.NETFLIX_OUTPUT_DIR + "\\globalEffects\\userIndexedResidualEffect4.data");
		
		int neighboarsNum = 50;
		KNNGlobalEffectPredictor predictorClass = new KNNGlobalEffectPredictor(PearsonCorrelationCoefifcientSimilarity.getSimilarityFromFile(Constants.NETFLIX_OUTPUT_DIR + "\\residualsEffect4SimilarityModelScores.data"), 
				uim, neighboarsNum, Constants.NETFLIX_OUTPUT_DIR + "globalEffects/Effect4.data");
		System.out.println("Finished creating KNNGlobalEffectPredictor");
		PredictionTester.getProbeError(predictorClass, Constants.NETFLIX_OUTPUT_DIR + "Predictions/KNNResidualsEffect4-" + neighboarsNum + "Neighboars.txt", Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PROBE_FILE_NAME);
	}		
}
