package objects;

import java.io.IOException;
import java.util.List;

/**
 * Represents basic point sensors for a specific parameter
 */

public abstract class Sensor {
	
	// What type of sensor
	protected String type;
	protected String alias;
    
    public Sensor(String type, String alias) {
    	this.type = type;
    	this.alias = alias;
    }
	
	public String getSensorType() {
		return type;
	}
	
	public void setSensorType(String type, String alias) {
		this.type = type;
		this.alias = alias;
	}
	
	public String getSensorAlias() {
		return alias;
	}
	
	public abstract Sensor makeCopy();
	public abstract String getFullSummary();
	public abstract String getSummary();
	public abstract List<Integer> getLocations();
	public abstract List<Integer> getLocationsAtTime(float time);
	public abstract List<Float> getTimes();
	public abstract Float getTTD(ScenarioSet set, Scenario scenario);
	public abstract Double getSensorCost(ScenarioSet set, float time);
	public abstract boolean modifySensor(ScenarioSet set, Campaign campaign) throws IOException;
}
