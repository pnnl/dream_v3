package objects;

import objects.SensorSetting.DeltaType;
import objects.SensorSetting.SensorType;
import objects.SensorSetting.Trigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utilities.Constants;
import utilities.Point3f;
import utilities.Point3i;
import wizardPages.Page_LeakDefinition.LeakData;


/**
 * Selections made by the user for a given run should be set here.
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
 */
public class ScenarioSet {
		
	/**
	 *  Scenarios must share common node structure
	 */
	private NodeStructure nodeStructure;
	private List<Scenario> allScenarios;
	private List<Scenario> scenarios;
	private List<VerticalWell> wells;
	
	// detectionMap stores values of TTD for all scenarios and specific sensors
	// IAM files are immediately loaded into detectionMap
	// H5 files are loaded to detectionMap at Page_DefineSensors based on user input settings, and saved as more are added
	private Map<String, Map<Scenario, Map<Integer, Float>>> detectionMap; //Specific Type <Scenario <Node Number, TTD>> 
	
	// varianceMap stores statistical variance for each parameter, location, and scenario
	// Variance is calculated as (value - mean across scenarios)^2
	private Map<String, Map<Scenario, Map<Float, Map<Integer, Float>>>> varianceMap; //Specific Type <Scenario, <Time, <Node Number, Variance>>
	
	/**
	 * User settings - 
	 */
	private int maxSensors;
	private int minSensors;
	private int maxMoves;
	private int maxSurveyLocations;
	private int minSurveyLocations;
	private int maxSurveys;
	private int maxWells;
	private int iterations;
	private String maxCostEq; //this is a String to allow equations
	private float exclusionRadius;
	private String wellCostEq; //this is a String to allow equations
	private String wellDepthCostEq; //this is a String to allow equations
	private String scenarioEnsemble; //the name of the group of scenarios, taken from the folder name
	private String costUnit; //symbol to represent cost unit
	
	private boolean equalWeights;
	private Map<Scenario, Float> scenarioWeights;
	private Map<String, SensorSetting> sensorSettings;
	private Map<String, SensorSetting> leakSettings;
	//Finding nodes removes some sensor settings, causing problems going back a page and then forward again
	//When removing sensor settings, they are instead saved in this Hashmap
	private Map<String, SensorSetting> sensorSettingsRemoved;
	private Map<String, String> sensorAliases;
	
	private InferenceTest inferenceTest;
	
	// Leak Nodes per scenario
	private Map<String, Map<Scenario, Set<Integer>>> leakNodes; //Parameter, Scenario, Nodes
	private static Map<Scenario, HashMap<Float, Float>> volumeDegradedByTime; //Scenario <time, VAD>
	
	
	// Reset everything in ScenarioSet - do this before loading new files
	public ScenarioSet() {
		allScenarios = new ArrayList<Scenario>();
		scenarios = new ArrayList<Scenario>();
		wells = new ArrayList<VerticalWell>();
		leakNodes = new HashMap<String, Map<Scenario, Set<Integer>>>();
		
		detectionMap = new HashMap<String, Map<Scenario, Map<Integer, Float>>>();
		varianceMap = new HashMap<String, Map<Scenario, Map<Float, Map<Integer, Float>>>>();
		
		nodeStructure = null;
		equalWeights = true;
		scenarioWeights = new HashMap<Scenario, Float>();
		leakSettings = new HashMap<String, SensorSetting>();
		sensorSettings = new HashMap<String, SensorSetting>();
		sensorSettingsRemoved = new HashMap<String, SensorSetting>();
		scenarioEnsemble = "";
		costUnit = "$";
		maxMoves = 0;
		maxSurveys = 5;
		minSensors = 1;
		maxSensors = 5;
		maxWells = 1000000;
		iterations = 10000;
		maxCostEq = "3E38";
		exclusionRadius = 0;
		wellCostEq = "5000";
		wellDepthCostEq = "10";
	}
	
	
	public void clearRun() {		
		allScenarios.clear();
		scenarios.clear();
		wells.clear();
		
		detectionMap.clear();
		varianceMap.clear();
		
		nodeStructure = null;
		equalWeights = true;
		scenarioWeights.clear();
		leakSettings.clear();
		sensorSettings.clear();
		sensorSettingsRemoved.clear();
		scenarioEnsemble = "";
		costUnit = "$";
		maxMoves = 0;
		maxSurveys = 5;
		minSensors = 1;
		maxSensors = 5;
		maxWells = 1000000;
		iterations = 10000;
		maxCostEq = "3E38";
		exclusionRadius = 0;
		wellCostEq = "5000";
		wellDepthCostEq = "10";
	}
	
	
	@Override
	public String toString() {
		boolean hasPointSensor = false;
		boolean hasSurface = false;
		String zUnit = nodeStructure.getUnit("z");
		int size = nodeStructure.getIJKDimensions().getI() * nodeStructure.getIJKDimensions().getJ() * nodeStructure.getIJKDimensions().getK();
		StringBuilder builder = new StringBuilder();
		builder.append("---- Input Summary ----\r\n");
		
		// Details about the scenario set read from the file being used
		builder.append("Scenario ensemble: " + scenarioEnsemble + "\r\n");
		builder.append("Scenario weights:\r\n");
		if(equalWeights)
			builder.append("\tEqually weighted\r\n");
		else {
			for(Scenario scenario: scenarios)
				builder.append("\t" + scenario.toString() + " = " + Constants.integerFormat.format(scenarioWeights.get(scenario)) + "\r\n");
		}
		
		// Leak definition
		builder.append("Leak Definition:\r\n");
		for(String parameter: leakSettings.keySet()) {
			String unit = nodeStructure.getUnit(parameter);
			SensorSetting leakSetting = leakSettings.get(parameter);
			builder.append("\t" + parameter + ":\r\n");
			builder.append("\t\tTriggering on: " + leakSetting.getTrigger() + "\r\n");
			if(leakSetting.getTrigger() == Trigger.BELOW_THRESHOLD || leakSetting.getTrigger() == Trigger.ABOVE_THRESHOLD) {
				builder.append("\t\tLeak threshold: " + leakSetting.getDetectionThreshold() + "\r\n");
			} else {
				builder.append("\t\tLeak threshold: ");
				if(leakSetting.getDeltaType() == DeltaType.DECREASE) builder.append("-");
				if(leakSetting.getDeltaType() == DeltaType.INCREASE) builder.append("+");
				if(leakSetting.getTrigger() == Trigger.ABSOLUTE_CHANGE)
					builder.append("\u0394"+leakSetting.getDetectionThreshold()+" "+unit+"\r\n");
				if(leakSetting.getTrigger() == Trigger.RELATIVE_CHANGE)
					builder.append("\u0394"+leakSetting.getDetectionThreshold()+"%\r\n");
			}
			builder.append("\t\tLeak nodes: "+countLeakNodes()+" of "+size+"\r\n");
		}
		
		// Technology criteria
		builder.append("Technology settings:\r\n");
		for(String parameter: sensorSettings.keySet()) {
			String unit = nodeStructure.getUnit(parameter);
			SensorSetting sensorSetting = sensorSettings.get(parameter);
			if(sensorSetting.getSensorType()==SensorType.POINT_SENSOR)
				hasPointSensor = true; //Determines whether to show sensor bounds
			else if(sensorSetting.getSensorType()==SensorType.SURFACE)
				hasSurface = true; //Determines whether to show surface bounds
			builder.append("\t" + parameter + ":\r\n");
			builder.append("\t\t" + sensorSetting.getSensorType() + "\r\n");
			builder.append("\t\tAlias: " + sensorAliases.get(parameter) + "\r\n");
			builder.append("\t\tCost: " + costUnit + sensorSetting.getSensorCostEq() + "\r\n");
			builder.append("\t\tTriggering on: " + sensorSetting.getTrigger() + "\r\n");
			if(sensorSetting.getTrigger() == Trigger.BELOW_THRESHOLD || sensorSetting.getTrigger() == Trigger.ABOVE_THRESHOLD) {
				builder.append("\t\tLeak threshold: " + sensorSetting.getDetectionThreshold() + "\r\n");
			} else {
				builder.append("\t\tLeak threshold: ");
				if(sensorSetting.getDeltaType() == DeltaType.DECREASE) builder.append("-");
				if(sensorSetting.getDeltaType() == DeltaType.INCREASE) builder.append("+");
				if(sensorSetting.getTrigger() == Trigger.ABSOLUTE_CHANGE)
					builder.append("\u0394"+sensorSetting.getDetectionThreshold()+" "+unit+"\r\n");
				if(sensorSetting.getTrigger() == Trigger.RELATIVE_CHANGE)
					builder.append("\u0394"+sensorSetting.getDetectionThreshold()+"%\r\n");
			}
			builder.append("\t\tZone bottom: " + Constants.percentageFormat.format(sensorSetting.getThisBottomZ()) + " " + zUnit + "\r\n");
			builder.append("\t\tZone top: " + Constants.percentageFormat.format(sensorSetting.getThisTopZ()) + " " + zUnit + "\r\n");
			builder.append("\t\tDetectable nodes: " + sensorSetting.getDetectableNodes().size() + " of " + size + "\r\n");
		}
		
		// Sensor minimums
		builder.append(inferenceTest);
		
		// Campaign settings page inputs
		builder.append("Monitoring Campaign settings:\r\n");
		if(maxCostEq.equals("3E38")) //set as 0
			builder.append("\tMax budget: no limit\r\n");
		else
			builder.append("\tMax budget: " + costUnit + maxCostEq + "\r\n");
		builder.append("\tNumber of sensors: "+minSensors+"-"+maxSensors+"\r\n");
		if(hasPointSensor) {
			builder.append("\tMaximum relocations: "+maxMoves+"\r\n");
			if(maxWells==1000000)
				builder.append("\tMax wells: no limit\r\n");
			else
				builder.append("\tMax wells: " + maxWells + "\r\n");
			builder.append("\tMin distance between wells: " + Constants.percentageFormat.format(exclusionRadius) + " " + zUnit + "\r\n");
			builder.append("\tCost per well: " + costUnit + wellCostEq + "\r\n");
			builder.append("\tCost per " + (zUnit=="" ? "unit" : zUnit) + " depth of well: " + costUnit + wellDepthCostEq + "\r\n");
		}
		if(hasSurface) {
			builder.append("\tNumber of station locations: "+minSurveyLocations+"-"+maxSurveyLocations+"\r\n");
			builder.append("\tMaximum repeated surveys: "+maxSurveys+"\r\n");
		}
		return builder.toString();
	}

	public void setupScenarios(List<Scenario> inputScenarios) {
		//Sort the scenarios
		Collections.sort(inputScenarios, Scenario.scenarioSort);
		
		// Convert string to scenario and add to lists
		for(Scenario scenario: inputScenarios) {
			if(!scenarios.contains(scenario))
				scenarios.add(scenario);
			if(!allScenarios.contains(scenario))
				allScenarios.add(scenario);
		}
		
		// Scenario weights should start at 1.0
		for(Scenario scenario: scenarios)
			scenarioWeights.put(scenario, (float)1);
	}
	
	public void setupSensorSettings() {
		// Setup the sensor settings array
		for(final String type: nodeStructure.getParameters()) {
			if(!sensorSettings.containsKey(type))
				sensorSettings.put(type, new SensorSetting(nodeStructure, type));
		}
	}
	
	// Setup the inference test
	public void setupInferenceTest() {
		inferenceTest = new InferenceTest("Any Technology", 1);
	}
	
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public List<Scenario> getScenarios() {
		return scenarios;
	}
	
	public Scenario getScenario(String scenarioName) {
		for(Scenario scenario : scenarios) {
			if(scenario.toString().equals(scenarioName))
				return scenario;
		}
		return null;
	}
	
	public String getScenarioEnsemble() {
		return scenarioEnsemble;
	}
	
	public void setScenarioEnsemble(String scenarioEnsemble) {
		this.scenarioEnsemble = scenarioEnsemble;
	}
	
	public List<Scenario> getAllScenarios() {
		return allScenarios;
	}
	
	public float getGloballyNormalizedScenarioWeight(Scenario scenario) {
		return scenarioWeights.get(scenario) / getTotalScenarioWeight();
	}
	
	public Map<Scenario, Float> getScenarioWeights() {
		return scenarioWeights;
	}
	
	public boolean getEqualWeights() {
		return equalWeights;
	}
	
	public void setEqualWeights(boolean equalWeights) {
		this.equalWeights = equalWeights;
	}
	
	public int getMaxMoves() {
		return maxMoves;
	}

	public void setMaxMoves(int maxMoves) {
		this.maxMoves = maxMoves;
	}
	
	public int getMaxWells() {
		return maxWells;
	}

	public void setMaxWells(int maxWells) {
		this.maxWells = maxWells;
	}
	
	public float getExclusionRadius() {
		return exclusionRadius;
	}

	public void setExclusionRadius(float exclusionRadius) {
		this.exclusionRadius = exclusionRadius;
	}
	
	public String getWellCost() {
		return wellCostEq;
	}
	
	public void setWellCost(String wellCost) {
		this.wellCostEq = wellCost;
	}
	
	public String getWellDepthCost() {
		return wellDepthCostEq;
	}
	
	public void setWellDepthCost(String wellDepthCost) {
		this.wellDepthCostEq = wellDepthCost;
	}
	
	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public InferenceTest getInferenceTest() {
		return inferenceTest;
	}
	
	/**
	 * Returns the cost of all wells in the campaign
	 * Includes both individual well cost and well depth cost.
	 * @param campaign
	 * @return float
	 */
	public double getWellCost(Campaign campaign, float time) {
		double cost = 0;
		for(VerticalWell well: campaign.getVerticalWells()) {
			// Length of time the well is maintained
			float wellTime = time<well.getInstallTime() ? 0 : time-well.getInstallTime();
			if(wellCostEq!="0") //skip if well cost is set to 0
				cost += Constants.evaluateCostExpression(wellCostEq, wellTime, 0, 0f, 0); //cost per well
			if(wellDepthCostEq!="0") //skip if well depth cost is set to 0
				cost += well.getDepth() * Constants.evaluateCostExpression(wellDepthCostEq, 0f, 0, 0f, 0); //cost per well depth
		}
		return cost;
	}
	
	/**
	 * Returns the cost of remediation for a given scenario at the time to
	 * detection. A node is considered degraded when it passes the thresholds
	 * entered in Page_DefineSensors. We then calculate the volume and factor
	 * in porosity, which gives us the volume to use with the entered cost.
	 * @param campaign
	 * @param scenario
	 * @return float
	 */
	//TODO: Not using this, but we may want this at some point, so keeping this method for now
	/*public double getRemediationCost(Campaign campaign, float time, Scenario scenario) {
		double cost = 0;
		if(remediationCostEq!="0") { //skip if remediation cost is set to 0
			HashSet<Integer> combinedDetectableNodes = new HashSet<Integer>();
			// Detectable nodes combines across all scenarios, need to use detection map instead
			for(SensorSetting sensorSetting: sensorSettings.values()) {
				for(Integer node: detectionMap.get(sensorSetting.getSpecificType()).get(scenario).keySet()) {
					if(detectionMap.get(sensorSetting.getSpecificType()).get(scenario).get(node) <= time)
						combinedDetectableNodes.add(node);
				}
			}
			float totalVolume = nodeStructure.getVolumeOfNodeSet(combinedDetectableNodes);
			cost = totalVolume * Constants.evaluateExpression(remediationCostEq, time, 0);
		}
		return cost;
	}*/
	
	/**
	 * Returns a list of affordable unoccupied node numbers from the list of detectable nodes.
	 * This method factors in cost constraints, campaign constraints, and occupied nodes for the sensor type
	 * @param sensorType
	 * @param campaign
	 * @return detectableNodes
	 */
	public List<Integer> getDetectableNodesWithConstraints(String sensorType, Campaign campaign) {
		List<Integer> detectableNodes = new ArrayList<Integer>();
		
		// Initializes with occupied nodes
		List<Integer> excludedNodes = campaign.getSensorPositions(sensorType);
		
		// If exclusion radius is set, remove nodes that are too close to existing wells
		// TODO: This doesn't work perfectly, only detects (x) the following neighbor wells:
		// x n n x n n x
		// n x n x n x n
		// n n x x x n n
		// x x x o x x x
		// n n x x x n n
		// n x n x n x n
		// x n n x n n x
		if(exclusionRadius > 0) {
			for(VerticalWell well: campaign.getVerticalWells()) {
				Point3i surfacePointIJ = well.getSurfaceIJK();
				Point3f surfacePointXY = well.getSurfaceXYZ();
				int[] xSet = new int[] {1, 1, -1, -1};
				int[] ySet = new int[] {1, -1, -1, 1};
				for(int i=1; i>100; i++) { //Hard-coded 100, but it should break long before then
					boolean noneFound = false;
					for(int x: xSet) { //Looks funky, but this efficiently tests radially around each well
						for(int y: ySet) {
							int newI = surfacePointIJ.getI()+x*i;
							int newJ = surfacePointIJ.getJ()+y*i;
							Point3f neighbor = new Point3f(newI,newJ);
							float distance = surfacePointXY.euclideanDistance(neighbor);
							if(distance < exclusionRadius) {
								for(int k=0; k<nodeStructure.getZ().size(); k++)
									excludedNodes.add(nodeStructure.ijkToNodeNumber(newI, newJ, k));
								noneFound = true;
							}
						}
					}
					if(noneFound)
						break;
				}
			}
		}
		
		// Loop through detectable nodes and add all nodes that don't break constraints
		for(Integer node: sensorSettings.get(sensorType).getDetectableNodes()) {
			if(excludedNodes.contains(node)) continue; //skip occupied sensors
			// We can't create another well, lets see if there is a spot left in one that already exists
			if(campaign.getVerticalWells().size() >= maxWells) {
				Point3i point = nodeStructure.nodeNumberToIJK(node);
				for(VerticalWell well: campaign.getVerticalWells()) {
					if(well.getI()==point.getI() && well.getJ()==point.getJ()) {
						detectableNodes.add(node); //node is within existing well
						break;
					}
				}
			} else {
				detectableNodes.add(node);
			}
		}
		return detectableNodes;
	}
	
	/**
	 * Returns a list of sensor types that we can switch to, factoring in cost constraints
	 * @param currentType
	 * @param campaign
	 * @return validTypes
	 */
	public List<String> getValidSwitchTypes(String currentType, Campaign campaign){
		List<String> validTypes = getDataTypes(); //Get all candidate types
		validTypes.remove(currentType); //Remove current type
		if(validTypes.size()==0) return validTypes; //No other types to switch to
		List<String> toRemove = new ArrayList<String>();
		for(String replaceType: validTypes) {
			Map<Float, Double> campaignCost = campaign.getCampaignCost();
			for(float time: campaignCost.keySet()) { //Check costs at each time
				double currentCost = Constants.evaluateCostExpression(getSensorSettings(currentType).getSensorCostEq(), time, 0, 0f, 0);
				double replaceCost = Constants.evaluateCostExpression(getSensorSettings(replaceType).getSensorCostEq(), time, 0, 0f, 0);
				double costLimit = Constants.evaluateCostExpression(maxCostEq, time, 0, 0f, 0);
				if(campaignCost.get(time) - currentCost + replaceCost > costLimit)
					toRemove.add(replaceType); //Too expensive
			}
		}
		for(String type: toRemove)
			validTypes.remove(type);
		return validTypes;
	}
	
	// Getters and setters for max cost
	public String getMaxCost() {
		return maxCostEq;
	}
	
	public void setMaxCost(String maxCost) {
		this.maxCostEq = maxCost;
	}
	
	// Getters and setters for max sensors
	public int getMaxSensors() {
		return maxSensors;
	}
	
	public void setMaxSensors(int maxSensors) {
		this.maxSensors = maxSensors;
	}
	
	// Getters and setters for min sensors
	public int getMinSensors() {
		return minSensors;
	}
	
	public void setMinSensors(int minSensors) {
		this.minSensors = minSensors;
	}
	
	public int getMaxSurveyLoactions() {
		return maxSurveyLocations;
	}
	
	public void setMaxSurveyLocations(int maxSurveys) {
		this.maxSurveyLocations = maxSurveys;
	}
	
	public int getMinSurveyLocations() {
		return minSurveyLocations;
	}
	
	public void setMinSurveyLocations(int minSurveys) {
		this.minSurveyLocations = minSurveys;
	}
	
	public int getMaxSurveys() {
		return maxSurveys;
	}
	
	public void setMaxSurveys(int maxSurveys) {
		this.maxSurveys = maxSurveys;
	}
	
	// Getters and setters for Node Structure
	public NodeStructure getNodeStructure() {
		return nodeStructure;
	}
	
	public void setNodeStructure(NodeStructure nodeStructure) {
		this.nodeStructure = nodeStructure;
		//Can only set these defaults after the node structure is set
		minSurveyLocations = (int)Math.round(nodeStructure.getTotalSurfaceNodes()*0.05); //Default of 5%
		maxSurveyLocations = (int)Math.round(nodeStructure.getTotalSurfaceNodes()*0.4); //Default of 40%
	}
	
	public Map<Integer, List<Integer>> getAllPossibleWells() {
		List<Integer> cloudNodes = new ArrayList<Integer>();
		
		for(String sensorType: this.getSensorSettings().keySet()) {
			for(Integer node: sensorSettings.get(sensorType).getDetectableNodes()) 
				cloudNodes.add(node);
		}
		
		Map<Integer, List<Integer>> ijs = new HashMap<Integer, List<Integer>>();
		for(Integer node: cloudNodes) {
			Point3i ijk = nodeStructure.nodeNumberToIJK(node);
			if(!ijs.containsKey(ijk.getI()))
				ijs.put(ijk.getI(), new ArrayList<Integer>());
			if(!ijs.get(ijk.getI()).contains(ijk.getJ()))
				ijs.get(ijk.getI()).add(ijk.getJ());
		}
		return ijs;
	}
	
	// For a given specificType, add all triggering nodes from detection map into a Set per scenario
	public void addLeakNodes(String type, String specificType) {
		for(Scenario scenario: getScenarios()) {
			if(!leakNodes.containsKey(type))
				leakNodes.put(type, new HashMap<Scenario, Set<Integer>>());
			if(!leakNodes.get(type).containsKey(scenario))
				leakNodes.get(type).put(scenario, new HashSet<Integer>());
			leakNodes.get(type).get(scenario).addAll(detectionMap.get(specificType).get(scenario).keySet());
		}
	}
	
	public Map<String, Map<Scenario, Set<Integer>>> getLeakNodes() {
		return leakNodes;
	}
	
	public Integer countLeakNodes() {
		if(leakNodes==null)
			return null;
		Set<Integer> allNodes = new HashSet<Integer>();
		for(String parameter : leakNodes.keySet()) {
			for(Scenario scenario : leakNodes.get(parameter).keySet())
				allNodes.addAll(leakNodes.get(parameter).get(scenario));
		}
		return allNodes.size();
	}
	
	public List<String> getAllPossibleParameters() {
		return nodeStructure.getParameters();
	}

	public void resetSensorSettings(String type) {
		if(sensorSettings.containsKey(type))
			return; // Keep those
		sensorSettings.put(type, new SensorSetting(nodeStructure, type));	// User should adjust these settings
	}

	public SensorSetting getSensorSettings(String sensorType) {
		return sensorSettings.get(sensorType);
	}
	
	public Map<String, SensorSetting> getSensorSettings() {
		return sensorSettings;
	}
	
	public void addSensorSetting(String name, String type) {
		sensorSettings.put(name, new SensorSetting(nodeStructure, type));	// User should adjust these settings
	}
	
	public void addSensorSetting(String type, String trigger, String threshold) {
		sensorSettings.put(type, new SensorSetting(nodeStructure, type, trigger, threshold));	// User should adjust these settings
	}
	
	public void removeSensorSettings(String dataType) {
		sensorSettingsRemoved.put(dataType, sensorSettings.get(dataType));
		sensorSettings.remove(dataType);
	}
	
	public SensorSetting getRemovedSensorSettings(String sensorType) {
		return sensorSettingsRemoved.get(sensorType);
	}
	
	public void resetRemovedSensorSettings() {
		sensorSettingsRemoved = new HashMap<String, SensorSetting>();
	}
	
	public void addLeakSetting(String type, String trigger, String threshold) {
		leakSettings.put(type, new SensorSetting(nodeStructure, type, trigger, threshold));
	}
	
	public void resetLeakSetting() {
		leakSettings = new HashMap<String, SensorSetting>();
	}
	
	public List<String> getDataTypes() {
		return new ArrayList<String>(sensorSettings.keySet());
	}
	
	/////////////////////////////////////////////////////
	//// Methods for interacting with the detectionMap //
	/////////////////////////////////////////////////////
	public Float getTTD(String specificType, Scenario scenario, Integer nodeNumber) {
		return detectionMap.get(specificType).get(scenario).get(nodeNumber);
	}
	
	public float getShortestTTD(String specificType, Integer nodeNumber) {
		float shortestTime = Float.MAX_VALUE;
		for (Scenario scenario : detectionMap.get(specificType).keySet()) {
			if(detectionMap.get(specificType).get(scenario).containsKey(nodeNumber)) {
				if(detectionMap.get(specificType).get(scenario).get(nodeNumber) < shortestTime)
					shortestTime = detectionMap.get(specificType).get(scenario).get(nodeNumber);
			}
		}
		return shortestTime;
	}
	
	public void setDetectionMap(String specificType, Scenario scenario) {
		if(!detectionMap.containsKey(specificType))
			detectionMap.put(specificType, new HashMap<Scenario, Map<Integer, Float>>());
		if(!detectionMap.get(specificType).containsKey(scenario))
			detectionMap.get(specificType).put(scenario, new HashMap<Integer, Float>());
	}
	
	public void addToDetectionMap(String specificType, Scenario scenario, int nodeNumber, float ttd) {
		// Only add if the new value is less than existing
		if(detectionMap.get(specificType).get(scenario).get(nodeNumber)==null || ttd < getTTD(specificType, scenario, nodeNumber)) 
			detectionMap.get(specificType).get(scenario).put(nodeNumber, ttd);
	}
	
	public Map<String, Map<Scenario, Map<Integer, Float>>> getDetectionMap() {
		return detectionMap;
	}
		
	//////////////////////////////////////////////////////
	//// Methods for interacting with the varianceMap ////
	//////////////////////////////////////////////////////
	public float getVariance(String specificType, Scenario scenario, float time, int nodeNumber) {
		return varianceMap.get(specificType).get(scenario).get(time).get(nodeNumber);
	}
	
	public void setVarianceMap(String specificType, Scenario scenario, float time, int nodeNumber, float variance) {
		if(!varianceMap.containsKey(specificType))
			varianceMap.put(specificType, new HashMap<Scenario, Map<Float, Map<Integer, Float>>>());
		if(!varianceMap.get(specificType).containsKey(scenario))
			varianceMap.get(specificType).put(scenario, new HashMap<Float, Map<Integer, Float>>());
		if(!varianceMap.get(specificType).get(scenario).containsKey(time))
			varianceMap.get(specificType).get(scenario).put(time, new HashMap<Integer, Float>());
		varianceMap.get(specificType).get(scenario).get(time).put(nodeNumber, variance);
	}
	
	public void resetVarianceMap() {
		varianceMap.clear();
	}
	
	public Map<String, Map<Scenario, Map<Float, Map<Integer, Float>>>> getVarianceMap() {
		return varianceMap;
	} 
	
	public float getTotalScenarioWeight() {
		float totalScenarioWeight = 0;
		for(float value: scenarioWeights.values()) totalScenarioWeight += value;
		return totalScenarioWeight;
	}
	
	public String getSensorAlias(String parameter) {
		return sensorAliases.get(parameter);
	}
	
	public void addSensorAlias(String parameter, String sensorAlias) {
		if(sensorAliases==null)
			sensorAliases = new HashMap<String, String>();
		sensorAliases.put(parameter, sensorAlias);
	}
	
	public boolean isSensorAliasInitialized() {
		return sensorAliases!=null;
	}
	
	public String getCostUnit() {
		return costUnit;
	}
	
	// Returns a map of VAD by scenario
	// volumeDegradedByTime = Scenario <time, VAD>
	public Map<Scenario, Float> getVolumeDegraded(Map<Scenario, Float> ttdMap) {
		HashMap<Scenario, Float> vadMap = new HashMap<Scenario, Float>();
		for(Scenario scenario: ttdMap.keySet()) {
			float ttd = ttdMap.get(scenario);
			float vad = volumeDegradedByTime.get(scenario).get(ttd);
			vadMap.put(scenario, vad);
		}
		return vadMap;
	}
	
	public Float getVolumeDegradedAtTime(Scenario scenario, Float ttd) {
		if(ttd==null) return null;
		return volumeDegradedByTime.get(scenario).get(ttd);
	}
	
	public float getTotalVolumeDegraded(Scenario scenario) {
		float lastTime = nodeStructure.getMaxTime();
		return volumeDegradedByTime.get(scenario).get(lastTime);
	}
	
	public HashMap<Float,Float> getAverageVolumeDegradedAtTimesteps() {
		HashMap<Float,Float> averageVADMap = new HashMap<Float,Float>(); // <time, VAD>
		for(TimeStep timeStep : nodeStructure.getTimeSteps()) {
			float time = timeStep.getRealTime();
			float totalVAD = 0;
			for(Scenario scenario : scenarios)
				totalVAD += volumeDegradedByTime.get(scenario).get(time);
			averageVADMap.put(time, totalVAD/scenarios.size());
		}
		return averageVADMap;
	}
	
	public HashMap<Float,Float> getMaxVolumeDegradedAtTimesteps() {
		HashMap<Float,Float> maxVADMap = new HashMap<Float,Float>(); // <time, VAD>
		for(TimeStep timeStep : nodeStructure.getTimeSteps()) {
			float time = timeStep.getRealTime();
			float maxVAD = Float.MIN_VALUE;
			for(Scenario scenario : scenarios) {
				if(volumeDegradedByTime.get(scenario).get(time) > maxVAD)
					maxVAD = volumeDegradedByTime.get(scenario).get(time);
			}
			maxVADMap.put(time, maxVAD);
		}
		return maxVADMap;
	}
	
	public HashMap<Float,Float> getMinVolumeDegradedAtTimesteps() {
		HashMap<Float,Float> minVADMap = new HashMap<Float,Float>(); // <time, VAD>
		for(TimeStep timeStep : nodeStructure.getTimeSteps()) {
			float time = timeStep.getRealTime();
			float minVAD = Float.MAX_VALUE;
			for(Scenario scenario : scenarios) {
				if(volumeDegradedByTime.get(scenario).get(time) < minVAD)
					minVAD = volumeDegradedByTime.get(scenario).get(time);
			}
			minVADMap.put(time, minVAD);
		}
		return minVADMap;
	}
	
	/**
	 * Determines the volume of aquifer degraded at each time step based on leak definition
	 * @param leakData
	 */
	public void calculateVolumeDegraded(Map<String, LeakData> leakData) {
		volumeDegradedByTime = new HashMap<Scenario, HashMap<Float, Float>>(); //Scenario <time, VAD>
		//Map<Scenario, HashMap<Integer, Float>> degradedNodes = new HashMap<Scenario, HashMap<Integer, Float>>(); //Scenario <NodeNumber, Detection>
		for(Scenario scenario : allScenarios) {
			
			// Initialize maps
			volumeDegradedByTime.put(scenario, new HashMap<Float, Float>());
			HashMap<Integer, Float> degradedNodes = new HashMap<Integer, Float>(); //initialize scenarios
			
			// Loop through different leak parameters and save shortest TTD
			for(String parameter : leakData.keySet()) {
				LeakData leak = leakData.get(parameter);
				String specificType = Constants.getSpecificType(parameter, leak.getTrigger(), leak.getDeltaType(), leak.getThreshold(), SensorType.POINT_SENSOR);
				if(detectionMap.containsKey(specificType) && detectionMap.get(specificType).containsKey(scenario)) {
					for(Integer nodeNumber : detectionMap.get(specificType).get(scenario).keySet()) {
						float ttd = detectionMap.get(specificType).get(scenario).get(nodeNumber);
						if(!degradedNodes.containsKey(nodeNumber) || ttd < degradedNodes.get(nodeNumber))
							degradedNodes.put(nodeNumber, ttd);
					}
				}
			}
			
			// Calculate the volume degraded at each time
			float volume = 0;
			HashSet<Integer> nodes = new HashSet<Integer>();
			for(TimeStep timeStep : nodeStructure.getTimeSteps()) {
				float time = timeStep.getRealTime();
				// Add all nodes that exceed the threshold at this exact time
				for(Integer nodeNumber : degradedNodes.keySet()) {
					if(degradedNodes.get(nodeNumber) == time)
						nodes.add(nodeNumber);
				}
				volume += nodeStructure.getVolumeOfNodeSet(nodes);
				volumeDegradedByTime.get(scenario).put(time, volume);
			}
		}
	}
}
