package objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utilities.Constants;
import utilities.Point3i;


/**
 * A subclass of Sensor, point sensors represent monitoring that occurs at fixed spatial point 
 */

public class PointSensor extends Sensor {
	
	protected List<Integer> locations = new ArrayList<Integer>();
	protected List<Float> times = new ArrayList<Float>();
	protected NodeStructure node;
	
	// Create a new Point Sensor at a random location, with a random number of moves
	// The sensor can move locations multiple times during the deployment
	public PointSensor(List<Integer> detectableNodes, String type, ScenarioSet set) {
		super(type, set.getSensorAlias(type));
    	node = set.getNodeStructure();
		// Calculate the number of moves allowed for this deployment
		int moves = Constants.random.nextInt(set.getMaxMoves()+1); //random number of moves
		String specificType = set.getSensorSettings(type).getSpecificType();
		int maxTimeIndex = node.getTimeSteps().size() - 1;
		// Loop over total moves and add a time and location for each move
    	for(int i=0; i<moves+1; i++) {
    		// Pick a random location
    		int newLocation = detectableNodes.get(Constants.random.nextInt(detectableNodes.size()));
    		// Skip the move if the sensor is already at the chosen new location
    		if(locations.size()>0 && locations.get(locations.size()-1).equals(newLocation)) continue;
    		// First time step should be at shortest time to detection
    		float newTime = 0;
    		if(times.size()==0) {
    			// Determine the earliest detection time for any scenario at this location
    			newTime = set.getShortestTTD(specificType, newLocation);
    		} else { //additional moves
    			// Pick a new random time index
        		int timeIndex = node.getTimeIndex(times.get(times.size()-1));
        		int newIndex = timeIndex + Constants.random.nextInt(maxTimeIndex - timeIndex);
        		newTime = node.getTimeAt(newIndex);
    		}
    		// Save values to locations and times
    		locations.add(newLocation);
    		times.add(newTime);
    		// Stop adding moves if we are already at the last time step
    		if(times.get(times.size()-1).equals(node.getTimeAt(maxTimeIndex))) break;
    	}
	}
	
	// Create a new Point Sensor at a fixed location, allowing no moves
	public PointSensor(int nodeNumber, String type, ScenarioSet set) {
		super(type, set.getSensorAlias(type));
		node = set.getNodeStructure();
		locations.add(nodeNumber);
		times.add(node.getTimeAt(0));
	}
	
	// Create an exact copy of an existing Point Sensor
	public PointSensor(PointSensor sensor) {
		super(sensor.type, sensor.alias);
		this.node = sensor.node;
		this.locations = sensor.getLocations();
		this.times = sensor.getTimes();
	}
	
	@Override
	public Sensor makeCopy() {
		return new PointSensor(this);
	}
	
	// Returns a string that defines the full history of the sensor
	@Override
	public String getFullSummary() {
		StringBuilder text = new StringBuilder();
		text.append((getSensorType()) + ": ");
		for (int i=0; i<times.size(); i++) {
			//text.append("time "+Constants.decimalFormat.format(i+1)+" = ");
			text.append(Constants.decimalFormat.format(times.get(i))+" "+node.getUnit("times"));
			String location = node.getXYZFromNodeNumber(locations.get(i)).toString();
			text.append(" "+location);
			if (i<times.size()-1) text.append(", ");
		}
		return text.toString();
	}
	
	// Returns a string summary that is used with the visualization tree
	@Override
	public String getSummary() {
		StringBuilder text = new StringBuilder();
		if (getSensorType().length() > 10) {
			text.append((getSensorType()).substring(0, 10)+" ");
		} else {
			text.append(getSensorType() + " ");
		}
		text.append(node.getXYZFromNodeNumber(locations.get(0)).toString());
		text.append(locations.size()==1 ? "" : " "+(locations.size()-1)+" moves");
		return text.toString();
	}
	
	@Override
	public List<Integer> getLocations() {
		return locations;
	}
	
	@Override
	public List<Integer> getLocationsAtTime(float time) {
		ArrayList<Integer> locations = new ArrayList<Integer>();
		// Loop through deployments, saving the most recent location
		for (int i=0; i<times.size(); i++) {
			if (times.get(i) <= time) {
				if(!locations.contains(this.locations.get(i))) //only unique locations, in case it returns to same location
					locations.add(this.locations.get(i));
			}
		}
		return locations;
	}
	
	@Override
	public List<Float> getTimes() {
		return times;
	}
	
	@Override
	public Float getTTD(ScenarioSet set, Scenario scenario) {
		float nextTime;
		// Point sensors can start at later times and can move to new locations
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
		return null;
	}
	
	@Override
	public Double getSensorCost(ScenarioSet set, float time) {
		// Evaluate cost equation
		// Available variables: time, installs
		// Not available variables: area, survey recurrence (not relevant for point sensors)
		
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
	
	
	//////////////////////////////////////////////////////
	//////// Functions for Modifying Point Sensor ////////
	//////////////////////////////////////////////////////
	
	// For point sensor, modifications involve: (1) change location, (2) add times, (3) remove times
	public boolean modifySensor(ScenarioSet set, Campaign campaign) throws IOException {
		
		ArrayList<String> options = new ArrayList<String>(Arrays.asList(
				"Change location","Add time","Remove time"));
		ArrayList<Integer> weights = new ArrayList<Integer>(Arrays.asList(6,2,2));
				
		while(!options.isEmpty()) {
			String choice = Constants.weightedRandomNumber(options, weights);
			System.out.print(choice+"... ");
			
			// Add nodes
			if(choice=="Change location") {
				if(mutateChangeLocation(set, campaign)) {
					System.out.println("Success");
					return true;
				}
			}
			// Add times
			if(choice=="Add time") {
				if(mutateAddTime(set, campaign)) {
					System.out.println("Success");
					return true;
				}
			}
			// Remove times
			if(choice=="Remove time") {
				if(mutateRemoveTime()) {
					System.out.println("Success");
					return true;
				}
			}
			//The action was unsuccessful, try something else
			weights.remove(options.indexOf(choice));
			options.remove(choice);
			
			System.out.println("Failed");
		}
		System.out.println("We were unable to modify the point sensor.");
		return false; //None of the mutate options were valid
	}
	
	private boolean mutateChangeLocation(ScenarioSet set, Campaign campaign) {
		// Pick a random location to remove
		int replaceIndex = Constants.random.nextInt(locations.size());
		
		// Pick a random location to add, from valid locations factoring in constraints
		List<Integer> detectableNodes = set.getDetectableNodesWithConstraints(type, campaign);
		// Check 1: Need to have a new move location
		if(detectableNodes.size()==0)
			return false;
		// Start at closer replacement nodes, then loop outward
		int newLocation = -1;
		Point3i start3i = Constants.nodeNumberToIJK(locations.get(replaceIndex), set.getNodeStructure().getIJKDimensions());
		int distance = 3; // start within 3 nodes of original location
		while (newLocation==-1) {
			List<Integer> copyDetectableNodes = new ArrayList<Integer>(detectableNodes);
			while (copyDetectableNodes.size()>0) {
				int compareNode = copyDetectableNodes.get(Constants.random.nextInt(copyDetectableNodes.size()));
				Point3i compare3i = Constants.nodeNumberToIJK(compareNode, set.getNodeStructure().getIJKDimensions());
				if (start3i.getI()+distance<compare3i.getI() || start3i.getI()-distance>compare3i.getI()) { //I
					copyDetectableNodes.remove(copyDetectableNodes.indexOf(compareNode));
					continue;
				}
				if (start3i.getJ()+distance<compare3i.getJ() || start3i.getJ()-distance>compare3i.getJ()) { //J
					copyDetectableNodes.remove(copyDetectableNodes.indexOf(compareNode));
					continue;
				}
				if (start3i.getK()+distance<compare3i.getK() || start3i.getK()-distance>compare3i.getK()) { //K
					copyDetectableNodes.remove(copyDetectableNodes.indexOf(compareNode));
					continue;
				}
				// Made it through all the checks, save and continue
				newLocation = compareNode;
				break;
			}
			distance += 3; // Keep increasing the distance by 3 nodes and try again
		}
		
		// Swap the old location for the new location
		locations.set(replaceIndex, newLocation);
		
		return true;
	}
	
	private boolean mutateAddTime(ScenarioSet set, Campaign campaign) {
		// Checks that would make this not possible
		// Check 1: No more available moves (adding a time implies moving the sensor to a new location)
		if(times.size()>=set.getMaxMoves())
			return false;
		// Check 2: Already too many times, no room to add
		int firstTimeIndex = node.getTimeIndex(times.get(0));
		int lastTimeIndex = node.getTimeSteps().size() - 1;
		if (lastTimeIndex - firstTimeIndex < times.size())
			return false;
		// Check 3: Must be an available location for the sensor
		// Pick a random location from valid locations, factoring in constraints
		List<Integer> detectableNodes = set.getDetectableNodesWithConstraints(type, campaign);
		if(detectableNodes.size()==0)
			return false;
		
		// Try to add a random unique time, must loop in case duplicate
		// Constant loop, must manually exit out
		while(lastTimeIndex>0) {
			int newTimeIndex = firstTimeIndex + Constants.random.nextInt(lastTimeIndex - firstTimeIndex);
			float newTime = node.getTimeAt(newTimeIndex);
			if(!times.contains(newTime)) {
				times.add(newTime);
				continue;
			}
		}
		int newLocation = detectableNodes.get(Constants.random.nextInt(detectableNodes.size()));
		locations.add(newLocation);
		
		// Now we need to sort so times are in order
		List<Integer> saveLocations = locations;
		List<Float> saveTimes = times;
		locations.clear();
		times.clear();
		while(locations.size()<saveLocations.size()) {
			float minTime = Float.MAX_VALUE;
			int correspondingLocation = 0;
			for(float time : saveTimes) {
				if(time<minTime) {
					minTime = time;
					correspondingLocation = saveLocations.get(saveTimes.indexOf(time));
				}
			}
			times.add(minTime);
			locations.add(correspondingLocation);
		}
		
		return true;
	}
	
	private boolean mutateRemoveTime() {
		// Checks that would make this not possible
		// Check 1: not enough times to remove
		if(times.size()<3)
			return false;
		// Pick a random time and remove
		int removeIndex = Constants.random.nextInt(times.size()-1)+1;
		float removeTime = times.get(removeIndex);
		times.remove(removeTime);
		return true;
	}

}
