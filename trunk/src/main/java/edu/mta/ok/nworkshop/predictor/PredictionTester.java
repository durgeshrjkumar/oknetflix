package edu.mta.ok.nworkshop.predictor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.RMSECalculator;
import edu.mta.ok.nworkshop.utils.FileUtils;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * An helper class that holds utility methods for calculating a predictor RMSE  
 */
public class PredictionTester {

	/**
	 * Calculate prediction RMSE score for predictions loaded from a given text file.
	 * The RMSE is calculated by comparing the predictions against Netflix probe data set. 
	 * 
	 * @param fileName a path to the text file containing prediction values 
	 * @return prediction accuracy (measured by RMSE) for predictions loaded from the given text file. 
	 * the RMSE is calculated by comparing the predictions against Netflix probe data set.
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static double getErrorFromPredictionsFile(String fileName) throws NumberFormatException, IOException{
		
		RMSECalculator rmseCalc = null;
		
		if (fileName != null && !fileName.isEmpty()){
				
			rmseCalc = new RMSECalculator();
			
			// Loads the probe data
			Object[] data = ModelUtils.loadProbeData(Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PROBE_FILE_NAME);			
			byte[] trainingRatings = (byte[])data[2];
			data = null;
			
			System.out.println("Start reading prediction file");
				
			final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			
			try{
				String line;
				int counter = 0;
					
				// Read predictions from the current file and calculate RMSE
				while ((line = reader.readLine()) != null) {
					rmseCalc.addErrorElement(Double.parseDouble(line), trainingRatings[counter]);
					counter++;
				}
			}			
			finally{
				FileUtils.outputClose(reader);
			}
		}
		
		System.out.println("Finished reading predictions file");
		
		return rmseCalc.getFinalScore();
	}
	
	/**
	 * Get the probe RMSE by using a given prediction class to predict the ratings
	 * 
	 * @param predictionClass predicts the missing ratings in the probe file
	 * @param probeFileName name of a binary file containing the probe data
	 * @return the calculated RMSE on the probe file when using the given prediction class
	 */
	public static double getProbeError(Predictor predictionClass) {
		return getProbeError(predictionClass, null, Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PROBE_FILE_NAME);
	}

	/**
	 * Get the probe RMSE by using a given prediction class to predict the ratings
	 * 
	 * @param predictionClass predicts the missing ratings in the probe file
	 * @param fileName name of a text file the algorithm predictions will be saved in. 
	 * in case the value is null the predictions won't be saved in a file.
	 * @param probeFileName name of a binary file containing the probe data
	 * @return the calculated RMSE on the probe file when using the given prediction class
	 */
	public static double getProbeError(Predictor predictionClass, String fileName, String probeFileName) {
		
		int[] trainingUserIDs = null;
		short[] trainingMoviesIDs = null;
		byte[] trainingRatings = null;
		
		// Loads the probe data from the given file
		Object[] data = ModelUtils.loadProbeData(probeFileName);
		trainingUserIDs = (int[])data[0];
		trainingMoviesIDs = (short[])data[1];
		trainingRatings = (byte[])data[2];
		
		// Calculate the error based on the probe data

		final int trainingLength = trainingUserIDs.length;
		
		double[] predictions = new double[trainingUserIDs.length];
		RMSECalculator rmseCalc = new RMSECalculator();

		for (int i = 0; i < trainingLength; i++) {
			double guess = predictionClass.predictRating(trainingUserIDs[i],
					trainingMoviesIDs[i], i);

			// Clip the prediction value
			if (guess > 5){
				guess = 5;
			}
			
			if  (guess < 1){
				guess = 1;
			}
			
			rmseCalc.addErrorElement(guess, trainingRatings[i]);
			predictions[i] = guess;
			
			if (i % 100000 == 0 && i > 0){
				System.out.println("Finished " + i + " predictions");
			}
		}
		
		if (fileName != null && !fileName.isEmpty()){
			savePredictionsToFile(fileName, predictions);
		}
		
		System.out.println("RMSE = " + rmseCalc.getFinalScore());
		
		return rmseCalc.getFinalScore();
	}
	
	/**
	 * Saves an array of predictions values into a given binary file
	 * 
	 * @param fileName name of a file the predictions will be saved in
	 * @param predictions array of double values predicted for the probe data set 
	 */
	private static void savePredictionsToFile(String fileName, double[] predictions){
		if (fileName != null && !fileName.isEmpty() && 
			predictions != null && predictions.length > 0){
			
			System.out.println("Start saving predictions to file");
			
			PrintWriter out = null;
			try {
				out = new PrintWriter(new FileWriter(fileName));

				for (double prediction : predictions) {
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
			
			System.out.println("Finished saving predictions to file " + fileName);
		}
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		String fileName = Constants.NETFLIX_OUTPUT_DIR + "predictions/svd-96.txt";
		System.out.println(fileName + " predictions RMSE = " + PredictionTester.getErrorFromPredictionsFile(fileName));
	}
}
