package edu.mta.ok.nworkshop.predictor;

/**
 * Interface to be implemented by any class that predicts ratings for the Netflix data set.
 */
public interface Predictor {

	/**
	 * Returns a prediction on what the given user would rate the given movie.
	 * This method doesn't use the date aspect.
	 * 
	 * @param userID id of a user that rated the given movie  
	 * @param movieID id of an unrated movie that a rating prediction should be given to
	 * @param probeIndex the index of the given movie/user IDs pair in Netflix probe file  
	 * @return A prediction of the given user's rating for the given movie
	 */
	public double predictRating(int userID, short movieID, int probeIndex);

	/**
	 * Returns a prediction on what the given user would rate the given movie.
	 * This method uses the date aspect.
	 * 
	 * @param userID id of a user that rated the given movie  
	 * @param movieID id of an unrated movie that a rating prediction should be given to
	 * @param date the date the rating took place in
	 * @param probeIndex the index of the given movie/user IDs pair in Netflix probe file 
	 * @return A prediction of the given user's rating for the given movie
	 */
	public double predictRating(int userID, short movieID, String date, int probeIndex);
	
}
