package asapEngineTestSuite;

import asapEngineTestSuite.testScenarios.ScenarioTCPChain;
import asapEngineTestSuite.testScenarios.ScenarioTCPStar;
import asapEngineTestSuite.utils.CommandListToFile;
import asapEngineTestSuite.utils.ScenarioParamAllocation;
import asapEngineTestSuite.utils.TestHost;

import java.io.File;
import java.io.IOException;

import static asapEngineTestSuite.utils.ScenarioParamAllocation.DEFAULT_SCENARIO_INDEX;

/**
 * This class is responsible for generating the command list for the peers based on the provided info sheet.
 * It reads the command list from a file and assigns the parameters to the corresponding variables.
 * It also generates a command list for the peers to execute the test scenario.
 * Alternatively, the test variables can be passed as command line arguments.
 * @author Cemre
 */
public class TestScriptOutput {
	/**
	 * Message with the two correct usages.
	 */
	public static final String USAGE_STRING = "Usage: java -jar ScriptGenerator.jar <Info sheet path>. Info sheet should include host IP address." +
		"\nAlternatively: java -jar SkriptGenerator.jar <Host IP address (or localhost)> <scenario index> <peer count> [optional: <file size> <file name>]";
	/**
	 * Error message in case of a faulty file path.
	 */
	public static final String FILE_NONEXISTENT = "File does not exist. Please check the file path or list parameters manually.";

	/**
	 * The current scenario index. Set to 0 by default.
	 */
	public static int scenarioIndex = DEFAULT_SCENARIO_INDEX.ordinal();

	/**
	 * Main method to run the SkriptGenerator.
	 * It generates the command list for the peers based on the provided info sheet or command line arguments.
	 *
	 * @param args command line arguments
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {

		String filePath;
		TestHost testHost = new TestHost();

		if (args.length == 0) {
			System.out.println(USAGE_STRING);
			return;
		}
		if (args.length == 1) {
			filePath = args[0];
			if (filePath.equals("default")) {
				parseArgs(args, testHost);
			} else {
				File file = new File(filePath);
				if (!file.exists()) {
					System.out.println(FILE_NONEXISTENT);
					return;
				}
				parseInfosheet(filePath, testHost);
			}
		}
		if (args.length > 1) {
			parseArgs(args, testHost);
		}
	}

	private static void parseInfosheet(String filePath, TestHost testHost) throws IOException {
		CommandListToFile commandlistToFile;
		ScenarioParamAllocation scenarioParamAllocation;
		String hostIP;

		commandlistToFile = new CommandListToFile(filePath);
		scenarioParamAllocation = commandlistToFile.getScenarioParamAllocation();
		hostIP = scenarioParamAllocation.getHostIPAddress();

		int peerCount = commandlistToFile.getPeerCount();
		scenarioScriptPrinter(commandlistToFile, peerCount);
		commandlistToFile.receiveCommandsToFile(hostIP);
		commandlistToFile.runScenarioPrinter(peerCount, hostIP);
		commandlistToFile.hostCommandListToFile(testHost);
	}

	private static void parseArgs(String[] args, TestHost testHost) throws IOException {
		CommandListToFile commandlistToFile;
		String hostIP;
		commandlistToFile = new CommandListToFile();
		ScenarioParamAllocation scenarioParamAllocation = commandlistToFile.getScenarioParamAllocation();
		commandlistToFile.argsParser(args);
		System.out.println(commandlistToFile.getScenarioParamAllocation().getFileNameToBeSent() + " will be sent to the Peers.");

		hostIP = scenarioParamAllocation.getHostIPAddress();
		int peerCount = commandlistToFile.getPeerCount();

		scenarioScriptPrinter(commandlistToFile, peerCount);

		commandlistToFile.receiveCommandsToFile(hostIP);
		commandlistToFile.runScenarioPrinter(peerCount, hostIP);
		commandlistToFile.hostCommandListToFile(testHost);
	}

	private static void scenarioScriptPrinter(CommandListToFile clf, int peerCount) {
		scenarioIndex = clf.getScenarioParamAllocation().getScenarioIndex();

		switch (scenarioIndex) {
			case 0: ScenarioTCPStar testScenarioTCPStar = new ScenarioTCPStar(clf);
				testScenarioTCPStar.testScenarioCommandsToFile(peerCount);
				System.out.println(testScenarioTCPStar.getPrintSuccess());

			case 1: ScenarioTCPChain testScenarioTCPChain = new ScenarioTCPChain(clf);
				testScenarioTCPChain.testScenarioCommandsToFile(peerCount);
				System.out.println(testScenarioTCPChain.getPrintSuccess());
		}
	}

}