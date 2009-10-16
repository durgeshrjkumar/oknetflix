package edu.mta.ok.nworkshop.similarity;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.model.MovieIndexedModel;
import edu.mta.ok.nworkshop.model.MovieIndexedModelResiduals;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.model.UserIndexedModelResiduals;
import edu.mta.ok.nworkshop.utils.FileUtils;

/**
 * Calculates the similarity between two movies using the interpolation similarity model suggested in 
 * the following Bellkor abstract: <a href="http://public.research.att.com/~volinsky/netflix/cfworkshop.pdf">Improved Neighborhood-based Collaborative Filtering</a>
 * on section 4.
 * 
 * The class uses residulas of global effects in order to calculate the similarities values. 
 * 
 * @see UserIndexedModelResiduals 
 * @see MovieIndexedModelResiduals
 */
public class InterpolationSimilarityResiduals extends InterpolationSimilarityAbstract {

	private static boolean loadedFromFile = false;
	
	private InterpolationSimilarityResiduals(){
		super();
	}
	
	public InterpolationSimilarityResiduals(String movieModelFileName, String userModelFileName){
		this(new MovieIndexedModelResiduals(movieModelFileName), new UserIndexedModelResiduals(userModelFileName));
	}
	
	public InterpolationSimilarityResiduals(MovieIndexedModel movieModel, UserIndexedModel userModel){
		super(movieModel, userModel);
		
		// Movie Indexed ratings aren't necessary		
		movieIndexedModel.removeRatings();
	}
	
	public InterpolationSimilarityResiduals(String movieModelFileName, String userModelFileName,
			String userIndicesFileName, String movieResidualsFileName, String userResidualsFileName){
		
		this(new MovieIndexedModelResiduals(movieModelFileName, movieResidualsFileName), 
				new UserIndexedModelResiduals(userModelFileName, userIndicesFileName, 
						userResidualsFileName));
	}
	
	/**
	 * Builds the full similarity model by calculating similarity between every two movies in the model.
	 * 
	 * @throws UnsupportedOperationException in case the similarity model had been loaded from a file using
	 * {@link #getSimilarityFromFile(String, boolean)}
	 */	
	@Override
	public void calculateSimilarities() {
		
		if (loadedFromFile){
			throw new UnsupportedOperationException("Can't calculate similarity on class loaded from file");
		}
		else{
			super.calculateSimilarities();
		}
	}
	
	@Override
	protected double getMovieRating(int userInd, int position) {
		return ((double[])userIndexedModel.getUserRatingsByIndex(userInd))[position];
	}

	/**
	 * Creates a new instance of the class by loading the similarity model from a given file
	 * 
	 * REMARK: Notice that no calculation will be available on the return instance, trying to call
	 * "calculateSimilarities" will throw an exception.
	 * 
	 * @see #calculateSimilarities() 
	 * @param fileName a file containing the similarity model (a double matrix)
	 * @param floatModel mark if the loaded model holds float data or double data
	 * @return a new instance of the class with a similarity model loaded from a given file
	 */
	public static InterpolationSimilarityResiduals getSimilarityFromFile(String fileName, boolean floatModel){
		
		if (fileName == null || fileName.isEmpty()){
			return null;
		}
		
		floatSimilarityModel = floatModel;
		
		InterpolationSimilarityResiduals retVal = new InterpolationSimilarityResiduals();
		
		if (!floatModel){
			retVal.setSimilarities((double[][])FileUtils.loadDataFromFile(fileName));
		}
		else{
			retVal.setSimilarities((float[][])FileUtils.loadDataFromFile(fileName));
		}		
		
		retVal.setLoadedFromFile(true);
		
		return retVal;
	}
	
	private void setLoadedFromFile(boolean loadedFromFile) {
		InterpolationSimilarityResiduals.loadedFromFile = loadedFromFile;
	}
	
	public static void main(String[] args) {
		InterpolationSimilarityResiduals sim = new InterpolationSimilarityResiduals(Constants.NETFLIX_OUTPUT_DIR + "movieIndexedModelNoProbeWithDates.data", 
				Constants.NETFLIX_OUTPUT_DIR + "userIndexedModelNoProbeWithDates.data");
		sim.calculateSimilarities();
		sim.saveCalculatedData(Constants.NETFLIX_OUTPUT_DIR + "interpolation\\similarityModel.data");
	}
}
