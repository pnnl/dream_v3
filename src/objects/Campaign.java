package objects;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import functions.ObjectiveValues;
import objects.SensorSetting.SensorType;
import functions.ObjectiveSelection.OBJECTIVE;
import utilities.Constants;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

/**
 * Base class for a particular distribution of sensors and its quality
 */

public class Campaign {
			
	protected List<Sensor> sensors = new ArrayList<Sensor>();	
	protected List<VerticalWell> verticalWells = new ArrayList<VerticalWell>();
	
	// Calculating costs is taxing, so save values so we don't have to repeat
	protected Map<Float, Double> campaignCost; // <Time, Cost>
	// The stored objective values are raw values
	protected Map<Scenario, ObjectiveValues> objectiveValues; // <Scenario, [TTD, VADAtDetection, Cost, Detected] Objectives>
	
	/**
	 * Initialize a random campaign bounded by user inputs
	 * @param ScenarioSet
	 * @throws IOException 
	 */
	public Campaign(ScenarioSet set, BufferedWriter fileOut) throws IOException {
		//TODO: fileOut not used, remove
		sensors = Collections.synchronizedList(new ArrayList<Sensor>());
		verticalWells = Collections.synchronizedList(new ArrayList<VerticalWell>());
		
		// Initialize these, they will be filled later
		campaignCost = Collections.synchronizedMap(new HashMap<Float, Double>());
		objectiveValues = Collections.synchronizedMap(new HashMap<Scenario, ObjectiveValues>());
		for(Scenario scenario : set.getScenarios()) {
			objectiveValues.put(scenario, new ObjectiveValues(0f, 0f, 0f, false));
		}
		
		addRandomSensors(set);
		// Update wells and sensor costs
		updateVerticalWells(set.getNodeStructure());
		calculateCostOfCampaign(set);
	}
	
	/**
	 * Create a copy of a campaign, usually preempts a mutation of some type
	 * @param ScenarioSet
	 * @param Campaign
	 */
	public Campaign(ScenarioSet set, Campaign toDuplicate) {
		// Initialize these, they will be filled later
		sensors = Collections.synchronizedList(new ArrayList<Sensor>());
		verticalWells = Collections.synchronizedList(new ArrayList<VerticalWell>());
		campaignCost = Collections.synchronizedMap(new HashMap<Float, Double>());
		objectiveValues = Collections.synchronizedMap(new HashMap<Scenario, ObjectiveValues>());
		for (Scenario scenario : toDuplicate.getObjectiveValues().keySet()) {
			objectiveValues.put(scenario, toDuplicate.getObjectiveValues().get(scenario));
		}
		
		// Add copies of sensors from copied campaign
		for (Sensor sensor : toDuplicate.getSensors()) {
			addSensor(sensor.makeCopy());
		}
		// Update wells and sensor costs
		updateVerticalWells(set.getNodeStructure());
		calculateCostOfCampaign(set);		
	}
	
	/**
	 * Helps create a random campaign by adding a random number of sensors, each of a random type
	 * @param ScenarioSet
	 */
	private synchronized void addRandomSensors(ScenarioSet set) {
		sensors.clear();
		
		// Pick a random number between the min and max sensors allowed
		int numberOfSensors = set.getMinSensors()+Constants.random.nextInt(set.getMaxSensors()-set.getMinSensors()+1);
		for (int i=0; i<numberOfSensors; i++) {
			// Pick a random sensor type from all possible
			String randomType = set.getDataTypes().get(Constants.random.nextInt(set.getDataTypes().size()));
			// Pick a random location from the list of detectable nodes
			List<Integer> detectableNodes = set.getDetectableNodesWithConstraints(randomType, this);
			// There may not be any detectable locations to place another sensor, skip
			if(detectableNodes.isEmpty())
				continue;
			// Add the new sensor
			addSensor(detectableNodes, randomType, set);
		}
	}
	
	
	/**
	 * Updates which wells are occupied by sensors
	 */
	private synchronized void updateVerticalWells(NodeStructure nodeStructure) {
		verticalWells.clear(); // Reset wells in case sensors changed
		
		// Loop over sensors
		for (Sensor sensor : sensors) {
			// Surface survey doesn't have an vertical wells, skip those sensors
			if(sensor instanceof Surface)
				continue;
			// Loop over sensor locations, might move to new locations
			for (int i=0; i<sensor.getLocations().size(); i++) {
				Point3i ijk = nodeStructure.nodeNumberToIJK(sensor.getLocations().get(i));
				float time = sensor.getTimes().get(0);
				VerticalWell well = new VerticalWell(ijk.getI(), ijk.getJ(), ijk.getK(), time, nodeStructure);
				addMergeVerticalWell(well);
			}
			if(sensor instanceof ERTSensor) { //also loop over paired wells for ERT
				for (int i=0; i<((ERTSensor)sensor).getLocationPairs().size(); i++) {
					Point3i ijk = nodeStructure.nodeNumberToIJK(((ERTSensor)sensor).getLocationPairs().get(i));
					float time = sensor.getTimes().get(i);
					VerticalWell wellPair = new VerticalWell(ijk.getI(), ijk.getJ(), ijk.getK(), time, nodeStructure);
					addMergeVerticalWell(wellPair);
				}
			}
		}
	}
	
	// Loop over existing wells, in case we already have a well at this location
	// If already a well, take greatest depth and earliest install time
	private void addMergeVerticalWell(VerticalWell well) {
		boolean found = false;
		for(int i=0; i<verticalWells.size(); i++) {
			if (verticalWells.get(i).isAt(well.getI(), well.getJ())) { //same vertical column
				verticalWells.get(i).mergeWells(well); //if found, merge
				found = true;
				break;
			}
		}
		if(!found) //new well location
			verticalWells.add(well);
	}
	
	public synchronized void updateVerticalWellsPublic(NodeStructure nodeStructure) {
		updateVerticalWells(nodeStructure);
	}

	public List<VerticalWell> getVerticalWells() {
		return verticalWells;
	}
	
	// Returns a string that defines all elements of the campaign
	// This is displayed to the left on the Run_DREAM page
	@Override
	public String toString() {
		StringBuilder toString = new StringBuilder();
		toString.append("Campaign: ");
		if (verticalWells.isEmpty()) {
			toString.append("Empty\n");
			return toString.toString();
		}

		toString.append("\nVertical Wells:\n");
		for (VerticalWell well : verticalWells) {
			toString.append("\n\tWell at " + well.toString() + "\n");
			for (Sensor sensor : sensors) {
				toString.append("\t\t" + sensor.toString() + "\n");
			}
		}

		toString.append("\n");

		Map<Scenario, Scenario> sorted = new TreeMap<Scenario, Scenario>();
		for (Scenario scenario : objectiveValues.keySet()) {
			sorted.put(scenario, scenario);
		}

		for (Scenario scenario : sorted.keySet()) {
			float ttd = objectiveValues.get(sorted.get(scenario)).getTTD();
			String ttdStr = ttd > 10000 ? Constants.exponentialFormat.format(ttd) : Constants.decimalFormat.format(ttd);
			toString.append("\tTime to detection for " + sorted.get(scenario).toString() + ": " + ttdStr + "\n");
		}

		sorted.clear();
		for (Scenario scenario : objectiveValues.keySet()) {
			sorted.put(scenario, scenario);
		}

		for (Scenario scenario : sorted.keySet()) {
			ObjectiveValues obj = objectiveValues.get(sorted.get(scenario));
			String objStr = obj.toString();
			toString.append("\tObjective values for " + sorted.get(scenario).toString() + ": " + objStr + "\n");
		}

		return toString.toString();
	}
	
	// Returns a string summary that is used with the visualization tree
	public String getSummary(NodeStructure nodeStructure) {
		StringBuffer summary = new StringBuffer();
		if (sensors.isEmpty()) //in case there are no sensors
			return "Empty";
		summary.append(verticalWells.size()+" wells");
		for (Sensor sensor : sensors)
			summary.append(", "+sensor.getSummary());
		return summary.toString();
	}
	
	public synchronized void addSensor(List<Integer> detectableNodes, String type, ScenarioSet set) {
		if(set.getSensorSettings(type).getSensorType().equals(SensorType.POINT_SENSOR))
			sensors.add(new PointSensor(detectableNodes, type, set));
		else if(set.getSensorSettings(type).getSensorType().equals(SensorType.SURFACE))
			sensors.add(new Surface(detectableNodes, type, set));
		//else if(set.getSensorSettings(type).getSensorType().equals(SensorType.CROSS_WELL))
		//	sensors.add(new ERTSensor(detectableNodes, type, set));
	}
	
	public synchronized void addSensor(Sensor sensor) {
		if (sensor instanceof PointSensor) {
			PointSensor pointSensor = (PointSensor)sensor;
			sensors.add(new PointSensor(pointSensor));
		} else if (sensor instanceof Surface) {
			Surface surfaceSensor = (Surface) sensor;
			sensors.add(new Surface(surfaceSensor));
		} else if (sensor instanceof ERTSensor) {
			ERTSensor ertSensor = (ERTSensor)sensor;
			sensors.add(new ERTSensor(ertSensor));
		}
	}
	
	public List<Sensor> getSensors() {
		return sensors;
	}
	
	/**
	 * Returns a list of locations for all sensors of a specific type in a campaign
	 * @param sensorType
	 * @return positions
	 */
	public synchronized List<Integer> getSensorPositions(String sensorType) {
		List<Integer> positions = new ArrayList<Integer>();
		for(Sensor sensor : sensors) {
			if(sensor.getSensorType().equals(sensorType)) {
				for(int location : sensor.getLocations())
					if (!positions.contains(location)) positions.add(location);
			}
		}
		return positions;
	}
	
	/**
	 * Creates a list of all scenarios that detect at a valid time
	 * @return ArrayList<Scenario>
	 */
	public ArrayList<Scenario> getDetectingScenarios() {
		ArrayList<Scenario> detectingScenarios = new ArrayList<Scenario>();
		for(Scenario scenario : objectiveValues.keySet()) {
			if(objectiveValues.get(scenario).getTTD()!=null)
				detectingScenarios.add(scenario);
		}
		return detectingScenarios;
	}
	
	/**
	 * Creates a list of all scenarios that do NOT detect
	 * @return ArrayList<Scenario>
	 */
	public ArrayList<Scenario> getNonDetectingScenarios() {
		ArrayList<Scenario> nonDetectingScenarios = new ArrayList<Scenario>();
		for(Scenario scenario : objectiveValues.keySet()) {
			if(objectiveValues.get(scenario).getTTD()==null)
				nonDetectingScenarios.add(scenario);
		}
		return nonDetectingScenarios;
	}
	
	
	/////////////////////////////////////////////////////////////////////
	//////////////// Helper Methods for Objectives Logic ////////////////
	/////////////////////////////////////////////////////////////////////
	public synchronized void calculateObjectives(STORMData data) {
		// Before we calculate objectives, update wells and sensor costs
		updateVerticalWells(data.getSet().getNodeStructure());
		calculateCostOfCampaign(data.getSet());
		Float cost = getAverageCampaignCost(); // Cost doesn't change by scenario
		// All other objectives depend on the scenario
		for (Scenario scenario : data.getSet().getScenarios()) {
			Float ttd = null;
			Float vadAtTTD = null;
			boolean detected = false;
			// TTD Objective
			if(data.getObjectives().contains(OBJECTIVE.TTD))
				ttd = data.getSet().getInferenceTest().getMinTTDForTests(getSensors(), data.getSet(), scenario);
			// VAD at Detection Objective
			if(data.getObjectives().contains(OBJECTIVE.VAD_AT_DETECTION) && ttd!=null)
				vadAtTTD = data.getSet().getVolumeDegradedAtTime(scenario, ttd);
			// Detected Objective
			if(data.getObjectives().contains(OBJECTIVE.SCENARIOS_DETECTED) && ttd!=null) {
				detected = true;
			}
			objectiveValues.put(scenario, new ObjectiveValues(ttd, vadAtTTD, cost, detected));
		}
	}
	
	public Map<Scenario, ObjectiveValues> getObjectiveValues() {
		return objectiveValues;
	}
	
	/**
	 * Returns the percent of scenarios detected
	 * @return float
	 */
	private synchronized float getPercentScenariosDetected() {
		int countScenarios = 0;
		for(Scenario scenario : objectiveValues.keySet()) {
			if(objectiveValues.get(scenario).getTTD()!=null) {
				countScenarios++;
			}
		}
		return (float)countScenarios/objectiveValues.size()*100;
	}
	
	/**
	 * Returns the weighted percent of scenarios detected
	 * @return float
	 */
	private synchronized float getWeightedPercentScenariosDetected(Map<Scenario, Float> scenarioWeights) {
		float sum = 0;
		float totalWeight = 0;
		for (Scenario scenario : objectiveValues.keySet()) {
			if(objectiveValues.get(scenario).getTTD()!=null) {
				sum += scenarioWeights.get(scenario);
			}
			totalWeight += scenarioWeights.get(scenario);
		}
		return sum / totalWeight * 100;
	}
	
	/**
	 * Returns the average time to detection of detecting scenarios
	 * @return float
	 */
	private synchronized Float getAverageTTD() {
		float sum = 0;
		int count = 0;
		for (Scenario scenario : objectiveValues.keySet()) {
			if(objectiveValues.get(scenario).getTTD()!=null) {
				sum += objectiveValues.get(scenario).getTTD();
				count++;
			}
		}
		if(sum==0.0 || count==0)
			return Float.MAX_VALUE;
		return sum / count;
	}
	
	/**
	 * Returns the weighted average time to detection of detecting scenarios
	 * @return float
	 */
	private synchronized Float getWeightedAverageTTD(Map<Scenario, Float> scenarioWeights) {
		float sum = 0;
		float totalWeight = 0;
		for (Scenario scenario : objectiveValues.keySet()) {
			if(objectiveValues.get(scenario).getTTD()!=null) {
				sum += objectiveValues.get(scenario).getTTD() * scenarioWeights.get(scenario);
				totalWeight += scenarioWeights.get(scenario);
			}
		}
		if(sum==0.0 || totalWeight==0.0)
			return Float.MAX_VALUE;
		return sum / totalWeight;
	}
	
	/**
	 * Returns the average volume of aquifer degraded at time of detection for detecting scenarios
	 * @return float
	 */
	private synchronized Float getAverageVADatTTD() {
		float sum = 0;
		int count = 0;
		for (Scenario scenario : objectiveValues.keySet()) {
			if(objectiveValues.get(scenario).getVADAtDetection()!=null) {
				sum += objectiveValues.get(scenario).getVADAtDetection();
				count++;
			}
		}
		if(sum==0.0 || count==0)
			return Float.MAX_VALUE;
		return sum / count;
	}
	
	/**
	 * Returns the weighted average volume of aquifer degraded at time of detection for detecting scenarios
	 * @return float
	 */
	private synchronized Float getWeightedAverageVADatTTD(Map<Scenario, Float> scenarioWeights) {
		float sum = 0;
		float totalWeight = 0;
		for (Scenario scenario : objectiveValues.keySet()) {
			if(objectiveValues.get(scenario).getVADAtDetection()!=null) {
				sum += objectiveValues.get(scenario).getVADAtDetection() * scenarioWeights.get(scenario);
				totalWeight += scenarioWeights.get(scenario);
			}
		}
		if(sum==0.0 || totalWeight==0.0)
			return Float.MAX_VALUE;
		return sum / totalWeight;
	}
	
	/**
	 * Sets the cost for a campaign per scenario and timestep, including: sensors, wells, well depth, remediation.
	 * @param ScenarioSet
	 */
	public Float calculateCostOfCampaign(ScenarioSet set) {
		// Cost equations may include time, so loop over available times
		for(TimeStep t : set.getNodeStructure().getTimeSteps()) {
			float time = t.getRealTime();
			double sensorCost = 0;
			for(Sensor sensor : getSensors())
				sensorCost += sensor.getSensorCost(set, time);
			double wellCost = set.getWellCost(this, time);
			campaignCost.put(time, sensorCost+wellCost);
		}
		return campaignCost.get(set.getNodeStructure().getMaxTime()).floatValue();
	}
	
	public Map<Float, Double> getCampaignCost() {
		return campaignCost;
	}
	
	private float getAverageCampaignCost() {
		Double cost = 0.0;
		for(float time : campaignCost.keySet()) {
			cost += campaignCost.get(time);
		}
		return cost.floatValue()/campaignCost.size();
	}
	
	/*private float getMaxCampaignCost() {
		Double maxCost = 0.0;
		for(float time : campaignCost.keySet()) {
			if(campaignCost.get(time)>maxCost)
				maxCost = campaignCost.get(time);
		}
		return maxCost.floatValue();
	}*/
	
	public Float getObjectiveValue(OBJECTIVE objective, boolean weighted, Map<Scenario, Float> scenarioWeights) {
		if(objective==OBJECTIVE.COST) //does not differ by scenario
			return getAverageCampaignCost();
		if(weighted) { //weighted
			if(objective==OBJECTIVE.TTD)
				return getWeightedAverageTTD(scenarioWeights);
			else if(objective==OBJECTIVE.VAD_AT_DETECTION)
				return getWeightedAverageVADatTTD(scenarioWeights);
			else if(objective==OBJECTIVE.SCENARIOS_DETECTED)
				return getWeightedPercentScenariosDetected(scenarioWeights);
		} else { //non-weighted
			if(objective==OBJECTIVE.TTD)
				return getAverageTTD();
			else if(objective==OBJECTIVE.VAD_AT_DETECTION)
				return getAverageVADatTTD();
			else if(objective==OBJECTIVE.SCENARIOS_DETECTED)
				return getPercentScenariosDetected();
		}
		return null;
	}
	
	public String printObjectiveSummary(STORMData data) {
		StringBuilder summary = new StringBuilder();
		if(getAverageTTD()==Float.MAX_VALUE) {
			summary.append("No detections");
			if(data.getObjectives().contains(OBJECTIVE.COST)) {
				summary.append(", Cost ");
				summary.append(Constants.formatCost(data.getSet().getCostUnit(), getAverageCampaignCost()));
			}
		} else {
			// Always include TTD
			summary.append("TTD ");
			summary.append(Constants.decimalFormat.format(getAverageTTD()));
			summary.append(" "+data.getSet().getNodeStructure().getUnit("times"));
			// All other objectives only when selected
			if(data.getObjectives().contains(OBJECTIVE.SCENARIOS_DETECTED)) {
				summary.append(", Scenarios Detected ");
				summary.append(Constants.percentageFormat.format(getPercentScenariosDetected()));
				summary.append("%");
			}
			if(data.getObjectives().contains(OBJECTIVE.COST)) {
				summary.append(", Cost ");
				summary.append(Constants.formatCost(data.getSet().getCostUnit(), getAverageCampaignCost()));
			}
			if(data.getObjectives().contains(OBJECTIVE.VAD_AT_DETECTION)) {
				summary.append(", VAD at Detection ");
				summary.append(Constants.decimalFormat.format(getAverageVADatTTD()));
				summary.append(" "+data.getSet().getNodeStructure().getUnit("x")+"^3");
			}
		}
		return summary.toString();
	}
	
	public String printWeightedObjectiveSummary(STORMData data) {
		StringBuilder summary = new StringBuilder();
		if(getWeightedAverageTTD(data.getSet().getScenarioWeights())==Float.MAX_VALUE) {
			summary.append("No detections");
			if(data.getObjectives().contains(OBJECTIVE.COST)) {
				summary.append(", Cost ");
				summary.append(Constants.formatCost(data.getSet().getCostUnit(), getAverageCampaignCost()));
			}
		} else {
			// Always include TTD
			summary.append("TTD ");
			summary.append(Constants.decimalFormat.format(getWeightedAverageTTD(data.getSet().getScenarioWeights())));
			summary.append(" "+data.getSet().getNodeStructure().getUnit("times"));
			// All other objectives only when selected
			if(data.getObjectives().contains(OBJECTIVE.SCENARIOS_DETECTED)) {
				summary.append(", Scenarios Detected ");
				summary.append(Constants.percentageFormat.format(getWeightedPercentScenariosDetected(data.getSet().getScenarioWeights())));
				summary.append("%");
			}
			if(data.getObjectives().contains(OBJECTIVE.COST)) {
				summary.append(", Cost ");
				summary.append(Constants.formatCost(data.getSet().getCostUnit(), getAverageCampaignCost()));
			}
			if(data.getObjectives().contains(OBJECTIVE.VAD_AT_DETECTION)) {
				summary.append(", VAD at Detection ");
				summary.append(Constants.decimalFormat.format(getWeightedAverageVADatTTD(data.getSet().getScenarioWeights())));
				summary.append(" "+data.getSet().getNodeStructure().getUnit("x")+"^3");
			}
		}
		return summary.toString();
	}
	
	
	/////////////////////////////////////////////////////////////////
	//////////////// Helper Methods for Mutate Logic ////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * We want to take an existing campaign and modify it with one 
	 * randomly-selected action.
	 * @param ScenarioSet
	 * @throws IOException 
	 */
	public synchronized boolean randomMutation(final ScenarioSet set, BufferedWriter writer) throws IOException {
		return randomMutationWeights(set, writer, 1,1,8 );
	}

	public synchronized boolean randomMutationWeights(final ScenarioSet set, BufferedWriter writer, int p1, int p2, int p3) throws IOException {		
		ArrayList<String> options = new ArrayList<String>(Arrays.asList(
				"Add sensor", "Remove sensor", "Modify sensor"));
		ArrayList<Integer> weights = new ArrayList<Integer>(Arrays.asList(1,1,8));
		
		while(!options.isEmpty()) {
			String choice = Constants.weightedRandomNumber(options, weights);
			System.out.print(choice+"...");
			
			// Add sensor
			if(choice=="Add sensor") {
				if(mutateAddSensor(set)) {
					System.out.println("Success");
					return true;
				}
			}
			// Remove sensor
			if(choice=="Remove sensor") {
				if(mutateRemoveSensor(set)) {
					System.out.println("Success");
					return true;
				}
			}
			// Modify sensor
			if(choice=="Modify sensor") {
				if(mutateModifySensor(set)) {
					return true;
				}
			}
			
			//The action was unsuccessful, try something else
			weights.remove(options.indexOf(choice));
			options.remove(choice);
			System.out.println("Failed");
		}
		System.out.println("Warning: We weren't able to mutate in any way...");
		return false; //None of the actions were valid (should never do this)
	}
	
	private boolean mutateAddSensor(final ScenarioSet set) {
		// First check that we can add another sensor
		if(sensors.size()>=set.getMaxSensors()) return false;
		
		// Get the available types and shuffle so we pick a random type
		List<String> types = set.getDataTypes();
		Collections.shuffle(types, Constants.random);
		
		// Loop over available types in case the first type doesn't have detectable nodes
		for(String type : types) {
			// Get a list of valid locations, factoring in constraints
			List<Integer> detectableNodes = set.getDetectableNodesWithConstraints(type, this);
			if(detectableNodes.size()==0) continue;
			
			// Create a new sensor based on the type and location
			System.out.print(type+"...");
			addSensor(detectableNodes, type, set);
			
			return true;
		}
		return false; //Failed for all available types
	}
	
	private boolean mutateRemoveSensor(final ScenarioSet set) {
		// First check that we can remove another sensor
		if(sensors.size()<=set.getMinSensors())
			return false;
		// Pick a random sensor to remove
		Sensor sensor = sensors.get(Constants.random.nextInt(sensors.size()));
		sensors.remove(sensor);
		return true;
	}
	
	private boolean mutateModifySensor(final ScenarioSet set) throws IOException {
		// In case there are no sensors yet
		if(sensors.size()==0) return false;
		
		// Pick a random sensor to move
		int id = Constants.random.nextInt(sensors.size());
		Sensor sensor = sensors.get(id);
		
		// Mutation functions vary based on the type of sensor
		return sensor.modifySensor(set, this);
	}
	
}
