package edu.mta.ok.nworkshop.preprocess;

import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.PredictorProperties.Predictors;
import edu.mta.ok.nworkshop.PredictorProperties.PropertyKeys;
import edu.mta.ok.nworkshop.model.MovieIndexedModel;
import edu.mta.ok.nworkshop.model.MovieIndexedModelResiduals;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelResiduals;
import edu.mta.ok.nworkshop.utils.FileUtils;

/**
 * Implements the pre-processing calculations for Bellkor "Improved KNN" algorithm.
 * 
 * The algorithm is described in <a href="http://public.research.att.com/~volinsky/netflix/cfworkshop.pdf">Improved Neighborhood-based Collaborative Filtering</a>
 * on section 4.
 */
public class PreProcessItemViewUsers {
	
	HashMap<Integer, Integer> userIndices = null;
	
	private MovieIndexedModel movieModel;
	
	private UserIndexedModel userModel;
	
	public PreProcessItemViewUsers(){
		this(PredictorProperties.getInstance().getMovieIndexedModelFile(),
				PredictorProperties.getInstance().getUserIndexedModelFile());
	}
	
	public PreProcessItemViewUsers(String movieIndexFileName, String userIndexFileName){
		this(new MovieIndexedModelResiduals(movieIndexFileName, 
				Constants.NETFLIX_OUTPUT_DIR + "\\globalEffects\\movieIndexedResidualEffect11.data"), 		
				new UserIndexedModelResiduals(userIndexFileName));
	}
	
	public PreProcessItemViewUsers(MovieIndexedModel movieModel, UserIndexedModel userModel){
		this.movieModel = movieModel;
		movieModel.removeRatings();
		this.userModel = userModel;
	}
	
	/**
	 * Calculate the values that fill the A-bar and B-bar matrices that are used to calculate the interpolation weights 
	 * (formulas 9+10 from the abstract).  
	 */
	public Object[] calcMoviesCommonUsersModel(){
		long startAll = System.currentTimeMillis(), start = startAll;
		double[][] values = new double[Constants.NUM_MOVIES][];
		int[][] sizes = new int [Constants.NUM_MOVIES][];
		
		int j;
		
		int size = Constants.NUM_MOVIES;
		
		double currMovieScore = 0;
		int k;
		
		System.out.println("Start calculating the values of the A-bar and B-bar matrices");
		
		for (short i = 0; i < Constants.NUM_MOVIES; i++){
			
			values[i] = new double[size];
			Arrays.fill(values[i], 0);
			
			sizes[i] = new int[size];
			Arrays.fill(sizes[i], 0);
			
			size--;
			
			// Move on all the users that rated the current movie
			for (int user : movieModel.getMovieRatersByIndex(i)){
				
				k = 0;
				
				// Get the score that the current user gave movie i
				for (short id : userModel.getRatedMoviesByIndex(user)){
					if (id == i+1){
						currMovieScore = ((double[])userModel.getUserRatingsByIndex(user))[k];
						break;
					}
					
					k++;
				}
				
				// Move on all the ratings that the current user gave to all his movies (indexed by j) and sum the multiplication between the rating 
				// he gave to movie i and the rating he gave to movie j. In addition, we count the number of users who rated each movie. 
				
				j = userModel.getRatedMoviesByIndex(user).length - 1;
				
				while (j > -1){
					
					// Because we calculate only half of the matrix (its a similar matrix), will run only on
					// all i < j movies, meaning that for each movie will fill it with data of movies that are bigger than him
					if (userModel.getRatedMoviesByIndex(user)[j] < (i+1)){
						break;
					}
					
					// increase the number of users that movie i has with movie j in common.
					// Because each movie array holds all the movies bigger than him (including the movie), and that i (the movie index) equals the movie id - 1 we performed -i-1. 
					// For example: movie 1 holds the number of similar users he has with movie 2 in common, the value is held at
					// place 1 in movie 1 array. in addition, i is the movie index but it starts from 0 so if we're checking movie 1 i value will be 0,
					// so in order to get into 0 place in the array we perform 2-i-1 = 1 (i is 0) as expected.
					values[i][userModel.getRatedMoviesByIndex(user)[j]-i-1] += currMovieScore * (((double[])userModel.getUserRatingsByIndex(user))[j]); 
					sizes[i][userModel.getRatedMoviesByIndex(user)[j]-i-1]++;
					j--;
				}
				
				movieModel.removeMovieDataByIndex(i);
			}
			
			// Averaging the sum values that are in the values matrix by dividing it with the sizes matrix
			for (j = 0; j < values[i].length; j++){
				
				if (values[i][j] != 0){
					values[i][j] /= (double)sizes[i][j];
				}
			}
			
			if (i % 100 == 0 && i > 0){
				System.out.println("finished " + i + " movies. took " + (System.currentTimeMillis() - start));
				start = System.currentTimeMillis();
			}
		}
		
		System.out.println("Finished entire process. took " + (System.currentTimeMillis() - startAll));
		
		// Save the calculation results to a file (formulas 9 + 10)
		ObjectOutputStream oos = FileUtils.getObjectOutputStream(Constants.NETFLIX_OUTPUT_DIR + "interpolation/moviesCommonUsersLists-Calculated.data");
		if (oos != null){
			try{
				oos.writeObject(values);
				oos.writeObject(sizes);
				System.out.println("Sucessfully wrote matrices value file");
			}
			catch (Exception e){
				e.printStackTrace();
			}
			finally{
				FileUtils.outputClose(oos);
			}
		}
		
		return new Object[]{values,sizes};
	}
	
	/**
	 * Calculate the A-hat and B-hat matrices values according to formulas 11+12 in the Bellkor abstract
	 * @return two dimension array containing the calculated interpolation values
	 */
	public double[][] calcFinalValues(){		
		
		Object[] commonResult = calcMoviesCommonUsersModel();
		
		double[][] values = (double[][])commonResult[0];
		int[][] sizes = (int[][])commonResult[1];
		
		final short beta = 5000;
		
		short j2;
		double diagonalSum = 0, nonDiagonalSum = 0;
		double avgDiagonal, avgNonDiagonal;
		long startAll = System.currentTimeMillis(), start = startAll;
		
		System.out.println("Start calculating avg values");

		int nonDiagonalElementsNum = 0;
		
		// Calculate the avg values (two different values, one for the diagonal values, and the other 
		// for the non diagonal values
		for (short i = 0; i < Constants.NUM_MOVIES; i++){

			for (j2 = 0; j2 < values[i].length; j2++){
				
				if (j2 == 0){
					diagonalSum += (double)values[i][j2];
				}
				else{
					nonDiagonalSum += (Math.round((double)values[i][j2] * 100000)/100000);
					
					if (values[i][j2] != 0){					
						nonDiagonalElementsNum++;
					}
				}
			}
		}
		
		// We calculated only half of the matrix so we need to address the other half to
		avgDiagonal = (double)diagonalSum / (double)Constants.NUM_MOVIES;
		avgNonDiagonal = (double)((double)nonDiagonalSum / (double)(nonDiagonalElementsNum));
		
		System.out.println("finished calculating avg values. took: " + (System.currentTimeMillis() - start) + ". diagonal = " + avgDiagonal + ", non = " + avgNonDiagonal);

		start = System.currentTimeMillis();
		double[][] result = new double[Constants.NUM_MOVIES][];

		System.out.println("Start calculating final A-hat and B-hat values");
		
		for (short i = 0; i < Constants.NUM_MOVIES; i++){
			
			result[i] = new double[values[i].length];
			
			for (j2 = 0; j2 < values[i].length; j2++){				 
				
				if (sizes[i][j2] == 0){
					
					result[i][j2] = (double)((double)(beta * avgNonDiagonal) / (double)beta);
				}
				
				// Check if the current value is a diagonal value (each row in the values and sizes matrices holds the current movie score in place 0)
				else if (j2 == 0){
					result[i][j2] = (double)((double)((values[i][j2] * sizes[i][j2]) + (beta * avgDiagonal)) / (double)(sizes[i][j2] + beta));
				}
				else{
					result[i][j2] = (double)((double)((values[i][j2] * sizes[i][j2]) + (beta * avgNonDiagonal)) / (double)(sizes[i][j2] + beta));
				}
			}

			if (i % 100 == 0 && i > 0){
				System.out.println("finished " + i + " movies. took " + (System.currentTimeMillis() - start));
				start = System.currentTimeMillis();
			}
		
			// Making sure that the space will be released by the GC to save up memory
			values[i] = null;
			sizes[i] = null;
		}
		
		System.out.println("Finished entire process. took " + (System.currentTimeMillis() - startAll));
		
		// Save the final calculation results (equations 11+12) to a file
		FileUtils.saveDataToFile(result, 
				Constants.NETFLIX_OUTPUT_DIR + PredictorProperties.getInstance().getPredictorStringProperty(Predictors.IMPROVED_KNN, PropertyKeys.INTERPOLATION_FILE_NAME, 
						"interpolation/moviesCommonUsersLists-Final.data"));
		
		return result;
	}
	
	public static void main(String[] args) {
		 PreProcessItemViewUsers processClass = new PreProcessItemViewUsers(Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data");
		 processClass.calcFinalValues();
	}
}
