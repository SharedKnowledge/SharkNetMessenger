package asapEngineTestSuite.testScenarios;

/**
 * This class has a single method which can be used to combine core scenarios.
 */
public class CombinedScenarios {

	/**
	 * This method appends scenarios to generate combined scenarios.
	 *
	 * @param commandLists core scenarios to combine
	 * @return an array of scenarios for the peers to execute
	 */
	public static String[] combineCoreScenarios(String[]... commandLists) {

		String[] commands = new String[2];
		commands[0] = commandLists[0][0];
		commands[1] = commandLists[0][1];

		for (String[] s : commandLists) {
			insertLinebreak(s, 0);
			commands[0] += s[0];
		}

		for (String[] s : commandLists) {
			insertLinebreak(s, 1);
			commands[1] += s[1];
		}
		return commands;
	}

	private static void insertLinebreak(String[] s, int x) {
		if (!s[x].endsWith(System.lineSeparator())) {
			s[x] = s[x] + System.lineSeparator();
		}
	}
}
