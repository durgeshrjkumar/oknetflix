package edu.mta.ok.nworkshop;

/**
 * Helper class that Holds general constants used in the project
 */
public class Constants {
	
	static{
		
		String inputDir = System.getProperty("workshop.binFiles.inputDir");
		String outputDir = System.getProperty("workshop.binFiles.outputDir");
		String modelDir = System.getProperty("workshop.modelDir");
		
		if (inputDir == null){
			NETFLIX_INPUT_DIR = "E:/FinalProject/DataSets/Netflix/Full/training_set/"; 
		}
		else{
			NETFLIX_INPUT_DIR = inputDir;
		}
		
		if (outputDir == null){
			NETFLIX_OUTPUT_DIR = "E:/FinalProject/code/netflixWorkshop/binFiles/"; 
		}
		else{
			NETFLIX_OUTPUT_DIR = outputDir;
		}
		
		if (modelDir == null){
			NETFLIX_MODEL_DIR = "E:/FinalProject/DataSets/Netflix/FULL/training_set/";
		}
		else{
			NETFLIX_MODEL_DIR =  modelDir; 
		}
	}
	
	public static final short NUM_MOVIES = 17770;
	
	public static final int NUM_USERS = 480189;

	public static final int NUM_RATINGS = 100480507;
	
	public static final int TRAIN_RATINGS_NUM = 99072112;
	
	public static final String MOVIE_TITLES_FILE_NAME = "movie_titles.txt";
	
	public static final String NETFLIX_INPUT_DIR;
	
	public static final String NETFLIX_OUTPUT_DIR;
	
	public static final String NETFLIX_MODEL_DIR;
	
	public static final String DEFAULT_USER_INDEXED_MODEL_FILE_NAME = "cleanedUserIndexedSlabDates.data";
	
	public static final String DEFAULT_MOVIE_INDEXED_MODEL_FILE_NAME = "cleanedMovieIndexedSlabDates.data";
	
	public static final String DEFAULT_USER_INDEX_MAPPING_FILE_NAME = "userIndexesMap.data";
	
	public static final String DEFAULT_PROBE_FILE_NAME = "probe.data";
	
	public static final String DEFAULT_PREDICTORS_FLODER_NAME = "Predictions";
	
	public static final int PROBES_NUM = 1408395;
	
	public static final String MIN_DATE_STR = "1990-01-01";
	
}
