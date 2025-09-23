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

public class CoreScenarioOutput {

	private static void finalizeAndWriteToFile(String commandList, String fileName, char peer) {
		try {
			commandList = finalizeCommandList(commandList);
			FileUtils.writeToFile(new FileOutputStream(fileName + "Peer" +  peer + ".txt"), commandList);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	private static void finalizeAndWriteToFile(String commandList, String fileName, char peer, int wait) {
		try {
			commandList = finalizeCommandList(commandList, wait);
			FileUtils.writeToFile(new FileOutputStream(fileName + "Peer" +  peer + ".txt"), commandList);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void finalizeAndWriteToFile(String[] commandLists, String fileName) {
		try {
			char peer = 'A';
			for (String s : commandLists) {
				String string = finalizeCommandList(s);
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
			finalizeAndWriteToFile(coreA1, "CoreA1/CoreA1_" );

			//----------------------------------------//

			System.out.println("CoreA2:");
			Files.createDirectories(Path.of("CoreA2"));
			String[] coreA2 = tcpChainScenario.coreACommandLists(2);
			finalizeAndWriteToFile(coreA2, "CoreA2/CoreA2_");

			//----------------------------------------//

			System.out.println("CoreB1:");
			Files.createDirectories(Path.of("CoreB1"));
			String[] coreB1 = tcpChainScenario.coreBCommandLists(1);
			finalizeAndWriteToFile(coreB1[0], "CoreB1/CoreB1_", 'A');
			finalizeAndWriteToFile(coreB1[1], "CoreB1/CoreB1_", 'B', 6000);

			//----------------------------------------//

			System.out.println("CoreB2:");
			Files.createDirectories(Path.of("CoreB2"));
			String[] coreB2 = tcpChainScenario.coreBCommandLists(2);
			finalizeAndWriteToFile(coreB2, "CoreB2/CoreB2_");

			//----------------------------------------//

			System.out.println("1.1 Combined TCP Chain scenarios");
			System.out.println("CoreA1_Dis:");
			Files.createDirectories(Path.of("CoreA1_Dis"));
			String[] coreA1_copy = tcpChainScenario.coreACommandLists(1);
			String[] coreA1Dis = CoreScenariosTCPChain.appendCommandListWithCloseEncounter(coreA1_copy, 'a');
			coreA1Dis = combineCoreScenarios(coreA1Dis, coreA1_copy);
			finalizeAndWriteToFile(coreA1Dis, "CoreA1_Dis/CoreA1_Dis_");


			//----------------------------------------//


			Files.createDirectories(Path.of("CoreA2_Dis"));
			System.out.println("CoreA2_Dis:");
			String[] coreA2Dis = CoreScenariosTCPChain.appendCommandListWithCloseEncounter(tcpChainScenario.coreACommandLists(2), 'b');
			coreA2Dis = combineCoreScenarios(coreA2Dis, coreA2);
			finalizeAndWriteToFile(coreA2Dis, "CoreA2_Dis/CoreA2_Dis_");


			//----------------------------------------//


			Files.createDirectories(Path.of("CoreB1_Dis"));
			System.out.println("CoreB1_Dis:");
			String[] coreB1Dis = CoreScenariosTCPChain.appendCommandListWithCloseEncounter(tcpChainScenario.coreBCommandLists(1), 'a');
			coreB1Dis = combineCoreScenarios(coreB1Dis, coreB1);
			finalizeAndWriteToFile(coreB1Dis, "CoreB1_Dis/CoreB1_Dis_");

			//----------------------------------------//

			Files.createDirectories(Path.of("CoreB2_Dis"));
			System.out.println("CoreB2_Dis:");
			String[] coreB2Dis = CoreScenariosTCPChain.appendCommandListWithCloseEncounter(tcpChainScenario.coreBCommandLists(2), 'b');
			coreB2Dis = combineCoreScenarios(coreB2Dis, coreB2);
			finalizeAndWriteToFile(coreB2Dis, "CoreB2_Dis/CoreB2_Dis_");


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
			finalizeAndWriteToFile(commands[0], "HubCoreB1/HubCoreB1_", 'A');
			finalizeAndWriteToFile(commands[1], "HubCoreB1/HubCoreB1_", 'B', 6000);

			//----------------------------------------//

			System.out.println("HubCoreB2:");
			Files.createDirectories(Path.of("HubCoreB2"));
			FileUtils.writeToFile(new FileOutputStream("HubCoreB2/HubHost.txt"), hubHost);
			commands = hubScenario.hubB2Commands();
			finalizeAndWriteToFile(commands[0], "HubCoreB2/HubCoreB2_", 'A', 6000);
			finalizeAndWriteToFile(commands[1], "HubCoreB2/HubCoreB2_", 'B');

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
			finalizeAndWriteToFile(commands[1], "HubCoreDisB1/HubCoreDisB1_", 'B', 6000);
			finalizeAndWriteToFile(commands[0], "HubCoreDisB1/HubCoreDisB1_", 'A');
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
