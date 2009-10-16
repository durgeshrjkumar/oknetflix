package edu.mta.ok.nworkshop.predictor;

/**
 * Interface to be implemented by any class that predicts ratings using a regularized SVD approach.
 * 
 * @see SVDPredictor
 */
public interface SVDPredictor {

	/**
	 * @return the dimension of the features arrays
	 */
	public int getFeaturesNum();
	
	/**
	 * @return a 1xMOVIES_NUM*FEATURES_NUM array of all the movie features the SVD predictor calculated
	 * MOVIES_NUM - the number of movies in the Netflix dataset.
	 * FEATURES_NUM - the number of features the SVD predictor calculated.
	 * @see #getFeaturesNum() 
	 */
	public float[] getMovieFetures();
	
	/**
	 * @return a 1xUSERS_NUM*FEATURES_NUM array of all the user features the SVD predictor calculated.
	 * USERS_NUM - the number of users in the Netflix dataset.
	 * FEATURES_NUM - the number of features the SVD predictor calculated.
	 * @see #getFeaturesNum()
	 */
	public float[] getUserFeatures();
}
