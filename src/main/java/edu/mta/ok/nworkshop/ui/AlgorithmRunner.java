package edu.mta.ok.nworkshop.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;

import edu.mta.ok.nworkshop.Constants;
import edu.mta.ok.nworkshop.PredictorProperties;
import edu.mta.ok.nworkshop.PredictorProperties.Predictors;
import edu.mta.ok.nworkshop.PredictorProperties.PropertyKeys;
import edu.mta.ok.nworkshop.globaleffects.EffectAbstract;
import edu.mta.ok.nworkshop.model.UserIndexedModelRatings;
import edu.mta.ok.nworkshop.predictor.CombinePredictions;
import edu.mta.ok.nworkshop.predictor.ImprovedSVDFeaturePredictor;
import edu.mta.ok.nworkshop.predictor.ImprovedKNNPredictionResiduals;
import edu.mta.ok.nworkshop.predictor.KNNGlobalEffectPredictor;
import edu.mta.ok.nworkshop.predictor.KNNPredictor;
import edu.mta.ok.nworkshop.predictor.KNNPredictorAbstract;
import edu.mta.ok.nworkshop.predictor.KNNSVDPredictor;
import edu.mta.ok.nworkshop.predictor.PredictionTester;
import edu.mta.ok.nworkshop.predictor.Predictor;
import edu.mta.ok.nworkshop.predictor.SVDFeaturePredictor;
import edu.mta.ok.nworkshop.predictor.SVDPredictor;

/**
 * A simple JFrame class that provides buttons to run various prediction algorithms
 */
public class AlgorithmRunner extends javax.swing.JFrame implements Runnable, ActionListener {
    
    public AlgorithmRunner() {
    	
        this.setResizable(false);
        initComponents();
        
           // Divert the console output        
        try
		{
			PipedOutputStream pout=new PipedOutputStream(this.outputPin);
                        System.setOut(new PrintStream(pout,true));
                        PipedOutputStream perr=new PipedOutputStream(this.errPin);
			System.setErr(new PrintStream(perr,true));
		}
		catch (java.io.IOException io)
		{
			consoleOutput.append("Couldn't redirect STDOUT to this console\n"+io.getMessage());
		}
		catch (SecurityException se)
		{
			consoleOutput.append("Couldn't redirect STDOUT to this console\n"+se.getMessage());
        	}

        outputThread = new Thread(this);
        outputThread.setDaemon(true);
        outputThread.start();
        
        errThread = new Thread(this);
        errThread.setDaemon(true);
        errThread.start();        
        
        this.addWindowListener(new WindowEventListener());
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        consoleOutput = new javax.swing.JTextArea();
        clearConsoleButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        knnRunButton = new javax.swing.JButton();
        improvedKnnRunButton = new javax.swing.JButton();
        knnSVDRunButton = new javax.swing.JButton();
        svdRunButton = new javax.swing.JButton();
        improvedSVDRunButton = new javax.swing.JButton();
        globalEffectsRunButton = new javax.swing.JButton();
        combineEffectsRunButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Netflix Workshop Runner");

        consoleOutput.setColumns(20);
        consoleOutput.setEditable(false);
        consoleOutput.setRows(5);
        jScrollPane1.setViewportView(consoleOutput);

        clearConsoleButton.setText("Clear");
        clearConsoleButton.addActionListener(this);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(222, 222, 222)
                .addComponent(clearConsoleButton)
                .addContainerGap(251, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 530, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(clearConsoleButton)
                .addContainerGap())
        );

        knnRunButton.setText("Run KNN Predictor");
        knnRunButton.addActionListener(this);

        improvedKnnRunButton.setText("Run Improved KNN Predictor");
        improvedKnnRunButton.addActionListener(this);

        knnSVDRunButton.setText("Run KNN-SVD Predictor");
        knnSVDRunButton.addActionListener(this);

        svdRunButton.setText("Run SVD Predictor");
        svdRunButton.addActionListener(this);

        improvedSVDRunButton.setText("Run Improved SVD Predictor");
        improvedSVDRunButton.addActionListener(this);

        globalEffectsRunButton.setText("Run Global Effects Calculator");
        globalEffectsRunButton.addActionListener(this);

        combineEffectsRunButton.setText("Combine Predictions");
        combineEffectsRunButton.addActionListener(this);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(svdRunButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(globalEffectsRunButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(improvedSVDRunButton, 0, 0, Short.MAX_VALUE))
                        .addGap(71, 71, 71)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(knnSVDRunButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(improvedKnnRunButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(knnRunButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(188, 188, 188)
                        .addComponent(combineEffectsRunButton)))
                .addContainerGap(70, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(svdRunButton, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(improvedSVDRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(globalEffectsRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(knnRunButton, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(improvedKnnRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(knnSVDRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(combineEffectsRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        // Position the window at the center of the screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - getPreferredSize().width) / 2;
        int y = (screen.height - getPreferredSize().height) / 2;
        setLocation(x, y);

        pack();
    }

    /**
     * Clears the console output text area.
     * The method is called when the "clear" button is pressed.
     * 
     * @param evt the pressing event
     */
    private void clearConsoleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        clearConsoleOutput();
    }

	private void clearConsoleOutput() {
		consoleOutput.setCaretPosition(0);
		consoleOutput.setText("");
	}

    private double predicitionButtonActionPreformed(ActionEvent evt){
    	
    	Object source = evt.getSource();
    	double rmse = 0;
    	
    	if (source == this.combineEffectsRunButton){
    		rmse = combinePredictions();
    	}
    	else if (source == this.knnRunButton){
    		rmse = knnRunButton();
    	}
    	else if (source == this.improvedKnnRunButton){
    		rmse = improvedKnnRunButton(null);
    	}
    	else if (source == this.knnSVDRunButton){
    		rmse = knnSVDRunButton();
    	}
    	else if (source == this.svdRunButton){
    		rmse = svdRunButton();
    	}
    	else if (source == this.improvedSVDRunButton){
    		rmse = improvedSVDRunButton();
    	}
    	else if (source == this.globalEffectsRunButton){
    		try{
    			rmse = globalEffectsRunButton();
    		}
    		catch (Exception e) {
    			System.out.println("Error running global effects. error: " + e.getMessage());
    			e.printStackTrace();
			}
    	}
    	
    	return rmse;
    }
    
    /**
     * Runs the CombinePredictions class.
     * The method is called when the "Combine Predictions" button is pressed.
     */
    private double combinePredictions(){

    	clearConsoleOutput();
    	
    	File f = new File(Constants.NETFLIX_OUTPUT_DIR + "Predictions");
    	
        if (!f.exists() || !f.isDirectory() || f.list().length == 0){
        	System.out.println("Error running CombinePredictions, predictions directory doesn't exist or is empty");
        	return 0;
        }
        
        double rmse = 0;
        
		try {			
			rmse = new CombinePredictions().startPrediction(f.listFiles());
		} catch (NumberFormatException e) {
			outputPredictorMessage("Error running CombinePredictions. error: " + e.getMessage());
		} catch (IOException e) {
			outputPredictorMessage("Error running CombinePredictions. error: " + e.getMessage());
		}
		
		return rmse;
    }
    
    /**
     * Runs the KNNPredictor algorithm.
     * The method is called when the "Run KNN Predictor" button is pressed.
     * 
     * @param evt the pressing event
     */
    private double knnRunButton() {
    	
    	boolean residualModel = (PredictorProperties.getInstance().getPredictorIntProperty(Predictors.KNN, PropertyKeys.RESIDUALS_MODEL, 1) == 1);
    	KNNPredictorAbstract predictor = null;
    	
    	if (residualModel){
    		System.out.println("Running predictor with residuals of global effects");
    		predictor = new KNNGlobalEffectPredictor();
    	}
    	else{
    		System.out.println("Running predictor with raw ratings data");
    		predictor = new KNNPredictor();
    	}

    	return runPredictor(predictor, Predictors.KNN); 
    }
   
    private double improvedKnnRunButton(String interpolationFileName) {
    	
    	double retVal = 0;
    	
		String interpolationFile = Constants.NETFLIX_OUTPUT_DIR + PredictorProperties.getInstance().getPredictorStringProperty(Predictors.IMPROVED_KNN, PropertyKeys.INTERPOLATION_FILE_NAME, null);
		
		// Check if the configured interpolation file exists 
		if (!(interpolationFile != null && (new File(interpolationFile)).exists())){
			System.out.println("Error finding interpolation data file, file doesn't exist or non is configured");
		}
		else{    	
			ImprovedKNNPredictionResiduals predictor = new ImprovedKNNPredictionResiduals(interpolationFile);
			retVal = runPredictor(predictor, Predictors.IMPROVED_KNN); 
		}
        
        return retVal;
    }

    private double knnSVDRunButton() {
    	SVDPredictor svdPred = new ImprovedSVDFeaturePredictor(PredictorProperties.getInstance().getPredictorIntProperty(Predictors.IMPROVED_SVD, PropertyKeys.FEATURES_NUM));
    	KNNSVDPredictor predictor = new KNNSVDPredictor(svdPred);
    	predictor.setUserModel(new UserIndexedModelRatings());
    	
    	return runPredictor(predictor, Predictors.KNN_SVD);
    }

    @SuppressWarnings("unchecked")
	private double globalEffectsRunButton() throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    	
    	String effectClassPackage = "edu.mta.ok.nworkshop.globaleffects.";
    	
    	Class effectClassOld = this.getClass().getClassLoader().loadClass(effectClassPackage + "Effect1");
    	Class effectClass;
    	
    	Constructor effectConstruct = effectClassOld.getConstructor(String.class, String.class);
    	EffectAbstract effect = (EffectAbstract)effectConstruct.newInstance(PredictorProperties.getInstance().getUserIndexedModelFile(), 
    			PredictorProperties.getInstance().getMovieIndexedModelFile());
    	
    	double retVal = effect.startEffectCalculation();
    	
    	for (int i=2; i<12; i++){
    		effectClass = this.getClass().getClassLoader().loadClass(effectClassPackage + "Effect" + i);
    		
    		effectConstruct = effectClass.getConstructor(String.class, String.class, String.class, String.class, String.class);
        	effect = (EffectAbstract)effectConstruct.newInstance(Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_USER_INDEXED_MODEL_FILE_NAME, 
        			Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_MOVIE_INDEXED_MODEL_FILE_NAME, 
    				Constants.NETFLIX_OUTPUT_DIR + EffectAbstract.MOVIE_INDEXED_RESIDUAL_FILE_NAME + effectClassOld.getSimpleName() + ".data", 
    				Constants.NETFLIX_OUTPUT_DIR + EffectAbstract.USER_INDEXED_RESIDUAL_FILE_NAME + effectClassOld.getSimpleName() + ".data", 
    				Constants.NETFLIX_OUTPUT_DIR + EffectAbstract.MAIN_DIR_NAME + effectClassOld.getSimpleName() + ".data");
        	retVal = effect.startEffectCalculation();
        	
        	effectClassOld = effectClass;
    	}
    	
        return retVal;
    }

    private double improvedSVDRunButton() {
    	ImprovedSVDFeaturePredictor predictor = new ImprovedSVDFeaturePredictor(PredictorProperties.getInstance().getPredictorIntProperty(Predictors.IMPROVED_SVD, PropertyKeys.FEATURES_NUM));
    	return runPredictor(predictor, Predictors.IMPROVED_SVD);
    }

    private double svdRunButton() {
    	SVDFeaturePredictor predictor = new SVDFeaturePredictor();
		return runPredictor(predictor, Predictors.SVD);
    }

    private double runPredictor(Predictor predictor, Predictors predictorType){
    	
    	// Load the predictions output text file property value
    	String predictionsFile = PredictorProperties.getInstance().getPredictorStringProperty(predictorType, PropertyKeys.PREDICTIONS_FILE);
    	
    	if (predictionsFile!= null){
    		predictionsFile = Constants.NETFLIX_OUTPUT_DIR + Constants.DEFAULT_PREDICTORS_FLODER_NAME + "/" + predictionsFile;
    	}
    	
    	return PredictionTester.getProbeError(predictor, predictionsFile, PredictorProperties.getInstance().getProbeFile());
    }
    
    private synchronized String readLine(PipedInputStream in) throws IOException
	{
		String input="";
		do
		{
			int available=in.available();
			if (available==0) break;
			byte b[]=new byte[available];
			in.read(b);
			input=input+new String(b,0,b.length);
		}while( !input.endsWith("\n") &&  !input.endsWith("\r\n") && !quit);
		return input;
	}

     @Override
    public synchronized void run() {
         
    	try
		{
			while (Thread.currentThread()==outputThread){				
                
				try { 
					this.wait(100);
				}
				catch(InterruptedException ie) {
					
				}
				
				if (outputPin.available()!=0)
				{
					String input=this.readLine(outputPin);
					consoleOutput.append(input);
					consoleOutput.setCaretPosition(consoleOutput.getText().length());
				}
				if (quit) return;
			}

			while (Thread.currentThread()==errThread)
			{		
				try { 
					this.wait(100);
				}
				catch(InterruptedException ie) {
					
				}

				if (errPin.available()!=0)
				{
					String input=this.readLine(errPin);                                        
					consoleOutput.append("ERROR: " + input);    
					consoleOutput.setCaretPosition(consoleOutput.getText().length());
				}
				if (quit) return;
			}
		} catch (Exception e){
			consoleOutput.append("\nConsole reports an Internal error.");
			consoleOutput.append("The error is: "+e);
		}                                          
    }
     
    private void outputPredictorMessage(String msg){
     	System.out.println(" ==== " + msg + " ==== ");
    }
    
    private void startRunningPredictor(){
    	setButtons(false);
    	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
    
    private void finishRunningPredictor(){
   	 	setButtons(true);
   	 	setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   }
    
    private void setButtons(boolean enabled){
   	 	combineEffectsRunButton.setEnabled(enabled);
   	 	globalEffectsRunButton.setEnabled(enabled);
   	 	improvedKnnRunButton.setEnabled(enabled);
   	 	improvedSVDRunButton.setEnabled(enabled);
   	 	knnRunButton.setEnabled(enabled);
   	 	knnSVDRunButton.setEnabled(enabled);
   	 	svdRunButton.setEnabled(enabled);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AlgorithmRunner().setVisible(true);
            }
        });
    }
    
    private class WindowEventListener extends WindowAdapter{

        @Override
        public void windowClosed(WindowEvent e) {
            super.windowClosed(e);
            quit = true;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            super.windowClosing(e);
            quit = true;
        }        
    }
    
    @Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() == clearConsoleButton){
			clearConsoleButtonActionPerformed(e);
		}
		else{
			// Start running the prediction class in a dedicated thread
    		new Thread(new Runnable(){

    			@Override
    			public void run() {
    				
    				double rmse = 0;
    				
    				try{
    			    	// Clears the console output panel to reduce memory consumption
    			    	clearConsoleOutput();
    			    	
    					outputPredictorMessage("Start executing \"" + ((JButton)e.getSource()).getText() + "\"");
    					startRunningPredictor();    					
    					rmse = predicitionButtonActionPreformed(e);
    				}
    				finally{
    					finishRunningPredictor();
    					outputPredictorMessage("Finish running " + ((JButton)e.getSource()).getText() + ". RMSE = " + rmse);
    				}
    			}
    		}).start();
		}
	}
    
	private javax.swing.JButton clearConsoleButton;
    private javax.swing.JButton combineEffectsRunButton;
    private javax.swing.JTextArea consoleOutput;
    private javax.swing.JButton globalEffectsRunButton;
    private javax.swing.JButton improvedKnnRunButton;
    private javax.swing.JButton improvedSVDRunButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton knnRunButton;
    private javax.swing.JButton knnSVDRunButton;
    private javax.swing.JButton svdRunButton;
    private final PipedInputStream outputPin = new PipedInputStream();
    private final PipedInputStream errPin = new PipedInputStream();
    private boolean quit = false;
    Thread outputThread;
    Thread errThread;  
}
