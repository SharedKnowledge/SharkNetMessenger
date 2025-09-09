package asapEngineTestSuite.testScenarios;

import asapEngineTestSuite.utils.CommandListToFile;
import asapEngineTestSuite.utils.fileUtils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CoreScenariosTCPChain extends ScenarioTCPChain {

    public static final int PORT_NUMBER = 4444;

    public static final String OPEN_PORT_LINE = CommandListToFile.OPEN_TCP + " " + PORT_NUMBER;

    public CoreScenariosTCPChain(CommandListToFile commandListToFile) {
        super(commandListToFile);
    }

    private String coreARecevingPeer() {
        return OPEN_PORT_LINE
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 2
                + System.lineSeparator()
                + CommandListToFile.LIST_MESSAGES;
    }

    private String coreASendingPeer() {
       return CommandListToFile
               .WAIT + " " + CommandListToFile.WAIT_TIME
               + System.lineSeparator()
               + CommandListToFile.CONNECT_TCP + "FILLER_IP" + PORT_NUMBER
               + System.lineSeparator()
               + CommandListToFile.SEND_MESSAGE + " " + "TCPChain_CoreA1" + "sn/char"
               +  System.lineSeparator()
               + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
               + System.lineSeparator()
               + CommandListToFile.LIST_MESSAGES
               + System.lineSeparator();
    }

    public void writeCoreAToFile(int order) {
        try {
            if (order == 1) {
                FileUtils.writeToFile(new FileOutputStream("/PeerA_CA1.txt"), coreASendingPeer());
                FileUtils.writeToFile(new FileOutputStream("/PeerB_CA1.txt"), coreARecevingPeer());
            } else {
                FileUtils.writeToFile(new FileOutputStream("/PeerB_CA2.txt"), coreASendingPeer());
                FileUtils.writeToFile(new FileOutputStream("/PeerA_CA2.txt"), coreARecevingPeer());
            }
        } catch (IOException e) {
                System.err.println(e.getMessage());
        }
    }

    private String coreBSendingPeer() {
        return CommandListToFile.SEND_MESSAGE + " " + "TCPChain_CoreB1" + "sn/char"
                +  System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.CONNECT_TCP + "FILLER_IP" + PORT_NUMBER
                + System.lineSeparator()
                + CommandListToFile.WAIT +  " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.LIST_MESSAGES
                + System.lineSeparator();
    }

    private String coreBReceivingPeer() {
        return OPEN_PORT_LINE
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 2
                + System.lineSeparator()
                + CommandListToFile.LIST_MESSAGES
                + System.lineSeparator();
    }

    //maybe change this logic... What about combined scenarios and exit at the end?
    public void writeCoreBToFile(int order) {
        try {
            if (order == 1) {
                FileUtils.writeToFile(new FileOutputStream("/PeerA_CB1.txt"), coreBSendingPeer());
                FileUtils.writeToFile(new FileOutputStream("/PeerB_CB1.txt"), coreBReceivingPeer());
            }
            else {
                FileUtils.writeToFile(new FileOutputStream("/PeerB_CB1.txt"), coreBSendingPeer());
                FileUtils.writeToFile(new FileOutputStream("/PeerA_CB1.txt"), coreBReceivingPeer());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}