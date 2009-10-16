package edu.mta.ok.nworkshop.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.globaleffects.EffectAbstract;
import edu.mta.ok.nworkshop.model.MovieIndexedModel;
import edu.mta.ok.nworkshop.model.MovieIndexedModelRatings;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelRatings;

/**
 * Helper class that provide static methods for saving/loading model data from/to binary files.
 */
public class ModelUtils {

	/**
	 * Loads the movie scores data from a given binary file 
	 * (two MxU sparse matrices that the first holds for each movie an array of all the user ids that rated the movie 
	 *  and the second holds all the rating scores the movie was given)
	 * 
	 * @param fileName the name of the file that holds the data
	 * @param loadDates Mark if we need to load the rating dates
	 * @param floatModel mark if the type of the loaded rating matrix should be float
	 * @return an array with two elements, the first is a MxU int matrix that holds the users ids that rated each movie,
	 * and the other is a MxU byte matrix that holds the rating scores that each movie got. in case of an error reading the
	 * file, the returned array will be empty (will hold 2 uninitialized places)
	 */
	public static Object[] loadMovieIndexedModel(String fileName, boolean loadDates, boolean floatModel){
		ObjectInputStream ois = null;
		Object[] retVal = null;		
		
		if (loadDates){
			retVal = new Object[3];
		}
		else{
			retVal = new Object[2];
		}
		
		try{
		
			ois = new ObjectInputStream(new FileInputStream(fileName));
			retVal[0] = (int[][]) ois.readObject();
			Object model = ois.readObject();
			
			if (floatModel && !(model instanceof float[][])){
				
				// Convert the byte rating matrix to a float rating matrix

				model = convertRatingMatrixToFloat(model);
			}
			
			retVal[1] = model;
			
			if (loadDates){
				retVal[2] = (short[][]) ois.readObject();
			}
			
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
		
		return retVal;
	}
	
	/**
	 * Loads a user indexed model containing residuals after global effect removal   
	 * 
	 * @param fileName a full path to the file the model will be loaded from
	 * @return an array containing an id and residuals matrices
	 */
	public static Object[] loadMovieIndexedResidualsModel(String fileName){
		ObjectInputStream ois = null;
		Object[] retVal = new Object[2];		
		
		
		try{
		
			ois = new ObjectInputStream(new FileInputStream(fileName));
			retVal[0] = (int[][]) ois.readObject();
			retVal[1] = (float[][])ois.readObject();
			
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
		
		return retVal;
	}

	/**
	 * Convert a given ratings model containing byte values to a float value matrix.
	 * 
	 * @param model a ratings model containing byte values
	 * @return a new matrix similar to the given one holding a float representation for every byte
	 * value from the given model matrix
	 */
	public static Object convertRatingMatrixToFloat(Object model) {
		byte[][] modelB = (byte[][])model;
		float[][] modelF = new float[modelB.length][];
		int counter = 0;
		
		for (byte[] ratings : modelB){
			modelF[counter] = new float[ratings.length];
			int ratingInd = 0;
			
			for (byte rating : ratings){
				modelF[counter][ratingInd++] = (float)rating;
			}
			
			counter++;
		}
		
		model = modelF;
		return model;
	}
	
	/**
	 * Loads the user scores data from a given binary file 
	 * (two UxM sparse matrices that the first holds for each user an array of all the movies ids that the user rated
	 *  and the second holds all the rating scores the user gave the movies)
	 * 
	 * @param fileName the name of the file that holds the data.
	 * @param loadDates mark if a dates model matrix should be loaded from the file in addition to the other
	 * models.
	 * @return an array with two elements, the first is a UxM short matrix that containing the movies ids that each user rated,
	 * and the other is a UxM byte matrix containing the rating scores that each movie got. 
	 * in case loadDates is set to true, the array will hold another cell containing a UxM short matrix containing the 
	 * ratings dates.
	 * in case of an error reading the
	 * file, the returned array will be empty (will hold 2/3 uninitialized places).
	 * 
	 */
	public static Object[] loadUserIndexedModel(String fileName, boolean loadDates){
		ObjectInputStream ois = null;
		Object[] retVal = null;
		
		if (loadDates){
			retVal = new Object[3]; 
		}
		else{
			retVal = new Object[2];
		}
		
		try{
			ois = new ObjectInputStream(new FileInputStream(fileName));
			retVal[0] = (short[][]) ois.readObject();
			retVal[1] = (byte[][]) ois.readObject();
			
			if (loadDates){
				retVal[2] = (short[][]) ois.readObject();
			}
			
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
		
		return retVal;
	}
	
	/**
	 * Loads a user indexed model containing residuals after global effect removal.
	 * 
	 * @param fileName a full path to the file the model will be loaded from
	 * @return an array with two elements, the first is a UxM short matrix containing the movies ids that each user rated,
	 * and the other is a UxM byte matrix containing the rating scores that each movie got. 
	 * in case of an error reading the file, the returned array will be empty (will hold 2 uninitialized places).
	 */
	public static Object[] loadUserIndexedResidualModel(String fileName){
		ObjectInputStream ois = null;
		Object[] retVal = new Object[2];
		
		try{
			ois = new ObjectInputStream(new FileInputStream(fileName));
			retVal[0] = (short[][]) ois.readObject();
			retVal[1] = (float[][]) ois.readObject();
			
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
		
		return retVal;
	}
	
	/**
	 * Loads the calculated models used for the Improved KNN prediction (Bellkor interpolation algorithm) from a binary file.
	 * 
	 * @param fileName a path to the binary file that contain the data
	 * @return an array with two elements, the first is a MxM double matrix containing the calculated values,
	 * and the second is a MxM int matrix containing the number of raters two every two movies have in common. 
	 * Notice that both matrices are sparse and contain only the upper diagonal values from the original matrix, meaning that every movie
	 * hold the values for the movies with id bigger than the one he has.
	 * in case of an error reading the file, the returned array will be empty (will hold 2 uninitialized places).
	 */
	public static Object[] loadCommonMoviesScores(String fileName){
		ObjectInputStream ois = null;
		Object[] retVal = new Object[2];
		
		try{
			ois = new ObjectInputStream(new FileInputStream(fileName));
			retVal[0] = (double[][]) ois.readObject();
			retVal[1] = (int[][]) ois.readObject();
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
		
		return retVal;
	}
	
	public static boolean saveMovieScoresData(String fileName, int[][] ids, byte[][] scores){
		ObjectOutputStream oos = FileUtils.getObjectOutputStream(fileName);
		boolean retVal = false;
		
		if (oos != null){
			try{
				oos.writeObject(ids);
				oos.writeObject(scores);
				retVal = true;
			}
			catch (Exception e){
				
			}
			finally{
				FileUtils.outputClose(oos);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Saves the given user indexed model into a binary file. 

	 * @param fileName the name of the file the data will be saved in.
	 * @param movieIds the model's movie ids
	 * @param scores the model's ratings
	 */
	public static boolean saveUserIndexedModel(String fileName, short[][] movieIds, byte[][] scores){
		ObjectOutputStream oos = FileUtils.getObjectOutputStream(fileName);
		boolean retVal = false;
		
		if (oos != null){
			try{
				oos.writeObject(movieIds);
				oos.writeObject(scores);
				retVal = true;
			}
			catch (Exception e){
				
			}
			finally{
				FileUtils.outputClose(oos);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Saves a global effect predictions data to a binary file.
	 * 
	 * @see EffectAbstract
	 * 
	 * @param fileName a path to the file the data should be saved in
	 * @param trainingUserIDs an array containing the user ids from the probe data 
	 * @param trainingMoviesIDs an array containing the movie ids from the probe data
	 * @param trainingRatings the real ratings given for the probe data
	 * @param trainingPredictions the ratings values that the effect predicts 
	 * @param teta the teta values calculated by the effect's
	 * @return true if the data had been saved successfully or false otherwise
	 */
	public static boolean saveEffectData(String fileName, int[] trainingUserIDs, short[] trainingMoviesIDs, 
			byte[] trainingRatings,	 double[] trainingPredictions, double[] teta){
	
		ObjectOutputStream oos = FileUtils.getObjectOutputStream(fileName);
		boolean retVal = false;
		
		if (oos != null){
			try{
				oos.writeObject(trainingUserIDs);
				oos.writeObject(trainingMoviesIDs);
				oos.writeObject(trainingRatings);
				oos.writeObject(trainingPredictions);
				oos.writeObject(teta);
				retVal = true;
			}
			catch (Exception e){
				
			}
			finally{
				FileUtils.outputClose(oos);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Loads a global effect predictions data from a binary file.
	 * 
	 * @see EffectAbstract
	 * 
	 * @param fileName a path to the file the data should be loaded from
	 * @return an array with five elements:
	 * 	1. An int array containing the probe data user ids
	 * 	2. A short array containing the probe data movie ids
	 * 	3. A byte array containing the real ratings values given to the probe data
	 * 	4. The effect's predictions for the probe data
	 * 	5. The effect's teta values 
	 */
	public static Object[] loadEffectProbeData(String fileName){
	
		ObjectInputStream ois = null;
		Object[] retVal = new Object[5];
		
		try{
			ois = new ObjectInputStream(new FileInputStream(fileName));
			retVal[0] = (int[]) ois.readObject();
			retVal[1] = (short[]) ois.readObject();
			retVal[2] = (byte[]) ois.readObject();
			retVal[3] = (double[]) ois.readObject();
			retVal[4] = (double[]) ois.readObject();
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
		
		return retVal;
	}
	
	/**
	 * Loads the probe data from a given file
	 * 
	 * @param fileName the name of the file we want to load the probe data from
	 * @return an object array containing the probe userIds, movieIds, ratings and ratings dates
	 */
	public static Object[] loadProbeData(String fileName){
		ObjectInputStream ois = null;
		Object[] retVal = new Object[4];
		
		try{
			ois = new ObjectInputStream(new FileInputStream(fileName));
			retVal[0] = (int[]) ois.readObject();
			retVal[1] = (short[]) ois.readObject();
			retVal[2] = (byte[]) ois.readObject();
			retVal[3] = (short[]) ois.readObject();
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
		
		return retVal;
	}
	
	/**
	 * Saves the probe data model to a binary file.
	 * 
	 * @param fileName a path to the file the data should be saved in
	 * @param userIDs an array containing the user ids from the probe data 
	 * @param moviesIDs an array containing the movie ids from the probe data
	 * @param ratings an array containing the real ratings given for the probe data
	 * @param dates an array containing the dates the ratings took place in
	 * @return true if the data had been saved successfully or false otherwise 
	 */
	public static boolean saveProbeData(String fileName, int[] userIds, short[] movieIds, byte[] ratings, short[] dates){
		
		ObjectOutputStream oos = FileUtils.getObjectOutputStream(fileName);
		boolean retVal = false;
		
		if (oos != null){
			try{
				oos.writeObject(userIds);
				oos.writeObject(movieIds);
				oos.writeObject(ratings);
				oos.writeObject(dates);
				retVal = true;
			}
			catch(IOException e){
				e.printStackTrace();
			}
			finally{
				FileUtils.outputClose(oos);			
			}			
		}
		
		return retVal;
	}
	
	/**
	 * Converts a given double two dimensional array into a float two dimensional array
	 * 
	 * @param sourceArray a two dimensional double array that we want to convert 
	 * @param savedFile a path to the file the new converted model will be saved in. 
	 * In case the given value is null the converted array won't be saved in a file.
	 * @return two dimensional float array converted from the given double array
	 */
	public static float[][] convertDoubleModelIntoFloat(double[][] sourceArray, String savedFile){
		float[][] retVal = null;
		
		if (sourceArray != null){
			
			retVal = new float[sourceArray.length][];
			int counter = 0;
			
			// Iterate on the given array and convert it to a float array
			for (double[] currLine : sourceArray){
				retVal[counter] = new float[currLine.length];
				int c2 = 0;
				
				for (double currVal : currLine){
					retVal[counter][c2] = (float)currVal;
					c2++;
				}
				
				counter++;
			}
		}
		
		if (savedFile != null){
			FileUtils.saveDataToFile(retVal, savedFile);
		}
		
		return retVal;
	}
	
	/**
	 * Converts a double two dimensional array loaded from a given file into a float two dimensional array
	 * 
	 * @param sourceArrayFile a path to the file the double array is located in
	 * @param savedFile a path to the file the new converted model will be saved in, or null in case the converted array shouldn't be saved in a file.
	 * @return two dimensional float array converted from the given double array
	 */
	public static float[][] convertDoubleModelIntoFloat(String sourceArrayFile, String savedFile){
		
		double[][] originalModel = FileUtils.loadDataFromFile(sourceArrayFile);
		
		return convertDoubleModelIntoFloat(originalModel, savedFile);
	}
	
	/**
	 * Calculate the number of raters every two movies have in common
	 * 
	 * @param movieIndexedModel the data model indexed by the movie IDs
	 * @param userIndexedModel the data model indexed by the user IDs
	 * @return a two dimensional array holding the common raters number. The array holds only half of the data, meaning that for every movie i it contains 
	 * values for all movies j when j > i.
	 */
	public static int[][] calculateCommonRatersNum(MovieIndexedModel movieIndexedModel, UserIndexedModel userIndexedModel){
		int[][] sizes = new int[Constants.NUM_MOVIES][];
		int size = Constants.NUM_MOVIES - 1;
		int j;
		long start = System.currentTimeMillis();
		
		for (short i = 0; i < Constants.NUM_MOVIES; i++){
			
			sizes[i] = new int[size];
			Arrays.fill(sizes[i], 0);
			
			size--;
			
			// Move on all the users that rated the current movie
			for (int user : movieIndexedModel.getMovieRatersByIndex(i)){
				
				// Move on all the ratings that the current user gave to all his movies (indexed by j) and sum the multiplication between the rating 
				// he gave to movie i and the rating he gave to movie j. In addition, we count the number of users who rated each movie. 
				
				j = userIndexedModel.getRatedMoviesByIndex(user).length - 1;
				
				while (j > -1){
					
					// Because we calculate only half of the matrix (its a similar matrix), will run only on
					// all i < j movies, meaning that for each movie will fill it with data of movies that are bigger than him
					if (userIndexedModel.getRatedMoviesByIndex(user)[j] <= (i+1)){
						break;
					}
					
					// increase the number of users that movie i has with movie j in common.
					// Because each movie array holds all the movies bigger than him, and that i (the movie index) equals the movie id - 1 we performed -i-2. 
					// For example: movie 1 holds the number of similar users he has with movie 2 in common, the value is held at
					// place 0 in movie 1 array. in addition, i is the movie index but it starts from 0 so if we're checking movie 1 i value will be 0,
					// so in order to get into 0 place in the array we perform 2-i-2 = 0 (i is 0) as expected.
					sizes[i][userIndexedModel.getRatedMoviesByIndex(user)[j]-i-2]++;
					j--;
				}
			}
			
			if (i % 1000 == 0 && i > 0){
				System.out.println("finished " + i + " movies. took " + (System.currentTimeMillis() - start));
				start = System.currentTimeMillis();
			}
		}
		
		// Save the common raters num between every two movies in a binary file
		FileUtils.saveDataToFile(sizes, Constants.NETFLIX_OUTPUT_DIR + "similarityCommonRatersNum.data");
		
		return sizes;
	}
}