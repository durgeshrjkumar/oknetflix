package edu.mta.ok.nworkshop.predictor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;
import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.RMSECalculator;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * Simple class that combines calculated predictions given by different algorithms.
 * 
 * Every predictor is given a weight (calculated using a simple linear equation solver), when the 
 * final prediction is calculated by summing the predictors predictions multiplied by their weight.
 */
public class CombinePredictions {
	
	private double[][] trainingRatings;
	
	public CombinePredictions(){
		this(Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PROBE_FILE_NAME);
	}
	
	public CombinePredictions(String probeFileName){
		
		// Initialize the probe data
		trainingRatings = new double[Constants.PROBES_NUM][1];
	    int counter = 0;
	    
	    // Initialize the probe ratings array	    
	    Object[] data = ModelUtils.loadProbeData(probeFileName);
	    for (byte rating : (byte[])data[2]){
	    	trainingRatings[counter][0] = rating;
	    	counter++;
	    }
	    
	    System.out.println("Finished loading probe data");
	}
	
	/**
	 * Predict probe ratings by combining data from given prediction files 
	 * 
	 * @param fileNames array of calculated prediction file names used for the final combined prediction
	 * @return The RMSE score achieved by combining different prediction files
	 * @throws NumberFormatException in case of an error converting prediction number value 
	 * loaded from a given prediction file
	 * @throws IOException in case of an error reading a prediction file
	 */
	public void startPrediction(String[] fileNames) throws NumberFormatException, IOException{
		List<File> files = new ArrayList<File>();
		
	    // Iterate on the given file names and load every file
	    for (String fileName : fileNames){
	    	
	    	// Making sure that the directory separator in the file name is valid 
	    	fileName = fileName.replace('\\', '/');
	    
	    	File currFile = new File(fileName);
	    	
	    	if (!currFile.exists()){
	    		System.err.println("Send file " + fileName + " does not exists, exiting ...");
	    		System.exit(1);
	    	}
	    	
	    	files.add(currFile);
	    }
	    
	    double rmse = startPrediction(files.toArray(new File[0]));
	    System.out.println("RMSE = " + rmse);
	}
	
	public double startPrediction(File[] files) throws NumberFormatException, IOException{
		int fileCount = 0;
		
		double[][] A_array = new double[1408395][files.length];

	    // Iterate on the given file names and load every file
	    for (File currFile : files){
	    	
	    	System.out.print("Loading predictions from file " + currFile.getName() + "...");
	    	
		    int counter = 0;
		    
		    final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(currFile)));
			String line;
			
			// Read predictions from the current file
			while ((line = reader.readLine()) != null) {

				A_array[counter][fileCount] = Double.parseDouble(line);
				
				counter++;
			}
			
			fileCount++;
			
			System.out.println("Done!");
	    }
	    
	    System.out.println("Finished loading prediction files");
	    
	    // Build a matrix containing all the algorithms predictions 
	    Matrix A = new Matrix(A_array);
	    
	    // Build a matrix containing the real ratings given to the probe data
	    Matrix b = new Matrix(trainingRatings);
	    
	    // Calculate every predictor weight by solving a linear equation
	    Matrix x = A.solve(b);

	    RMSECalculator rmseCalc = new RMSECalculator();
	    double currGuess;
	    
	    // Predict
	    for (int i = 0; i < A_array.length; i++) {
	    	
	    	currGuess = 0;
	    	
	    	for (int j = 0; j < A_array[i].length; j++) {
				currGuess += (A_array[i][j] * x.get(j, 0));
			}
	    	
			rmseCalc.addErrorElement(currGuess, new Double(trainingRatings[i][0]).byteValue());
		}
	    
	    return rmseCalc.getFinalScore();
	}
	
	public static void main(String[] args) throws IOException {
	  
	  if (args == null || args.length == 0){
		  System.out.println("No file names had been sent, exiting ...");
	  }
	  else{
		  CombinePredictions cp = new CombinePredictions();
		  cp.startPrediction(args);
	  }
	}
}
