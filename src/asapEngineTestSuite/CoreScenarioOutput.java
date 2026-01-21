package asapEngineTestSuite;

import asapEngineTestSuite.testScenarios.core.basic.CoreBasicEncounter;
import asapEngineTestSuite.testScenarios.core.CoreScenariosHub;
import asapEngineTestSuite.testScenarios.core.complex.CoreScenariosTCPChain;
import asapEngineTestSuite.utils.fileUtils.FileUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static asapEngineTestSuite.utils.CommandListFinalizer.finalizeCommandList;


public class  CoreScenarioOutput {

	public static final String HUB = "hub";
	public static final String COMPLEX = "complex";
	public static final String CHAIN = "chain";
    public static final String BASIC = "basic";

	/**
	 * Directory name for the basic encounter core scenarios
	 */
	public static final String BASIC_DIR = BASIC; // no trailing slash, join later
	/**
	 * Directory name for hub core scenarios
	 */
	public static final String HUB_DIR = "hub"; // no trailing slash, join later


    public static final String CHAIN_DIR = "chain";
	/**
	 * Core Scenario Names
	 */
	public static final String CS = "CS";
	//public static final String CORE_A2 = "CoreA2";

	public static final String SC = "SC";
	//public static final String CORE_B2 = "CoreB2";

	public static final String PEER = "Peer";

	private static final String DISCONNECT = "Dis";

	public static final String HUB_HOST_TXT = "HubHost.txt";

	// default wait used for some peers
	private static final int DEFAULT_PEER_WAIT_MS = 1000;
    private static final String CHAINX = "CHAINX" ;
	public static final String CHAINLTX = "CHAINLTX" ;

	public static String tcpChainCore(String coreName) {
		return CHAIN + coreName;
	}

	private static String hubCoreName(String name) {
		return HUB + name;
	}

	private static String disconnected(String name) {
		return name + "_" + DISCONNECT;
	}

	private static void finalizeAndWriteToFile(String commandList, String fileName, char peerIndex) {
		try {
			Path outFile = extractTargetFileNames(fileName, peerIndex);

			commandList = finalizeCommandList(commandList);
			try (FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
				FileUtils.writeToFile(fos,commandList);
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	// helper that uses the project default wait; kept private to avoid callers passing different values
	private static void finalizeAndWriteToFileWithDefaultWait(String commandList, String fileName, char peerIndex) {
		try {
			Path outFile = extractTargetFileNames(fileName, peerIndex);

			commandList = finalizeCommandList(commandList, DEFAULT_PEER_WAIT_MS);

			try (FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
				FileUtils.writeToFile(fos,commandList);
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private static Path extractTargetFileNames(String fileName, char peerIndex) throws IOException {
		Path filepath = Path.of(fileName);
		Path parentDir = filepath.getParent();
		if (parentDir == null)
			parentDir = Path.of(".");
		Path peerDir = parentDir.resolve(PEER + peerIndex);
		Files.createDirectories(peerDir);

		String basename = filepath.getFileName().toString();

		return peerDir.resolve(basename + PEER + peerIndex + ".txt");
	}

	/**
	 * Concatenate directory name from scenario type and name
	 * @param scenarioType the scenario type directory (e.g. TCPChain or hub)
	 * @param scenarioName the scenario name to append
	 * @return the concatenated directory name
	 */
	public static String concatenateDirectoryName(String scenarioType, String scenarioName) {
		return scenarioType + "/" + scenarioName + "_";
	}

	public static void finalizeAndWriteToFile(String[] commandLists, String fileName) {
		try {
			Path filepath = Path.of(fileName);
			Path parentDir = filepath.getParent();
			if (parentDir != null) {
				Files.createDirectories(parentDir);
			}
			String basename = filepath.getFileName().toString();
			char peerIndex = 'A';
			for (String s : commandLists) {
				if (parentDir != null) {
					Path peerDir = parentDir.resolve(PEER + peerIndex);
					Files.createDirectories(peerDir);

					String string = finalizeCommandList(s);
					Path outFile = peerDir.resolve(basename + PEER + peerIndex + ".txt");

					try (FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
						FileUtils.writeToFile(fos, string);
					}
					peerIndex++;
				}
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}


	public static void main(String[] args) {
		String filepath;

		CoreBasicEncounter coreBasicEncounter = new CoreBasicEncounter();
        CoreScenariosTCPChain coreScenariosTCPChain = new CoreScenariosTCPChain();
		CoreScenariosHub coreScenariosHub = new CoreScenariosHub();

		try {
			filepath = concatenateDirectoryName(BASIC, CS);
			Files.createDirectories(Path.of(filepath));
			String[] coreA1 = coreBasicEncounter.csCommandLists(CS);
			finalizeAndWriteToFile(coreA1, filepath + '/' + CS + '_');

			//----------------------------------------//

//			filepath = concatenateDirectoryName(CHAIN_DIR, tcpChainCore(CORE_A2));
//			Files.createDirectories(Path.of(filepath));
//			String[] coreA2 = coreScenariosTCPChain.coreACommandLists(2, CORE_A2);
//			finalizeAndWriteToFile(coreA2, filepath + '/' + tcpChainCore(CORE_A2) + '_');

			//----------------------------------------//

			filepath = concatenateDirectoryName(BASIC, SC);
			Files.createDirectories(Path.of(filepath));
			String[] coreB1 = coreBasicEncounter.scCommandLists(SC);
			finalizeAndWriteToFile(coreB1[0], filepath + '/' + SC + '_', 'A');
			finalizeAndWriteToFile(coreB1[1], filepath + '/' + SC + '_', 'B');

			//----------------------------------------//

			filepath = concatenateDirectoryName(COMPLEX, CHAINX);
			Files.createDirectories(Path.of(filepath));
			String[] chainx = coreScenariosTCPChain.chainXCommands(CHAINX, 4);
			finalizeAndWriteToFile(chainx, filepath + '/' + CHAINX + '_');

			//----------------------------------------//

			filepath = concatenateDirectoryName(COMPLEX, CHAINLTX);






//			filepath = concatenateDirectoryName(CHAIN_DIR, tcpChainCore(CORE_B2));
//			Files.createDirectories(Path.of(filepath));
//			String[] coreB2 = tcpChainScenario.coreBCommandLists(2);
//			finalizeAndWriteToFile(coreB2, filepath + '/' + tcpChainCore(CORE_B2) + '_');

			//----------------------------------------//

//			filepath  = concatenateDirectoryName(CHAIN_DIR, TCP + disconnected(CORE_A1));
//			Files.createDirectories(Path.of(filepath));
//			String[] coreA1Dis = tcpChainScenario.coreADisCommandLists(1, disconnected(CORE_A1));
//			finalizeAndWriteToFile(coreA1Dis, filepath + "/" + TCP + disconnected(CORE_A1) + "_");
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(CHAIN_DIR, TCP + disconnected(CORE_A2));
//			Files.createDirectories(Path.of(filepath));
//			String[] coreA2Dis = tcpChainScenario.coreADisCommandLists(2, disconnected(CORE_A2));
//			finalizeAndWriteToFile(coreA2Dis, filepath + '/' + TCP + disconnected(CORE_A2) + '_');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(CHAIN_DIR, TCP + disconnected(CORE_B1));
//			Files.createDirectories(Path.of(filepath));
//			String[] coreB1Dis = tcpChainScenario.coreBDisCommandLists(1);
//			finalizeAndWriteToFile(coreB1Dis, filepath + '/' + TCP + disconnected(CORE_B1) + '_');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(CHAIN_DIR, TCP + disconnected(CORE_B2));
//			Files.createDirectories(Path.of(filepath));
//			String[] coreB2Dis = tcpChainScenario.coreBDisCommandLists(2);
//			finalizeAndWriteToFile(coreB2Dis, filepath + '/' + TCP + disconnected(CORE_B2) + '_');

//			String hubHost = finalizeCommandListHubHost(coreScenariosHub.hubHostCommand(), 60000);
//
//			Files.createDirectories(Path.of(HUB_DIR));
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName("Core1"));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			finalizeAndWriteToFile(coreScenariosHub.hubCoreCommands(1), filepath + '/' + hubCoreName("Core1") + '_', 'A');
//			finalizeAndWriteToFile(coreScenariosHub.hubCoreCommands(2), filepath + '/' + hubCoreName("Core1") + '_', 'B');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName("Core2"));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			finalizeAndWriteToFile(coreScenariosHub.hubCoreCommands(2), filepath + '/' + hubCoreName("Core2") + '_', 'A');
//			finalizeAndWriteToFile(coreScenariosHub.hubCoreCommands(1), filepath + '/' + hubCoreName("Core2") + '_', 'B');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName(CORE_A1));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			String[] commands = coreScenariosHub.hubA1Commands();
//			finalizeAndWriteToFile(commands, filepath + '/' + hubCoreName(CORE_A1) + '_');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName(CORE_A2));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			commands = coreScenariosHub.hubA2Commands();
//			finalizeAndWriteToFile(commands, filepath + '/' + hubCoreName(CORE_A2) + '_');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName(CORE_B1));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			commands = coreScenariosHub.hubB1Commands();
//			finalizeAndWriteToFileWithDefaultWait(commands[0], filepath + '/' + hubCoreName(CORE_B1) + '_', 'A');
//			finalizeAndWriteToFileWithDefaultWait(commands[1], filepath + '/' + hubCoreName(CORE_B1) + '_', 'B');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName(CORE_B2));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			commands = coreScenariosHub.hubB2Commands();
//			finalizeAndWriteToFileWithDefaultWait(commands[0], filepath + '/' + hubCoreName(CORE_B2) + '_', 'A');
//			finalizeAndWriteToFileWithDefaultWait(commands[1], filepath + '/' + hubCoreName(CORE_B2) + '_', 'B');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName(disconnected(CORE_A1)));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			commands = coreScenariosHub.hubDisA1Commands();
//			finalizeAndWriteToFile(commands, filepath + '/' + hubCoreName(disconnected(CORE_A1)) + '_');
//
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName(disconnected(CORE_A2)));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			commands = coreScenariosHub.hubDisA2Commands();
//			finalizeAndWriteToFile(commands, filepath + '/' + hubCoreName(disconnected(CORE_A2)) + '_');
//
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName(disconnected(CORE_B1)));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			commands = coreScenariosHub.hubDisB1Commands();
//			finalizeAndWriteToFileWithDefaultWait(commands[1], filepath + '/' + hubCoreName(disconnected(CORE_B1)) + '_', 'B');
//			finalizeAndWriteToFileWithDefaultWait(commands[0], filepath + '/' + hubCoreName(disconnected(CORE_B1)) + '_', 'A');
//			//----------------------------------------//
//
//			filepath = concatenateDirectoryName(HUB_DIR, hubCoreName(disconnected(CORE_B2)));
//			Files.createDirectories(Path.of(filepath));
//			FileUtils.writeToFile(new FileOutputStream(filepath + '/' + HUB_HOST_TXT), hubHost);
//			commands = coreScenariosHub.hubDisB2Commands();
//			finalizeAndWriteToFile(commands, filepath + '/' + hubCoreName(disconnected(CORE_B2)) + '_');

			System.out.println("Basic Encounter Scenarios generation completed.");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
