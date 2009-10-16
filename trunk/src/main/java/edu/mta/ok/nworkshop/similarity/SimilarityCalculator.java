package edu.mta.ok.nworkshop.similarity;

/**
 * Interface to be implemented by any class that calculates similarity between objects.
 * 
 * @see MovieSimilarityCalculator
 */
public interface SimilarityCalculator {

	/**
	 * Calculate full similarity model between every two items.
	 */
	public void calculateSimilarities();
	
	/**
	 * Saves the calculated similarity data into a binary file.
	 * 
	 * @param fileName a path to the file the data should be saved in
	 * @return true if the data had been saved successfully or false otherwise
	 */
	public boolean saveCalculatedData(String fileName);

	/**
	 * @return similarity score between two given item ids
	 */
	public double getSimilarityScore(int firstItemId, int secondItemId);
}
