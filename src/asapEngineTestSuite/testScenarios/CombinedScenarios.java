package asapEngineTestSuite.testScenarios;

import com.sun.source.doctree.EscapeTree;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class CombinedScenarios {

	/**
	 * This method appends scenarios to generate combined scenarios.
	 * @param commandlists core scenarios to combine
	 * @return an array of scenarios for the peers to execute
	 */
	public static String[] combineScenarios(String[]... commandlists) {

		String[] commands = new String[2];
		commands[0] = commandlists[0][0];
		commands[1] = commandlists[0][1];
		//first element has to be saved in commands[0], second element in commands[1]
		for (String[] s : commandlists) {
			if (!s[0].endsWith(System.lineSeparator())) {
				s[0] = s[0] + System.lineSeparator();
			}
			commands[0] += s[0];
		}
		for (String[] s : commandlists) {
			if (!s[1].endsWith(System.lineSeparator())) {
				s[1] = s[1] + System.lineSeparator();
			}
			commands[1] += s[1];
		}
		return commands;
	}

	public static void main(String[] args) {
		CoreScenariosTCPChain coreScenariosTCPChain = new CoreScenariosTCPChain();
		String[] testcommands = combineScenarios(coreScenariosTCPChain.coreACommandLists(1), coreScenariosTCPChain.coreBCommandLists(1), coreScenariosTCPChain.coreACommandLists(1));
		System.out.println(testcommands[1]);
		System.out.println(
		);

		System.out.println(testcommands[0]);
	}

}
