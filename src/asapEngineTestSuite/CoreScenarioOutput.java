package asapEngineTestSuite;

import asapEngineTestSuite.testScenarios.CoreScenariosHub;
import asapEngineTestSuite.testScenarios.CoreScenariosTCPChain;
import asapEngineTestSuite.utils.CommandListFinalizer;
import asapEngineTestSuite.utils.fileUtils.FileUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CoreScenarioOutput {

	private static void finalizeAndWriteToFile(String commandList, String fileName, char peer) {
		try {
				commandList = CommandListFinalizer.finalizeCommandList(commandList);
				FileUtils.writeToFile(new FileOutputStream(fileName + "Peer" +  peer + ".txt"), commandList);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void finalizeAndWriteToFile(String[] commandLists, String fileName) {
		try {
			char peer = 'A';
			for (String s : commandLists) {
				String string = CommandListFinalizer.finalizeCommandList(s);
				FileUtils.writeToFile(new FileOutputStream(fileName + "Peer" +  peer + ".txt"), string);
				peer++;
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public static void main(String[] args) {

		CoreScenariosTCPChain tcpChainScenario = new CoreScenariosTCPChain();
		CoreScenariosHub hubScenario = new CoreScenariosHub();

		System.out.println("Core Scenarios for ASAP Engine Test Suite");

		try {
			System.out.println("1. TCP Chain Scenarios");
			//----------------------------------------//
			System.out.println("CoreA1:");
			Files.createDirectories(Path.of("CoreA1"));
			String[] coreA1 = tcpChainScenario.coreACommandLists(1);
			finalizeAndWriteToFile(coreA1, "CoreA1/CoreA1_");

			//----------------------------------------//

			System.out.println("CoreA2:");
			Files.createDirectories(Path.of("CoreA2"));
			String[] coreA2 = tcpChainScenario.coreACommandLists(2);
			finalizeAndWriteToFile(coreA2, "CoreA2/CoreA2_");

			//----------------------------------------//

			System.out.println("CoreB1:");
			Files.createDirectories(Path.of("CoreB1"));
			String[] coreB1 = tcpChainScenario.coreBCommandLists(1);
			finalizeAndWriteToFile(coreB1, "CoreB1/CoreB1_");
			//----------------------------------------//

			System.out.println("CoreB2:");
			Files.createDirectories(Path.of("CoreB2"));
			String[] coreB2 = tcpChainScenario.coreBCommandLists(2);
			finalizeAndWriteToFile(coreB2, "CoreB2/CoreB2_");


			System.out.println("1.2 Combined TCP Chain scenarios");
			//todo

			System.out.println("2. Hub Core Scenarios");
			String hubHost = hubScenario.hubHostCommand();

			System.out.println("HubCore1:");
			Files.createDirectories(Path.of("HubCore1"));
			FileUtils.writeToFile(new FileOutputStream("HubCore1/HubHost.txt"), hubHost);
			finalizeAndWriteToFile(hubScenario.hubCoreCommands(1), "HubCore1/HubCore1_", 'A');
			finalizeAndWriteToFile(hubScenario.hubCoreCommands(2), "HubCore1/HubCore1_", 'B');

			//----------------------------------------//

			System.out.println("HubCore2:");
			Files.createDirectories(Path.of("HubCore2"));
			FileUtils.writeToFile(new FileOutputStream("HubCore2/HubHost.txt"), hubHost);
			finalizeAndWriteToFile(hubScenario.hubCoreCommands(2), "HubCore2/HubCore2_", 'A');
			finalizeAndWriteToFile(hubScenario.hubCoreCommands(1), "HubCore2/HubCore2_", 'B');

			//----------------------------------------//

			System.out.println("HubCoreA1:");
			Files.createDirectories(Path.of("HubCoreA1"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreA1/HubHost.txt"), hubHost);
			String[] commands = hubScenario.hubA1Commands();
			finalizeAndWriteToFile(commands, "HubCoreA1/HubCoreA1_");

			//----------------------------------------//

			System.out.println("HubCoreA2:");
			Files.createDirectories(Path.of("HubCoreA2"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreA2/HubHost.txt"), hubHost);
			commands = hubScenario.hubA2Commands();
			finalizeAndWriteToFile(commands, "HubCoreA2/HubCoreA2_");

			//----------------------------------------//

			System.out.println("HubCoreB1:");
			Files.createDirectories(Path.of("HubCoreB1"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreB1/HubHost.txt"), hubHost);
			commands = hubScenario.hubB1Commands();
			finalizeAndWriteToFile(commands, "HubCoreB1/HubCoreB1_");

			//----------------------------------------//

			System.out.println("HubCoreB2:");
			Files.createDirectories(Path.of("HubCoreB2"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreB2/HubHost.txt"), hubHost);
			commands = hubScenario.hubB2Commands();
			finalizeAndWriteToFile(commands, "HubCoreB2/HubCoreB2_");

			//----------------------------------------//

			System.out.println("HubCoreDisA1:");
			Files.createDirectories(Path.of("HubCoreDisA1"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreDisA1/HubHost.txt"), hubHost);
			commands = hubScenario.hubDisA1Commands();
			finalizeAndWriteToFile(commands, "HubCoreDisA1/HubCoreDisA1_");

			//----------------------------------------//

			System.out.println("HubCoreDisA2:");
			Files.createDirectories(Path.of("HubCoreDisA2"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreDisA2/HubHost.txt"), hubHost);
			commands = hubScenario.hubDisA2Commands();
			finalizeAndWriteToFile(commands, "HubCoreDisA2/HubCoreDisA2_");

			//----------------------------------------//

			System.out.println("HubCoreDisB1:");
			Files.createDirectories(Path.of("HubCoreDisB1"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreDisB1/HubHost.txt"), hubHost);
			commands = hubScenario.hubDisB1Commands();
			finalizeAndWriteToFile(commands, "HubCoreDisB1/HubCoreDisB1_");

			//----------------------------------------//

			System.out.println("HubCoreDisB2:");
			Files.createDirectories(Path.of("HubCoreDisB2"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreDisB2/HubHost.txt"), hubHost);
			commands = hubScenario.hubDisB2Commands();
			finalizeAndWriteToFile(commands, "HubCoreDisB2/HubCoreDisB2_");

			System.out.println("Core Scenarios generation completed.");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
