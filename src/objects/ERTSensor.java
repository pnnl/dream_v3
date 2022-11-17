package objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utilities.Constants;

public class ERTSensor extends Sensor {
	
	protected List<Integer> locations = new ArrayList<Integer>();
	protected List<Integer> locationPairs = new ArrayList<Integer>();
	protected List<Float> times = new ArrayList<Float>();
	protected NodeStructure node;
	
	public ERTSensor(List<Integer> detectableNodes, String type, ScenarioSet set) {
		super(type, set.getSensorAlias(type));
		node = set.getNodeStructure();
		// Calculate the number of moves allowed for this deployment
		int moves = Constants.random.nextInt(set.getMaxMoves()+1); //random number of moves
		String specificType = set.getSensorSettings(type).getSpecificType();
		float threshold = Float.parseFloat(type.substring(type.lastIndexOf("_")+1, type.length()));
		int maxTimeIndex = set.getNodeStructure().getTimeSteps().size() - 1;
		// Loop over total moves and add a time and location for each move
    	for(int i=0; i<moves+1; i++) {
    		// Pick a random location and location pair
    		int newLocation = detectableNodes.get(Constants.random.nextInt(detectableNodes.size()));
    		int newLocationPair = E4DSensorSettings.getWellPairing(threshold, newLocation);
    		// Skip the move if the sensor is already at the chosen new location
    		if(locations.size()>0 && locations.get(locations.size()-1).equals(newLocation)) continue;
    		// First time step should be at shortest time to detection
    		float newTime = 0;
    		if(times.size()==0) {
    			// Determine the earliest detection time for any scenario at this location
    			newTime = set.getShortestTTD(specificType, newLocation);
    		} else { //additional moves
    			// Pick a new random time index
        		int timeIndex = set.getNodeStructure().getTimeIndex(times.get(times.size()-1));
        		int newIndex = timeIndex + Constants.random.nextInt(maxTimeIndex - timeIndex);
        		newTime = set.getNodeStructure().getTimeAt(newIndex);
    		}
    		// Save values to locations, location pairs, and times
    		locations.add(newLocation);
    		locationPairs.add(newLocationPair);
    		times.add(newTime);
    		// Stop adding moves if we are already at the last time step
    		if(times.get(times.size()-1).equals(set.getNodeStructure().getTimeAt(maxTimeIndex))) break;
    	}
	}
	
	// Create an exact copy of an existing ERT Sensor
	public ERTSensor(ERTSensor sensor) {
		super(sensor.type, sensor.alias);
		this.node = sensor.node;
		this.locations = sensor.getLocations();
		this.locationPairs = sensor.getLocationPairs();
		this.times = sensor.getTimes();
	}
	
	public Sensor makeCopy() {
		return new ERTSensor(this);
	}
	
	// Returns a string that defines the full history of the sensor
	public String getFullSummary() {
		StringBuilder text = new StringBuilder();
		text.append((getSensorType()) + ": ");
		for (int i=0; i<times.size(); i++) {
			//text.append("time "+Constants.decimalFormat.format(i+1)+" = ");
			text.append(Constants.decimalFormat.format(times.get(i))+" "+node.getUnit("times"));
			String location = node.getXYZFromNodeNumber(locations.get(i)).toString();
			String locationPair = node.getXYZFromNodeNumber(locationPairs.get(i)).toString();
			text.append(" "+location+" "+locationPair);
			if (i<times.size()-1) text.append(", ");
		}
		return text.toString();
	}
	
	// Returns a string summary that is used with the visualization tree
	public String getSummary() {
		StringBuilder text = new StringBuilder();
		text.append((getSensorType()).substring(0, 10)+" ");
		text.append(node.getXYZFromNodeNumber(locations.get(0)).toString()+" ");
		text.append(node.getXYZFromNodeNumber(locationPairs.get(0)).toString());
		text.append(locations.size()==1 ? "" : " "+(locations.size()-1)+" moves");
		return text.toString();
	}
	
	public void setLocation(int nodeNumber) {
		locations.set(0, nodeNumber);
	}
	
	public List<Integer> getLocations() {
		return locations;
	}
	
	public List<Integer> getLocationPairs() {
		return locationPairs;
	}
	
	public List<Integer> getLocationsAtTime(float time) {
		ArrayList<Integer> locations = new ArrayList<Integer>();
		// Loop through deployments, saving the most recent location
		for(int i=0; i<times.size(); i++) {
			if(times.get(i)<=time)
				locations.add(locations.get(i));
		}
		return locations;
	}
	
	public Integer getLocationPairAtTime(float time) {
		// Loop through deployments, greater than the input time means we need the previous location Pair
		for(int i=0; i<times.size(); i++) {
			if(times.get(i) > time)
				return locationPairs.get(i-1);
		}
		// If the deployment time is not surpassed, use the last location Pair
		return locationPairs.get(locationPairs.size()-1);
	}
	
	public List<Float> getTimes() {
		return times;
	}
	
	public Float getTimeAtLocation(int nodeNumber) {
		for(int i=0; i<locations.size(); i++) {
			if(locations.get(i).equals(nodeNumber))
				return times.get(i);
		}
		return null;
	}
	
	public Float getTTD(ScenarioSet set, Scenario scenario) {
		float nextTime;
		// ERT sensors can start at later times and can move to new locations
		for(int i=0; i<times.size(); i++) {
			// The time of next move or non-detection value
			if(times.size()>i+1)
				nextTime = times.get(i+1);
			else
				nextTime = set.getNodeStructure().getMaxTime()+1;
			// The time the current sensor was placed
			float currentTime = times.get(i);
			Float detectability = set.getDetectionMap().get(set.getSensorSettings(type).getSpecificType()).get(scenario).get(locations.get(i));
			if(detectability==null) continue; //IAM doesn't always have a value for all locations with all scenarios
			// (1) If the threshold was exceeded before the sensor was placed, detection as soon as sensor was placed
			if(detectability<=currentTime)
				return currentTime;
			// (2) Exceed threshold after sensor was placed, as long as sensor didn't move before detection
			else if(currentTime<detectability && detectability<nextTime)
				return detectability;
			// (3) The sensor moved before detection, try the next location
			else
				continue;
		}
		return set.getNodeStructure().getMaxTime()+1;
	}
	
	public Double getSensorCost(ScenarioSet set, float time) {
		// Evaluate cost equation
		// Available variables: time, installs
		// Not available variables: area, surveys (not relevant for cross well)
		
		// Case 1: the sensor hasn't been installed yet
		if (time < times.get(0))
			return 0.0;
		
		// Case 2: at least one install
		float installTime = 0;
		int installs = 0;
		for (float timeLoop : times) {
			if(time < timeLoop) //hasn't reached this time yet
				break;
			installs++;
			installTime = time - times.get(0); //time since initial install
		}
		
		return Constants.evaluateCostExpression(set.getSensorSettings(getSensorType()).getSensorCostEq(), installTime, installs, 0f, 0);
	}
	
	// For cross well, modifySensor simply moves one sensor to another location
	// TODO: This isn't going to work
	public boolean modifySensor(ScenarioSet set, Campaign campaign) throws IOException {
		
		// Pick a random location from valid locations, factoring in constraints
		List<Integer> detectableNodes = set.getDetectableNodesWithConstraints(type, campaign);
		if(detectableNodes.size()==0) return false;
		
		// Remove the old sensor
		campaign.getSensors().remove(this);
		
		// Create a new point sensor at a random location
		campaign.addSensor(this);
		
		return true;
	}
	
}
