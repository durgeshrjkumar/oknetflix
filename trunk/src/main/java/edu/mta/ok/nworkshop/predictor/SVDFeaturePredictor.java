
package edu.mta.ok.nworkshop.predictor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.PredictorProperties.Predictors;
import edu.mta.ok.nworkshop.PredictorProperties.PropertyKeys;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelRatings;
import edu.mta.ok.nworkshop.utils.FileUtils;

/**
 * Implementation of a regularized singular value decomposition (SVD) which 
 * is used to compute a low-rank approximation of the original partial rating matrix.
 * 
 * The technique was originally suggested by Simon Funk (Brandyn Webb) in the following 
 * <a href="http://sifter.org/~simon/journal/20061211.html">blog</a>.
 * 
 * The code in this class is a java implementation of a code originally written in python and published in the following
 * <a href="http://code.google.com/p/pyrsvd/">site</a>. 
 * 
 */
public class SVDFeaturePredictor implements Predictor, SVDPredictor {

	private static final int TRAIN_NUM = Constants.TRAIN_RATINGS_NUM;
	
	private final static int DEFAULT_FEATURES_NUM = 10;

	private final static int DEFAULT_MAX_EPOCHS = 100;

	private final static double LEARNING_RATE = 0.001;

	private final static double K_m = 0.011;

	private final static double K_u = 0.011;

	private final static double MIN_IMPROVEMENT = 0.00001;

	// Temporary features matrices that allows us to get maximum performance speed while 
	// calculating the features matrices. When saving the calculated matrices into a file, 
	// the temporary matrices are converted into float in order to save memory consumption 
	// when using the data for predictions (it doesn't affect the algorithm's RMSE).
	// Notice that using float array instead of byte arrays when calculating the features decreases
	// the performance dramatically.
	private double[] UserFeaturesTemp = new double[DEFAULT_FEATURES_NUM * Constants.NUM_USERS];
	private double[] MovieFeaturesTemp = new double[DEFAULT_FEATURES_NUM * Constants.NUM_MOVIES];
	
	private float[] UserFeatures = null;
	private float[] MovieFeatures = null;

	private UserIndexedModel userModel;
	
	private int[] userIds = null;
	
	private short[] movieIds = null;
	
	private byte[] ratings = null;

	private int featuresNum;
	
	private int maxEpochsNum;

	public SVDFeaturePredictor() {
		this(PredictorProperties.getInstance().getPredictorIntProperty(Predictors.SVD, PropertyKeys.FEATURES_NUM, DEFAULT_FEATURES_NUM),
		PredictorProperties.getInstance().getPredictorIntProperty(Predictors.SVD, PropertyKeys.MAX_EPHOCS_NUM, DEFAULT_MAX_EPOCHS));
	}
	
	public SVDFeaturePredictor(int featuresNum) {
		this(featuresNum,
			PredictorProperties.getInstance().getPredictorIntProperty(Predictors.SVD, PropertyKeys.MAX_EPHOCS_NUM, DEFAULT_MAX_EPOCHS));
	}
	
	public SVDFeaturePredictor(int featuresNum, int maxEphocsNum) {
		this(true, featuresNum, maxEphocsNum);
	}
	
	private SVDFeaturePredictor(boolean calculateFeatures, int featuresNum, int maxEphocsNum){
		
		this.maxEpochsNum = maxEphocsNum;
		
		initModel();
		
		if (calculateFeatures){
			this.featuresNum = featuresNum;
			
			int startingInd;
			
			UserFeaturesTemp = new double[this.featuresNum * Constants.NUM_USERS];
			MovieFeaturesTemp = new double[this.featuresNum * Constants.NUM_MOVIES];
			
			// Initializing the features arrays
			for (int i = 0; i < this.featuresNum; i++) {
				
				for (int j = 0; j < Constants.NUM_USERS; j++) {
					
					startingInd = j * this.featuresNum;
					
					UserFeaturesTemp[i + startingInd] = initValue();
				}
				
				for (int j = 0; j < Constants.NUM_MOVIES; j++) {
	
					startingInd = j * this.featuresNum;
				
					MovieFeaturesTemp[i + startingInd] = initValue();
				}
			}

			calculateFeatures();
			saveMatrices(Constants.NETFLIX_OUTPUT_DIR + "SVD/svd-" + this.featuresNum + "-features.data");
		}
	}
	
	/**
	 * Initialize the train model data
	 */
	private void initModel(){

		System.out.println("start initializing model data");
		
		userModel = new UserIndexedModelRatings();
		
		Object[] model = userModel.getModelArray();
		
		userIds = (int[])model[0];
		movieIds = (short[])model[1];
		ratings = (byte[])model[2];
		
		System.out.println("Finish initializing model data");
	}
	
	/**
	 * Convert the calculated temporary features matrices from double into a float array
	 */
	private void initFeatures(){
		
		// Make sure that the temp features arrays had been initialized
		if (UserFeaturesTemp == null || MovieFeaturesTemp == null){
			throw new NullPointerException("Features temp arrays are null, can't convert to float arrays");
		}
		
		UserFeatures = new float[UserFeaturesTemp.length];
		
		for (int i=0; i < UserFeaturesTemp.length; i++){
			UserFeatures[i] = (float)UserFeaturesTemp[i];
		}
		
		// Remove the pointer so that the GC will release the memory  
		UserFeaturesTemp = null;
		
		MovieFeatures = new float[MovieFeaturesTemp.length];
		
		for (int i=0; i < MovieFeaturesTemp.length; i++){
			MovieFeatures[i] = (float)MovieFeaturesTemp[i];
		}
		
		// Remove the pointer so that the GC will release the memory		
		MovieFeaturesTemp = null;
	}
	
	/**
	 * Saves the pre calculated features matrices into a file
	 * 
	 * @param filename a path to the file the features matrices should be saved in
	 */
	private void saveMatrices(String filename) {
		
		// Convert the calculated matrices from double into float in order to save disk memory
		// consumption
		if (UserFeatures == null || MovieFeatures == null){
			initFeatures();
		}
		
		ObjectOutputStream oos = FileUtils.getObjectOutputStream(filename);
		boolean saveFile = false;
		
		if (oos != null){
		
			try {
				
				System.out.print("Blitting matrices to a file...");
				
				// Write the features number to the file
				oos.writeInt(featuresNum);
				
				// Write the arrays to the file
				oos.writeObject(UserFeatures);
				oos.writeObject(MovieFeatures);
				
				saveFile = true;
				
				System.out.println("done");
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally{
				FileUtils.outputClose(oos);
			}
		}
		
		if (!saveFile){
			System.err.println("Error saving model into file " + filename + ". see previous messages");
		}
	}

	/**
	 * Calculate a random value that will be used to fill the features matrices 
	 * 
	 * @return (global mean rating / features num) + (a random value between (-0.005) and (0.005))
	 */
	private double initValue() {	
		
		double globalAverage = 3.0663f;
		
		double r = 0.005;
		
		// Convert the random noise from 0 - 1 to be from (-r) - r
		double noise = (Math.random() / (1 / (2 * r))) - r;
		
		return Math.sqrt(globalAverage / this.featuresNum) + noise;
	}
	
	/**
	 * Calculate the movie and user features matrices
	 */
	private void calculateFeatures() {
		double rmse_last = 2.0, rmse = 2.0;
		long start = System.currentTimeMillis(), startEpoch;
		double squaredError;
		double p;
		int k;
		double err;
		int numRatings;
		
		for (int epoch = 0; (epoch < maxEpochsNum) || (rmse <= rmse_last - MIN_IMPROVEMENT); epoch++) {
			
			rmse_last = rmse;
			
			startEpoch = System.currentTimeMillis();
			
			squaredError = 0.0;
			numRatings = 0;
			int startingIndMovie;
			int startingIndUser;
			
			double uf, mf;
			
			// Train the model
			
			for (int modelIndex = 0; modelIndex < TRAIN_NUM; modelIndex++) {
				numRatings++;
					
				// Getting prediction
				p = 0.0;
				
				startingIndMovie = this.featuresNum * (movieIds[modelIndex] - 1);
				startingIndUser = this.featuresNum * userIds[modelIndex];
				
				for (k=0; k < this.featuresNum; k++){

					// Add contribution of current feature
					p += MovieFeaturesTemp[k + startingIndMovie] * UserFeaturesTemp[k + startingIndUser];
				}
				
				err = ratings[modelIndex] - p;
				squaredError += err * err;
					
				for (int f = 0; f < this.featuresNum; f++) {
		
					// Cross-train the features
					uf = UserFeaturesTemp[f + startingIndUser];
					mf = MovieFeaturesTemp[f + startingIndMovie];
					
					UserFeaturesTemp[f + startingIndUser] += LEARNING_RATE * (err * mf - K_u * uf);
					MovieFeaturesTemp[f + startingIndMovie] += LEARNING_RATE * (err * uf - K_m * mf);
				}
				
			}
			
			rmse = Math.sqrt(squaredError / numRatings);
			
			if ((rmse_last - rmse < MIN_IMPROVEMENT) || (epoch >= maxEpochsNum)){
				
				if (rmse_last - rmse < MIN_IMPROVEMENT){
					System.out.println("Early stopping\nRelative improvement " + (rmse_last-rmse));
				}
				
                break;
			}
			
			System.out.println("Epoch took " + (System.currentTimeMillis() - startEpoch));
			System.out.printf("\t<set error='%f' e='%d'/>\n", rmse, epoch);
		}
		
		System.out.println("Feature calculation took " + (double)((double)(System.currentTimeMillis() - start) / (double)(60 * 1000)) + " minutes");
	}
	
	/**
	 * Create a new instance of the class and loads the features matrices from a given file
	 * 
	 * @param fileName full path to the file that the features matrices should be loaded from
	 * @return a new instance of the class containing pre-calculated features loaded from a file
	 */
	public static SVDFeaturePredictor getPredictor(String fileName){
		SVDFeaturePredictor retVal = new SVDFeaturePredictor(false, -1, 
			PredictorProperties.getInstance().getPredictorIntProperty(Predictors.SVD, PropertyKeys.MAX_EPHOCS_NUM, DEFAULT_MAX_EPOCHS));
		retVal.loadFeaturesFromFile(fileName);
		
		return retVal;
	}
	
	/**
	 * Loads a pre-calculated features matrices from a given file 
	 * 
	 * @param fileName a binary file that holds the features matrices
	 */
	private void loadFeaturesFromFile(String fileName){
		ObjectInputStream ois = null;
		
		System.out.println("Start loading features data from file");
		
		try{
			ois = new ObjectInputStream(new FileInputStream(fileName));
			featuresNum = ois.readInt();
			UserFeatures = (float[])ois.readObject();
			MovieFeatures = (float[])ois.readObject();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
		}
		finally{
			FileUtils.outputClose(ois);			
		}			
		
		System.out.println("Finish loading " + featuresNum + " features from file");
	}
	
	@Override
	public double predictRating(int userID, short movieID, int probeIndex) {

		double sum = 0;

		int userIndex = userModel.getUserIndex(userID) * this.featuresNum;
		int movieIndex = (movieID - 1) * this.featuresNum;

		for (int feature = 0; feature < this.featuresNum; feature++) {
			sum += MovieFeatures[feature + movieIndex]
					* UserFeatures[feature + userIndex];
		}
		if (sum < 1.0)
			sum = 1.0;
		else if (sum > 5.0)
			sum = 5.0;
		return sum;
	}
	
	@Override
	public int getFeaturesNum() {
		return featuresNum;
	}

	@Override
	public float[] getMovieFetures() {
		return MovieFeatures;
	}

	@Override
	public float[] getUserFeatures() {
		return UserFeatures;
	}

	@Override
	public double predictRating(int userID, short movieID, String date, int probeIndex) {
		return predictRating(userID, movieID, probeIndex);
	}
	
	public static void main(String[] args) {
		SVDFeaturePredictor predictor = new SVDFeaturePredictor(10);
		
		double rmse = PredictionTester.getProbeError(predictor, 
				Constants.NETFLIX_OUTPUT_DIR + "Predictions/SVD-10.txt", Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PROBE_FILE_NAME);	
	}
}