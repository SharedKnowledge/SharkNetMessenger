package asapEngineTestSuite.testScenarios.core.complex;

import asapEngineTestSuite.testScenarios.core.basic.CoreBasicEncounter;
import asapEngineTestSuite.utils.CommandListToFile;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Intermediate peers connect to a central peer which routes communications.
 */
public class CoreScenariosTCPStar extends CoreBasicEncounter {

	/**
	 * This method generates command lists for a star constellation with 4 peers using CS order.
	 * Cab;Cbc;Cbd;Sa:1;Sd:2;Eb:1;Ec:1;Ed:1;Ea:2;Eb:2;Ec:2
	 * @return the command lists for each peer as a String array
	 */
	public String[] star4CSCommands() {
		String[] commandsLists = new String[4];

		//center of the constellation: A opens Port for B to connect
		commandsLists[0] = WAIT + " 100" + CLI_SEPARATOR + centerPeerCommands() + CLI_SEPARATOR + WAIT + " 2000" + CLI_SEPARATOR;
		//B connects to A and sends a message
		commandsLists[1] = csSendingPeer("P1", 700) + CLI_SEPARATOR + WAIT + " 2000" + CLI_SEPARATOR;
		//C opens port and does *Nothing*
		commandsLists[2] = idlePeerCommands();
		//D opens port and sends message
		commandsLists[3] = csSendingPeer("P3") + CLI_SEPARATOR + WAIT + " 1000" + CLI_SEPARATOR;
		return commandsLists;
	}

	/**
	 * Commands for the center peer in the star constellation.
	 * @return the command list as a String
	 * todo: make port number configurable
	 * todo: make ip address configurable
	 */
	private String centerPeerCommands() {
		return openPortLine
			+ CLI_SEPARATOR
			+ "echo openTCP"
			+ CLI_SEPARATOR
			+ BLOCK + " P1"
			+ CLI_SEPARATOR
			// B connects to C and D
			+ WAIT + " 1500"
			//+ CommandListToFile.CONNECT_TCP + "FILLER_IP" + " " + "FILLER_PORT"
			+ CLI_SEPARATOR
			+ CommandListToFile.CONNECT_TCP + " " + "FILLER_IP" + " " + (portNr + 1)
			+ CLI_SEPARATOR
			+ "echo connectTCP"
			+ CLI_SEPARATOR
			+ WAIT + " 1500"
			+ CLI_SEPARATOR
			+ RELEASE + " P2"
			+ CLI_SEPARATOR
			+ "echo connectTCP"
			+ CLI_SEPARATOR
			+ CommandListToFile.CONNECT_TCP + " " + "FILLER_IP" + " " + (portNr + 2)
			+ CLI_SEPARATOR
			+ "echo connectTCP"
			+ CLI_SEPARATOR
			+ WAIT + " 4000"
			+CLI_SEPARATOR
			+ RELEASE + " P3"
			+ CLI_SEPARATOR
			+ "echo connectTCP"
			+ CLI_SEPARATOR;
	}

	/**
	 * Method to generate SC command lists for a star constellation with 4 peers.
	 * Sa:1;Sd:2;Cab;Cbc;Cbd;Eb:1;Ec:1;Ed:1;Ea:2;Eb:2;Ec:2
	 * @return the command lists for each peer as a String array
	 */
	public String[] star4SCCommands() {
		String[] commandsLists = new String[4];

		//center: a
		commandsLists[0] = centerPeerCommands();
		//b sends message and connects
		commandsLists[1] = scSendingPeer("P1");
		//C gets connected to and idles
		commandsLists[2] = idlePeerCommands();
		// d sends message and connects
		commandsLists[3] = scSendingPeer("P3");
		return commandsLists;
	}

	/**
	 * Commands for idle peers in the star constellation.
	 * @return the command list as a String
	 * todo: make port number configurable
	 */
	private String idlePeerCommands() {
		return CommandListToFile.OPEN_TCP + " " + (portNr + 1)
			+ CLI_SEPARATOR
			+ "echo openTCP"
			+ CLI_SEPARATOR
			+ BLOCK + " P2"
			+ CLI_SEPARATOR
			+ WAIT + " " + 5000
			+ CLI_SEPARATOR;
	}

	public static void main(String[] args) {
		CoreScenariosTCPStar star = new CoreScenariosTCPStar();
		String[] commands = star.star4CSCommands();
		for (int i = 0; i < commands.length; i++) {
			System.out.println("Peer " + (i + 1) + " commands:");
			System.out.println(commands[i]);
			System.out.println("-----");
		}

		commands = star.star4SCCommands();
		for (int i = 0; i < commands.length; i++) {
			System.out.println("Peer " + (i + 1) + " commands:");
			System.out.println(commands[i]);
			System.out.println("-----");
		}
	}
}