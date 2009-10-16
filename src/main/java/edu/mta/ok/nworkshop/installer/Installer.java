package edu.mta.ok.nworkshop.installer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.utils.DateUtils;
import edu.mta.ok.nworkshop.utils.FileUtils;
import edu.mta.ok.nworkshop.utils.ModelUtils;
import edu.mta.ok.nworkshop.utils.ProgressOutput;
import edu.mta.ok.nworkshop.utils.TimeProgressOutput;

/**
 * Loads the data from the original download folder supplied by Netflix, Generates extra information
 * (userIDs, etc), converts to user and movie indexed slabs, and saves it all in easily loaded files.
 *
 * The original code was taken from {@link http://code.google.com/p/jnetflix/source/browse/trunk/netflixPrize/installer/Installer.java}. 
 */
public class Installer {

	private HashMap<Integer, Integer> userIndices = new HashMap<Integer, Integer>();

	private String downloadFolder;
	private String outputFolder;

	private boolean getDate;

	private boolean movieIndexedLoaded = false;
	private boolean userIndexedLoaded = false;
	private boolean userIndexesDataLoaded = false;

	private int[][] movieIndexedUserIDs = new int[Constants.NUM_MOVIES][];
	private byte[][] movieIndexedRatings = new byte[Constants.NUM_MOVIES][];
	private short[][] movieIndexedDates = new short[Constants.NUM_MOVIES][];

	private short[][] userIndexedMovieIDs;
	private byte[][] userIndexedRatings;
	private short[][] userIndexedDates;
	private int[] userIDs;

	// Probe data
	private boolean probeScrubbed = false;
	private static int[] probeUserIDs;
	private static short[] probeMovieIDs;
	private static byte[] probeRatings;
	private static short[] probeDates;
	
	private Calendar minCal;
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");	
	
	public Installer(String downloadFolder, String outputFolder, boolean getDate) throws ParseException {
		this.downloadFolder = downloadFolder;
		this.outputFolder = outputFolder;
		this.getDate = getDate;
		
		this.minCal = Calendar.getInstance();
		this.minCal.setTime(dateFormat.parse(Constants.MIN_DATE_STR));
		minCal.set(Calendar.SECOND, 0);
		minCal.set(Calendar.MINUTE, 0);
		minCal.set(Calendar.HOUR, 0);
		minCal.set(Calendar.MILLISECOND, 0);
	}

	/**
	 * Controls the entire process of parsing the Netflix data from the text files and saving it
	 * into binary files for easy access.
	 *  
	 * @see #createMovieIndexedRatings() 
	 * @see #createUserIndexedRatings() 
	 * @see #createAveragesFiles()
	 * @see #scrubProbe()
	 * 
	 * @throws IOException in case of an error accessing a file 
	 */
	public void install() throws IOException {
		System.out.println("Beginning Data Check and Installation procedure");
		createMovieIndexedRatings();
		createUserIndexedRatings();
		System.out.println("Creating data models with probe dataset is complete.");
		
		scrubProbe();
	}

	/**
	 * Removes the probe data from the user/movie models created from the Netflix data files
	 */
	private void scrubProbe() {
		// Remove the probe data
		loadUserIndexedRatings();
		loadUserIndices();
		loadTrainingData();		

		final int trainingLength = probeUserIDs.length;
		
		System.out.println("Scrubbing probe from data");
		ProgressOutput po = new TimeProgressOutput("Finished %d entries, took %d millis", 100000);
		
		for (int i = 0; i < trainingLength; i++) {
			short movieID = probeMovieIDs[i];
			int userID = probeUserIDs[i];
			removeRating(movieID, userID);
			po.incrementProgressCounter();
		}
		
		System.out.println("Finished scrubing probe from data!");

		probeScrubbed = true;
		blitMovies();
		blitUsers();
	}

	/**
	 * Removes a certain rating from the movie and user models according to a given
	 * movie + user IDs pair
	 * 
	 * @param movieID the id of the movie we want to remove the rating for 
	 * @param userID the id of the user we want to remove the rating for
	 */
	private void removeRating(short movieID, int userID) {
		// Remove by movie-indexed
		int userIndex = userIndices.get(userID);
		int[] userIDs = movieIndexedUserIDs[movieID - 1];

		byte[] mRatings = movieIndexedRatings[movieID - 1];
		short[] mDates = null;		
		
		movieIndexedUserIDs[movieID - 1] = new int[userIDs.length - 1];
		movieIndexedRatings[movieID - 1] = new byte[userIDs.length - 1];
		
		if (getDate){
			mDates = movieIndexedDates[movieID - 1];
			movieIndexedDates[movieID - 1] = new short[userIDs.length - 1];
		}

		int curIndex = 0;
		int i = -1;
		
		for (int currUser : userIDs){
			
			i++;
			
			if (currUser == userIndex){
				continue;
			}
			
			movieIndexedUserIDs[movieID - 1][curIndex] = currUser;
			movieIndexedRatings[movieID - 1][curIndex] = mRatings[i];
				
			if (getDate){
				movieIndexedDates[movieID - 1][curIndex] = mDates[i];
			}
			curIndex++;			
		}

		if (curIndex != movieIndexedUserIDs[movieID - 1].length) {
			System.err.println("Invalid number of entries (movie): " + curIndex
					+ "/" + movieIndexedUserIDs[movieID - 1].length);
		}
		// Remove by user-indexed
		short[] movieIDs = userIndexedMovieIDs[userIndex];
		byte[] uRatings = userIndexedRatings[userIndex];
		
		userIndexedMovieIDs[userIndex] = new short[movieIDs.length - 1];
		userIndexedRatings[userIndex] = new byte[movieIDs.length - 1];
		
		if (getDate){
			mDates = userIndexedDates[userIndex];
			userIndexedDates[userIndex] = new short[movieIDs.length - 1];
		}
		
		curIndex = 0;
		i = -1;

		for (short currMovie : movieIDs){
			
			i++;
			
			if (currMovie == movieID) {
				continue;
			}
			
			userIndexedMovieIDs[userIndex][curIndex] = currMovie;
			userIndexedRatings[userIndex][curIndex] = uRatings[i];
			
			if (getDate){
				userIndexedDates[userIndex][curIndex] = mDates[i];
			}
				
			curIndex++;
		}
		
		if (curIndex != userIndexedMovieIDs[userIndex].length) {
			System.err.println("Invalid number of entries (user): " + curIndex
					+ "/" + userIndexedMovieIDs[userIndex].length);
		}
	}

	/**
	 * Loads the probe data, convert it and saves it in a binary file for easy access
	 */
	private void loadTrainingData() {
		String filename = Constants.NETFLIX_MODEL_DIR + "betterprobe.txt";
		File probeFile = new File(filename);
		
		System.out.print("Loading probe set...");
		int count = 0;

		probeUserIDs = new int[Constants.PROBES_NUM];
		probeMovieIDs = new short[Constants.PROBES_NUM];
		probeRatings = new byte[Constants.PROBES_NUM];
		
		if (getDate){
			probeDates = new short[Constants.PROBES_NUM];
		}
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(probeFile));
			if (!in.ready())
				throw new IOException();
			
			// First line is the movie number: "200:\n"

			String line = "";
			// Get the movieID only
			short movieID = 0;
			int userID = 0;

			while ((line = in.readLine()) != null) {

				// If the line is a new movieID
				if (line.contains(":")) {
					movieID = Short.parseShort(line.substring(0, line
							.length() - 1));
				} 
				else {
					// Create the ratings
					ArrayList<String> tokens = splitProbeLine(
							line, ',');
					userID = Integer.parseInt(tokens.get(1));
					byte rating = Byte.parseByte(tokens.get(0));
					if (userID < 1) {
						System.err.println("Invalid user ID: " + userID);
						System.err.println("Line: " + line);
					}
					// Store the rating in memory
					probeUserIDs[count] = userID;

					probeMovieIDs[count] = movieID;

					probeRatings[count] = rating;
					
					if (getDate){
						
						probeDates[count] = -1;
						
						int counter2 = 0;
						int userInd = userIndices.get(userID);
						
							for (short movieId : userIndexedMovieIDs[userInd]){
								if (movieId == movieID){
									probeDates[count] = userIndexedDates[userInd][counter2];
									break;
								}
								
								counter2++;
							}
						if (probeDates[count] < 0){
							System.err.println("Error getting probe " + count + " rating date");
						}							
					}
					count++;
				}
			}
			
			System.out.println("Done!");

			in.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		System.out.println("Loaded " + count + " entries in the probe set.");
		
		boolean sucess = ModelUtils.saveProbeData(Constants.NETFLIX_OUTPUT_DIR + "probe.data", probeUserIDs, probeMovieIDs, probeRatings, probeDates);
		
		if (sucess){
		
			System.out.println("Sucessfully save probe to file");
		}
		else{
			System.err.println("Error saving probe to file");
		}
	}
	
	/**
	 * Split a certain data row loaded from the probe text file into the data components 
	 * (userid and original rating) 
	 * 
	 * @param line a string containing a single probe data separated by a delimiter 
	 * @param separator a char that separates the probe data 
	 * @return string array containing the separated data extracted from the given line
	 */
	public static ArrayList<String> splitProbeLine(String line, char separator) {

		ArrayList<String> tokens = new ArrayList<String>();
		String cur = "";
		for (char c : line.toCharArray()) {
			if (c == separator) {
				// Separator found, new token
				tokens.add(cur);
				cur = "";
			} else {
				// Append to cur String
				cur += c;
			}
		}
		// Add the last token
		tokens.add(cur);
		return tokens;
	}

	/**
	 * Creates a movie indexed model
	 */
	private void createMovieIndexedRatings() {
		// Check if the movie Indexed slab has been created
		boolean movieFileCreated = false;

		String filename = outputFolder + "movieIndexedSlab.data";
		
		if (getDate){
			filename = outputFolder + "movieIndexedSlabDates.data";
		}
		
		File f2 = new File(filename);
		movieFileCreated = f2.exists();
		if (!movieFileCreated) {
			System.out
					.println("Movie indexed file does not exist. Creating it now.");
			// If the file hasn't been created, then create it
			// Load all files into movie-indexed arrays
			File folder = new File(downloadFolder + "/training_set");
			// For each file in this folder
			File[] listOfFiles = folder.listFiles();
			System.out.println("There are " + listOfFiles.length
					+ " files to load.");
			System.out.println("Start loading data from movie files");
			ProgressOutput po = new TimeProgressOutput("Finished loading %d movies files, took %d millis", 1000);

			for (File f : listOfFiles) {
				// parse each file
				parseFile(f, getDate);
				po.incrementProgressCounter();
			}
			
			System.out.println("Finished Loading data from movie files!");

			createUserIndices();
			blitMovies();
		} else {
			System.out.println("Movie indexed ratings already created.");
			loadMovieIndexedRatings();
		}
		movieIndexedLoaded = true;
	}

	/**
	 * Replace all the user ids with a sequential ids (replace it in the movie array as well as in the user
	 * array)
	 */
	private void createUserIndices() {
		System.out.print("Obtaining user indexes...");		

		// Iterate through all the ratings we have, getting new indices as we go
		HashSet<Integer> allUserIDs = new HashSet<Integer>();
		for (short movieID = 1; movieID <= Constants.NUM_MOVIES; movieID++) {
			int L = movieIndexedUserIDs[movieID - 1].length;
			for (int j = 0; j < L; j++) {
				Integer userID = movieIndexedUserIDs[movieID - 1][j];
				allUserIDs.add(userID);
			}
		}
		// Check we have the correct number of user IDs
		if (allUserIDs.size() != Constants.NUM_USERS) {
			int difference = Constants.NUM_USERS - allUserIDs.size();
			System.err.println("Invalid number of user IDs found: delta of "
					+ difference);
		}
		
		System.out.println("Done!");
		
		// Find all the unique userIDs
		userIDs = new int[allUserIDs.size()];
		int index = 0;
		for (int userID : allUserIDs) {
			userIDs[index] = userID;
			index++;
		}

		// Sort the user indexes
		Arrays.sort(userIDs);

		// Create a new hashmap for the sequential IDs
		userIndices = new HashMap<Integer, Integer>();
		for (int i = 0; i < userIDs.length; i++) {
			// this gives each value a sequential ID
			userIndices.put(userIDs[i], i);
		}

		// Save the user IDs and their indexes to a file
		String indexfilename = outputFolder + "userIndexes.data";
		System.out.print("Blitting indexes to file...");
		BufferedOutputStream out = null;
		ObjectOutputStream oos = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(indexfilename));
			oos = new ObjectOutputStream(out);
			// Write the arrays to the file
			oos.writeObject(userIDs);
			oos.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
		
		System.out.print("Blitting indices map to file...");
		String indexMapFileName = outputFolder + "userIndexesMap.data";
		FileUtils.saveDataToFile(userIndices, indexMapFileName);
		System.out.println("Done!");

		// Iterate through all ratings, replacing userID with userIndex
		System.out.print("Replacing user IDs with indices...");
		
		for (short movieID = 0; movieID < Constants.NUM_MOVIES; movieID++) {
			int L = movieIndexedUserIDs[movieID].length;
			for (int i = 0; i < L; i++) {
				int ui = userIndices.get(movieIndexedUserIDs[movieID][i]);
				if (ui == -1) {
					throw new RuntimeException("Invalid user index: "
							+ movieIndexedUserIDs[movieID][i]);
				}
				movieIndexedUserIDs[movieID][i] = ui;
			}
		}
		
		this.userIndexesDataLoaded = true;
		
		System.out.println("Done!");
	}

	/**
	 * Creates a user indexed model
	 */
	private void createUserIndexedRatings() {

		String filename = outputFolder + "userIndexedSlab.data";
		if (getDate) {
			filename = outputFolder + "userIndexedSlabDates.data";
		}
		File f2 = new File(filename);
		boolean userFileCreated = f2.exists();
		if (userFileCreated) {
			System.out.println("User indexed file has already been created");
			loadUserIndexedRatings();
			return;
		} else {
			System.out
					.println("User indexed file not created, creating it now");
			// Load movie indexed file to memory
			loadMovieIndexedRatings();

			// Get the user indexes here from file
			loadUserIndices();

			// Find out how many ratings each user has
			System.out.print("Obtaining rating counts...");
			int[] numRatings = new int[Constants.NUM_USERS];
			
			Arrays.fill(numRatings, 0);
			
			for (short movieID = 1; movieID <= Constants.NUM_MOVIES; movieID++) {
				for (int i = 0; i < movieIndexedUserIDs[movieID - 1].length; i++) {
					int userIndex = movieIndexedUserIDs[movieID - 1][i];
					numRatings[userIndex]++;
				}
			}
			System.out.println("Done!");

			// Create the arrays that the ratings will go in.
			System.out.print("Initializing arrays...");
			userIndexedMovieIDs = new short[Constants.NUM_USERS][];
			userIndexedRatings = new byte[Constants.NUM_USERS][];
			
			if (getDate){
				userIndexedDates = new short[Constants.NUM_USERS][];
			}
			
			for (int i = 0; i < userIDs.length; i++) {
				userIndexedMovieIDs[i] = new short[numRatings[i]];
				userIndexedRatings[i] = new byte[numRatings[i]];
				
				if (getDate){
					userIndexedDates[i] = new short[numRatings[i]];
				}
			}

			// Index stores where we are up to with each uesr's ratings
			int[] index = new int[Constants.NUM_USERS];
			for (int i = 0; i < index.length; i++) {
				index[i] = 0;
			}
			System.out.println("Done!");
			
			// Now put all of the ratings into the proper user-indexed buckets
			System.out.print("Indexing by user...");
			for (short movieID = 1; movieID <= Constants.NUM_MOVIES; movieID++) {
				for (int i = 0; i < movieIndexedUserIDs[movieID - 1].length; i++) {
					int userIndex = movieIndexedUserIDs[movieID - 1][i];
					byte rating = movieIndexedRatings[movieID - 1][i];
					// Save the rating to user indexed
					userIndexedMovieIDs[userIndex][index[userIndex]] = (short) (movieID);
					userIndexedRatings[userIndex][index[userIndex]] = rating;
					
					if (getDate){
						userIndexedDates[userIndex][index[userIndex]] = movieIndexedDates[movieID - 1][i];
					}
					
					index[userIndex]++;
				}
			}
			System.out.println("Done!");

			// Verify that we found all ratings
			System.out.print("Verifying indexes...");
			for (int i = 0; i < numRatings.length; i++) {
				if (index[i] != numRatings[i] || numRatings[i] == 0) {
					System.err
							.println("Did not get enough ratings for user index: "
									+ i);
				}
			}
			System.out.println("Done!");	

			// Blit the user indexed to file
			blitUsers();
			// Blit the movie indexed file again (it has been sorted now)
			blitMovies();
			
			this.userIndexedLoaded = true;
		}
	}

	/**
	 * Load a user indexed model from a file
	 */
	private void loadUserIndexedRatings() {
		if (userIndexedLoaded) {
			return;
		}
		ObjectInputStream ois = null;
		System.out.print("Loading user indexed ratings...");
		try {			
			String movieFilename = outputFolder
					+ "userIndexedSlab.data";
			
			if (getDate){
				movieFilename = outputFolder
				+ "userIndexedSlabDates.data";
			}
			
			ois = new ObjectInputStream(new FileInputStream(movieFilename));
			userIndexedMovieIDs = (short[][]) ois.readObject();
			userIndexedRatings = (byte[][]) ois.readObject();
			
			if (getDate){			
				userIndexedDates = (short[][]) ois.readObject();
			}
			
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("done");
		userIndexedLoaded = true;
	}

	/**
	 * Saves a movie indexed model into a binary file
	 */
	private void blitMovies() {
		String filename = outputFolder + "movieIndexedSlab.data";
		if (probeScrubbed && !getDate) {
			filename = outputFolder + "cleanedMovieIndexedSlab.data";
		} else if (getDate) {
			if (probeScrubbed) {
				filename = outputFolder + "cleanedMovieIndexedSlabDates.data";
			} else {
				filename = outputFolder + "movieIndexedSlabDates.data";
			}
		}
		System.out.print("Blitting to movie index to file...");
		BufferedOutputStream out = null;
		ObjectOutputStream oos = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(filename));
			oos = new ObjectOutputStream(out);
			// Write the arrays to the file
			oos.writeObject(movieIndexedUserIDs);
			oos.writeObject(movieIndexedRatings);
			if (getDate) {
				oos.writeObject(movieIndexedDates);
			}
			oos.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
	}

	/**
	 * Parse a given movie data text file 
	 * 
	 * @param f a File object that points a movie data file for parsing
	 * @param getDate mark if we need to load the rating dates data from the file
	 */
	private void parseFile(File f, boolean getDate) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(f));
			if (!in.ready())
				throw new IOException();
			// First line is the movie number: "200:\n"
			String line = in.readLine();
			// Get the movieID only
			short movieID = Short.parseShort(line.substring(0,
					line.length() - 1));

			// Load all of the rest of the lines into memory
			ArrayList<String> lines = new ArrayList<String>();
			// System.out.print("Movie: " + movieID);
			while ((line = in.readLine()) != null) {
				lines.add(line);
			}

			// Setup the array
			movieIndexedUserIDs[movieID - 1] = new int[lines.size()];
			movieIndexedRatings[movieID - 1] = new byte[lines.size()];
			if (getDate) {
				movieIndexedDates[movieID - 1] = new short[lines.size()];
			}

			Calendar date = Calendar.getInstance();
			int i = 0;
			for (String curLine : lines) {
				// Create the ratings
				ArrayList<String> tokens = splitLine(curLine, ',');
				movieIndexedUserIDs[movieID - 1][i] = Integer.parseInt(tokens
						.get(0));
				movieIndexedRatings[movieID - 1][i] = Byte.parseByte(tokens
						.get(1));
				if (getDate) {
					
					try {
						date.setTime(dateFormat.parse(tokens.get(2)));
						movieIndexedDates[movieID - 1][i] = DateUtils.calculateDiffBetweenDates(minCal, date);
					} catch (ParseException e) {						
						System.out.println("Error getting date. movieId = " + movieID + ", i = " + i + e.getMessage());						
					}
				}
				i++;
			}

			in.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	private ArrayList<String> splitLine(String line, char separator) {
		ArrayList<String> tokens = new ArrayList<String>();
		String cur = "";
		for (char c : line.toCharArray()) {
			if (c == separator) {
				// Separator found, new token
				tokens.add(cur);
				cur = "";
			} else {
				// Append to cur String
				cur += c;
			}
		}
		// Add the last token
		tokens.add(cur);
		return tokens;
	}

	/**
	 * Load a movie indexed model from a file
	 */
	private void loadMovieIndexedRatings() {
		if (movieIndexedLoaded) {
			return;
		}
		ObjectInputStream ois = null;
		System.out.print("Loading movie indexed ratings...");
		try {
			String movieFilename = outputFolder
					+ "movieIndexedSlab.data";
			
			if (getDate){
				movieFilename = outputFolder
				+ "movieIndexedSlabDates.data";
			}
			
			ois = new ObjectInputStream(new FileInputStream(movieFilename));
			movieIndexedUserIDs = (int[][]) ois.readObject();
			movieIndexedRatings = (byte[][]) ois.readObject();
			
			if (getDate){			
				movieIndexedDates = (short[][]) ois.readObject();
			}
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("done");
		movieIndexedLoaded = true;

		// Find out if the userIDs have been swapped with user indexes or not
		boolean highFound = false;
		for (int m = 0; m < Constants.NUM_MOVIES; m++) {
			int size = movieIndexedUserIDs[m].length;
			for (int u = 0; u < size; u++) {
				if (movieIndexedUserIDs[m][u] > Constants.NUM_USERS) {
					highFound = true;
				}
			}
		}
		if (highFound) {
			System.err
					.println("High user ID found, haven't been swapped with user indexes yet");
		} else {
			System.out.println("User indices have been replaced in the data");
		}
	}

	/**
	 * Loads the user indices mapping data from a binary file
	 */
	private void loadUserIndices() {
		
		if (this.userIndexesDataLoaded){
			return;
		}
		
		ObjectInputStream ois = null;
		System.out.print("Loading user indices data...");
		try {
			String userIndexesFilename = outputFolder
					+ "userIndexes.data";
			ois = new ObjectInputStream(
					new FileInputStream(userIndexesFilename));
			this.userIDs = (int[]) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");

		// Now load them into our hashMap for fast retrieval
		userIndices = new HashMap<Integer, Integer>();
		int i = 0;
		for (int userID : userIDs) {
			if (userID < 0) {
				throw new RuntimeException(
						"Invalid User ID found in index file");
			}
			userIndices.put(userID, i++);
		}
		
		userIndexesDataLoaded = true;
	}

	/**
	 * Saves the user indexed model in a binary file
	 */
	private void blitUsers() {
		String filename = outputFolder + "userIndexedSlab.data";
		if (probeScrubbed && !getDate) {
			filename = outputFolder + "cleanedUserIndexedSlab.data";
		} else if (getDate) {
			if (!probeScrubbed) {
				filename = outputFolder + "userIndexedSlabDates.data";
			} else {
				filename = outputFolder + "cleanedUserIndexedSlabDates.data";
			}
		}
		System.out.print("Blitting to user index model...");
		BufferedOutputStream out = null;
		ObjectOutputStream oos = null;
		try {
			
			out = new BufferedOutputStream(new FileOutputStream(filename));
			oos = new ObjectOutputStream(out);
			// Write the arrays to the file
			oos.writeObject(userIndexedMovieIDs);
			oos.writeObject(userIndexedRatings);

			if (getDate) {
				oos.writeObject(userIndexedDates);
			}
			oos.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
	}

	public static void main(String args[]) throws ParseException {
		
		boolean getDate = true;
		Installer i = new Installer(Constants.NETFLIX_MODEL_DIR, Constants.NETFLIX_OUTPUT_DIR, getDate);
		try {
			i.install();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
