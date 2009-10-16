package edu.mta.ok.nworkshop.utils;

public class TimeProgressOutput implements ProgressOutput {

	private static final String OUTPUT_TEMPLATE = "Finished %d %s, took %l millis"; 
	
	private String messageTemplate;
	private int outputThreshold;
	private int progressCounter;
	private long startTime;
	
	public TimeProgressOutput(String template, int outputThreshold) {
		super();

		this.messageTemplate = template;
		this.outputThreshold = outputThreshold;
		this.startTime = System.currentTimeMillis();
		this.progressCounter = 0;
	}

	@Override
	public void incrementProgressCounter() {
		progressCounter++;
		
		if (progressCounter % outputThreshold == 0){
			System.out.println(String.format(messageTemplate, progressCounter,
					(System.currentTimeMillis() - startTime)));
			startTime = System.currentTimeMillis();
		}
	}
}
