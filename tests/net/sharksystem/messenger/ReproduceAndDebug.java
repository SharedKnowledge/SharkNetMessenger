package net.sharksystem.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.hubaccess.UICommandAddHubDescription;
import net.sharksystem.app.messenger.commands.hubaccess.UICommandConnectHubFromDescriptionList;
import net.sharksystem.app.messenger.commands.hubaccess.UICommandListConnectedHubs;
import net.sharksystem.app.messenger.commands.hubaccess.UICommandListHubDescriptions;
import net.sharksystem.app.messenger.commands.hubaccess.UICommandRemoveHubDescription;
import net.sharksystem.app.messenger.commands.messenger.UICommandCreateChannel;
import net.sharksystem.app.messenger.commands.messenger.UICommandListChannels;
import net.sharksystem.app.messenger.commands.messenger.UICommandRemoveChannelByIndex;
import net.sharksystem.app.messenger.commands.messenger.UICommandSendMessage;
import net.sharksystem.app.messenger.commands.messenger.UICommandSetChannelAge;
import net.sharksystem.app.messenger.commands.pki.UICommandSendCredentialMessage;
import net.sharksystem.app.messenger.commands.pki.UICommandCreateNewKeyPair;
import net.sharksystem.app.messenger.commands.pki.UICommandShowCertificatesBySubject;
import net.sharksystem.app.messenger.commands.pki.UICommandGetOwnerInfo;
import net.sharksystem.app.messenger.commands.persons.UICommandSetSigningFailure;
import net.sharksystem.app.messenger.commands.tcp.UICommandCloseTCP;
import net.sharksystem.app.messenger.commands.tcp.UICommandConnectTCP;
import net.sharksystem.app.messenger.commands.tcp.UICommandOpenTCP;
import net.sharksystem.fs.ExtraData;
import net.sharksystem.fs.ExtraDataFS;
import net.sharksystem.fs.FSUtils;
import net.sharksystem.app.messenger.commands.test.TestMessageReceivedListener;
import net.sharksystem.app.messenger.commands.test.UICommandExecuteCommands;
import net.sharksystem.app.messenger.commands.test.UICommandSaveTestResults;
import net.sharksystem.app.messenger.commands.test.UICommandSendTestMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

/**
 * Test class for easily reconstruct bugs found in interactive use of the SharkMessenger command line UI.
 * Command history of the interactive mode can be print to screen or saved to file and used here for setting up the
 * test szenario.
 * Those Commands can bet passed to the initializeSharkMessengerUI method as parameter to create a UI
 * Object with the required behaviour for each peer.
 * <p>
 * For Szenarios where the correct alternating sequence of command execution of each peer is required the execution of
 * each command with the sharkMessengerUI.handleUserInput(command) might be the better solution.
 */
public class ReproduceAndDebug {

    // It is probably better to place data storage directory inside playground directory but the sharkPeerFS class
    // doesn't allow to change that directory.
    private static final String TEST_DATA_STORAGE = "sharkMessengerDataStorage";
    private static final String EXECUTE_BATCH_AT_ONCE = "executeCommands false";
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String TEST_CHANNEL = "test://t1";

    private ExtraData aliceSettings;

    private ExtraData getAliceSettings() throws SharkException, IOException {
        if(this.aliceSettings == null) {
            this.aliceSettings = new ExtraDataFS(TEST_DATA_STORAGE + "/" + ALICE + "Settings");
        }

        return this.aliceSettings;
    }

    private ExtraData getBobSettings() throws SharkException, IOException {
        if(this.aliceSettings == null) {
            this.aliceSettings = new ExtraDataFS(TEST_DATA_STORAGE + "/" + BOB + "Settings");
        }

        return this.aliceSettings;
    }

    private SharkNetMessengerUI initializeSharkMessengerUI(SharkNetMessengerApp sharkMessengerApp, String batchCommands)
            throws SharkException, IOException {

        SharkNetMessengerUI smUI = new SharkNetMessengerUI(batchCommands, System.in, System.out, System.err);

        // Add test received listener.
        sharkMessengerApp.getSharkMessengerComponent().addSharkMessagesReceivedListener(
                new TestMessageReceivedListener(sharkMessengerApp));

        //General
        //smUI.addCommand(new UICommandSaveLog(sharkMessengerApp, smUI, "saveLog", false));
        //smUI.addCommand(new UICommandShowLog(sharkMessengerApp, smUI, "showLog", false));
        //smUI.addCommand(new UICommandExit(sharkMessengerApp, smUI, "exit", false));

        // messages
        smUI.addCommand(new UICommandSendMessage(sharkMessengerApp, smUI, "sendMessage", true));

        // channels
        smUI.addCommand(new UICommandListChannels(sharkMessengerApp, smUI, "lsChannel", true));
        smUI.addCommand(new UICommandCreateChannel(sharkMessengerApp, smUI, "mkChannel", true));
        smUI.addCommand(new UICommandSetChannelAge(sharkMessengerApp, smUI, "setChAge", true));
        smUI.addCommand(new UICommandRemoveChannelByIndex(sharkMessengerApp, smUI, "rmCh", true));

        //PKI
        smUI.addCommand(new UICommandSendCredentialMessage(sharkMessengerApp, smUI, "sendCredential", true));
        smUI.addCommand(new UICommandGetOwnerInfo(sharkMessengerApp, smUI, "ownerInfo", true));
        smUI.addCommand(new UICommandCreateNewKeyPair(sharkMessengerApp, smUI, "mkKeys", true));
        smUI.addCommand(new UICommandShowCertificatesBySubject(sharkMessengerApp, smUI, "certBySubject", true));
        smUI.addCommand(new UICommandSetSigningFailure(sharkMessengerApp, smUI, "setSF", true));

        // Hub control
        smUI.addCommand(new UICommandListHubDescriptions(sharkMessengerApp, smUI, "lsHubDescr", true));
        smUI.addCommand(new UICommandAddHubDescription(sharkMessengerApp, smUI, "addHubDescr", true));
        smUI.addCommand(new UICommandRemoveHubDescription(sharkMessengerApp, smUI, "rmHubDescr", true));
        smUI.addCommand(new UICommandListConnectedHubs(sharkMessengerApp, smUI, "lsHubs", true));
        smUI.addCommand(new UICommandConnectHubFromDescriptionList(sharkMessengerApp, smUI, "connectHub", true));

        // Test
        smUI.addCommand(new UICommandOpenTCP(sharkMessengerApp, smUI, "openTCP", true));
        smUI.addCommand(new UICommandCloseTCP(sharkMessengerApp, smUI, "closeTCP", true));
        smUI.addCommand(new UICommandConnectTCP(sharkMessengerApp, smUI, "connectTCP", true));
        smUI.addCommand(new UICommandExecuteCommands(sharkMessengerApp, smUI, "executeCommands", false));
        smUI.addCommand(new UICommandSendTestMessage(sharkMessengerApp, smUI, "sendMessageTest", true));
        smUI.addCommand(new UICommandSaveTestResults(sharkMessengerApp, smUI, "saveTestResults", true));

        return smUI;
    }

    @BeforeEach
    public void resetStorageFolder() throws InterruptedException {
        // delete test dir if already exists
        FSUtils.removeFolder(TEST_DATA_STORAGE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                         REPRODUCE BUGS WITH UI COMMAND LOG                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * This problem only occurs on Linux OS!!!
     */
    @Test
    public void reconstructIncomingChunkStorageBugLinux() throws Exception {
        String cmdLogAlice = "openTCP 8888" +
                System.lineSeparator() +
                "mkChannel test://t1 channel1 false" +
                System.lineSeparator() +
                "sendMessage 0 false false hi_bob bob";

        String cmdLogBob = "connectTCP 8888 localhost" +
                System.lineSeparator() +
                "mkChannel test://t1 channel1 false";

        SharkNetMessengerApp smaAlice = new SharkNetMessengerApp(ALICE, System.out, System.err);
        SharkNetMessengerApp smaBob = new SharkNetMessengerApp(BOB, System.out, System.err);
        SharkNetMessengerUI smUIAlice = this.initializeSharkMessengerUI(smaAlice, cmdLogAlice);
        SharkNetMessengerUI smUIBob = this.initializeSharkMessengerUI(smaBob, cmdLogBob);

        // The Command execution is not alternating as in the original scenario but the chunk storage problem still can
        // be reconstructed.
        smUIAlice.handleUserInput(EXECUTE_BATCH_AT_ONCE);
        smUIBob.handleUserInput(EXECUTE_BATCH_AT_ONCE);


        // Give peers time for message exchange
        Thread.sleep(1000);

        // Compare number of sent and received messages
        int amountOutgoingMessages = smaAlice.getSharkMessengerComponent().getChannel("test://t1").getMessages().size();
        int amountIncomingMessages = smaBob.getSharkMessengerComponent().getChannel("test://t1").getMessages().size();
        Assertions.assertEquals(amountOutgoingMessages, amountIncomingMessages);
    }

    /**
     * This problem only occurs on Linux OS!!!
     * This tests basically the same problem as the first test but with control over the execution order.
     */
    @Test
    public void reconstructIncomingChunkStorageBugLinuxAlternatingExecution() throws Exception {
        String cmdAlice1st = "openTCP 8889";
        String cmdBob2nd = "connectTCP 8889 localhost";
        String cmdAlice3rd = "mkChannel test://t1 channel1 false";
        String cmdBob4th = "mkChannel test://t1 channel1 false";
        String cmdAlice5th = "sendMessage 0 false false hi_bob bob";

        SharkNetMessengerApp smaAlice = new SharkNetMessengerApp(ALICE, System.out, System.err);
        SharkNetMessengerApp smaBob = new SharkNetMessengerApp(BOB, System.out, System.err);
        SharkNetMessengerUI smUIAlice = this.initializeSharkMessengerUI(smaAlice, "");
        SharkNetMessengerUI smUIBob = this.initializeSharkMessengerUI(smaBob, "");

        smUIAlice.handleUserInput(cmdAlice1st);
        smUIBob.handleUserInput(cmdBob2nd);
        smUIAlice.handleUserInput(cmdAlice3rd);
        smUIBob.handleUserInput(cmdBob4th);
        smUIAlice.handleUserInput(cmdAlice5th);

        Thread.sleep(1000);

        // Compare number of sent and received messages
        int amountOutgoingMessages = smaAlice.getSharkMessengerComponent().getChannel("test://t1").getMessages().size();
        int amountIncomingMessages = smaBob.getSharkMessengerComponent().getChannel("test://t1").getMessages().size();
        Assertions.assertEquals(amountOutgoingMessages, amountIncomingMessages);
    }

    /**
     * Distributed tests (on Windows) showed that an exception in the ASAPMementoFS is thrown when around 15 messages
     * and more are sent without delay in between. The exception is thrown for each further sent Message starting from
     * around the 15th message. See the exception below.
     * With a delay somewhere between 30ms and 50ms the problem disappears.
     * (30ms it occurs, 50ms it didn't occur even with 500 messages)
     *
     * This test reproduces the problem close to the breaking point (25 messages with 30ms delay).
     *
     * Exception in thread "Thread-47" java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
     *         at java.base/java.io.DataOutputStream.writeUTF(DataOutputStream.java:360)
     *         at java.base/java.io.DataOutputStream.writeUTF(DataOutputStream.java:334)
     * DefaultSecurityAdministrator: ok
     *         at net.sharksystem.asap.engine.ASAPMementoFS.save(ASAPMementoFS.java:53)
     *         at net.sharksystem.asap.engine.ASAPEngineFS.getASAPEngineFS(ASAPEngineFS.java:179)
     *         at net.sharksystem.asap.engine.ASAPEngineFS.getASAPEngineFS(ASAPEngineFS.java:110)
     *         at net.sharksystem.asap.engine.ASAPEngineFS.getExistingASAPEngineFS(ASAPEngineFS.java:70)
     *         at net.sharksystem.asap.utils.ASAPLogHelper.getMessagesByChunkReceivedInfos(ASAPLogHelper.java:22)
     *         at net.sharksystem.asap.ASAPPeerFS.chunkStored(ASAPPeerFS.java:105)
     *         at net.sharksystem.asap.engine.ASAPEngine.handleASAPAssimilate(ASAPEngine.java:474)
     *         at net.sharksystem.asap.protocol.ASAPPersistentConnection$ASAPPDUExecutor.run(ASAPPersistentConnection.java:430)
     *
     */
    @Test
    public void reconstructThreadBugFromTests060324() throws Exception {
        String cmdAlice1st = "openTCP 8890";
        String cmdBob2nd = "connectTCP 8890 localhost";
        String cmdAlice3rd = "mkChannel test://t1 channel1 false";
        String cmdBob4th = "mkChannel test://t1 channel1 false";
        String cmdAlice5th = "sendMessageTest 25 30 0 false false hi_bob bob";

        SharkNetMessengerApp smaAlice = new SharkNetMessengerApp(ALICE, System.out, System.err);
        SharkNetMessengerApp smaBob = new SharkNetMessengerApp(BOB, System.out, System.err);
        SharkNetMessengerUI smUIAlice = this.initializeSharkMessengerUI(smaAlice, "");
        SharkNetMessengerUI smUIBob = this.initializeSharkMessengerUI(smaBob, "");

        smUIAlice.handleUserInput(cmdAlice1st);
        smUIBob.handleUserInput(cmdBob2nd);
        smUIAlice.handleUserInput(cmdAlice3rd);
        smUIBob.handleUserInput(cmdBob4th);
        smUIAlice.handleUserInput(cmdAlice5th);

        Thread.sleep(1000);

        // Compare number of sent and received messages
        int amountOutgoingMessages = smaAlice.getSharkMessengerComponent().getChannel("test://t1").getMessages().size();
        int amountIncomingMessages = smaBob.getSharkMessengerComponent().getChannel("test://t1").getMessages().size();
        Assertions.assertEquals(amountOutgoingMessages, amountIncomingMessages);
    }

}
