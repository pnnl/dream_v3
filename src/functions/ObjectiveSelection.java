package functions;

import objects.Campaign;
import wizardPages.DREAMWizard.STORMData;

/**
 * Enum for what objective to use in the optimization
 * @author whit162
 * @author huan482
 */

public interface ObjectiveSelection {
	
	public static enum OBJECTIVE {
		TTD {
			public String toString() {
				return "Time to Detection";
			}
		},
		COST {
			public String toString() {
				return "Cost";
			}
		},
		VAD_AT_DETECTION {
			public String toString() {
				return "VAD at Detection";
			}
		},
		SCENARIOS_DETECTED {
			public String toString() {
				return "Scenarios Detected";
			}
		};
		
		public static OBJECTIVE getObjective(String string) {
			if(string.equals("Time to Detection"))
				return OBJECTIVE.TTD;
			else if(string.equals("Cost"))
				return OBJECTIVE.COST;
			else if(string.equals("VAD at Detection"))
				return OBJECTIVE.VAD_AT_DETECTION;
			else if(string.equals("Scenarios Detected"))
				return OBJECTIVE.SCENARIOS_DETECTED;
			return null;
		}
	}
	
	public ObjectiveValues objective(Campaign campaign, STORMData data);
}
