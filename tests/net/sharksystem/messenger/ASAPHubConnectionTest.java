package net.sharksystem.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.SharkPeerFS;
import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPEncounterConnectionType;
import net.sharksystem.asap.ASAPEncounterManager;
import net.sharksystem.asap.ASAPEncounterManagerImpl;
import net.sharksystem.asap.apps.TCPServerSocketAcceptor;
import net.sharksystem.fs.FSUtils;
import net.sharksystem.hub.hubside.ASAPTCPHub;
import net.sharksystem.hub.peerside.HubConnector;
import net.sharksystem.hub.peerside.NewConnectionListener;
import net.sharksystem.hub.peerside.SharedTCPChannelConnectorPeerSide;
import net.sharksystem.messenger.*;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.pki.SharkPKIComponentFactory;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.streams.StreamPairImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

public class ASAPHubConnectionTest {
    private String TEST_FOLDER;
    private final CharSequence EXAMPLE_APP_FORMAT = "shark/x-connectPeersExample";
    private final int portNumberAlice = 6000;
    private final int portNumberBob = 6001;
    private final int hubPort = 6600;

    @BeforeEach
    public void init() {
        // get current user dir
        String currentDir = System.getProperty("user.dir");
        TEST_FOLDER = currentDir + "/ASAPHubConnectionTest";
        // delete test dir if already exists
        FSUtils.removeFolder(TEST_FOLDER);
    }


    /**
     * This test sends messages with the SharkMessenger using a TCP-Socket. The test uses instances of
     * ASAPConnectionHandler, ASAPEncounterManager and TCPServerSocketAcceptor to be able to send the messages
     * over a network.
     */
    /*
    @Test
    public void sendMessageUsingTCPSocket() throws IOException, SharkException, InterruptedException {
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(EXAMPLE_APP_FORMAT);
        String aliceFolder = TEST_FOLDER + "/" + TestConstants.ALICE_ID;
        String bobFolder = TEST_FOLDER + "/" + TestConstants.BOB_ID;

        ////////////////////////// set up peer alice
        SharkPeerFS alice = new SharkPeerFS(TestConstants.ALICE_ID, aliceFolder);

        SharkPKIComponentFactory certificateComponentFactory = new SharkPKIComponentFactory();
        // register this component with shark peer - note: we use interface SharkPeer
        alice.addComponent(certificateComponentFactory, SharkPKIComponent.class);
        SharkMessengerComponentFactory messengerFactory = new SharkMessengerComponentFactory(
                (SharkPKIComponent) alice.getComponent(SharkPKIComponent.class));
        alice.addComponent(messengerFactory, SharkMessengerComponent.class);
        alice.start();

        ////////////////////////// set up peer bob
        SharkPeerFS bob = new SharkPeerFS(TestConstants.BOB_ID, bobFolder);

        certificateComponentFactory = new SharkPKIComponentFactory();
        // register this component with shark peer - note: we use interface SharkPeer
        bob.addComponent(certificateComponentFactory, SharkPKIComponent.class);
        messengerFactory = new SharkMessengerComponentFactory(
                (SharkPKIComponent) bob.getComponent(SharkPKIComponent.class));
        bob.addComponent(messengerFactory, SharkMessengerComponent.class);
        bob.start();

        // alice creates new channel
        SharkMessengerComponent peerMessenger = (SharkMessengerComponent) alice.getComponent(SharkMessengerComponent.class);
        peerMessenger.createChannel("my_channel/test", "aliceChannel");

        // alice sends a message to bob
        peerMessenger.sendSharkMessage("Hi Bob".getBytes(), "my_channel/test", false);

        ASAPConnectionHandler aliceConnectionHandler = (ASAPConnectionHandler) alice.getASAPPeer();
        ASAPConnectionHandler bobConnectionHandler = (ASAPConnectionHandler) bob.getASAPPeer();

        ASAPEncounterManager aliceEncounterManager =
                new ASAPEncounterManagerImpl(aliceConnectionHandler, alice.getASAPPeer().getPeerID());

        ASAPEncounterManager bobEncounterManager =
                new ASAPEncounterManagerImpl(bobConnectionHandler, bob.getASAPPeer().getPeerID());

        //////////////////////////// set up server socket and handle connection requests
        TCPServerSocketAcceptor aliceTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberAlice, aliceEncounterManager);

        TCPServerSocketAcceptor bobTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberBob, bobEncounterManager);

        // give it a moment to settle
        Thread.sleep(5);
        // now, both side wit for connection establishment. Example

        // alice creates a client socket to be able to open a connection to Bob.
        // This part should be replaced using the ASAPHub
        Socket socket = new Socket("localhost", portNumberBob);

        // let Alice handle it
        aliceEncounterManager.handleEncounter(
                StreamPairImpl.getStreamPair(socket.getInputStream(), socket.getOutputStream()),
                ASAPEncounterConnectionType.INTERNET);

        // give it a moment to run ASAP session and receive message
        Thread.sleep(2000);

        // bob reads message
        peerMessenger = (SharkMessengerComponent) bob.getComponent(SharkMessengerComponent.class);

        SharkMessengerChannel channel = peerMessenger.getChannel("my_channel/test");

        SharkMessageList list = channel.getMessages();
        String receivedMessage = new String(list.getSharkMessage(0, true).getContent(), StandardCharsets.UTF_8);

        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("Hi Bob", receivedMessage);
    }

     */

    /**
     * This test exchanges a message between the peers Alice and Bob using the ASAPHub. The test uses instances of
     * ASAPConnectionHandler, ASAPEncounterManager and NewConnectionListener to reach this goal.
     */
    /*
    @Test
    public void sendMessageUsingHubConnector() throws IOException, SharkException, InterruptedException {
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(EXAMPLE_APP_FORMAT);
        String aliceFolder = TEST_FOLDER + "/" + TestConstants.ALICE_ID;
        String bobFolder = TEST_FOLDER + "/test/" + TestConstants.BOB_ID;

        // create hub instance
        ASAPTCPHub hub = new ASAPTCPHub(hubPort, false);
        new Thread(hub).start();

        ////////////////////////// set up peer alice
        SharkPeerFS alice = new SharkPeerFS(TestConstants.ALICE_ID, aliceFolder);

        SharkPKIComponentFactory certificateComponentFactory = new SharkPKIComponentFactory();
        // register this component with shark peer - note: we use interface SharkPeer
        alice.addComponent(certificateComponentFactory, SharkPKIComponent.class);
        SharkMessengerComponentFactory messengerFactory = new SharkMessengerComponentFactory(
                (SharkPKIComponent) alice.getComponent(SharkPKIComponent.class));
        alice.addComponent(messengerFactory, SharkMessengerComponent.class);
        alice.start();

        ////////////////////////// set up peer bob
        SharkPeerFS bob = new SharkPeerFS(TestConstants.BOB_ID, bobFolder);

        certificateComponentFactory = new SharkPKIComponentFactory();
        // register this component with shark peer - note: we use interface SharkPeer
        bob.addComponent(certificateComponentFactory, SharkPKIComponent.class);
        messengerFactory = new SharkMessengerComponentFactory(
                (SharkPKIComponent) bob.getComponent(SharkPKIComponent.class));
        bob.addComponent(messengerFactory, SharkMessengerComponent.class);
        bob.start();

        // alice creates new channel
        SharkMessengerComponent peerMessenger = (SharkMessengerComponent) alice.getComponent(SharkMessengerComponent.class);
        peerMessenger.createChannel("my_channel/test", "aliceChannel");

        // alice sends a message to bob
        peerMessenger.sendSharkMessage("Hi Bob".getBytes(), "my_channel/test", false);

        ASAPConnectionHandler aliceConnectionHandler = (ASAPConnectionHandler) alice.getASAPPeer();
        ASAPConnectionHandler bobConnectionHandler = (ASAPConnectionHandler) bob.getASAPPeer();

        ASAPEncounterManager aliceEncounterManager =
                new ASAPEncounterManagerImpl(aliceConnectionHandler, alice.getPeerID());
        ASAPEncounterManager bobEncounterManager =
                new ASAPEncounterManagerImpl(bobConnectionHandler, bob.getPeerID());

        // the TCPServerSocketAcceptor isn't needed here, because the NewConnectionListener of the
        // SharedTCPChannelConnectorPeerSide handles the encounter
////        ////////////////////////// set up server socket and handle connection requests
//        TCPServerSocketAcceptor aliceTcpServerSocketAcceptor =
//                new TCPServerSocketAcceptor(portNumberAlice, aliceEncounterManager);
//
//        TCPServerSocketAcceptor bobTcpServerSocketAcceptor =
//                new TCPServerSocketAcceptor(portNumberBob, bobEncounterManager);

        // give it a moment to settle
        Thread.sleep(5);

        // now, both side wit for connection establishment. Example

        // create connectors for alice and bob
        HubConnector aliceConnector = SharedTCPChannelConnectorPeerSide.createTCPHubConnector("127.0.0.1", hubPort);
        aliceConnector.addListener(new TestConnectionListener(aliceEncounterManager));

        HubConnector bobConnector = SharedTCPChannelConnectorPeerSide.createTCPHubConnector("127.0.0.1", hubPort);
        bobConnector.addListener(new TestConnectionListener(bobEncounterManager));

        // register alice and bob to Hub
        aliceConnector.connectHub(TestConstants.ALICE_ID);
        bobConnector.connectHub(TestConstants.BOB_ID);

        // alice establishes a connection to bob
        aliceConnector.connectPeer(TestConstants.BOB_ID);

        // make sure data exchange has finished
        Thread.sleep(2000);

        // bob reads message
        peerMessenger = (SharkMessengerComponent) bob.getComponent(SharkMessengerComponent.class);

        SharkMessengerChannel channel = peerMessenger.getChannel("my_channel/test");

        SharkMessageList list = channel.getMessages();
        String receivedMessage = new String(list.getSharkMessage(0, true).getContent(), StandardCharsets.UTF_8);

        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("Hi Bob", receivedMessage);
    }


     */
	class TestConnectionListener implements NewConnectionListener {
		private final ASAPEncounterManager encounterManager;

		public TestConnectionListener(ASAPEncounterManager encounterManager){
			this.encounterManager = encounterManager;
		}

		/**
		 * The method 'notifyPeerConnected' is called after the connection to a target peer was established.
		 * After the connection was established the method 'handleEncounter' of the ASAPEncounterManager is called to
		 * start the message exchange.
		 */
		@Override
		public void notifyPeerConnected(CharSequence charSequence, StreamPair streamPair) {
			try {
				encounterManager.handleEncounter(streamPair, ASAPEncounterConnectionType.INTERNET);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
