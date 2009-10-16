package edu.mta.ok.nworkshop.predictor;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.PredictorProperties.Predictors;
import edu.mta.ok.nworkshop.PredictorProperties.PropertyKeys;
import edu.mta.ok.nworkshop.model.MovieIndexedModelResiduals;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelResiduals;
import edu.mta.ok.nworkshop.similarity.InterpolationSimilarityRawScores;
import edu.mta.ok.nworkshop.similarity.InterpolationSimilarityResiduals;
import edu.mta.ok.nworkshop.similarity.SimilarityCalculator;
import edu.mta.ok.nworkshop.utils.FileUtils;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Implementation of Bellkor interpolation KNN predictor.
 * The interpolation model used in this class was calculated using residuals of global effects as ratings data.
 */
public class ImprovedKNNPredictionResiduals extends ImprovedKNNPredictorAbstract {

	private static final String DEFAULT_INTERPOLATION_FILE = "interpolation/moviesCommonUsersLists-Final.data";
	
	private double[] residualPredictions = null;
	
	public ImprovedKNNPredictionResiduals(String interpolationFileName){
		UserIndexedModelResiduals userModelTemp = new UserIndexedModelResiduals();
		MovieIndexedModelResiduals movieModelTemp = new MovieIndexedModelResiduals();
		
		this.neighborsNum = PredictorProperties.getInstance().getPredictorIntProperty(Predictors.IMPROVED_KNN, PropertyKeys.NEIGHBORS_NUM, DEFAULT_NEIGHBOARS_NUM);
		interpolationSimilarityScores = new InterpolationSimilarityResiduals(movieModelTemp, userModelTemp);
		this.userModel = userModelTemp;
		this.alpha = DEFAULT_ALPHA;
		
		// Start calculate similarities
		interpolationSimilarityScores.calculateSimilarities();

		// Load the calculated interpolation data
		System.out.println("Loading interpolation data from: " + interpolationFileName);
		this.interpolationVals = FileUtils.loadDataFromFile(interpolationFileName);
		
		initEffectData();
	}
	
	public ImprovedKNNPredictionResiduals(int neighborsNum){
		this(new InterpolationSimilarityResiduals(Constants.DEFAULT_MOVIE_INDEXED_MODEL_FILE_NAME, Constants.DEFAULT_USER_INDEXED_MODEL_FILE_NAME),
			new UserIndexedModelResiduals(), neighborsNum, 
			Constants.NETFLIX_OUTPUT_DIR + PredictorProperties.getInstance().getPredictorStringProperty(Predictors.IMPROVED_KNN, PropertyKeys.INTERPOLATION_FILE_NAME, DEFAULT_INTERPOLATION_FILE));
	}
	
	public ImprovedKNNPredictionResiduals(SimilarityCalculator simModel, UserIndexedModel userModel, 
			String interpolationValsFileName) {
		super(simModel, userModel, interpolationValsFileName, DEFAULT_NEIGHBOARS_NUM);
		
		initEffectData();
	}
	
	public ImprovedKNNPredictionResiduals(SimilarityCalculator simModel, UserIndexedModel userModel, 
			int neighboarsNum, String interpolationValsFileName) {
		super(simModel, userModel, neighboarsNum, interpolationValsFileName);
		
		initEffectData();
	}
	
	public ImprovedKNNPredictionResiduals(SimilarityCalculator simModel, UserIndexedModel userModel, 
			int neighboarsNum, int alpha, String interpolationValsFileName) {
		super(simModel, userModel, alpha, interpolationValsFileName, 30);
		
		initEffectData();
	}
	
	/**
	 * Load the effect data used for prediction from a file
	 */
	private void initEffectData(){
		Object[] effect = ModelUtils.loadEffectProbeData(Constants.NETFLIX_OUTPUT_DIR + "globalEffects/Effect11.data");
		residualPredictions = (double[])effect[3];
		effect = null;
	}
	
	@Override
	protected double getFinalPrediction(double currPrediction, int probeIndex) {
		return currPrediction + residualPredictions[probeIndex];
	}
	
	@Override
	protected double getRatingValue(int userID, int position) {
		return ((double[])userModel.getUserRatings(userID))[position];
	}

	public static void main(String[] args) {
		ModelUtils.convertDoubleModelIntoFloat("D:\\FinalProject\\code\\netflixWorkshop\\binFiles\\interpolation\\similarityModel.data", 
			"D:\\FinalProject\\code\\netflixWorkshop\\binFiles\\interpolation\\similarityModel-Float.data");
		
		ImprovedKNNPredictionResiduals predictor = new ImprovedKNNPredictionResiduals(
				InterpolationSimilarityRawScores.getSimilarityFromFile("D:\\FinalProject\\code\\netflixWorkshop\\binFiles\\interpolation\\similarityModel-Float.data", true),
				new UserIndexedModelResiduals(),
				Constants.NETFLIX_OUTPUT_DIR + "interpolation\\moviesCommonUsersLists-Final.data");
	
		PredictionTester.getProbeError(predictor, Constants.NETFLIX_OUTPUT_DIR + "Predictions/InterpolationPredictorResiduals-" + DEFAULT_NEIGHBOARS_NUM + "Neighbors.txt", Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PROBE_FILE_NAME);
	}
}
