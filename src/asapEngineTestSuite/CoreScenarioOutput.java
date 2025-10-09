package asapEngineTestSuite;

import asapEngineTestSuite.testScenarios.core.CoreScenariosHub;
import asapEngineTestSuite.testScenarios.core.CoreScenariosTCPChain;
import asapEngineTestSuite.utils.fileUtils.FileUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static asapEngineTestSuite.testScenarios.CombinedScenarios.combineCoreScenarios;
import static asapEngineTestSuite.utils.CommandListFinalizer.finalizeCommandList;

public class  CoreScenarioOutput {

	//add new directory for the whole set up to go into?

	/**
	 * Directory name for the TCP Chain core scenarios
	 */
	static final String TCP_CHAIN_DIR = "tcpChain/";
	/**
	 * Directory name for Hub core scenarios
	 */
	static final String HUB_DIR = "hub/";

	static final String HUB = "Hub";


	/**
	 * Core Scenario Names
	 */
	public static final String CORE_A1 = "CoreA1";
	public static final String CORE_A2 = "CoreA2";

	public static final String CORE_B1 = "CoreB1";
	public static final String CORE_B2 = "CoreB2";

	public static final String HUB_CORE_1 = HUB + "Core1";
	public static final String HUB_CORE_2 = HUB + "Core2";

	public static final String PEER = "Peer";

	private static final String DISCONNECT = "Dis";


	public static final String CORE_A1_DIS = CORE_A1 + "_" + DISCONNECT;
	public static final String CORE_A2_DIS = CORE_A2 + "_" + DISCONNECT;
	public static final String CORE_B1_DIS = CORE_B1 + "_" + DISCONNECT;
	public static final String CORE_B2_DIS = CORE_B2 + "_" + DISCONNECT;

	public static final String HUB_HOST_TXT = "HubHost.txt";

	public static final String HUB_CORE_A1 = HUB + CORE_A1;
	public static final String HUB_CORE_A2 = HUB + CORE_A2;
	public static final String HUB_CORE_B1 = HUB + CORE_B1;
	public static final String HUB_CORE_B2 = HUB + CORE_B2;
	public static final String HUB_CORE_DIS_A1 = HUB + CORE_A1_DIS;
	public static final String HUB_CORE_DIS_A2 = HUB + CORE_A2_DIS;
	public static final String HUB_CORE_DIS_B2 = HUB + CORE_B2_DIS;
	private static final String HUB_CORE_DIS_B1 = HUB + CORE_B1_DIS;


	private static void finalizeAndWriteToFile(String commandList, String fileName, char peerIndex) {
		try {
			commandList = finalizeCommandList(commandList);
			FileUtils.writeToFile(new FileOutputStream(fileName + PEER +  peerIndex + ".txt"), commandList);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	private static void finalizeAndWriteToFile(String commandList, String fileName, char peerIndex, int wait) {
		try {
			commandList = finalizeCommandList(commandList, wait);
			FileUtils.writeToFile(new FileOutputStream(fileName + PEER +  peerIndex + ".txt"), commandList);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static String generateDirectoryName(String scenarioType, String scenarioName) {
		return scenarioType + "/" + scenarioName + "_";
	}

	private static void finalizeAndWriteToFile(String[] commandLists, String fileName) {
		try {
			char peerIndex = 'A';
			for (String s : commandLists) {
				String string = finalizeCommandList(s);
				FileUtils.writeToFile(new FileOutputStream(fileName + PEER +  peerIndex + ".txt"), string);
				peerIndex++;
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}


	public static void main(String[] args) {
		String filepath;

		CoreScenariosTCPChain tcpChainScenario = new CoreScenariosTCPChain();
		CoreScenariosHub hubScenario = new CoreScenariosHub();

		System.out.println("Core Scenarios for ASAP Engine Test Suite");

		try {
			Files.createDirectories(Path.of(TCP_CHAIN_DIR));
			//----------------------------------------//
			filepath = generateDirectoryName(TCP_CHAIN_DIR, CORE_A1);
			Files.createDirectories(Path.of(filepath));
			String[] coreA1 = tcpChainScenario.coreACommandLists(1);
			finalizeAndWriteToFile(coreA1, filepath + '/' + CORE_A1 + '_');

			//----------------------------------------//

			filepath = generateDirectoryName(TCP_CHAIN_DIR, CORE_A2);
			Files.createDirectories(Path.of(filepath));
			String[] coreA2 = tcpChainScenario.coreACommandLists(2);
			finalizeAndWriteToFile(coreA2, filepath + '/' + CORE_A2 + '_');

			//----------------------------------------//

			filepath = generateDirectoryName(TCP_CHAIN_DIR, CORE_B1);
			Files.createDirectories(Path.of(filepath));
			String[] coreB1 = tcpChainScenario.coreBCommandLists(1);
			finalizeAndWriteToFile(coreB1[0], filepath + '/' + CORE_B1 + '_', 'A');
			finalizeAndWriteToFile(coreB1[1], filepath + '/' + CORE_B1 + '_', 'B', 6000);

			//----------------------------------------//

			filepath = generateDirectoryName(TCP_CHAIN_DIR, CORE_B2);
			Files.createDirectories(Path.of(filepath));
			String[] coreB2 = tcpChainScenario.coreBCommandLists(2);
			finalizeAndWriteToFile(coreB2, filepath + '/' + CORE_B2 + '_');

			//----------------------------------------//

			System.out.println("1.1 Combined TCP Chain scenarios");
			filepath  = generateDirectoryName(TCP_CHAIN_DIR, CORE_A1_DIS);
			Files.createDirectories(Path.of(filepath));
			String[] coreA1_copy = tcpChainScenario.coreACommandLists(1);
			String[] coreA1Dis = CoreScenariosTCPChain.appendCommandListWithCloseEncounter(coreA1_copy, 'a');
			coreA1Dis = combineCoreScenarios(coreA1Dis, coreA1_copy);
			finalizeAndWriteToFile(coreA1Dis, filepath + "/CoreA1_Dis_");

			//----------------------------------------//

			filepath = generateDirectoryName(TCP_CHAIN_DIR, CORE_A2_DIS);
			Files.createDirectories(Path.of(filepath));
			String[] coreA2Dis = CoreScenariosTCPChain.appendCommandListWithCloseEncounter(tcpChainScenario.coreACommandLists(2), 'b');
			coreA2Dis = combineCoreScenarios(coreA2Dis, coreA2);
			finalizeAndWriteToFile(coreA2Dis, filepath + '/' + CORE_A2_DIS + '_');

			//----------------------------------------//

			filepath = generateDirectoryName(TCP_CHAIN_DIR, CORE_B1_DIS);
			Files.createDirectories(Path.of(filepath));
			String[] coreB1Dis = CoreScenariosTCPChain.appendCommandListWithCloseEncounter(tcpChainScenario.coreBCommandLists(1), 'a');
			coreB1Dis = combineCoreScenarios(coreB1Dis, coreB1);
			finalizeAndWriteToFile(coreB1Dis, filepath + '/' + CORE_B1_DIS + '_');

			//----------------------------------------//

			filepath = generateDirectoryName(TCP_CHAIN_DIR, CORE_B2_DIS);
			Files.createDirectories(Path.of(filepath));
			String[] coreB2Dis = CoreScenariosTCPChain.appendCommandListWithCloseEncounter(tcpChainScenario.coreBCommandLists(2), 'b');
			coreB2Dis = combineCoreScenarios(coreB2Dis, coreB2);
			finalizeAndWriteToFile(coreB2Dis, filepath + CORE_B2_DIS + '/' + CORE_B2_DIS + '_');


			System.out.println("2. Hub Core Scenarios");
			String hubHost = hubScenario.hubHostCommand();
			
			Files.createDirectories(Path.of(HUB_DIR));

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_1);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			finalizeAndWriteToFile(hubScenario.hubCoreCommands(1), filepath + '/' + HUB_CORE_1 + '_', 'A');
			finalizeAndWriteToFile(hubScenario.hubCoreCommands(2), filepath + '/' + HUB_CORE_1 + '_', 'B');

			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_2);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			finalizeAndWriteToFile(hubScenario.hubCoreCommands(2), filepath + '/' + HUB_CORE_2 + '_', 'A');
			finalizeAndWriteToFile(hubScenario.hubCoreCommands(1), filepath + '/' + HUB_CORE_2 + '_', 'B');

			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_A1);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			String[] commands = hubScenario.hubA1Commands();
			finalizeAndWriteToFile(commands, filepath + '/' + HUB_CORE_A1 + '_');

			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_A2);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			commands = hubScenario.hubA2Commands();
			finalizeAndWriteToFile(commands, filepath + '/' + HUB_CORE_A2 + '_');

			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_B1);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			commands = hubScenario.hubB1Commands();
			finalizeAndWriteToFile(commands[0], filepath + '/' + HUB_CORE_B1 + '_', 'A');
			finalizeAndWriteToFile(commands[1], filepath + '/' + HUB_CORE_B1 + '_', 'B', 6000);

			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_B2);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			commands = hubScenario.hubB2Commands();
			finalizeAndWriteToFile(commands[0], filepath + '/' + HUB_CORE_B2 + '_', 'A', 6000);
			finalizeAndWriteToFile(commands[1], filepath + '/' + HUB_CORE_B2 + '_', 'B');

			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_DIS_A1);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			commands = hubScenario.hubDisA1Commands();
			finalizeAndWriteToFile(commands, filepath + '/' + HUB_CORE_DIS_A1 + '_');

			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_DIS_A2);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			commands = hubScenario.hubDisA2Commands();
			finalizeAndWriteToFile(commands, filepath + '/' + HUB_CORE_DIS_A2 + '_');

			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_DIS_B1);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			commands = hubScenario.hubDisB1Commands();
			finalizeAndWriteToFile(commands[1], filepath + '/' + HUB_CORE_DIS_B1 + '_', 'B', 6000);
			finalizeAndWriteToFile(commands[0], filepath + '/' + HUB_CORE_DIS_B1 + '_', 'A');
			//----------------------------------------//

			filepath = generateDirectoryName(HUB_DIR, HUB_CORE_DIS_B2);
			Files.createDirectories(Path.of(filepath));
			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
			commands = hubScenario.hubDisB2Commands();
			finalizeAndWriteToFile(commands, filepath + '/' + HUB_CORE_DIS_B2 + '_');

			System.out.println("Core Scenarios generation completed.");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
