package functions;

import utilities.Constants;

// This class stores values for each scenario
public class ObjectiveValues {
	
	private Float ttd;
	private Float vadAtDetection;
	private Float cost;
	private boolean detected;

	public ObjectiveValues(final Float ttd, final Float vadAtDetection, final Float cost, final boolean detected) {
		this.ttd = ttd;
		this.vadAtDetection = vadAtDetection;
		this.cost = cost;
		this.detected = detected;
	}
	
	@Override
	public String toString() {
		StringBuilder toString = new StringBuilder();
		if(ttd==null) {
			toString.append("No Detection, ");
			toString.append(Constants.decimalFormat.format(cost));
		} else {
			toString.append(Constants.decimalFormat.format(ttd)+", ");
			toString.append(Constants.decimalFormat.format(vadAtDetection)+", ");
			toString.append(Constants.decimalFormat.format(cost));
		}
		return toString.toString();
	}
	
	public Float getTTD() {
		return ttd;
	}
	
	public Float getVADAtDetection() {
		return vadAtDetection;
	}
	
	public Float getCost() {
		return cost;
	}
	
	public boolean getDetected() {
		return detected;
	}
	
	public void setTTD(final Float ttd) {
		this.ttd = ttd;
	}
	
	public void setVADAtDetection(final Float vadAtDetection) {
		this.vadAtDetection = vadAtDetection;
	}
	
	public void setCost(final Float cost) {
		this.cost = cost;
	}
	
	public void setScenariosDetected(final boolean detected) {
		this.detected = detected;
	}
}
