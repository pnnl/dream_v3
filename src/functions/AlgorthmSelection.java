package functions;


/**
 * Enum for what algorithm logic to use in the optimization
 * @author whit162
 * @author huan482
 */

public interface AlgorthmSelection {

	public static enum ALGORITHM {
		/*GRID_SEARCH {
			public String toString() {
				return "Grid Search";
			}
		},*/
		MONTE_CARLO {
			public String toString() {
				return "Monte Carlo";
			}
		},
		/*MONTE_CARLO_GRID {
			public String toString() {
				return "Monte Carlo (Grid)";
			}
		},*/
		SIMULATED_ANNEALING {
			public String toString() {
				return "Simulated Annealing";
			}
		},
		HEURISTIC {
			public String toString() {
				return "Heuristic";
			}
		},
		NSGAII {
			public String toString() {
				return "NSGAII";
			}
		},
		/*SIMULATED_ANNEALING_GRID {
			public String toString() {
				return "Simulated Annealing (Grid)";
			}
		},*/
		//GENETIC {
		//	public String toString() {
		//		return "Genetic";
		//	}
		//},
		//MCMC {
		//	public String toString() {
		//		return "MCMC";
		//	}
		//}
	}
}
