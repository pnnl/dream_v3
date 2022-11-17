package objects;

import java.util.HashSet;
import java.util.Set;

import utilities.Constants;
import utilities.Point3f;

/**
 * Holds the logic for a specific sensor type and threshold
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
 */

public class SensorSetting {
		
	public enum Trigger {
		
		ABOVE_THRESHOLD("Above threshold"),
		BELOW_THRESHOLD("Below threshold"), 
		RELATIVE_CHANGE("Relative change"),
		ABSOLUTE_CHANGE("Absolute change");
		
		private String trigger;
		private Trigger(String trigger) {
			this.trigger = trigger;
		}
		@Override
		public String toString() {
			return trigger;
		}
	}
	
	public enum SensorType {
		
		POINT_SENSOR("Point Sensor"),
		SURFACE("Surface Survey");
		//BOREHOLE("Borehole"),
		//CROSS_WELL("Cross Well");
		
		private String sensorType;
		private SensorType(String sensorType) {
			this.sensorType = sensorType;
		}
		@Override
		public String toString() {
			return sensorType;
		}
	}
	
	public enum DeltaType {
		
		INCREASE("Delta Increase"),
		DECREASE("Delta Decrease"), 
		BOTH("Delta Both");
		
		private String deltaType;
		private DeltaType(String deltaType) {
			this.deltaType = deltaType;
		}
		@Override
		public String toString() {
			return deltaType;
		}
	}
		
	private String parameter;
	private String alias;
	private String sensorCostEq; //this is a String to allow equations

	private float bottomZ;
	private float topZ;
	
	private Trigger trigger;
	private DeltaType deltaType;

	private float detectionThreshold;
	private SensorType sensorType;

	private HashSet<Integer> detectableNodes;
	private String specificType;
	/**
	 * Percentage to increase nodes in your pareto space.
	 * Higher percentage = more nodes.
	 */
	
	// Sensor Settings for H5 Files
	public SensorSetting(NodeStructure nodeStructure, String parameter) {
		this.parameter = parameter;
		sensorCostEq = "500+5t";
		getTriggerFromText("below", "0"); //Sets the trigger and delta type
		detectionThreshold = 0; //Based on the trigger, this represents the range for detectable nodes
		detectableNodes = new HashSet<Integer>(); //Added later, initialize here
		// specificType can be set after inputting parameters in Page_DefineSensors for H5 files
	}
	
	// Sensor Settings for IAM files
	public SensorSetting(NodeStructure nodeStructure, String type, String trigger, String threshold) {
		this.parameter = type;
		sensorCostEq = "500+5t";
		getTriggerFromText(trigger, threshold); //Sets the trigger and delta type
		detectionThreshold = Float.parseFloat(threshold); //Based on the trigger, this represents the range for detectable nodes
		detectableNodes = new HashSet<Integer>(); //Added later, initialize here
		specificType = Constants.getSpecificType(type, this.trigger, deltaType, detectionThreshold, sensorType);//Initialize before the detection map is created
	}

	public void setUserSettings(String sensorCostEq, float detectionThreshold, Trigger trigger, DeltaType deltaType, float bottomZ, float topZ, String alias, SensorType sensorType) {
		this.alias = alias;
		this.sensorCostEq = sensorCostEq;
		this.trigger = trigger;
		this.deltaType = deltaType;
		this.detectionThreshold = detectionThreshold;
		this.sensorType = sensorType;
		this.bottomZ = bottomZ;
		this.topZ = topZ;
		this.specificType = Constants.getSpecificType(parameter, trigger, deltaType, detectionThreshold, sensorType);
	}
	
	
	public void setNodes(ScenarioSet set) {
		
		detectableNodes.clear();
		
		if(parameter.contains("Electrical Conductivity"))
			detectableNodes = E4DSensorSettings.setDetectableNodesERT(detectionThreshold);
		else {
			// From the detectionMap, we just need to get a list of nodes that exist across selected scenarios (fullCloudNodes)
			for(Scenario scenario: set.getScenarios()) {
				if(set.getDetectionMap().get(specificType).get(scenario).size() > 0) {
					for(Integer node: set.getDetectionMap().get(specificType).get(scenario).keySet())
						detectableNodes.add(node);
				}
			}
			trimZ(set.getNodeStructure()); // Remove nodes outside of Z range
		}
	}
	
	
	private void trimZ(NodeStructure nodeStructure) {
		HashSet<Integer> nodesToRemove = new HashSet<Integer>();
		//Find the nodes that fit this z restriction
		for(Integer node: detectableNodes) {
			Point3f test = nodeStructure.getXYZFromNodeNumber(node);
			if(nodeStructure.getPositive().equals("up")) {
				if(test.getZ() < bottomZ || test.getZ() > topZ) //outside of bounds
					nodesToRemove.add(node);
			} else {
				if(test.getZ() > bottomZ || test.getZ() < topZ) //outside of bounds
					nodesToRemove.add(node);
			}
		}
		detectableNodes.removeAll(nodesToRemove);
	}
	
	
//	private void paretoOptimal(Map<String, Map<String, Map<Integer, Float>>> detectionMap, List<String> scenarios) {
//		HashMap<Integer, ArrayList<Float>> optimalSolutions = new HashMap<Integer, ArrayList<Float>>();
//		
//		double percentOfScenarios = scenarios.size() * PERCENTAGE;
//		
//		for(Integer nodeNumber: detectableNodes) {
//			//build up the string ID and the li9-st of ttds (for the ones that detect)
//			ArrayList<Float> ttds = new ArrayList<Float>();
////			int numberOfScenarios = 0;
//			//This loop adds all the nodes from each scenario
//			for(Scenario scenario: scenarios) { 
//				Float timeToDegredation = Float.MAX_VALUE;
//				if(detectionMap.get(specificType).get(scenario).containsKey(nodeNumber))
//					timeToDegredation = detectionMap.get(specificType).get(scenario).get(nodeNumber);
//				ttds.add(timeToDegredation);
//			}
//			ArrayList<Integer> toRemove = new ArrayList<Integer>(); //If this new campaign replaces one, it might replace multiple.
//			boolean everyReasonTo = false;
//			boolean everyReasonNot = false;
//			
//			
//			for(Integer paretoSolutionLocation: optimalSolutions.keySet()){
//				ArrayList<Float> paretoSolution = optimalSolutions.get(paretoSolutionLocation);
//				boolean greater = false;
//				boolean less = false;
//				for(int i = 0; i < paretoSolution.size(); i++){
//					if ( (paretoSolution.get(i) < ttds.get(i)) ) {
//						greater = true;
//					}
//					if (paretoSolution.get(i) > ttds.get(i)) {
//						less = true;
//					}
//				}
////				System.out.println("Greater is " + greater + " Less is " + less);
//				if(greater && !less){
//					everyReasonNot = true; //This solution is redundant, as there is another that is parwise optimal
//					break; //we don't need to look anymore, don't include this new campaign
//				}
//				else if(!greater && less){
//					everyReasonTo = true; //This solution is pareto optimal to this stored one
//					toRemove.add(paretoSolutionLocation); //We need to remove this one, it has been replaced
//				}
//			}
//			if(everyReasonTo){
//				//We need to add this one and remove some.
//				for(Integer x : toRemove){
//					optimalSolutions.remove(x);
//				}
//				optimalSolutions.put(nodeNumber, ttds);
//			}
//			else if(everyReasonNot){
//				//Lets not add this one, it's redundant
//			}
//			else { 
//				//No reason not to add it and it didn't replace one, it must be another pareto optimal answer.
//				//Let's add it.
//				optimalSolutions.put(nodeNumber, ttds); 
//			}
//		}
//		System.out.println("Pareto Optimal just pared down detectable nodes for " 
//		+ specificType + " from " + detectableNodes.size() + " to " + optimalSolutions.size());
//		detectableNodes.clear();
//		detectableNodes.addAll(optimalSolutions.keySet());
//	}
	
	/**
	 * @author huan482
	 * @author whit162
	 * @param theDetectionMap - The detection map with all our information.
	 * @param theScenarios - The list of scenarios.
	 * The Pareto Algorithm that looks at the number of scenarios detected and gets rid of any nodes
	 * with scenarios that are less than the node with the highest number of scenarios.
	 */
	/*private void primaryParetoOptimization(final Map<String, Map<String, Map<Integer, Float>>> theDetectionMap,
			final List<String> theScenarios) {
		double PERCENTAGE = 0.05;
		int max = 0;
		// Our "fuzziness" based off of a percent of the number of total scenarios.
		int threshold = (int) (theScenarios.size() * PERCENTAGE);
		HashMap<Integer, Integer> myOptimalSolutions = new HashMap<Integer, Integer>();
		//Loops through all the nodes that are above the threshold.
		for (Integer nodeNumber: detectableNodes) {
			int counter = 0;
			// Loops through our list of scenarios
			for (String theScenario: theScenarios) {
				//If the scenario contains the node number than we add a counter to indicate that the scenario
				//Is part of that node.
				if (theDetectionMap.get(specificType).get(theScenario).containsKey(nodeNumber)) {
					counter++;
				}
				
			}
			//Keep track of the highest number of scenario a node has.
			if (counter > max) {
				max = counter;
			}
			myOptimalSolutions.put(nodeNumber, counter);
		}
		detectableNodes.clear();
		//Go through every node and if the node has less number of scenarios than max
		//(accounting for the threshold) then we don't add it to our valid node set.
		for (Integer theNode: myOptimalSolutions.keySet()) {
			if (myOptimalSolutions.get(theNode) + threshold >= max) {
				detectableNodes.add(theNode);
			}
		}
	}*/
	
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public String getParameter() {
		return parameter;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public String getSensorCostEq() {
		return sensorCostEq;
	}
	
	public void setSensorCostEq(String sensorCostEq) {
		this.sensorCostEq = sensorCostEq;
	}
	
	public Set<Integer> getDetectableNodes() {
		return detectableNodes;
	}
	
	public void removeNode(Integer node) {
		detectableNodes.remove(node);
	}

	public Trigger getTrigger() {
		return trigger;
	}
	
	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}
	
	public DeltaType getDeltaType() {
		return deltaType;
	}

	public void setDeltaType(DeltaType deltaType) {
		this.deltaType = deltaType;
	}
	
	public float getDetectionThreshold() {
		return detectionThreshold;
	}
	
	public void setDetectionThreshold(Float detection) {
		detectionThreshold = detection;
	}
	
	public SensorType getSensorType() {
		return sensorType;
	}
	
	public void setSensorType(SensorType sensorType) {
		this.sensorType = sensorType;
	}
	
	public float getThisBottomZ() {
		return bottomZ;
	}
	
	public float getThisTopZ() {
		return topZ;
	}
	
	public String getSpecificType() {
		return specificType;
	}

	public void clearNodes() {
		detectableNodes.clear();
	}
	
	public void getTriggerFromText(String trigger, String threshold) {
		if(trigger.toLowerCase().contains("below"))
			this.trigger = Trigger.BELOW_THRESHOLD;
		else if(trigger.toLowerCase().contains("above"))
			this.trigger = Trigger.ABOVE_THRESHOLD;
		else if(trigger.toLowerCase().contains("rel"))
			this.trigger = Trigger.RELATIVE_CHANGE;
		else if(trigger.toLowerCase().contains("abs"))
			this.trigger = Trigger.ABSOLUTE_CHANGE;
		if(threshold.contains("-") && (trigger.contains("abs") || trigger.contains("rel")))
			deltaType = DeltaType.DECREASE;
		else if(threshold.contains("+") && (trigger.contains("abs") || trigger.contains("rel")))
			deltaType = DeltaType.INCREASE;
		else
			deltaType = DeltaType.BOTH;
	}
}