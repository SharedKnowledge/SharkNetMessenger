package net.sharksystem.ui.messenger.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import net.sharksystem.SharkException;
import net.sharksystem.ui.messenger.cli.commands.basics.*;
import net.sharksystem.ui.messenger.cli.commands.encounter.UICommandCloseEncounter;
import net.sharksystem.ui.messenger.cli.commands.encounter.UICommandDenyEncounter;
import net.sharksystem.ui.messenger.cli.commands.encounter.UICommandListEncounter;
import net.sharksystem.ui.messenger.cli.commands.external.UICommandDecryptFile;
import net.sharksystem.ui.messenger.cli.commands.external.UICommandEncryptFile;
import net.sharksystem.ui.messenger.cli.commands.external.UICommandProduceSignature;
import net.sharksystem.ui.messenger.cli.commands.external.UICommandVerifySignedFile;
import net.sharksystem.ui.messenger.cli.commands.hubmanagement.UICommandListHub;
import net.sharksystem.ui.messenger.cli.commands.hubmanagement.UICommandStartHub;
import net.sharksystem.ui.messenger.cli.commands.hubmanagement.UICommandStopHub;
import net.sharksystem.ui.messenger.cli.commands.messenger.*;
import net.sharksystem.ui.messenger.cli.commands.persons.UICommandListPersons;
import net.sharksystem.ui.messenger.cli.commands.persons.UICommandRenamePerson;
import net.sharksystem.ui.messenger.cli.commands.persons.UICommandSetSigningFailure;
import net.sharksystem.ui.messenger.cli.commands.pki.*;
import net.sharksystem.ui.messenger.cli.commands.testing.*;
import net.sharksystem.ui.messenger.cli.commands.tcp.UICommandCloseTCP;
import net.sharksystem.ui.messenger.cli.commands.tcp.UICommandConnectTCP;
import net.sharksystem.ui.messenger.cli.commands.tcp.UICommandOpenTCP;
import net.sharksystem.ui.messenger.cli.commands.tcp.UICommandShowOpenTCPPorts;
import net.sharksystem.fs.ExtraData;
import net.sharksystem.fs.ExtraDataFS;
import net.sharksystem.hub.peerside.ASAPHubManager;
import net.sharksystem.ui.messenger.cli.commands.hubaccess.*;
import net.sharksystem.utils.Log;

/**
 * This class is the entry point for the application.
 * Only commands a user should be able to execute are used below.
 *
 * (I have no idea if that class serves any reason any longer. thsc, Aug'24)
 */
public class ProductionUI {
    public static final String SETTINGSFILENAME = ".sharkNetMessengerSessionSettings";
    public static final String PEERNAME_KEY = "peername";
    public static final String SYNC_WITH_OTHERS_IN_SECONDS_KEY = "syncWithOthersInSeconds";

    SharkNetMessengerUI smUI;

    public static void main(String[] args) throws SharkException, IOException {
        ProductionUI cli = new ProductionUI(args);
        cli.startCLI();
    }

    public void startCLI() {
        System.out.println("type 'help' to see the list of commands");
        smUI.runCommandLoop();
    }

    public ProductionUI(String[] args) throws SharkException, IOException {
        String peerName = null;
        int syncWithOthersInSeconds = ASAPHubManager.DEFAULT_WAIT_INTERVAL_IN_SECONDS;
        ExtraData sessionSettings = new ExtraDataFS("./" + SETTINGSFILENAME);
        boolean isBack = false;

        String batchCommands = "";

        /**
         * possible arguments
         * peerName
         */
        switch(args.length) {
            case 3:
                // batch commands expected
                batchCommands = args[2];
            case 2:
                try {
                    syncWithOthersInSeconds = Integer.parseInt(args[1]);
                }
                catch(NumberFormatException re) {
                    System.err.println("could not parse second parameter " +
                            "/ meant to be an integer telling how many seconds to wait for syncing with hubs"
                            + re.getLocalizedMessage());
                }
            case 1:
                peerName = args[0];
            case 0:
                break;
            default:
                System.out.println("possible arguments: ");
                System.out.println("\n peerName syncWithHubInSeconds");
                System.exit(1);
                break;

        }

        System.out.println("Welcome to SharkNetMessenger version 0.1");
        if(peerName == null) {
            try {
                byte[] storedPeerNameBytes = sessionSettings.getExtra(PEERNAME_KEY);
                // we have a peer name
                peerName = new String(storedPeerNameBytes);
                isBack = true;
            } catch(SharkException se) {
                // nothing from a previous session
            }
        }

        if(peerName == null) {
            peerName = "";
            // ask for peer name
            do {
                System.out.print("Please enter peer name (must not be empty; first character is a letter): ");
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                    peerName = bufferedReader.readLine();
                } catch (IOException e) {
                    System.err.println(e.getLocalizedMessage());
                    System.exit(0);
                }
            } while (peerName.equals(""));
            // store it
            sessionSettings.putExtra(PEERNAME_KEY, peerName.getBytes());
        }

        sessionSettings.putExtra(SYNC_WITH_OTHERS_IN_SECONDS_KEY, Integer.valueOf(syncWithOthersInSeconds));

        // Re-direct asap/shark log messages.
        PrintStream asapLogMessages = new PrintStream("asapLogs" + peerName + ".txt");
        Log.setOutStream(asapLogMessages);
        Log.setErrStream(asapLogMessages);

        if(isBack) System.out.println("Welcome back " + peerName);
        else System.out.println("Welcome " + peerName);

        this.smUI = new SharkNetMessengerUI(batchCommands, System.in, System.out, System.err);
        SharkNetMessengerApp sharkMessengerApp =
//                new SharkNetMessengerApp(peerName, syncWithOthersInSeconds, System.out, System.err);

           // use test support in this CLI - use base class in any other SNM View like Web, Android etc.
           new SharkNetMessengerAppSupportingDistributedTesting(
                   peerName, syncWithOthersInSeconds, System.out, System.err);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        //                              commands for test supporting views only                               //
        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Tests - do not use those commands in other implementation beside test support environment
        SharkNetMessengerAppSupportingDistributedTesting snmTestSupport =
                (SharkNetMessengerAppSupportingDistributedTesting) sharkMessengerApp;
        smUI.addCommand(new UICommandBlock(snmTestSupport, smUI, "block", false));
        smUI.addCommand(new UICommandScriptRQ(snmTestSupport, smUI, "scriptRQ", false));
        smUI.addCommand(new UICommandRelease(snmTestSupport, smUI, "release", false));

        smUI.addCommand(new UICommandSaveLog(sharkMessengerApp, smUI, "saveLog", false));
        smUI.addCommand(new UICommandShowLog(sharkMessengerApp, smUI, "showLog", false));
        smUI.addCommand(new UICommandClearLog(sharkMessengerApp, smUI, "clearLog", false));
        smUI.addCommand(new UICommandWait(sharkMessengerApp, smUI, "wait", false));
        smUI.addCommand(new UICommandEcho(sharkMessengerApp, smUI, "echo", false));
        smUI.addCommand(new UICommandMarkStep(sharkMessengerApp, smUI, "markstep", false));
        smUI.addCommand(new UICommandExistsMessage(sharkMessengerApp, smUI, "exists (NYI - TODO)", false));

        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        //                                 commands for any SNM view implementation                           //
        ////////////////////////////////////////////////////////////////////////////////////////////////////////

        // basics
        smUI.addCommand(new UICommandExit(sharkMessengerApp, smUI, "exit", true));
        smUI.addCommand(new UICommandDestroyPeer(sharkMessengerApp, smUI, "destroyPeer", false));
        smUI.addCommand(new UICommandStatus(sharkMessengerApp, smUI, "status", false));
        smUI.addCommand(new UICommandHelp(sharkMessengerApp, smUI, "help", false));

        // simple messenger
        smUI.addCommand(new UICommandListChannels(sharkMessengerApp, smUI, "lsChannels", true));
        smUI.addCommand(new UICommandCreateChannel(sharkMessengerApp, smUI, "mkChannel", true));
        smUI.addCommand(new UICommandRemoveChannelByIndex(sharkMessengerApp, smUI, "rmChannel", true));
        smUI.addCommand(new UICommandSendMessage(sharkMessengerApp, smUI, "sendMessage", true));
        smUI.addCommand(new UICommandListMessages(sharkMessengerApp, smUI, "lsMessages", true));

        // external io
        smUI.addCommand(new UICommandProduceSignature(sharkMessengerApp, smUI, "signFile", true));
        smUI.addCommand(new UICommandVerifySignedFile(sharkMessengerApp, smUI, "verifyFile", true));
        smUI.addCommand(new UICommandEncryptFile(sharkMessengerApp, smUI, "encryptFile", true));
        smUI.addCommand(new UICommandDecryptFile(sharkMessengerApp, smUI, "decryptFile", true));

        // TCP connection management
        smUI.addCommand(new UICommandOpenTCP(sharkMessengerApp, smUI, "openTCP", true));
        smUI.addCommand(new UICommandConnectTCP(sharkMessengerApp, smUI, "connectTCP", true));
        smUI.addCommand(new UICommandCloseTCP(sharkMessengerApp, smUI, "closeTCP", true));
        smUI.addCommand(new UICommandShowOpenTCPPorts(sharkMessengerApp, smUI, "lsTCPPorts", false));

        // encounter control
        smUI.addCommand(new UICommandListEncounter(sharkMessengerApp, smUI, "lsEncounter", false));
        smUI.addCommand(new UICommandCloseEncounter(sharkMessengerApp, smUI, "closeEncounter", true));
        smUI.addCommand(new UICommandDenyEncounter(sharkMessengerApp, smUI, "denyEncounter", false));

        // Persons
        smUI.addCommand(new UICommandListPersons(sharkMessengerApp, smUI, "lsPersons", false));
        smUI.addCommand(new UICommandSetSigningFailure(sharkMessengerApp, smUI, "setSF", true));
        smUI.addCommand(new UICommandRenamePerson(sharkMessengerApp, smUI, "rnPerson", true));

        // PKI
        smUI.addCommand(new UICommandListCertificates(sharkMessengerApp, smUI, "lsCerts", false));
        smUI.addCommand(new UICommandShowCertificatesByIssuer(sharkMessengerApp, smUI, "certsByIssuer", false));
        smUI.addCommand(new UICommandShowCertificatesBySubject(sharkMessengerApp, smUI, "certsBySubject", false));
        smUI.addCommand(new UICommandShowPendingCredentials(sharkMessengerApp, smUI, "lsCredentials", false));
        smUI.addCommand(new UICommandSendCredentialMessage(sharkMessengerApp, smUI, "sendCredential", true));
        smUI.addCommand(new UICommandAcceptCredential(sharkMessengerApp, smUI, "acceptCredential", true));
        smUI.addCommand(new UICommandRefuseCredential(sharkMessengerApp, smUI, "refuseCredential", true));

        // hub access
        smUI.addCommand(new UICommandConnectHub(sharkMessengerApp, smUI, "connectHub", true));
        smUI.addCommand(new UICommandDisconnectHub(sharkMessengerApp, smUI, "disconnectHub", true));
        smUI.addCommand(new UICommandListConnectedHubs(sharkMessengerApp, smUI, "lsConnectedHubs", true));
        smUI.addCommand(new UICommandConnectHubFromDescriptionList(sharkMessengerApp, smUI, "connectHubFromList", true));
        smUI.addCommand(new UICommandForceSyncHubInformation(sharkMessengerApp, smUI, "syncHubInfo", true));

        // hub description management
        smUI.addCommand(new UICommandListHubDescriptions(sharkMessengerApp, smUI, "lsHubDescr", false));
        smUI.addCommand(new UICommandAddHubDescription(sharkMessengerApp, smUI,"addHubDescr", true));
        smUI.addCommand(new UICommandRemoveHubDescription(sharkMessengerApp, smUI, "rmHubDescr", true));

        // hub management
        smUI.addCommand(new UICommandStartHub(sharkMessengerApp, smUI, "startHub", true));
        smUI.addCommand(new UICommandStopHub(sharkMessengerApp, smUI, "stopHub", true));
        smUI.addCommand(new UICommandListHub(sharkMessengerApp, smUI, "lsHubs", false));

    }
}