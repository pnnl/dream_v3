package objects;

import java.util.Comparator;

/**
 * Scenario functions and helper methods
 * @author whit162
 */

public class Scenario {

	private String scenario;
	
	public Scenario(String scenario) {
		this.setScenarioName(scenario);
	}

	public void setScenarioName(String scenarioName) {
		this.scenario = scenarioName;
	}

	@Override
	public String toString() {
		return scenario;
	}
	
	// This is a special comparator to handle strings + numbers
	public static final Comparator<Scenario> scenarioSort = new Comparator<Scenario>() {
		@Override
		public int compare(Scenario o1, Scenario o2) {
	        return extractInt(o1.toString()) - extractInt(o2.toString());
	    }
	    int extractInt(String s) {
	        String num = s.replaceAll("\\D", "");
	        // return 0 if no digits found
	        return num.isEmpty() ? 0 : Integer.parseInt(num);
	    }
	};
	
}