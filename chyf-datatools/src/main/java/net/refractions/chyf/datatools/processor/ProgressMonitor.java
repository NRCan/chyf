package net.refractions.chyf.datatools.processor;

public class ProgressMonitor {

	private int taskLength;
	private int worked;
	
	public ProgressMonitor() {
		this.taskLength = 0;
		this.worked = 0;
	}
	
	public void setTaskLength(int length) {
		this.taskLength = length;
	}
	
	public void worked(int amount) {
		worked+=amount;
	}
	
	public int getPercentage() {
		if (taskLength == 0) return 0;
		return (int) (Math.round((worked / (double)taskLength) * 100));
	}
	
}
