package edu.mta.ok.nworkshop.globaleffects;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.RMSECalculator;
import edu.mta.ok.nworkshop.utils.FileUtils;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * An abstract class that all the different global effects classes inherit from.
 * The class holds the base implementation for all global effects described in Bellkor abstract section 3 
 * {@link http://public.research.att.com/~volinsky/netflix/cfworkshop.pdf}
 */
public abstract class EffectAbstract {

	// MxU matrices
	protected int[][] movieIndexedUserIDs = null;

	protected byte[][] movieIndexedRatings = null;
	
	protected double[][] movieIndexedResiduals = null;
	
	protected short[][] movieIndexedDates = null;
	
	protected short[][] userIndexedMovieIDs = null;

	protected byte[][] userIndexedRatings = null;
	
	protected double[][] userIndexedResiduals = null;
	
	protected short[][] userIndexedDates = null;
	
	// Training data
	protected int[] trainingUserIDs;

	protected short[] trainingMoviesIDs;

	protected byte[] trainingRatings;
	
	protected double[] trainingPredictions;
	
	protected String userModelFileName;
	
	protected String movieModelFileName;
	
	private String movieResidualsFileName;
	
	private String userResidualsFileName;
	
	protected HashMap<Integer, Integer> userIndices = new HashMap<Integer, Integer>();
	
	protected int alpha;
	
	protected double[] teta;
	
	protected double[] residuals;
	
	private RMSECalculator calculator = new RMSECalculator();
	
	public static final String MAIN_DIR_NAME = "globalEffects\\";
	
	public static final String MOVIE_INDEXED_RESIDUAL_FILE_NAME = MAIN_DIR_NAME + "movieIndexedResidual";
	
	public static final String USER_INDEXED_RESIDUAL_FILE_NAME = MAIN_DIR_NAME + "userIndexedResidual";
	
	protected enum WantedModel{
		USER_INDEXED_MODEL, MOVIE_INDEXED_MODEL, BOTH;
	}
	
	public EffectAbstract(String userModelFileName, String movieModelFileName, String movieResidualFileName, 
			String userResidualFileName, String probeFileName, int alpha) {
		super();
		this.userModelFileName = userModelFileName;
		this.movieModelFileName = movieModelFileName;
		this.movieResidualsFileName = movieResidualFileName;
		this.userResidualsFileName = userResidualFileName;
		this.alpha = alpha;				
		
		initModels();
		initProbe(probeFileName);
		initTeta();
	}

	/**
	 * Initialize the models data according to the wanted model type (movie/user indexed or both)
	 * 
	 *  @see #getWantedModel()
	 */
	private void initModels(){
			
		if (getWantedModel() != null){
			if (getWantedModel().equals(WantedModel.MOVIE_INDEXED_MODEL) || getWantedModel().equals(WantedModel.BOTH)){
				initMovieIndexedModel();
			}
							
			if (getWantedModel().equals(WantedModel.USER_INDEXED_MODEL) || getWantedModel().equals(WantedModel.BOTH)){
				initUserIndexedModel();
			}
		}
		
		System.out.println("finished initializing models");
		
		userIndices = FileUtils.loadDataFromFile(PredictorProperties.getInstance().getUserIndicesMappingFile());
	}

	/**
	 * Initialize the user indexed model data according to the model file name attribute
	 * 
	 *  @see #userModelFileName
	 *  @see #loadModelDates()
	 */
	private void initUserIndexedModel() {
		
		if (userIndexedMovieIDs != null){
			return;
		}
		
		Object[] retVal;
		retVal = ModelUtils.loadUserIndexedModel(userModelFileName, loadModelDates());

		userIndexedMovieIDs = (short[][])retVal[0];
		
		if (loadModelRatings()){
			userIndexedRatings = (byte[][])retVal[1];
		}
		
		if (loadModelDates()){
			userIndexedDates = (short[][])retVal[2];
		}
		
		if (userResidualsFileName != null && userResidualsFileName.length() > 0){
			userIndexedResiduals = FileUtils.loadDataFromFile(userResidualsFileName);
		}		
	}

	/**
	 * Initialize the movie indexed model data according to the model file name attribute
	 * 
	 *  @see #movieModelFileName
	 *  @see #loadModelDates()  
	 */
	private void initMovieIndexedModel() {
		
		if (movieIndexedUserIDs != null){
			return;
		}
		
		Object[] retVal;
		retVal = ModelUtils.loadMovieIndexedModel(movieModelFileName, this.loadModelDates(), false);

		movieIndexedUserIDs = (int[][])retVal[0];
		
		if (loadModelRatings()){
			movieIndexedRatings = (byte[][])retVal[1];
		}
		
		if (loadModelDates()){
			movieIndexedDates = (short[][])retVal[2];
		}

		// Load the residuals from the dedicated file
		retVal[1] = null;
		
		if (movieResidualsFileName != null && movieResidualsFileName.length() > 0){
			movieIndexedResiduals = FileUtils.loadDataFromFile(movieResidualsFileName);
		}
	}
	
	/**
	 * Initialize the probe data
	 * 
	 * @param filename The name of the file want to load the probe data from
	 */
	private void initProbe(String filename){
		
		if (filename == null || filename.length() == 0){
			return;
		}
		
		Object[] retVal = ModelUtils.loadEffectProbeData(filename);
		
		trainingUserIDs = (int[])retVal[0];
		trainingMoviesIDs = (short[])retVal[1];
		trainingRatings = (byte[]) retVal[2];
		trainingPredictions = (double[])retVal[3];
		residuals = (double[]) retVal[4];
	}
	
	/**
	 * The main method for effect calculation.
	 * Calculate an effect, saves the calculated data into a bin file and print out the effect's RMSE.
	 * @return The global effect RMSE
	 * 
	 * @see #calculateEffect()
	 * @see #updatePredictions()
	 * @see #saveData()
	 * @see #calcRMSE()
	 * @see #saveResidualModels()
	 */
	public double startEffectCalculation(){
		
		System.out.print("Start calculating effect...");
		
		calculateEffect();
		
		System.out.println("Done");
		
		updatePredictions();
		
		System.out.println("Finish updating predictions");
		
		saveData();
		
		System.out.println("Finish saving data");
		
		calcRMSE();
		
		System.out.print("Saving residual data ... ");
		
		saveResidualModels();
		
		System.out.println("Done.");
		
		return calculator.getFinalScore();
	}
	
	/**
	 * Update the residuals matrices and save them to a binary file
	 * 
	 * @see #updateMovieResidualsMatrix()
	 * @see #updateUserResidualsMatrix()
	 */
	protected void saveResidualModels(){

		// Check which model is already in the memory to save space
		if (getWantedModel().equals(WantedModel.MOVIE_INDEXED_MODEL)){
			updateMovieResidualsMatrix();
			saveMovieIndexedResiduals();
			updateUserResidualsMatrix();
			saveUserIndexedResiduals();
			
		}
		else{
			updateUserResidualsMatrix();
			saveUserIndexedResiduals();
			updateMovieResidualsMatrix();
			saveMovieIndexedResiduals();
		}
	}

	protected final void saveUserIndexedResiduals() {
		FileUtils.saveDataToFile(userIndexedResiduals, Constants.NETFLIX_OUTPUT_DIR + USER_INDEXED_RESIDUAL_FILE_NAME + this.getClass().getSimpleName() + ".data");
		
		// Free space
		userIndexedResiduals = null;
		userIndexedMovieIDs = null;
		userIndexedDates = null;
	}

	protected final void saveMovieIndexedResiduals() {
		FileUtils.saveDataToFile(movieIndexedResiduals, Constants.NETFLIX_OUTPUT_DIR + MOVIE_INDEXED_RESIDUAL_FILE_NAME + this.getClass().getSimpleName() + ".data");
		
		// Free space
		movieIndexedResiduals = null;
		movieIndexedUserIDs = null;
		movieIndexedDates = null;
	}
	
	/**
	 * Updates the movie residual matrix by substracting the teta array from the previous residual matrix, and save the result to a file
	 */
	private void updateMovieResidualsMatrix(){
		
		initMovieIndexedModel();
		
		int counter = 0;
		
		for (double[] residuals : movieIndexedResiduals){
			
			int ind = 0;
			
			for (double residual : residuals){
				
				movieIndexedResiduals[counter][ind] = residual - getRatingTetaVal(movieIndexedUserIDs[counter][ind], (short)(counter + 1), WantedModel.MOVIE_INDEXED_MODEL, ind);
				ind++;
			}
			
			counter++;
		}
	}
	
	/**
	 * Updates the user residual matrix by substracting the teta array from the previous residual matrix, and save the result to a file
	 */
	private void updateUserResidualsMatrix(){
		
		initUserIndexedModel();
		
		int counter = 0;
		
		for (double[] residuals : userIndexedResiduals){
			
			int ind = 0;
			
			for (double residual : residuals){
				
				// In case the residual is indexed according to the movie id then we need to fetch the data by movie id of 
				// the current rating
					
				userIndexedResiduals[counter][ind] = residual - getRatingTetaVal(counter, userIndexedMovieIDs[counter][ind], WantedModel.USER_INDEXED_MODEL, ind); 
				ind++;
			}
			
			counter++;
		}
	}
		
	/**
	 * Calculate the effects data according to the current effect formula
	 */
	protected abstract void calculateEffect(); 
	
	/**
	 * Initialize the teta array
	 */
	protected abstract void initTeta();
	
	/**
	 * Calculate tetaI value according to the effect's formula
	 * 
	 * @param n number of ratings
	 * @param tetaHat the teta hat value used in the effect's formula
	 * @param objectId the index of the calculated object in the tetaI index
	 */
	protected void calculateTetaI(int n, float tetaHat, int objectId){
		
		teta[objectId] = ((n * tetaHat) / (n + alpha));
	}
	
	/**
	 * Calculate tetaI value according to the effect's formula
	 * 
	 * @param n number of ratings
	 * @param tetaHat the teta hat value used in the effect's formula
	 * @param objectId the index of the calculated object in the tetaI index
	 */
	protected void calculateTetaI(int n, double tetaHat, int objectId){
		
		teta[objectId] = ((n * tetaHat) / (n + alpha));
	}
	
	/**
	 * Return a prediction value for a given entry in the probe file
	 * 
	 * @param trainingInd index of an entry in the probe file
	 * @return prediction value for the given entry index
	 */
	protected double getPrediction(int trainingInd){
		double tetaVal = getProbeTetaVal(trainingInd);
		
		return tetaVal + trainingPredictions[trainingInd] ;
	}

	/**
	 * Return the effect's teta value used for prediction
	 * 
	 * @param trainingInd the index of the predicted entry in the probe file
	 * @return a double value that is the teta value used for prediction
	 */
	protected double getProbeTetaVal(int trainingInd) {
		double tetaVal = (getWantedModel().equals(WantedModel.MOVIE_INDEXED_MODEL)) ? teta[trainingMoviesIDs[trainingInd] - 1] : 
			teta[userIndices.get(trainingUserIDs[trainingInd])];
		return tetaVal;
	}
	
	protected double getRatingTetaVal(int userId, short movieId, WantedModel model, int modelInd) {
		
		// Get the teta value according to the current effect calculated model (in case it's a userIndex we'll fetch by the userId, otherwise by the
		// movieId)
		double tetaVal = (getWantedModel().equals(WantedModel.MOVIE_INDEXED_MODEL)) ? teta[movieId - 1] : 
			teta[userId];
		return tetaVal;
	}
	
	/**
	 * Calculate the predictions using the calculated effect data
	 */
	protected void updatePredictions(){
		
		int counter = 0;
		
		do{
			trainingPredictions[counter] = getPrediction(counter);
			
			if (trainingPredictions[counter] > 5){
				trainingPredictions[counter] = 5;
			}			
			else if (trainingPredictions[counter] < 1){
				trainingPredictions[counter] = 1;
			}
			
			counter++;
		} while (counter < trainingRatings.length);
	}

	/**
	 * Saves the calculated effect data into a binary file
	 */
	private void saveData(){
		
		boolean success = ModelUtils.saveEffectData(Constants.NETFLIX_OUTPUT_DIR + MAIN_DIR_NAME + getClass().getSimpleName() + ".data", trainingUserIDs, trainingMoviesIDs, trainingRatings, 
				trainingPredictions, teta);
		
		if (!success){
			System.out.println("Error while saving effect data");
		}
		else{
			System.out.println("Effect data was saved successfully");
		}
	}
	
	/** 
	 * @return The type of model the class need to load (user index, movie index or both)
	 */
	protected abstract WantedModel getWantedModel();
	
	/**
	 * 
	 * @return true if the effect class needs to load rating dates data or false otherwise
	 */
	protected abstract boolean loadModelDates();
	
	/**
	 * Calculate the RMSE achieved by the effect predictions
	 */
	private void calcRMSE(){
		calculator.reset();
		
		int counter = 0;
		
		for (byte rating : trainingRatings){
			calculator.addErrorElement(this.trainingPredictions[counter], rating);
			counter++;
		}
		
		System.out.println("RMSE = " + calculator.getFinalScore());
	}
	
	/**
	 * 
	 * @return true if the effect class needs to load ratings data or false otherwise
	 */
	protected boolean loadModelRatings(){
		return false;
	}
	
	/**
	 * Export the effect's predictions to a text file
	 * 
	 * @param fileName a full path to a file the predictions will be saved in
	 */
	public void exportPredictions(String fileName){
		if (fileName != null && !fileName.isEmpty()){
			
			System.out.print("Start saving predictions to file " + fileName + "...");
			
			PrintWriter out = null;
			try {
				out = new PrintWriter(new FileWriter(fileName));

				for (double prediction : trainingPredictions) {
					out.println(prediction);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally{
				FileUtils.outputClose(out);
			}
			
			System.out.println("Done");
		}
	}
	
	public static void main(String[] args) {
		Effect2 effect = new Effect2(Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\movieIndexedResidual-Effect1.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\userIndexedResidual-Effect1.data", 
				Constants.NETFLIX_OUTPUT_DIR + "globalEffects\\effect1.data");
		
		effect.startEffectCalculation();
	}
}
