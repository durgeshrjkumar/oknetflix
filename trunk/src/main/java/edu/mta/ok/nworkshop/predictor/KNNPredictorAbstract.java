package edu.mta.ok.nworkshop.predictor;

import java.util.HashSet;
import java.util.Set;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.similarity.SimilarityCalculator;
import edu.mta.ok.nworkshop.utils.FileUtils;

/**
 * Abstract class that is a super class for all KNN predictor classes.
 * KNN (K Nearest Neighbors) is a neighborhood approach that identifies pairs of movies that tend to be rated similarly, and uses those 
 * similar items in order to predict a rating score for an unrated item.
 *
 * @see KNNGlobalEffectPredictor 
 * @see KNNPredictor
 */
public abstract class KNNPredictorAbstract implements Predictor {

	protected static String DEFAULT_RATERS_NUM_FILE_NAME = Constants.NETFLIX_OUTPUT_DIR + "similarityCommonRatersNum.data";
	protected static int DEFAULT_NEIGHBOARS_NUM = 20;
	protected static double HIGH_SIM_DEFAULT = -1000;
	protected static int DEFAULT_ALPHA = 20;
	
	protected SimilarityCalculator similarityModel;
	protected UserIndexedModel userModel;
	protected int neighboarsNum;
	protected int alpha;	
	protected int[][] commonRatersNum;
	
	public KNNPredictorAbstract(SimilarityCalculator simModel, UserIndexedModel userModel) {
		this(simModel, userModel, DEFAULT_NEIGHBOARS_NUM, DEFAULT_ALPHA);
	}
	
	public KNNPredictorAbstract(SimilarityCalculator simModel, UserIndexedModel userModel, int neighboarsNum) {
		this(simModel, userModel, neighboarsNum, DEFAULT_ALPHA);
	}
	
	public KNNPredictorAbstract(int neighboarsNum, int alpha){
		super();
		
		this.neighboarsNum = neighboarsNum;
		this.alpha = alpha;
	}
	
	public KNNPredictorAbstract(SimilarityCalculator simModel, UserIndexedModel userModel, int neighboarsNum, int alpha) {
		this(neighboarsNum, alpha);
		
		similarityModel = simModel;
		this.userModel = userModel;
		
		initCommonRatersData();
	}

	protected void initCommonRatersData() {
		commonRatersNum = FileUtils.loadDataFromFile(DEFAULT_RATERS_NUM_FILE_NAME);
	}

	/**
	 * Gets a set of k movies similar to the given movie that had been rated by the given user,
	 * and return a weighted average of the ratings of those movies a prediction value.
	 * 
	 * In order to support different prediction models the method calculates the similar scores for all models and than
	 * calls {@link #getFinalPrediction(int, short, double, double, int, int)} in order to calculate the final prediction score. 
	 */
	@Override
	public double predictRating(int userID, short movieID, int probeIndex) {
		
		int counter = 0;
		int k = neighboarsNum;
		double prediction = 0;
		double totalCorrelation = 0;
		double currCorrelation;
		double currSimScore = 0;
		
		Set<Short> itemSelectedNeighbors = new HashSet<Short>();
		short[] ratedMovies = userModel.getRatedMovies(userID);
		
		double currHighSim;
		short currHighId = -1;
		double currHighRating = HIGH_SIM_DEFAULT;
		int ratingsIndex;

		boolean finished = false;
		prediction = 0;
		short minMovieId, maxMovieId;
		
		// Get the k most similar movies or all the movies the user rated rated (in case he didn't rate enough movies).
		// Instead of sorting the list of movies rated by the user according to their similarity score, we'll iterate the rated movies set k times
		// and get the most similar movie to the given movieID in every iteration. 
		while (k > 0 && !finished){
		
			currHighSim = HIGH_SIM_DEFAULT;
			currHighId = -1;
			currHighRating = -1;
			finished = true;
			counter = 0;
			
			ratingsIndex = 0;
			
			for (short s : ratedMovies){
		
				minMovieId = (short)Math.min(movieID, s);
				maxMovieId = (short)Math.max(movieID, s);
				
				int currRatersNum = commonRatersNum[minMovieId - 1][maxMovieId - minMovieId - 1];
				currSimScore = similarityModel.getSimilarityScore(movieID, s);
				
				// Shrink the similarity according to the number of users rated both movies
				currSimScore *= (double)currRatersNum / (double)(currRatersNum + alpha);
				
				if ((currSimScore > currHighSim) && (!itemSelectedNeighbors.contains(s))){
					currHighSim = currSimScore;
					currHighId = s;
					currHighRating = userModel.getUserRating(ratingsIndex, userID);
				}
				
				counter++;
				ratingsIndex++;
			}				
			
			if (currHighSim != HIGH_SIM_DEFAULT){
				currCorrelation = currHighSim;
				prediction += currHighRating * currCorrelation;
				totalCorrelation += Math.abs(currCorrelation);
				itemSelectedNeighbors.add(currHighId);
				finished = false;
				k--;
			}
		}
		
		prediction = getFinalPrediction(userID, movieID, prediction, totalCorrelation, neighboarsNum - k, probeIndex);
		
		return prediction;
	}
	
	/**
	 * Calculate the final prediction by dividing the given prediction score by the given total correlation. 
	 * 
	 * @param userId id of a user that rated the given movie 
	 * @param movieId the id of an unrated movie that a rating prediction should be given to 
	 * @param prediction the pre-calculated prediction value that equals to the sum of ratings given to movies similar to the unrated movie multiplied by 
	 * their matching correlation value
	 * @param totalCorrelation sum of the correlation values for all the items similar to the movieId 
	 * @param neighboarsNum the number of movies similar to movieId  
	 * @param probeIndex the index of the given movie/user IDs pair in Netflix probe file
	 * @return final prediction value 
	 */
	protected double getFinalPrediction(int userId, short movieId, double prediction, double totalCorrelation, 
			int neighboarsNum, int probeIndex){
		
		if (!(prediction == 0 || Double.isNaN(prediction) || Double.isInfinite(prediction))){
			prediction = prediction / (double)totalCorrelation;
		}
		
		return prediction;
	}
	
	@Override
	public double predictRating(int userID, short movieID, String date, int probeIndex) {
		return predictRating(userID, movieID, probeIndex);
	}
}
