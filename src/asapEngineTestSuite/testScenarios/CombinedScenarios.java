package asapEngineTestSuite.testScenarios;

import java.util.Arrays;

/**
 *
 * This class has a single method which can be used to combine core scenarios.
 */
@Deprecated
public class CombinedScenarios {

	/**
	 * This method appends 2 scenarios to generate combined scenarios.
	 *
	 * @param firstCommandList core scenarios to combine
	 * @return an array of scenarios for the peers to execute
	 */
	public static String[] combineCoreScenarios(String[] firstCommandList, String[] secondCommandList) {
		String[] commands = new String[2];


		commands[0] = firstCommandList[0] += secondCommandList[0];
		commands[1] = firstCommandList[1] += secondCommandList[1];

		return commands;
	}

	private static void insertLinebreak(String[] s, int x) {
		if (!s[x].endsWith(System.lineSeparator())) {
			s[x] = s[x] + System.lineSeparator();
		}
	}
}
