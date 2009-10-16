package edu.mta.ok.nworkshop.predictor;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.PredictorProperties.Predictors;
import edu.mta.ok.nworkshop.PredictorProperties.PropertyKeys;
import edu.mta.ok.nworkshop.model.MovieIndexedModel;
import edu.mta.ok.nworkshop.model.MovieIndexedModelRatings;
import edu.mta.ok.nworkshop.model.UserIndexedModelRatings;
import edu.mta.ok.nworkshop.model.UserIndexedModelResiduals;
import edu.mta.ok.nworkshop.similarity.PearsonCorrelationCoefifcientSimilarity;
import edu.mta.ok.nworkshop.similarity.PearsonCorrelationCoefifcientSimilarityRawScores;
/**
 * KNN predictor class that uses the raw rating scores given by Netflix.
 * The class uses raw scores models and similarity calculator in order to predict ratings.
 * 
 * @see UserIndexedModelResiduals
 * @see PearsonCorrelationCoefifcientSimilarity
 */
public class KNNPredictor extends KNNPredictorAbstract {

	public KNNPredictor() {
		this(PredictorProperties.getInstance().getPredictorIntProperty(Predictors.KNN, PropertyKeys.NEIGHBORS_NUM, DEFAULT_NEIGHBOARS_NUM));
	}
	
	public KNNPredictor(int neighboarsNum){
		super(neighboarsNum, DEFAULT_ALPHA);
		
		this.userModel = new UserIndexedModelRatings();
		MovieIndexedModel movieModelTemp = new MovieIndexedModelRatings();
		this.similarityModel = new PearsonCorrelationCoefifcientSimilarityRawScores(movieModelTemp,  this.userModel);
		
		System.out.println("Start calculating similarities");
		this.similarityModel.calculateSimilarities();
		System.out.println("Finish calculating similarities");
		
		initCommonRatersData();
	}

	/**
	 * Calculate the final prediction by dividing the prediction with the number of neighbors used in order to calculate it
	 */
	
	@Override
	protected double getFinalPrediction(int userId, short movieId, double prediction, double totalCorrelation, 
			int neighboarsNum, int probeIndex){
		
		if (!(prediction == 0 || Double.isNaN(prediction) || Double.isInfinite(prediction))){
			prediction = prediction / (double)neighboarsNum;
		}
		
		return prediction;
	}

	public static void main(String[] args) {
		int neighboarsNum = 50;
		KNNPredictor predictorClass = new KNNPredictor(neighboarsNum);
		System.out.println("Finished creating KNNPredictor");
		PredictionTester.getProbeError(predictorClass, Constants.NETFLIX_OUTPUT_DIR + "Predictions/KNNRawScores-" + neighboarsNum + "Neighboars.txt", Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PROBE_FILE_NAME);
	}		
}
