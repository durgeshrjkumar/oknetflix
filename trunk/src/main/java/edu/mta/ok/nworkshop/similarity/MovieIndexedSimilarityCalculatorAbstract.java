package edu.mta.ok.nworkshop.similarity;

import edu.mta.ok.nworkshop.model.MovieIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.utils.FileUtils;

/**
 * A super class for all classes that calculates similarity between movie items.
 *   
 * The class provides a closed implementation for {@link SimilarityCalculator#saveCalculatedData(String)} 
 * and holds the data models (user/movie indexed model) needed for calculating the similarities.
 * 
 * @see PearsonCorrelationCoefifcientSimilarity, PearsonCorrelationCoefifcientSimilarityRawScores
 */
public abstract class MovieIndexedSimilarityCalculatorAbstract implements SimilarityCalculator {

	protected MovieIndexedModel movieIndexedModel;
	protected UserIndexedModel userIndexedModel;
	protected double[][] movieIndexedSimilarityData;
	
	@Override
	public final boolean saveCalculatedData(String fileName) {
		return FileUtils.saveDataToFile(movieIndexedSimilarityData, fileName);
	}
}
