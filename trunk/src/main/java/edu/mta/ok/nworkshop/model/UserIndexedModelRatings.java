package edu.mta.ok.nworkshop.model;

import java.util.HashMap;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.utils.FileUtils;
import edu.mta.ok.nworkshop.utils.ModelUtils;

/**
 * A class that holds a user indexed model with movie IDs and raw rating scores.
 * Ratings are held in a two dimensional byte type array.
 */
public class UserIndexedModelRatings implements UserIndexedModel {

	private short[][] movieIds;
	private byte[][] ratings;	
	private HashMap<Integer, Integer> userIndices;
	private ModelSorter sorter = new ModelSorter();
	
	public UserIndexedModelRatings(){
		this(PredictorProperties.getInstance().getUserIndexedModelFile(), PredictorProperties.getInstance().getUserIndicesMappingFile());
	}
	
	public UserIndexedModelRatings(String modelFileName){
		this(modelFileName, PredictorProperties.getInstance().getUserIndicesMappingFile());
	}
	
	public UserIndexedModelRatings(String modelFileName, String indicesFileName) {
		super();
		
		loadModel(modelFileName);
		loadUserIndices(indicesFileName);		
	}
	
	/**
	 * Loads the model from a given file
	 * 
	 * @param fileName the full path of the file we want to load the model from
	 */
	private void loadModel(String fileName){
		
		Object[] retVal = ModelUtils.loadUserIndexedModel(fileName, false);
		
		movieIds = (short[][])retVal[0];
		ratings = (byte[][])retVal[1];
	}
	
	/**
	 * Loads the user indices mappings from a given file
	 * 
	 * @param fileName the full path of the file that we want to load the user indices mappings from
	 */
	private void loadUserIndices(String fileName){
		
		if (fileName != null){
			userIndices = FileUtils.loadDataFromFile(fileName);
		}
	}

	@Override
	public void sortModel() {
		sorter.sortData();
	}
	
	/**
	 * Convert a given user id to the matching index in the model
	 * 
	 * @param userId the user id we want to convert
	 * @return a number representing the index in the model the given user id is placed in
	 */
	private int convertIdToIndex(int userId){
		
		if (userIndices == null){
			throw new IllegalArgumentException("User Indices hadn't been loaded");
		}
		
		return userIndices.get(userId);
	}	

	@Override
	public short[] getRatedMovies(int userId) {	
		return getRatedMoviesByIndex(convertIdToIndex(userId));
	}
	
	@Override
	public short[] getRatedMoviesByIndex(int userId) {	
		return movieIds[userId];
	}
	
	@Override
	public Object[] getUserData(int userId) {
		Object[] retVal = new Object[2];
		
		retVal[0] = movieIds[convertIdToIndex(userId)];
		retVal[1] = ratings[convertIdToIndex(userId)];
		
		return retVal;
	}
	
	@Override
	public Object getUserRatings(int userId) {
		return getUserRatingsByIndex(convertIdToIndex(userId));
	}
	
	@Override
	public Object getUserRatingsByIndex(int userInd) {
		return ratings[userInd];
	}
	
	@Override
	public double getUserRating(int index, int userID) {
		return ratings[getUserIndex(userID)][index];
	}

	@Override
	public void removeUserDataByIndex(int userInd) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public short[][] getMovieIds() {
		return movieIds;
	}

	@Override
	public Object[] getRatings() {
		return ratings;
	}
	
	@Override
	public Object[] getModelArray() {
		
		int[] userIds = new int[Constants.TRAIN_RATINGS_NUM];
		short[] movieIds = new short[Constants.TRAIN_RATINGS_NUM];
		byte[] ratings = new byte[Constants.TRAIN_RATINGS_NUM];
		
		short[] ratedMovies = null;
		byte[] currRatings = null;
		int counter = 0;
		
		System.out.println("Start reshaping the user indexed model into three dimensional array");
		
		for (int userIndex = 0; userIndex < Constants.NUM_USERS; userIndex++) {
			
			ratedMovies = getRatedMoviesByIndex(userIndex);
			currRatings = (byte[])getUserRatingsByIndex(userIndex);
			
			for (int mi = 0; mi < ratedMovies.length; mi++) {
							
				userIds[counter] = userIndex;
				movieIds[counter] = ratedMovies[mi];
				ratings[counter] = currRatings[mi]; 
				
				counter++;
			}
		}
		
		System.out.println("Finish reshaping the user indexed model into three dimensional array");
		
		return new Object[]{userIds, movieIds, ratings};
	}
	
	@Override
	public int getUserIndex(int userId) {
		return convertIdToIndex(userId);
	}

	/**
	 * Sorts the model in ascending order according to the movie IDs 
	 *
	 */
	private class ModelSorter{

	    public void sortData(){
            if (movieIds != null && movieIds.length > 0){
            	
            	long start = System.currentTimeMillis();
            	
            	for (int i=0; i < movieIds.length; i++){
            		if (movieIds[i] != null && movieIds[i].length > 0){
            			sort(i);
            		}
            		
            		if (i % 100 == 0 && i > 0){
            			System.out.println("Finished " + i + " movies. took: " + (System.currentTimeMillis() - start));
            			start = System.currentTimeMillis();
            		}
            	}
            }
	    }
	    
	    private void sort(int itemIndex){
	    	byte[] scoreTmpArray = new byte[ratings[itemIndex].length];
	    	short[] contentsTmpArray = new short[movieIds[itemIndex].length];
	    	mergeSort(contentsTmpArray, scoreTmpArray, 0, ratings[itemIndex].length - 1, itemIndex);
	    }

	    public void mergeSort( short[] tempArrayContents, byte[] tempArrayScores, int left, int right, int itemIndex ) {

	        if( left < right ) {
	            int center = ( left + right ) / 2;
	            mergeSort( tempArrayContents, tempArrayScores, left, center, itemIndex );
	            mergeSort( tempArrayContents, tempArrayScores, center + 1, right, itemIndex );
	            merge( tempArrayContents, tempArrayScores, left, center + 1, right, itemIndex );
	        }
	    }
   
	    private void merge( short[] contentsArray, byte[] scoresArray, int leftPos, int rightPos, int rightEnd, int itemIndex ) {
	
	        int leftEnd = rightPos - 1;
	        int tmpPos = leftPos;
	        int numElements = rightEnd - leftPos + 1;
	
	        // Main loop
	        while( leftPos <= leftEnd && rightPos <= rightEnd ){
	            if( movieIds[itemIndex][leftPos] < movieIds[itemIndex][ rightPos ]){
	                scoresArray[tmpPos] = ratings[itemIndex][leftPos];
	                contentsArray[tmpPos++] = movieIds[itemIndex][leftPos++];
	            }
	            else
	            {
	                scoresArray[tmpPos] = ratings[itemIndex][rightPos];
	                contentsArray[tmpPos++] = movieIds[itemIndex][ rightPos++ ];
	            }
	        }
	
	        // Copy rest of first half
	        while( leftPos <= leftEnd ){
	            scoresArray[tmpPos] = ratings[itemIndex][leftPos];
	            contentsArray[tmpPos++] = movieIds[itemIndex][ leftPos++ ];
	        }
	
	        // Copy rest of right half
	        while( rightPos <= rightEnd ){         
	            scoresArray[tmpPos] = ratings[itemIndex][rightPos];
	            contentsArray[tmpPos++] = movieIds[itemIndex][rightPos++];
	        }
	
	        // Copy tmpArray back
	        for( int i = 0; i < numElements; i++, rightEnd-- ){       
	        	ratings[itemIndex][rightEnd] = scoresArray[rightEnd];
	        	movieIds[itemIndex][rightEnd] = contentsArray[rightEnd];
	        }
	    }
	}
}
