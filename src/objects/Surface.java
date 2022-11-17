package objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utilities.Constants;
import utilities.Point3i;

/**
 * Represents technology deployed on the surface
 */

public class Surface extends Sensor {

	private int low = 0;
	private int high = 0;
	private Point3i start;
	private int density = 1;

	protected List<Integer> mySurvey = new ArrayList<Integer>();
	protected List<Float> times = new ArrayList<Float>();
	protected NodeStructure node;

	// Create a new Surface at a random location, with a random number of surveys
	// The survey currently keeps the same nodes, but changes times
	public Surface(List<Integer> detectableNodes, String type, ScenarioSet set) {
		super(type, set.getSensorAlias(type));
		node = set.getNodeStructure();
		low = set.getMinSurveyLocations();
		high = set.getMaxSurveyLoactions();

		// First, determine the spatial extent of the survey
		// Randomly select a valid node for the start location
		int startPoint = randSelectInt(detectableNodes);
		start = Constants.nodeNumberToIJK(startPoint, node.getIJKDimensions());
		// Create a random survey around the point
		mySurvey = randomSurvey();
		System.out.println("Survey size " + mySurvey.size());
		// Determine the earliest TTD for the survey across all scenarios
		float earliestTTD = getEarliestTTD(set);
		// Populate random times from then until the end, up to the max number of times
		times = randomTimes(earliestTTD, set);
		System.out.println("Populating times for survey: " + times);
	}

	public Surface(Surface sensor) {
		super(sensor.type, sensor.alias);
		this.node = sensor.node;
		this.mySurvey = sensor.getLocations();
		this.times = sensor.getTimes();
		this.start = sensor.start;
		this.low = sensor.low;
		this.high = sensor.high;
	}

	
	// Start with a given node number and add LxW rows until we have a survey
	// This random survey starts at a provided location
	public List<Integer> randomSurvey() {
		// Get max length and width
		int nx = node.getX().size();
		int ny = node.getY().size();
		
		// TODO: Alex wants this between 1 - 5%
		density = 1+Constants.random.nextInt(3); //between 1-3 spacing
		
		// Determine a length and width for surveys that gets us within valid size
		int max = (int)Math.floor(Math.sqrt(high));
		int min = (int)Math.ceil(Math.sqrt(low));
		int x = min + Constants.random.nextInt(max - min);
		int y = min + Constants.random.nextInt(max - min);
		
		// Determine bounds in each direction
		// Aim for a survey size within the proper bounds
		// West
		int west = Constants.random.nextInt(x); //skew to the west or east of the start
		int east = x - west;
		int i0 = start.getI() - density*west;
		if(i0<0) i0 = 0; //prevents going outside domain edges
		// East
		int i1 = start.getI() + density*east;
		if(i1>nx) i1 = nx; //prevents going outside domain edges
		// South
		int south = Constants.random.nextInt(y); //skew to the south or north of the start
		int north = y - south;
		int j0 = start.getJ() - density*south;
		if(j0<0) j0 = 0; //prevents going outside domain edges
		// North
		int j1 = start.getJ() + density*north;
		if(j1>ny) j1 = ny; //prevents going outside domain edges
			
		// Now add all the points making up the survey as node numbers
		System.out.println("Creating a new random survey: "+i0+" "+i1+" "+j0+" "+j1+" (i0, i1, j0, j1), Density = "+density);
		return getSurveyFromEdges(i0, i1, j0, j1);
	}
	
	// Surface surveys should always start at the earliest TTD
	// Determine the shortest time to detection for all scenarios and nodes in the survey
	private float getEarliestTTD(ScenarioSet set) {
		float shortestTTD = Float.MAX_VALUE;
		for (Scenario scenario : set.getScenarios()) {
			Float ttd = getEarliestTTDforSurvey(scenario, set);
			if (ttd!=null && ttd < shortestTTD)
				shortestTTD = ttd;
		}
		return shortestTTD; // first time should be the shortest TTD
	}
	
	// Find the earliest TTD and randomly assign redeployments until end
	public List<Float> randomTimes(float earliestTTD, ScenarioSet set) {
		List<Float> timeList = new ArrayList<Float>();
		int firstTimeIndex = node.getTimeIndex(earliestTTD);
		int lastTimeIndex = node.getTimeSteps().size() - 1;
		// we have to check that firstTimeIndex is before lastTimeIndex
		// is earliestTTD ever null?
		// or if there's no detections at that node location, does earliestTTD just return
		// and otherwise maybe return an empty list or a boolean false?
		timeList.add(earliestTTD); //add the earliest time
		int addTimes = 1+Constants.random.nextInt(set.getMaxSurveys());
		System.out.println("First and Last time index: "+firstTimeIndex+" "+lastTimeIndex+", can pick up to "+addTimes);
		for (int i=1; i<addTimes; i++) {
			// Pick a new random time index
			if (lastTimeIndex==firstTimeIndex)
				break;
			int newTimeIndex = firstTimeIndex + Constants.random.nextInt(lastTimeIndex - firstTimeIndex);
			float newTime = node.getTimeAt(newTimeIndex);
			if(!timeList.contains(newTime))
				timeList.add(newTime);
			// Next time will have to come after this one, set new bound
			firstTimeIndex = newTimeIndex;
			// If no more times available, break
		}
		return timeList;
	}

	private Integer randSelectInt(List<Integer> list) {
		return list.get(Constants.random.nextInt(list.size()));
	}

	@Override
	public Sensor makeCopy() {
		return new Surface(this);
	}

	@Override
	public String getFullSummary() {
		StringBuilder text = new StringBuilder();
		text.append((getSensorType()) + ":");
		for (int i = 0; i < times.size(); i++) {
			text.append(" "+Constants.decimalFormat.format(times.get(i)));
		}
		text.append(" "+node.getUnit("times"));
		for (int i = 0; i < this.mySurvey.size(); i++) {
			String location = node.getXYZFromNodeNumber(mySurvey.get(i)).toString();
			text.append(" " + location);
		}
		return text.toString();
	}

	@Override
	public String getSummary() {
		StringBuilder text = new StringBuilder();
		if (getSensorType().length() > 10) {
			text.append((getSensorType()).substring(0, 10) + " ");
		} else {
			text.append(getSensorType() + " ");
		}
		text.append(times.size() + " surveys ");
		text.append(mySurvey.size() + " nodes");
		return text.toString();
	}

	@Override
	public List<Integer> getLocations() {
		return mySurvey;
	}

	@Override
	public List<Integer> getLocationsAtTime(float time) {
		ArrayList<Integer> locations = new ArrayList<Integer>();
		// Surveys are discrete, needs to match time exactly
		for (int i = 0; i < times.size(); i++) {
			if (times.get(i) == time) {
				locations.addAll(mySurvey);
				break; //Don't want duplicates, so break after adding once
			}
		}
		return locations;
	}

	@Override
	public List<Float> getTimes() {
		return times;
	}

	public Float getTTD(ScenarioSet set, Scenario scenario) {
		// Point sensors can start at later times and can move to new locations
		for (int i = 0; i < times.size(); i++) {
			// The time the current sensor was placed
			float currentTime = times.get(i);
			Float detectability = getEarliestTTDforSurvey(scenario, set);
			if(detectability==null)
				continue;
			// (1) If the threshold was exceeded before the sensor was placed, detection as
			// soon as sensor was placed
			if (detectability <= currentTime)
				return currentTime;
			// (2) Surface is a discrete survey rather than perpetual, detectability after a
			// survey isn't seen. This was a case for Point Sensor, doesn't work for surface.
			// else if(currentTime<detectability && detectability<nextTime)
			// 	return detectability;
			// (3) The sensor moved before detection, try the next location
			else
				continue;
		}
		return null;
	}

	public Float getEarliestTTDforSurvey(Scenario scenario, ScenarioSet set) {
		Float ttd = null;
		for (int location : mySurvey) {
			Float detection = set.getDetectionMap().get(set.getSensorSettings(type).getSpecificType()).get(scenario).get(location);
			if (detection == null)
				continue; // Some surface locations are not in the detection map
			if (ttd == null || detection < ttd)
				ttd = detection;
		}
		return ttd;
	}

	public Double getSensorCost(ScenarioSet set, float time) {
		// Evaluate cost equation
		// Available variables: area, number of surveys
		// Not available variables: time, installs (not relevant for surveys)
		float area = 0;
		for (int node : mySurvey) {
			area += set.getNodeStructure().getAreaOfNode(set.getNodeStructure().nodeNumberToIJK(node));
			// System.out.println(area);
		}
		// Cost should count how many survey recurrences by this time
		int count = 1; //start with 1 by default, for baseline measurement
		for (float surveyRecurrence : times) {
			count++;
			if (surveyRecurrence==time)
				break;
		}
		
		return Constants.evaluateCostExpression(set.getSensorSettings(getSensorType()).getSensorCostEq(), 0f, 0, area, count);
	}
	
	// Returns x0, x1, y0, y1 bounds for the survey
	private List<Integer> getEdgesOfSurvey(List<Integer> survey) {
		int i0 = Integer.MAX_VALUE;
		int i1 = Integer.MIN_VALUE;
		int j0 = Integer.MAX_VALUE;
		int j1 = Integer.MIN_VALUE;
		for(int nodeNumber : survey) {
			Point3i point = node.nodeNumberToIJK(nodeNumber);
			if(point.getI() < i0) i0 = point.getI();
			if(point.getI() > i1) i1 = point.getI();
			if(point.getJ() < j0) j0 = point.getJ();
			if(point.getJ() > j1) j1 = point.getJ();
		}
		return Arrays.asList(i0,i1,j0,j1);
	}
	
	// Return a survey, given x and y bounds, starting at the start point
	private List<Integer> getSurveyFromEdges(int i0, int i1, int j0, int j1) {
		List<Integer> survey = new ArrayList<Integer>();
		for(int i=start.getI(); i<=i1; i+=density) {
			for(int j=start.getJ(); j<=j1; j+=density) { //north-east
				int location = node.ijkToNodeNumber(i, j, start.getK());
				survey.add(location);
			}
			if(start.getJ()-density>j0) {
				for(int j=start.getJ()-density; j>=j0; j-=density) { //south-east
					int location = node.ijkToNodeNumber(i, j, start.getK());
					survey.add(location);
				}
			}
		}
		if(start.getI()-density>i0) {
			for(int i=start.getI()-density; i>=i0; i-=density) {
				if(start.getJ()-density>j0) {
					for(int j=start.getJ()-density; j>=j0; j-=density) { //south-west
						int location = node.ijkToNodeNumber(i, j, start.getK());
						survey.add(location);
					}
				}
				for(int j=start.getJ(); j<=j1; j+=density) { //north-west
					int location = node.ijkToNodeNumber(i, j, start.getK());
					survey.add(location);
				}
			}
		}
		return survey;
	}
	
	/////////////////////////////////////////////////
	//////// Functions for Modifying Surface ////////
	/////////////////////////////////////////////////
	
	// For surface, modifications involve: (1) add nodes, (2) remove nodes, (3) add times, (4) remove times
	public boolean modifySensor(ScenarioSet set, Campaign campaign) throws IOException {
		
		ArrayList<String> options = new ArrayList<String>(Arrays.asList(
				"Add nodes","Remove nodes","Alter spacing","Add times","Remove times"));
		ArrayList<Integer> weights = new ArrayList<Integer>(Arrays.asList(3,3,2,1,1));
				
		while(!options.isEmpty()) {
			String choice = Constants.weightedRandomNumber(options, weights);
			System.out.print(choice+"... ");
			
			// Add nodes
			if(choice=="Add nodes") {
				if(mutateAddNodes()) {
					System.out.println("Success");
					return true;
				}
			}
			// Remove nodes
			if(choice=="Remove nodes") {
				if(mutateRemoveNodes()) {
					System.out.println("Success");
					return true;
				}
			}
			// Alter survey spacing
			if(choice=="Alter spacing") {
				if(mutateAlterSpacing()) {
					System.out.println("Success");
					return true;
				}
			}
			// Add times
			if(choice=="Add times") {
				if(mutateAddTimes()) {
					System.out.println("Success");
					return true;
				}
			}
			// Remove times
			if(choice=="Remove times") {
				if(mutateRemoveTimes()) {
					System.out.println("Success");
					return true;
				}
			}
			//The action was unsuccessful, try something else
			weights.remove(options.indexOf(choice));
			options.remove(choice);
			
			System.out.println("Failed");
		}
		System.out.println("We were unable to modify the surface survey.");
		return false; //None of the mutate options were valid
	}
	
	private boolean mutateAddNodes() {
		// Determine the existing bounds of the survey
		// i0, i1, j0, j1
		List<Integer> bounds = getEdgesOfSurvey(mySurvey);
		int i0 = bounds.get(0);
		int i1 = bounds.get(1);
		int j0 = bounds.get(2);
		int j1 = bounds.get(3);
		int nx = node.getX().size();
		int ny = node.getY().size();
		int addRowSize = 1+(i1-i0)/density; //number of nodes in a new row
		int addColumnSize = 1+(j1-j0)/density; //number of nodes in a new column
		
		// Choose whether to add row(0) or column(1)
		int ch = Constants.random.nextInt(2);
		// Choose whether to add south/west(0) or north/east(1)
		int side = Constants.random.nextInt(2);
		int count = 0;
		
		// Constant loop, must manually exit out
		// North row -> South row -> east column -> west column ->
		while(bounds.size()>0) {
			count++;
			if(ch==0) { //row, south/north
				// First, check to verify we can add to this dimension
				// Would make survey too large || already at max possible rows
				if (mySurvey.size() + addRowSize > high || j1-j0 >= node.getY().size()) {
					ch = 1;
				} else if(side==0 && j0>density) {
					mySurvey = getSurveyFromEdges(i0, i1, j0-density, j1);
					return true;
				} else if (side==1 && j1<ny-density) {
					mySurvey = getSurveyFromEdges(i0, i1, j0, j1+density);
					return true;
				} else { //try another side
					if(side==1) { // north row -> south row
						side = 0;
					} else if(side==0) { // south row -> east column
						side = 1;
						ch = 1;
					}
				}
			} else if(ch==1) { //column, west/east
				// First, check to verify we can add to this dimension
				// Would make survey too large || already at max possible columns
				if (mySurvey.size() + addColumnSize > high || i1-i0 >= node.getX().size()) {
					ch = 0;
				} else if(side==0 && i0>density) {
					mySurvey = getSurveyFromEdges(i0-density, i1, j0, j1);
					return true;
				} else if (side==1 && i1<nx-density) {
					mySurvey = getSurveyFromEdges(i0, i1+density, j0, j1);
					return true;
				} else { //try another side
					if(side==1) { //east column -> west column
						side = 0;
					} else if(side==0) { //west column -> north row
						side = 1;
						ch = 0;
					}
				}
			}
			if(count==4)
				break;
		}
		return false;
	}
			
	private boolean mutateRemoveNodes() {
		// Determine the existing bounds of the survey
		// i0, i1, j0, j1
		List<Integer> bounds = getEdgesOfSurvey(mySurvey);
		int i0 = bounds.get(0);
		int i1 = bounds.get(1);
		int j0 = bounds.get(2);
		int j1 = bounds.get(3);
		int addRowSize = 1+(i1-i0)/density; //number of nodes in a new row
		int addColumnSize = 1+(j1-j0)/density; //number of nodes in a new column
		
		// Choose whether to remove row(0) or column(1)
		int ch = Constants.random.nextInt(2);
		// Choose whether to remove south/west(0) or north/east(1)
		int side = Constants.random.nextInt(2);
		int count = 0;
		
		// Constant loop, must manually exit out
		// North row -> South row -> east column -> west column ->
		while(bounds.size()>0) {
			count++;
			if(ch==0) { //row, south/north
				// First, check to verify we can add to this dimension
				// Would make survey too large || already at max possible rows
				if (mySurvey.size() - addRowSize < low || j1-j0 <= 2*density) { // Don't go less than 2 rows
					ch = 1;
				} else if(side==0) {
					mySurvey = getSurveyFromEdges(i0, i1, j0+density, j1);
					return true;
				} else if (side==1) {
					mySurvey = getSurveyFromEdges(i0, i1, j0, j1-density);
					return true;
				} else { //try another side
					if(side==1) { // north row -> south row
						side = 0;
					} else if(side==0) { // south row -> east column
						side = 1;
						ch = 1;
					}
				}
			} else if(ch==1) { //column, west/east
				// First, check to verify we can add to this dimension
				// Would make survey too large || already at max possible columns
				if (mySurvey.size() - addColumnSize < low || i1-i0 < 2*density) { // Don't go less than 2 columns
					ch = 0;
				} else if(side==0) {
					mySurvey = getSurveyFromEdges(i0+density, i1, j0, j1);
					return true;
				} else if (side==1) {
					mySurvey = getSurveyFromEdges(i0, i1-density, j0, j1);
					return true;
				} else { //try another side
					if(side==1) { //east column -> west column
						side = 0;
					} else if(side==0) { //west column -> north row
						side = 1;
						ch = 0;
					}
				}
			}
			if(count==4)
				break;
		}
		return false;
	}
		
	private boolean mutateAlterSpacing() {
		// Determine the existing bounds of the survey
		// i0, i1, j0, j1
		List<Integer> bounds = getEdgesOfSurvey(mySurvey);
		int i0 = bounds.get(0);
		int i1 = bounds.get(1);
		int j0 = bounds.get(2);
		int j1 = bounds.get(3);
		// Pick new survey density
		int previousDensity = density;
		int newDensity = 1+Constants.random.nextInt(3);
		// Loop until we pick a new density
		while(newDensity==previousDensity) {
			newDensity = 1+Constants.random.nextInt(3); //between 1-3 spacing
		}
		if(i1-i0 <= 2*newDensity) // Don't go less than 2 columns
			return false;
		if(j1-j0 <= 2*newDensity) // Don't go less than 2 rows
			return false;
		// Calculate the size with density factored in, to make sure we have the right number of nodes
		int iSize = 1+(start.getI()-i0)/newDensity+(i1-start.getI())/newDensity;
		int jSize = 1+(start.getJ()-j0)/newDensity+(j1-start.getJ())/newDensity;
		if(iSize*jSize < low || iSize*jSize > high) {
			return false;
		}
		
		// Valid change in density
		density = newDensity;
		mySurvey = getSurveyFromEdges(i0+1, i1, j0, j1);
		return true;
	}
	
	private boolean mutateAddTimes() {
		// Checks that would make this not possible
		int firstTimeIndex = node.getTimeIndex(times.get(0));
		int lastTimeIndex = node.getTimeSteps().size() - 1;
		// Check 1: Already too many times, no room to add
		if ((lastTimeIndex-firstTimeIndex) < times.size())
			return false;
		// Try to add a random unique time, must loop in case duplicate
		// Constant loop, must manually exit out
		if (firstTimeIndex>=lastTimeIndex) return false;
		
		while(times.size()>1) {
			int newTimeIndex = firstTimeIndex + Constants.random.nextInt(lastTimeIndex - firstTimeIndex)+1;
			float newTime = node.getTimeAt(newTimeIndex);
			if(!times.contains(newTime)) {
				times.add(newTime);
				return true;
			}
		}
		return false;
	}
	
	private boolean mutateRemoveTimes() {
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
