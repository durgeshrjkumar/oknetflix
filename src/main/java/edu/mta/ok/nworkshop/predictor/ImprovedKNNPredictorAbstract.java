package edu.mta.ok.nworkshop.predictor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import Jama.Matrix;
import edu.mta.ok.nworkshop.model.UserIndexedModel;
import edu.mta.ok.nworkshop.preprocess.PreProcessItemViewUsers;
import edu.mta.ok.nworkshop.similarity.SimilarityCalculator;
import edu.mta.ok.nworkshop.utils.FileUtils;

/**
 * Abstract class that is a super class for all Improved KNN predictor classes.
 * The class is an implementation of an algorithm developed by Bellkor team which is described in the following abstract:
 * <a href="http://public.research.att.com/~volinsky/netflix/cfworkshop.pdf">Improved Neighborhood-based Collaborative Filtering</a>
 * 
 * All preprocessing presented in the above abstract are implemented in {@link PreProcessItemViewUsers}
 *
 * @see ImprovedKNNPredictionRawScore 
 * @see ImprovedKNNPredictionResiduals
 */
public abstract class ImprovedKNNPredictorAbstract implements Predictor {

	private static final float MIN_VALUE = -10000;
	protected static int DEFAULT_NEIGHBOARS_NUM = 30;
	protected static double HIGH_SIM_DEFAULT = -1000;
	protected static int DEFAULT_ALPHA = 20;
	private static final double NON_NEGATIVE_STOP_VAL = 0.00005;
	private static final double EPS = (1.e-20);
	private static final double INF = (1.e20);
	
	protected SimilarityCalculator interpolationSimilarityScores;
	protected UserIndexedModel userModel;
	protected int alpha;
	protected double[][] interpolationVals;
	protected int neighborsNum;
	
	public ImprovedKNNPredictorAbstract(){
		
	}
	
	public ImprovedKNNPredictorAbstract(SimilarityCalculator simModel, UserIndexedModel userModel, 
			String interpolationValsFileName, int neighborsNum) {
		this(simModel, userModel, neighborsNum, interpolationValsFileName);
	}
	
	public ImprovedKNNPredictorAbstract(SimilarityCalculator simModel, UserIndexedModel userModel, 
			int neighborsNum, String interpolationValsFileName) {
		this(simModel, userModel, DEFAULT_ALPHA, interpolationValsFileName, neighborsNum);
	}
	
	public ImprovedKNNPredictorAbstract(SimilarityCalculator simModel, UserIndexedModel userModel, 
			int alpha, String interpolationValsFileName, int neighborsNum) {
		super();
	
		this.neighborsNum = neighborsNum;
		interpolationSimilarityScores = simModel;
		this.userModel = userModel;
		this.alpha = alpha;
		interpolationVals = FileUtils.loadDataFromFile(interpolationValsFileName);
	}
	
	public ImprovedKNNPredictorAbstract(SimilarityCalculator simModel, UserIndexedModel userModel, 
			int alpha, int neighborsNum) {
		super();
	
		this.neighborsNum = neighborsNum;
		interpolationSimilarityScores = simModel;
		this.userModel = userModel;
		this.alpha = alpha;
	}
	
	@Override
	public double predictRating(int userID, short movieID, int probeIndex) {
		
		Set<Short> itemSelectedNeighbors = new HashSet<Short>();//Sets.newHashSet();
		Map<Short, Double> ratings = new HashMap<Short, Double>();
		
		Random r = new Random();
		ArrayList<Short> movieIds = new ArrayList<Short>(); 
		Matrix a = null,b = null,w = null, w2 = null;
		int k1,k2,wInd;
		int k = neighborsNum;
		boolean finished = false;
		double currHighSim = MIN_VALUE;
		short currHighId = -1;
		double currHighRating = -1;
		int errorsNum = 0;
		int counter = 0;
		double prediction = 0;
			
		finished = false;
		prediction = 0;
			
		// Get the k most similar movies, or in case the user didn't rate enough movies, we'll get all the movies he rated
		while (k > 0 && !finished){
		
			currHighSim = MIN_VALUE;
			currHighId = -1;
			currHighRating = -1;
			finished = true;
			counter = 0;
			
			for (short s : userModel.getRatedMovies(userID)){
			
				if ((interpolationSimilarityScores.getSimilarityScore(movieID, s) > currHighSim) &&					
					!(itemSelectedNeighbors.contains(s))){
					currHighSim = interpolationSimilarityScores.getSimilarityScore(movieID, s);
					currHighId = s;
					currHighRating = getRatingValue(userID, counter);
				}
				
				counter++;
			}				
			
			if (currHighSim != MIN_VALUE){
				finished = false;
				ratings.put(currHighId, currHighRating);
				movieIds.add(currHighId);
				itemSelectedNeighbors.add(currHighId);
				k--;
			}
		}
		
		if (movieIds.size() == 0){
			prediction = r.nextInt(4)+1;
		}
		else{
		
			a = new Matrix(movieIds.size(), movieIds.size());
			b = new Matrix(movieIds.size(), 1);
			
			// Create the a and b values arrays
			for (k1=0; k1<movieIds.size(); k1++){

				b.set(k1, 0, interpolationVals[Math.min(movieIds.get(k1), movieID) - 1][Math.max(movieIds.get(k1), movieID) - Math.min(movieIds.get(k1), movieID)]);										
				
				for (k2=0; k2<movieIds.size(); k2++){
					a.set(k1, k2, interpolationVals[Math.min(movieIds.get(k1), movieIds.get(k2)) - 1][Math.max(movieIds.get(k1), movieIds.get(k2)) - Math.min(movieIds.get(k1), movieIds.get(k2))]);
				}
			}

			double[] wVals = nonNegativeQuadraticOpt(a.getArray(), b.getRowPackedCopy(), a.getColumnDimension());
				
			w = new Matrix(wVals.length, 1);
		
			for (wInd = 0; wInd < w.getRowDimension(); wInd++){
				w.set(wInd, 0, wVals[wInd]);
			}
			
			for (wInd = 0; wInd < w.getRowDimension(); wInd++){
				
				if (Double.isNaN(w.get(wInd, 0) * ratings.get(movieIds.get(wInd)))){
					System.err.println("Curr prediction value is NaN, continue");
					continue;
				}
				
				prediction += w.get(wInd, 0) * ratings.get(movieIds.get(wInd));
			}
		}
		
		prediction = getFinalPrediction(prediction, probeIndex);
		
		return prediction;
	}
	
	protected abstract double getRatingValue(int userID, int position);
	
	protected abstract double getFinalPrediction(double currPrediction, int probeIndex);

	@Override
	public double predictRating(int userID, short movieID, String date,
			int probeIndex) {
		return predictRating(userID, movieID, probeIndex);
	}

	/**
	 * Implementation of a quadratic function suggested in the Bellkor abstract in order to decrease RMSE.
	 * This is a java implementation of a c code taken from <a href="http://code.google.com/p/nprize/source/browse/trunk/basic.c">nPrize Project</a>
	 * 
	 * @param aValues a two dimensional array containing the A matrix values
	 * @param bValues a one dimensional array containing the b matrix values
	 * @param k the number of neighbors the A and b matrices contains 
	 * @return a 1*k array containing the interpolation values used in order to predict ratings
	 */
	private double[] nonNegativeQuadraticOpt(double[][] aValues, double[] bValues, int k){
		double[] xValues = new double[k];
		double[] wValues = new double[k], r = new double[k];
		double bestrr=INF,rr = 0;
		int i,j;
		boolean divergence = false;
		int iterationsNum = 0;
		
		// Initial guess		
		for(i=0;i<k;i++){
			wValues[i]=(double)1/k; 
		}
		
		do{
			for(i=0;i<k;i++) {
				double sum=0.;
				
				for(j=0;j<k;j++){
					sum += aValues[i][j]*wValues[j];
				}
				
				r[i]=bValues[i] - sum; //http://www.netflixprize.com/community/viewtopic.php?pid=6025#p6025
			}
			
			// find active variables - those that are pinned because of
			// nonnegativity constraint, and set respective ri’s to zero
			for(i=0;i<k;i++){
				
				if((wValues[i]<EPS)&&(r[i]<0)){
					r[i]=0;
				}
			}
			
			// max step size
			double rAr=0;
			rr=0;
			for(i=0;i<k;i++) {
				double sum=0.;
				
				for(j=0;j<k;j++){
					sum+=aValues[i][j]*r[j];
				}
				
				rAr+=sum*r[i];
				rr+=r[i]*r[i];
			}
			
			double alpha=rr/rAr;
			
			//adjust step size to prevent negative values:
			//http://www.netflixprize.com/community/viewtopic.php?pid=6139#p6139
			if(Double.isNaN(alpha) || alpha<EPS){
				alpha=0.001;
			}
			
			for(i=0;i<k;i++) {
				if (r[i] * alpha < -EPS ) {
					alpha = (Math.abs(alpha) < Math.abs(wValues[i]/r[i])) ? Math.abs(alpha): Math.abs(wValues[i]/r[i]);
	            } else if (r[i] * alpha > EPS){
					alpha = Math.abs(alpha);
	            }
			}
			for(i=0;i<k;i++) {
				wValues[i]+=alpha*r[i];
				
				if(wValues[i]<1.e-10){
					wValues[i]=0.; //http://www.netflixprize.com/community/viewtopic.php?pid=6025#p6025
				}
			}

			if(rr<bestrr) {
				bestrr=rr;
				for(i=0;i<k;i++){
					xValues[i]=wValues[i];
				}
			}

			if (iterationsNum  > 15000){
				divergence = true;
			}
			
			iterationsNum++;
		}
		while((rr > NON_NEGATIVE_STOP_VAL) && (!(divergence)));
		
		if (divergence){
			System.err.println("Divergence");
		}
		
		return xValues;
	}
}
