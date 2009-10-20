package edu.mta.ok.nworkshop.similarity;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.model.MovieIndexedModel;
import edu.mta.ok.nworkshop.model.MovieIndexedModelRatings;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelRatings;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Calculates the similarity between two movies using the interpolation similarity model suggested in 
 * the following Bellkor abstract: <a href="http://public.research.att.com/~volinsky/netflix/cfworkshop.pdf">Improved Neighborhood-based Collaborative Filtering</a>
 * on section 4.
 * 
 * The class uses the raw data given by Netflix in order to calculate the similarities values. 
 * 
 * @see UserIndexedModelRatings
 * @see MovieIndexedModelRatings
 */
public abstract class InterpolationSimilarityAbstract extends MovieIndexedSimilarityCalculatorAbstract {

	private static boolean loadedFromFile = false;
	
	protected float[][] movieIndexedSimilarityFloatData = null;
	protected static boolean floatSimilarityModel = false;
	protected boolean similaritiesCalculated = false;
	
	protected InterpolationSimilarityAbstract(){
		
	}
	
	public InterpolationSimilarityAbstract(MovieIndexedModel movieModel, UserIndexedModel userModel){
		super();
		
		this.movieIndexedModel = movieModel;
		this.userIndexedModel = userModel;
	}
	
	/**
	 * Builds the full similarity model by calculating similarity between every two movies in the model.
	 * 
	 * @throws UnsupportedOperationException in case the similarity model had been loaded from a file using
	 * {@link #getSimilarityFromFile(String, boolean)}
	 */	
	@Override
	public void calculateSimilarities() {
		
		if (loadedFromFile){
			throw new UnsupportedOperationException("Can't calculate similarity on class loaded from file");
		}
		else if (similaritiesCalculated){
			System.out.println("Similarity model had been calculated already, terminate calculation");
			return;
		}
		
		long startAll = System.currentTimeMillis(), start = startAll;
		float alpha = 10f;
		int[][] sizes = new int [Constants.NUM_MOVIES][];
		
		// The similarity model will hold only the half side up of the full model including the diagonal values (similarity between a movie and itself)
		movieIndexedSimilarityData = new double[Constants.NUM_MOVIES][];
		int j;
		
		int size = Constants.NUM_MOVIES;
		
		double currMovieScore = 0;
		int k;
		
		System.out.println("Start calculating similarity");
		
		for (short i = 0; i < Constants.NUM_MOVIES; i++){
			
			movieIndexedSimilarityData[i] = new double[size];
			Arrays.fill(movieIndexedSimilarityData[i], 0);
			
			sizes[i] = new int[size];
			Arrays.fill(sizes[i], 0);
			
			size--;
			
			// Move on all the users that rated the current movie
			for (int user : movieIndexedModel.getMovieRatersByIndex(i)){
				
				k = 0;
				
				// Get the score that the current user gave movie i
				for (short id : userIndexedModel.getRatedMoviesByIndex(user)){
					if (id == i+1){
						currMovieScore = getMovieRating(user, k);
						break;
					}
					
					k++;
				}
				
				// Move on all the ratings that the current user gave to all his movies (indexed by j) and sum the multiplication between the rating 
				// he gave to movie i and the rating he gave to movie j. In addition, we count the number of users who rated each movie. 
				
				j = userIndexedModel.getRatedMoviesByIndex(user).length - 1;
				double movie2Rating = 0;
				
				while (j > -1){
					
					// Because we calculate only half of the matrix (its a similar matrix), will run only on
					// all i < j movies, meaning that for each movie will fill it with data of movies that are bigger than him
					if (userIndexedModel.getRatedMoviesByIndex(user)[j] < (i+1)){
						break;
					}
					
					// increase the number of users that movie i has with movie j in common.
					// Because each movie array holds all the movies bigger than him (including the movie), and that i (the movie index) equals the movie id - 1 we performed -i-1. 
					// For example: movie 1 holds the number of similar users he has with movie 2 in common, the value is held at
					// place 1 in movie 1 array. in addition, i is the movie index but it starts from 0 so if we're checking movie 1 i value will be 0,
					// so in order to get into 0 place in the array we perform 2-i-1 = 1 (i is 0) as expected.
					sizes[i][userIndexedModel.getRatedMoviesByIndex(user)[j]-i-1]++;

					movie2Rating = getMovieRating(user, j); 
					movieIndexedSimilarityData[i][userIndexedModel.getRatedMoviesByIndex(user)[j]-i-1] += ((currMovieScore - movie2Rating) * (currMovieScore - movie2Rating));
					j--;
				}
				
				movieIndexedModel.removeMovieDataByIndex(i);
			}
			
			// Averaging the sum values that are in the values matrix by dividing it with the sizes matrix
			for (j = 0; j < movieIndexedSimilarityData[i].length; j++){
				movieIndexedSimilarityData[i][j] = (double)((double)sizes[i][j] / (double)(movieIndexedSimilarityData[i][j] + alpha));
			}
			
			if (i % 1000 == 0 && i > 0){
				System.out.println("finished " + i + " movies. took " + (System.currentTimeMillis() - start));
				start = System.currentTimeMillis();
			}
		}
		
		movieIndexedModel = null;
		userIndexedModel = null;

		similaritiesCalculated = true;
		
		System.out.println("Finished entire process. took " + (System.currentTimeMillis() - startAll));
	}
	
	/**
	 * Convert the calculated similarity model from two dimensional double array into two dimensional float array, 
	 * in order to decrease memory consumption.
	 */
	private void convertModelToFloat(){
		
		setSimilarities(ModelUtils.convertDoubleModelIntoFloat(movieIndexedSimilarityData, null));
		
	}
	
	protected abstract double getMovieRating(int userInd, int position);
	
	@Override
	public double getSimilarityScore(int firstMovieId, int secondMovieId) {
		
		double retVal = -1;
		
		if (floatSimilarityModel){
			// Verify that the model contains data for the given ids
			if (movieIndexedSimilarityFloatData == null || 
				movieIndexedSimilarityFloatData.length < (Math.min(firstMovieId, secondMovieId) - 1) || 
				movieIndexedSimilarityFloatData[Math.min(firstMovieId, secondMovieId) - 1] == null ||
				movieIndexedSimilarityFloatData[Math.min(firstMovieId, secondMovieId) - 1].length < Math.max(firstMovieId, secondMovieId) - Math.min(firstMovieId, secondMovieId)){
				throw new IllegalArgumentException("send parameters are invalid or similarity model is null");
			}
	
			// The model holds for every movie the similarity scores for all movies bigger than him (bigger by id) including the current movie, 
			// so the similarity is located at [minMovieIndex][maxMovieId - movieId] (minMovieIndex = min(movieId) - 1).
			// Examples: the similarity value between movies 1 and 2 it will be held at position [0][1], 
			// the similarity value between movies 2 and 5 will be held at position [1][3], 
			// the similarity value between movies 2 and 3 at position [1][1], etc.
			retVal = movieIndexedSimilarityFloatData[Math.min(firstMovieId, secondMovieId) - 1][Math.max(firstMovieId, secondMovieId) - Math.min(firstMovieId, secondMovieId)];
		}
		else{
		
			// Verify that the model contains data for the given ids
			if (movieIndexedSimilarityData == null || 
				movieIndexedSimilarityData.length < (Math.min(firstMovieId, secondMovieId) - 1) || 
				movieIndexedSimilarityData[Math.min(firstMovieId, secondMovieId) - 1] == null ||
				movieIndexedSimilarityData[Math.min(firstMovieId, secondMovieId) - 1].length < Math.max(firstMovieId, secondMovieId) - Math.min(firstMovieId, secondMovieId)){
				throw new IllegalArgumentException("send parameters are invalid or similarity model is null");
			}
	
			// The model holds for every movie the similarity scores for all movies bigger than him (bigger by id) including the current movie, 
			// so the similarity is located at [minMovieIndex][maxMovieId - movieId] (minMovieIndex = min(movieId) - 1).
			// Examples: the similarity value between movies 1 and 2 it will be held at position [0][1], 
			// the similarity value between movies 2 and 5 will be held at position [1][3], 
			// the similarity value between movies 2 and 3 at position [1][1], etc.
			retVal = movieIndexedSimilarityData[Math.min(firstMovieId, secondMovieId) - 1][Math.max(firstMovieId, secondMovieId) - Math.min(firstMovieId, secondMovieId)];
		}
		
		return retVal;
	}
	
	protected void setSimilarities(double[][] similarityModel){
		this.movieIndexedSimilarityData = similarityModel;
		
		floatSimilarityModel = false;
	}
	
	protected void setSimilarities(float[][] similarityModel){
		this.movieIndexedSimilarityFloatData = similarityModel;
		
		floatSimilarityModel = true;
		
		// Making sure that there is no double similarity model in order to keep memory consumption to minimum
		this.movieIndexedSimilarityData = null;
	}
}
