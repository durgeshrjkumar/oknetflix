package edu.mta.ok.nworkshop.similarity;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.model.MovieIndexedModel;
import edu.mta.ok.nworkshop.model.MovieIndexedModelResiduals;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelResiduals;
import edu.mta.ok.nworkshop.utils.CorrelationUtils;
import edu.mta.ok.nworkshop.utils.FileUtils;

/**
 * Calculates the similarity between two movies using the pearson correlation coefficient algorithm.
 * The class uses residuals of global effects as the ratings models data.
 * 
 * @see UserIndexedModelResiduals 
 * @see MovieIndexedModelResiduals
 */
public class PearsonCorrelationCoefifcientSimilarity extends MovieIndexedSimilarityCalculatorAbstract{

	double[] movieAvgScores = new double[Constants.NUM_MOVIES];
	
	private int[][] movieIndexedUserIds;

	private short[][] userIndexedMovieIds;

	private double[][] userIndexedRatings;

	private double[][] movieIndexedRatings;
	
	private static boolean loadedFromFile = false;
	
	public PearsonCorrelationCoefifcientSimilarity(){
		this(true);
	}
	
	private PearsonCorrelationCoefifcientSimilarity(boolean loadModels){
		
		super();
		
		if (loadModels){
			this.movieIndexedModel = new MovieIndexedModelResiduals();
			this.userIndexedModel = new UserIndexedModelResiduals();
		}
	}
	
	public PearsonCorrelationCoefifcientSimilarity(MovieIndexedModel movieModel, UserIndexedModel userModel){
		this.movieIndexedModel = movieModel;
		this.userIndexedModel = userModel;
	}
	
	public PearsonCorrelationCoefifcientSimilarity(String movieModelFileName, String userModelFileName,
			String userIndicesFileName, String movieResidualsFileName, String userResidualsFileName){
		super();
		
		this.movieIndexedModel = new MovieIndexedModelResiduals(movieModelFileName, movieResidualsFileName);
		this.userIndexedModel = new UserIndexedModelResiduals(userModelFileName, userIndicesFileName, 
				userResidualsFileName);
	}
	
	/**
	 * Builds the full similarity model by calculating similarity between every two movies in the model.
	 * 
	 * @throws UnsupportedOperationException in case the similarity model had been loaded from a file using
	 * {@link #getSimilarityFromFile(String)}
	 */	
	@Override
	public void calculateSimilarities() {
		
		if (loadedFromFile){
			throw new UnsupportedOperationException("Can't calculate similarity on class loaded from file");
		}
		
		int j;
		short sizesSize = Constants.NUM_MOVIES - 1;
		movieIndexedSimilarityData = new double[Constants.NUM_MOVIES - 1][];
		double[] xySum, xPowSum, yPowSum;
		double currMovieAvg, secondMovieAvg;
		double secondMovieRating;
		
		// Loads the full movie/user models data into a class member for easy access
		movieIndexedUserIds = this.movieIndexedModel.getUserIds();
		userIndexedMovieIds = this.userIndexedModel.getMovieIds();
		userIndexedRatings = (double[][])this.userIndexedModel.getRatings();
		movieIndexedRatings = (double[][])this.movieIndexedModel.getRatings();
		
		System.out.println("Start calculating average ratings");
		
		calculateAvgRatings();
		
		System.out.println("Finished calculating average ratings");
		
		System.out.println("Start calculating similarities");
		
		int ratingCounter;
		long start = System.currentTimeMillis();						
		long startAll = start;		
		double currMovieRating;
		
		for (int i=0; i < Constants.NUM_MOVIES - 1; i++){
			
			movieIndexedSimilarityData[i] = new double[sizesSize];
			xySum = new double[sizesSize];
			Arrays.fill(xySum, 0.0);
			xPowSum = new double[sizesSize];
			Arrays.fill(xPowSum, 0.0);
			yPowSum = new double[sizesSize];
			Arrays.fill(yPowSum, 0.0);
			
			sizesSize--;			
			
			ratingCounter = 0;			
			short currMovieInd;
			
			currMovieAvg = movieAvgScores[i];
			
			// Pass on the model again and fill the ratings array with each movie ratings
			for (int user : movieIndexedUserIds[i]){
				
				currMovieRating = movieIndexedRatings[i][ratingCounter];
				 
				j = userIndexedMovieIds[user].length - 1;
				
				while (j > -1){
					
					short currMovieId = userIndexedMovieIds[user][j];
					
					// Because we calculate only half of the matrix (its a similar matrix), will run only on
					// all i < j movies, meaning that for each movie will fill it with data of movies that are bigger than him
					if (currMovieId <= (i+1)){
						break;
					}
					
					currMovieInd = (short)(currMovieId-i-2);
					secondMovieAvg = movieAvgScores[currMovieId - 1];
					
					secondMovieRating = userIndexedRatings[user][j];

					xySum[currMovieInd] += (currMovieRating - currMovieAvg) * (secondMovieRating - secondMovieAvg);
					xPowSum[currMovieInd] += Math.pow((currMovieRating - currMovieAvg), 2);
					yPowSum[currMovieInd] += Math.pow((secondMovieRating - secondMovieAvg), 2);
					
					j--;
				}	
				
				ratingCounter++;
			}
			
			// Calculate the similarity values
			for (j=0; j<=sizesSize; j++){
				movieIndexedSimilarityData[i][j] = CorrelationUtils.getPearsonCorrelationCoefficient(xySum[j], xPowSum[j], yPowSum[j]);
			}
			
			if (i % 1000 == 0 && i > 0){
				System.out.println("finished " + i + " movies. took " + (System.currentTimeMillis() - start));
				start = System.currentTimeMillis();
			}	
			
			// Free up memory (we don't need the movie data any more)
			movieIndexedModel.removeMovieDataByIndex((short)i);
		}
		
		// Release memory. the models aren't necessary anymore
		movieIndexedModel = null;
		userIndexedModel = null;
		
		System.out.println("Finish calculate similarities took " + (System.currentTimeMillis() - startAll));
	}
	
	@Override
	public double getSimilarityScore(int firstMovieId, int secondMovieId) {
		
		// Verify that the model contains data for the given ids
		if (movieIndexedSimilarityData == null || 
			movieIndexedSimilarityData.length < (Math.min(firstMovieId, secondMovieId) - 1) || 
			movieIndexedSimilarityData[Math.min(firstMovieId, secondMovieId) - 1] == null ||
			movieIndexedSimilarityData[Math.min(firstMovieId, secondMovieId) - 1].length < Math.max(firstMovieId, secondMovieId) - Math.min(firstMovieId, secondMovieId) - 1){
			throw new IllegalArgumentException("send parameters are invalid or similarity model is null");
		}

		// The model holds for every movie the similarity scores for all movies bigger than him (bigger by id), 
		// so the similarity is located at [minMovieIndex][maxMovieId - minMovieId] (minMovieIndex = movieId - 1).
		// Examples: the similarity value between movies 1 and 2 it will be held at position [0][0], 
		// the similarity value between movies 2 and 5 will be held at position [1][2], 
		// the similarity value between movies 2 and 3 at position [1][0], etc.
		return movieIndexedSimilarityData[Math.min(firstMovieId, secondMovieId) - 1][Math.max(firstMovieId, secondMovieId) - Math.min(firstMovieId, secondMovieId) - 1];
	}
	
	/**
	 * Creates a new instance of the class by loading the similarity model from a given file
	 * 
	 * REMARK: Notice that no calculation will be available on the return instance, trying to call
	 * "calculateSimilarities" will throw an exception.
	 * 
	 * @see #calculateSimilarities() 
	 * @param fileName a file containing the similarity model (a double matrix)
	 * @return a new instance of the class with a similarity model loaded from a given file
	 */
	public static PearsonCorrelationCoefifcientSimilarity getSimilarityFromFile(String fileName){
		
		if (fileName == null || fileName.isEmpty()){
			return null;
		}
		
		PearsonCorrelationCoefifcientSimilarity retVal = new PearsonCorrelationCoefifcientSimilarity(false);
		retVal.setSimilarities((double[][])FileUtils.loadDataFromFile(fileName));		
		
		retVal.setLoadedFromFile(true);
		
		return retVal;
	}
	
	private void setLoadedFromFile(boolean loadedFromFile) {
		PearsonCorrelationCoefifcientSimilarity.loadedFromFile = loadedFromFile;
	}
	
	private void setSimilarities(double[][] similarityModel){
		this.movieIndexedSimilarityData = similarityModel;
	}

	/**
	 * Calculate the average rating for every movie
	 */
	private void calculateAvgRatings(){
		
		int movieId = 0;
		
		for (double[] ratings : movieIndexedRatings){
			
			double currSum = 0;
			
			for (double rating : ratings){
				currSum += rating;
			}
			
			movieAvgScores[movieId] = (double)(currSum / (double)ratings.length);
			movieId++;
		}
	}
	
	public static void main(String[] args) {
		
		SimilarityCalculator simCalc = new PearsonCorrelationCoefifcientSimilarity(Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data",				
				Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data",
				Constants.NETFLIX_OUTPUT_DIR + "userIndexesMap.data",
				Constants.NETFLIX_OUTPUT_DIR + "\\globalEffects\\movieIndexedResidualEffect11.data",
				Constants.NETFLIX_OUTPUT_DIR + "\\globalEffects\\userIndexedResidualEffect11.data");
		
		simCalc.calculateSimilarities();
		
		simCalc.saveCalculatedData(Constants.NETFLIX_OUTPUT_DIR + "residualsSimilarityModelScores.data");
		
	}
}
