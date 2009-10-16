package edu.mta.ok.nworkshop.predictor;

import java.util.Arrays;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelRatings;
import edu.mta.ok.nworkshop.utils.FileUtils;

/**
 * A KNN predictor class that uses a pre-calculated SVD features in order to create the KNN
 * similarity model as cosine similarity between every two movies in the following way:
 * 
 * s(v_j, v_j2) = (v_j^t * v_j2) / ||v_j|| * ||v_j2||
 * 
 * when v_j and v_j2 are the movies features from the SVD predictor
 * 
 * The approach is explained in section 3.5 in the following abstract: 
 * <a href="http://rainbow.mimuw.edu.pl/~ap/ap_kdd.pdf">Improving regularized singular value decomposition for collaborative filtering</a> 
 * 
 * @see SVDPredictor#getMovieFetures()
 * 
 */
public class KNNSVDPredictor implements Predictor{

	private SVDPredictor svdPredictor;
	
	private double[][] similarityModel = new double[Constants.NUM_MOVIES - 1][];
	
	UserIndexedModel userModel;
	
	public KNNSVDPredictor(SVDPredictor svdPredictor) {
		super();
		this.svdPredictor = svdPredictor;
		
		calculateSimilarities();
		saveModel(Constants.NETFLIX_OUTPUT_DIR + "KNN/svdSinSimilarity.data");
	}
	
	/**
	 * Calculate the neighbor similarity values
	 */
	private void calculateSimilarities(){
		
		float[] movieFeatures = svdPredictor.getMovieFetures();
		int featuresNum = svdPredictor.getFeaturesNum();

		double[] moviesNorms = new double[Constants.NUM_MOVIES];
		Arrays.fill(moviesNorms, 0);
		int movie1FeatureStartIndex = 0, movie2FeatureStartIndex = 0, similarityIndex = 0;
		int movie2Index;
		long start = System.currentTimeMillis();
		
		// Calculate the movies norms
		for (int i=0; i < Constants.NUM_MOVIES; i++){
			movie1FeatureStartIndex = i * featuresNum;
			
			for (int j = 0; j < featuresNum; j++) {
				moviesNorms[i] += (movieFeatures[j + movie1FeatureStartIndex] * movieFeatures[j + movie1FeatureStartIndex]);
			}
			
			moviesNorms[i] = Math.sqrt(moviesNorms[i]); 
		}
		
		System.out.println("Finish calculating movies norms took " + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		
		// Calculate the sin between every two movies
		for (int i = 0; i < Constants.NUM_MOVIES - 1; i++) {
			movie1FeatureStartIndex = i * featuresNum;
			movie2FeatureStartIndex = movie1FeatureStartIndex + featuresNum;
			movie2Index = i + 1;
			
			similarityModel[i] = new double[Constants.NUM_MOVIES - i - 1];
			Arrays.fill(similarityModel[i], 0);
			
			similarityIndex = 0;
			
			while (movie2FeatureStartIndex < movieFeatures.length){
				
				// Multiply the first and the second movies features
				for (int j = 0; j < featuresNum; j++) {
					similarityModel[i][similarityIndex] += (movieFeatures[movie1FeatureStartIndex + j] * 
							movieFeatures[movie2FeatureStartIndex + j]);
				}
				
				// Calculate the sin between the two movies
				similarityModel[i][similarityIndex] = (double)similarityModel[i][similarityIndex] / (double)(moviesNorms[i] * moviesNorms[movie2Index]);
		
				movie2Index++;
				similarityIndex++;
				movie2FeatureStartIndex += featuresNum;
			}
		}
		
		System.out.println("Finish calculating similarities took " + (System.currentTimeMillis() - start));
		
		// Release the memory, we don't need to svd model for prediction 
		svdPredictor = null;
	}
	
	/**
	 * Saves the similarity model to a given file 
	 * 
	 * @param filename a file the calculated similarity model should be saved in
	 */
	private void saveModel(String filename){
		
		boolean save = FileUtils.saveDataToFile(similarityModel, filename);
		
		if (save){
			System.out.println("Sucessfully save the similarity model to a file");
		}
		else{
			System.err.println("Error saving model into file " + filename + ". see previous messages");
		}
	}
	
	public void setUserModel(UserIndexedModel userModel) {
		this.userModel = userModel;
	}

	@Override
	public double predictRating(int userID, short movieID, String date, int probeIndex) {	
		return predictRating(userID, movieID, probeIndex);
	}

	@Override
	public double predictRating(int userID, short movieID, int probeIndex) {

		double retVal = 0;
		
		if (userModel == null){
			setUserModel(new UserIndexedModelRatings());
		}
		
		short[] movies = userModel.getRatedMovies(userID);
		byte[] ratings = (byte[])userModel.getUserRatings(userID);
		
		double highSim = -10000;
		
		// Get the most similar movie that the user rated
		for (int i = 0; i < movies.length; i++) {
			short movie = movies[i];

			if (highSim < similarityModel[Math.min(movieID - 1, movie-1)][Math.max(movieID - 1, movie-1) - Math.min(movieID - 1, movie-1) - 1]){
				highSim = similarityModel[Math.min(movieID - 1, movie-1)][Math.max(movieID - 1, movie-1) - Math.min(movieID - 1, movie-1) - 1];
				retVal = ratings[i];
			}
		} 
			
		return retVal;
	}

	public static void main(String[] args) {
		KNNSVDPredictor predictor = new KNNSVDPredictor(SVDFeaturePredictor.getPredictor(Constants.NETFLIX_OUTPUT_DIR + 
				"SVD/SVD-256-features.data"));
		
		predictor.setUserModel(new UserIndexedModelRatings());
		double RMSE = PredictionTester.getProbeError(predictor, Constants.NETFLIX_OUTPUT_DIR + "Predictions/KNNRegularSVD.txt", Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PROBE_FILE_NAME);
		
		System.out.println("RMSE = " + RMSE);
	}
}
