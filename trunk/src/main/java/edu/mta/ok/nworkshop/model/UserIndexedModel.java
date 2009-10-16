package edu.mta.ok.nworkshop.model;

/**
 * Interface to be implemented by any class that holds a Netflix indexed model data in memory.
 * 
 * The interface assumes the model is indexed by user IDs and thus define access methods to 
 * the model's data by user IDs
 * 
 * @see UserIndexedModelRatings
 * @see UserIndexedModelResiduals
 */
public interface UserIndexedModel{

	/**
	 * Sorts the model in ascending order according to the movie id
	 */
	public void sortModel();
	
	/**
	 * Return all the ratings a certain user gave
	 * 
	 * @param userId The id of the user we want to get the ratings for
	 * @return An array with all the ratings a certain user gave
	 * (a byte or double array according to the loaded model) 
	 */
	public Object getUserRatings(int userId);
	
	/**
	 * Return an array of ratings made by a given user
	 * 
	 * @param userInd the index of the user in the model
	 * @return an array of byte/double ratings (a byte or double array according to the loaded model)
	 */
	public Object getUserRatingsByIndex(int userInd);
	
	/**
	 * Return a rating score given by a certain user
	 * 
	 * @param index the index that the rating exists in the given user ratings array.  
	 * @param userID ID of a user in the model 
	 * @return a rating score that matches the given user and index
	 */
	public double getUserRating(int index, int userID);
	
	/**
	 * Return the IDs of all the movies rated by a certain user
	 * 
	 * @param userId The id of the user we want to get his ratings
	 * @return An array with all the movie IDs rated by a certain user
	 */
	public short[] getRatedMovies(int userId);
	
	/**
	 * Return an array of movie IDs from a given index in the model
	 * 
	 * @param userIndex an index to the place the IDs are kept in the model
	 * @return An array with all the movie IDs rated by a certain user
	 */
	public short[] getRatedMoviesByIndex(int userIndex);
	
	/**
	 * Return an array with the movie IDs a certain user rated and their matching ratings    
	 * 
	 * @param userId the id of the user that we want to get data for
	 * @return an array with two elements, the first is short array with the movie ids, and the second 
	 * is a byte or float array with the ratings (a byte or double array according to the loaded model)
	 */
	public Object[] getUserData(int userId);
	
	/**
	 * Remove the entire data a certain user has from the model 
	 * 
	 * @param userInd the index in which the user data is held in the model
	 */
	public void removeUserDataByIndex(int userInd);
	
	/**
	 * Return the full movie ids model
	 *  
	 * @return two dimensional array containing all the movie IDs rated by every user 
	 */
	public short[][] getMovieIds();
	
	/**
	 * Return a matrix containing the full ratings model.
	 * 
	 * @return an object array when every element is an array of byte/double ratings that 
	 * the model contain (the array type is set according to the loaded model).
	 */
	public Object[] getRatings();
	
	/**
	 * Convert the loaded user indexed model matrices into three one dimensional arrays
	 *  
	 * @return An array containing three arrays: int array (containing the user IDs), 
	 * short array (containing the movie IDs) and byte/double array containing the ratings. 
	 * All arrays are the same length and contain all the train data (99072112 elements)   
	 */
	public Object[] getModelArray();
	
	/**
	 * @param userId a certain user id 
	 * @return the index the given user data is held in the model
	 */
	public int getUserIndex(int userId);
}
