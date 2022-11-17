package objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wizardPages.Page_DetectionCriteria.DetectionCriteria;
/**
 * Class to store the criteria for what constitutes a detection.
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class InferenceTest {
	
	private List<HashMap<String, Integer>> activeTests;
	
	//Create a test, starting with only one sensor
	public InferenceTest(String sensorName, int min) {
		activeTests = new ArrayList<HashMap<String, Integer>>();
		HashMap<String, Integer> test = new HashMap<String, Integer>();
		test.put(sensorName, min);
		activeTests.add(test);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Detection Criteria:\r\n");
		for(int i=0; i<activeTests.size(); i++) {
			builder.append("\tTest " + i+1 + "\r\n");
			for(String sensor: activeTests.get(i).keySet()) {
				builder.append("\t\t" + sensor + ": " + activeTests.get(i).get(sensor) + "\r\n");
			}
		}
		return builder.toString();
	}
	
	// Does the campaign pass the active tests?
	public Float getMinTTDForTests(List<Sensor> sensors, ScenarioSet set, Scenario scenario) {		
		for (Map<String, Integer> test : set.getInferenceTest().getActiveTests()) {
			// Pre-check: determine the minimum sensor requirement to verify valid campaign
			int minReq = 0;
			for (int count : test.values())
				minReq += count;
			if (sensors.size() < minReq)
				continue; // Not enough sensors to complete test
			// Primary check: determine that sensor types match up to verify valid campaign
			boolean testPass = true;
			float testValue = 0;
			for (String testType : test.keySet()) { // Loop through sensors in a test
				// We already checked that there were enough technologies, so skip
				if (testType.equals("Any Technology")) continue;
				// Create a list of TTDs for the given sensor from the test
				List<Float> ttds = listOfValidTTDs(sensors, set, scenario, testType);
				// If there are not enough of this sensor, the test fails
				if (ttds.size() < test.get(testType)) {
					testPass = false;
					break;
				}
				// Save the largest TTD at the minimum test requirement
				if (ttds.get(test.get(testType) - 1) > testValue)
					testValue = ttds.get(test.get(testType) - 1);
			}
			
			// The campaign passed this test
			if (testPass) {
				// Do a final check to confirm that the "Any Technology" requirement doesn't increase the inferenceValue
				List<Float> allTTDs = listOfValidTTDs(sensors, set, scenario, "");
				if (allTTDs.size() < minReq)
					continue; // Not enough detecting sensors to complete test
				if (allTTDs.get(minReq - 1) > testValue) // Save the largest TTD at the minimum "All Sensor" test requirement
					testValue = allTTDs.get(minReq - 1);

				return testValue; // This is the best TTD that passes the test
			}
		}
		return null; // Failed all tests
	}
	
	// Create a list of TTDs for the given sensor from the test
	private List<Float> listOfValidTTDs(List<Sensor> sensors, ScenarioSet set, Scenario scenario, String testType) {
		List<Float> ttds = new ArrayList<Float>();
		for (Sensor sensor : sensors) { // Loop through sensors in campaign
			String sensorType = sensor.getSensorType();
			Float ttd = sensor.getTTD(set, scenario);
			// We only want to keep detections (not null) for a given sensor that is being tested
			if(ttd!=null && sensorType.contains(testType)) // TODO: Doesn't work with duplicate sensors, need to key to alias or specificType
				ttds.add(ttd);
		}
		Collections.sort(ttds); // Sort the TTDs, smallest to largest
		return ttds;
	}
	
	public void addActiveTest(HashMap<String, Integer> test) {
		activeTests.add(test);
	}
	
	public List<HashMap<String, Integer>> getActiveTests() {
		return activeTests;
	}
	
	// Reset active tests and replace with new list from Page_DetectionCriteria
	public void copyInferenceTest(List<DetectionCriteria> testList) {
		activeTests.clear();
		for(DetectionCriteria test: testList) {
			activeTests.add(test.activeTests);
		}
	}
}
