package net.sharksystem.ui.messenger.cli;

import net.sharksystem.SharkException;
import net.sharksystem.SharkPeer;
import net.sharksystem.SharkPeerEncounterChangedListener;
import net.sharksystem.SharkPeerFS;
import net.sharksystem.asap.*;
import net.sharksystem.asap.apps.TCPServerSocketAcceptor;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.asap.crypto.InMemoASAPKeyStore;
import net.sharksystem.asap.pki.ASAPCertificate;
import net.sharksystem.asap.utils.DateTimeHelper;
import net.sharksystem.asap.utils.PeerIDHelper;
import net.sharksystem.fs.ExtraData;
import net.sharksystem.fs.ExtraDataFS;
import net.sharksystem.fs.FSUtils;
import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.hub.HubConnectionManagerImpl;
import net.sharksystem.hub.NewHubConnectedListener;
import net.sharksystem.hub.hubside.ASAPTCPHub;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.app.messenger.SharkNetMessengerComponentFactory;
import net.sharksystem.hub.peerside.HubConnectorDescription;
import net.sharksystem.pki.*;
import net.sharksystem.ui.messenger.cli.commands.hubaccess.HubDescriptionPrinter;
import net.sharksystem.utils.Log;
import net.sharksystem.utils.streams.StreamPairImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.*;

/**
 * Proposed and suggested pattern for Shark app. Implement a central entity (could even be a singleton)
 * that provides access to any component that is part of this application
 */
public class SharkNetMessengerApp implements SharkPeerEncounterChangedListener, NewHubConnectedListener {
    private static final CharSequence PEER_ID_KEY = "peerIDKey";
    private final SharkPeerFS sharkPeerFS;

    // keystore
    private static final CharSequence KEYSTORE_MEMENTO_KEY = "keyStoreMemento";

    //private static final CharSequence ROOTFOLDER = "sharkMessengerDataStorage";
    private final SharkNetMessengerComponent messengerComponent;
    private final SharkPKIComponent pkiComponent;
    private final HubConnectionManager hubConnectionManager;
    private final ASAPEncounterManager encounterManager;
    private final ASAPEncounterManagerAdmin encounterManagerAdmin;
    private final String peerDataFolderName;
    private final ExtraData appSettings;
    private final String peerName;
    private PrintStream outStream;
    private PrintStream errStream;
    private CharSequence peerID;

    public SharkNetMessengerApp(String peerName, PrintStream out, PrintStream err)
            throws SharkException, IOException {
        this(peerName, 60*10, out, err);
    }

    /**
     * Setup the system
     * @param peerName
     * @param syncWithOthersInSeconds
     * @param out
     * @param err
     * @throws SharkException
     * @throws IOException
     */
    public SharkNetMessengerApp(String peerName, int syncWithOthersInSeconds, PrintStream out, PrintStream err)
            throws SharkException, IOException {

        this.peerDataFolderName = "./" + peerName;
        this.appSettings = new ExtraDataFS(".", "SharkMessengerSetting" + peerName);
        this.outStream = out;
        this.errStream = err;
        this.sharkPeerFS = new SharkPeerFS(peerName, this.peerDataFolderName);
        this.peerName = peerName;

        try {
            this.peerID = this.sharkPeerFS.getSharkPeerExtraData().getExtraString(PEER_ID_KEY);
            this.tellUI("asap peer id for " + peerName + " is " + this.peerID);
        }
        catch (SharkException se) {
            this.peerID = peerName + "_" + PeerIDHelper.createUniqueID();
            this.tellUI("created a new asap peer id for " + peerName + ": " + this.peerID);
            this.sharkPeerFS.getSharkPeerExtraData().putExtra(PEER_ID_KEY, this.peerID);
        }

        // set up shark components
        // get PKI factory
        SharkPKIComponentFactory pkiComponentFactory = new SharkPKIComponentFactory();

        // tell peer
        this.sharkPeerFS.addComponent(pkiComponentFactory, SharkPKIComponent.class);

        // get messenger factory with pki component as parameter.
        SharkNetMessengerComponentFactory messengerComponentFactory = new SharkNetMessengerComponentFactory(
                (SharkPKIComponent) sharkPeerFS.getComponent(SharkPKIComponent.class));

        // tell peer
        this.sharkPeerFS.addComponent(messengerComponentFactory, SharkNetMessengerComponent.class);

        // all component in place - create asap peer and launch the system
        ASAPPeer asapPeer = new ASAPPeerFS(this.peerID, this.peerDataFolderName, this.sharkPeerFS.getSupportedFormats());
        // create Key Store
        ASAPKeyStore keyStore = new InMemoASAPKeyStore(this.peerID);
        asapPeer.setASAPKeyStore(keyStore);

//        this.sharkPeerFS.start(this.peerID);
        this.sharkPeerFS.start(asapPeer);

        // get component to add listener
        this.messengerComponent = (SharkNetMessengerComponent) this.sharkPeerFS.
                getComponent(SharkNetMessengerComponent.class);
        this.messengerComponent.addSharkMessagesReceivedListener(new MessageReceivedListener(this));

        // get component to add listener
        this.pkiComponent = (SharkPKIComponent) this.sharkPeerFS.getComponent(SharkPKIComponent.class);
        this.pkiComponent.setSharkCredentialReceivedListener(new CredentialReceivedListener(this));

        //System.out.println("Fill PKI with example data - for testing purposes");
        //HelperPKITests.fillWithExampleData(this.pkiComponent);

        // get informed about encounter changes
        this.sharkPeerFS.addSharkPeerEncounterChangedListener(this);

        //////////////////////// setup hub connection management
        // TODO: that's still a design flaw. We need something that extracts a connection handler from a peer. Do we?

        ASAPConnectionHandler asapHandler = (ASAPConnectionHandler) asapPeer;
        // this code runs on service side - this peer should be a connection handler
        if (asapPeer instanceof ASAPConnectionHandler) { // TODO: aaaaargs
            // yes it is
            ASAPEncounterManagerImpl asapEncounterManager =
                    new ASAPEncounterManagerImpl(asapHandler, asapPeer.getPeerID(),
                            syncWithOthersInSeconds * 100L);
            // same object - different roles
            this.encounterManager = asapEncounterManager;
            this.encounterManagerAdmin = asapEncounterManager;

            this.hubConnectionManager =
//                    new HubConnectionManagerImpl(this.encounterManager, asapPeer, syncWithOthersInSeconds);
                  new HubConnectionManagerImpl(this.encounterManager, asapPeer); // default time is okay

            this.hubConnectionManager.addNewConnectedHubListener(this);

            if(this.getSettings().getHubReconnect()) {
                // try to reconnect to any known hub
                List<HubConnectorDescription> hubDescriptions = this.getSharkPeer().getHubDescriptions();
                if(!hubDescriptions.isEmpty()) {
                    this.tellUI("try to reconnect to hubs");
                    this.hubConnectionManager.connectHubs(hubDescriptions);
                    try {
                        Thread.sleep(100); // some output can be produced.
                    } catch (InterruptedException e) {
                        //
                    }
                }
            }
        } else {
            Log.writeLogErr(this,
                    "ASAP peer set but is not a connection handler - cannot set up connection management");
            throw new SharkException("Cannot set up connection management, see error logs.");
        }
    }

    //////////////////// components
    public SharkPeer getSharkPeer() {
        return this.sharkPeerFS;
    }

    public SharkNetMessengerComponent getSharkMessengerComponent() {
        return this.messengerComponent;
    }

    public HubConnectionManager getHubConnectionManager() throws SharkException {
        return this.hubConnectionManager;
    }

    public SharkPKIComponent getSharkPKIComponent() {
        return this.pkiComponent;
    }

    public void destroyAllData() throws SharkException, IOException {
        FSUtils.removeFolder(this.peerDataFolderName);
        this.appSettings.removeAll();
    }

    public String getPeerName() {
        return this.peerName;
    }

    /////////////////// settings
    private final SharkNetMessengerSettings settings = new Settings();
    public SharkNetMessengerSettings getSettings() {
        return this.settings;
    }

    private class Settings implements SharkNetMessengerSettings {
        private boolean rememberNewHubConnections = true;
        private boolean hubReconnect = true;
        public boolean getRememberNewHubConnections() {
            return this.rememberNewHubConnections;
        }
        public void setRememberNewHubConnections(boolean rememberNewHubConnections) {
            this.rememberNewHubConnections = rememberNewHubConnections;
        }

        @Override
        public boolean getHubReconnect() {
            return this.hubReconnect;
        }

        @Override
        public void setHubReconnect(boolean hubReconnect) {
            this.hubReconnect = hubReconnect;
        }

        public String toString() {
            String sb = "rememberNewHubConnections: " +
                    this.rememberNewHubConnections +
                    " | hubReconnect: " +
                    this.hubReconnect +
                    "\n";
            return sb;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                    direct TCP connections                               //
    /////////////////////////////////////////////////////////////////////////////////////////////
    private final Map<Integer, TCPServerSocketAcceptor> openSockets = new HashMap<>();

    private boolean portAlreadyInUse(int port) {
        // hub using that port?
        Object something = this.asapHubs.get(port);
        if(something == null) {
            // an open tcp port?
            something = this.openSockets.get(port);
            if (something == null) return false; // exit - port is available
        }

        tellUI("port already in use - choose another one");
        return true;
    }

    public void openTCPConnection(int portNumber) throws IOException {
        if(this.portAlreadyInUse(portNumber)) return;
        TCPServerSocketAcceptor tcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumber, this.encounterManager, true);
        this.openSockets.put(portNumber, tcpServerSocketAcceptor);
        Log.writeLog(this, "Socket open to connect on port: " + portNumber);
    }

    public void connectTCP(String host, int portNumber) throws IOException {
        if(host.equalsIgnoreCase("127.0.0.1") || host.equalsIgnoreCase("localhost")) {
            if(this.openSockets.containsKey(portNumber)) {
                System.err.println("attempt to establish a connection to same process/peer refused");
                return;
            }
        }
        Socket socket = new Socket(host, portNumber);
        this.encounterManager.handleEncounter(StreamPairImpl.getStreamPair(
                socket.getInputStream(), socket.getOutputStream()), ASAPEncounterConnectionType.INTERNET);
        Log.writeLog(this, "connected to: " + host + " on port: " + portNumber);
    }

    public void closeTCPConnection(int portNumber) throws IOException {
        this.openSockets.remove(portNumber).close();
        Log.writeLog(this, "Socket closed on port: " + portNumber);
    }

    public Iterator<Integer> getOpenSockets() {
        return this.openSockets.keySet().iterator();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                    encounter management                                 //
    /////////////////////////////////////////////////////////////////////////////////////////////

    public ASAPEncounterManagerAdmin getEncounterManagerAdmin() {
        return this.encounterManagerAdmin;
    }


    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                         hub management                                  //
    /////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<Integer, ASAPTCPHub> asapHubs = new HashMap<>();

    public Set<Integer> getOpenHubPorts() {
        return this.asapHubs.keySet();
    }

    public void startHub(int portNumber, boolean createNewConnection, int maxIdleInSeconds) throws IOException {
        if(this.portAlreadyInUse(portNumber)) return;

        ASAPTCPHub asapHub = new ASAPTCPHub(portNumber, createNewConnection);
        asapHub.setMaxIdleConnectionInSeconds(maxIdleInSeconds);
        this.asapHubs.put(portNumber, asapHub);
        new Thread(asapHub).start();
    }

    public void stopHub(int portNumber) throws IOException {
        ASAPTCPHub asapHub = this.asapHubs.remove(portNumber);
        if(asapHub == null) {
            this.tellUIError("there is no ASAP hub listening on port " + portNumber);
            return;
        }
        // stop it
        asapHub.kill();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                    credential management                                //
    /////////////////////////////////////////////////////////////////////////////////////////////

    private final List<CredentialMessage> pendingCredentialMessages = new ArrayList<>();
    public void addPendingCredentialMessage(CredentialMessage credentialMessage) {
        this.pendingCredentialMessages.add(credentialMessage);
        Log.writeLog(this, "credentialMessage received");
        this.tellUI("credential message received .. please handle pending messages asap");
    }

    public List<CredentialMessage> getPendingCredentialMessages() {
        return this.pendingCredentialMessages;
    }


    public void actionOnPendingCredentialMessageOnIndex(int index, boolean accept)
            throws ASAPSecurityException, IOException {
        if (index < 1) {
            this.tellUI("\nminimal index is 1");
            return;
        }
        if(this.pendingCredentialMessages.size() < index) {
            this.tellUI("\nindex " + index + " exceeds maximum of " + this.pendingCredentialMessages.size());
            return;
        }
        // we are in the range
        index--; // adjust to internal counting .. we start with 0 as any normal person ;)
        CredentialMessage actionedCredential = this.pendingCredentialMessages.remove(index);

        if(accept) {
            try {
                this.getSharkPKIComponent().getPersonValuesByName(actionedCredential.getSubjectName());
                this.tellUI("we already know a peer with that name - take id instead their name for your address book");
                actionedCredential.setSubjectName(actionedCredential.getSubjectID());
            }
            catch(ASAPException ae) {
                this.tellUI("You do not know a peer with that name yet.");
            }

            this.getSharkPKIComponent().acceptAndSignCredential(actionedCredential);
            this.tellUI("\nissued certificate based on:\n" + PKIHelper.credentialMessage2String(actionedCredential));
        } else {
            this.tellUI("\nrefused to issue a certificate:\n" + PKIHelper.credentialMessage2String(actionedCredential));
        }

        this.tellUI("\nnote - indices of pending credential message has changed. Produce a new list before further actions.");
    }

    public void refusePendingCredentialMessageOnIndex(int index) throws ASAPSecurityException, IOException {
        this.actionOnPendingCredentialMessageOnIndex(index, false);
    }

    public void acceptPendingCredentialMessageOnIndex(int index) throws ASAPSecurityException, IOException {
        this.actionOnPendingCredentialMessageOnIndex(index, true);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                 environment changed handling                            //
    /////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////// Encounter
    public class EncounterLog {
        public final ASAPEncounterConnectionType encounterType;
        public final long startTime;
        public long stopTime;
        public final CharSequence peerID;

        EncounterLog(ASAPEncounterConnectionType encounterType, CharSequence peerID) {
            this.encounterType = encounterType;
            this.peerID = peerID;
            this.startTime = System.currentTimeMillis();
            this.stopTime = -1;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.peerID);
            try {
                CharSequence peerName = SharkNetMessengerApp.this.getSharkPKIComponent()
                        .getPersonValuesByID(this.peerID).getName();
                sb.append(" | name: ");
                sb.append(peerName);
            }
            catch(ASAPException ae) {
                // ignore - do not know that peer.
            }
            sb.append(" | connection: ");
            sb.append(this.encounterType.toString());
            sb.append(" | started: ");
            sb.append(DateTimeHelper.long2ExactTimeString(this.startTime));
            if(this.stopTime > -1) {
                sb.append(" | closed: ");
                sb.append(DateTimeHelper.long2ExactTimeString(this.stopTime));
            } else {
                sb.append(" | open");
            }
            sb.append("\n");
            return sb.toString();
        }
    }

    private final Map<CharSequence, List<EncounterLog>> encounterLogs = new HashMap<>();
    public Map<CharSequence, List<EncounterLog>> getEncounterLogs() {
        return this.encounterLogs;
    }

    @Override
    public void encounterStarted(CharSequence peerID) {
        ASAPEncounterConnectionType connectionType = ASAPEncounterConnectionType.UNKNOWN;

        try {
             connectionType = this.encounterManagerAdmin.getConnectionType(peerID);
            this.tellUI("\nnew encounter to " + peerID + " via " + connectionType);
        } catch (ASAPException e) {
            String s = "new connection but no connection type?? " + e.getLocalizedMessage();
            Log.writeLogErr(this, s);
            this.tellUIError(s);
            return;
        }

        // log
        // already an entry for that peer present?
        List<EncounterLog> logs = this.encounterLogs.get(peerID);
        if(logs == null) {
            logs = new ArrayList<>();
            this.encounterLogs.put(peerID, logs);
        }
        logs.add(new EncounterLog(connectionType, peerID));

        // check if better ask for a (fresh) certificate
        if(!(
            connectionType == ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK // if directly connected
            || connectionType == ASAPEncounterConnectionType.INTERNET) // or via TCP for testing
        ) return;

        try {
            try {
                ASAPCertificate certificateByIssuerAndSubject =
                        this.getSharkPKIComponent().
                                getCertificateByIssuerAndSubject(peerID, this.getSharkPeer().getPeerID());

                String sb = "\nYou have an encounter with peer " +
                        peerID +
                        ". It issued a certificate for you that runs out " +
                        DateTimeHelper.
                                long2DateString(certificateByIssuerAndSubject.getValidUntil().getTimeInMillis());
                this.tellUI(sb);
            }
            catch(ASAPSecurityException ase) {
                String sb = "\nPeer " +
                        peerID +
                        " has not yet issued a certificate for you. You are connected now. A good time to ask for one?";
                this.tellUI(sb);
            }
        } catch (SharkException e) {
            this.tellUIError("unexpected problems when dealing with present certificates: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void encounterTerminated(CharSequence peerID) {
        // assume it the last encounter with that peer
        List<EncounterLog> logs = this.encounterLogs.get(peerID);
        if(logs == null) {
            this.tellUIError("encounter terminated but no record - that's unexpected: " + peerID);
        }

        EncounterLog log = logs.get(logs.size() - 1);
        log.stopTime = System.currentTimeMillis();
        this.tellUI("\nencounter closed: " + log);
    }

    ////////////////////////// Hub Connections
    @Override
    public void newHubConnected(HubConnectorDescription hubConnectorDescription) {
        this.tellUI("connected to a new hub:\n");
        HubDescriptionPrinter.print(new PrintStream(this.getOutStream()), hubConnectorDescription);
        if(this.getSettings().getRememberNewHubConnections()) {
            this.getSharkPeer().addHubDescription(hubConnectorDescription);
            this.tellUI("\nremembered hub description\n");
        } else {
            this.tellUI("\n");
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                    communicate with UI                                  //
    /////////////////////////////////////////////////////////////////////////////////////////////

    public OutputStream getOutStream() { return this.outStream;}
    public OutputStream getErrorStream() { return this.errStream;}

    public void setUIStreams(PrintStream outStream, PrintStream errStream) {
        this.outStream = outStream;
        this.errStream = errStream;
    }

    private String enhanceMessageBeforePrinting(String msg) {
        String sb = DateTimeHelper.long2ExactTimeString(System.currentTimeMillis()) +
                ": " +
                msg;
        return sb;
    }

    public void tellUI(String message) {
        this.outStream.println(this.enhanceMessageBeforePrinting(message));
    }

    public void tellUIError(String message) {
        this.errStream.println(this.enhanceMessageBeforePrinting(message));
    }

}
