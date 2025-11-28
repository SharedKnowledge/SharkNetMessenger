package net.sharksystem.app.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.SharkNotSupportedException;
import net.sharksystem.asap.*;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.*;

public class SharkNetMessengerComponentImpl extends SharkNetMessagesReceivedListenerManager
        implements SharkNetMessengerComponent, ASAPMessageReceivedListener {

    private final SharkPKIComponent sharkPKIComponent;
    private ASAPPeer asapPeer;

    public SharkNetMessengerComponentImpl(SharkPKIComponent sharkPKIComponent) {
        this.sharkPKIComponent = sharkPKIComponent;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        this.asapPeer = asapPeer;

        // make sure the default channel exists.
        try {
            this.createChannel(
                SharkNetMessengerComponent.GENERAL_CHANNEL_URI,
                SharkNetMessengerComponent.GENERAL_CHANNEL_NAME);
        } catch (IOException ex) {
            throw new SharkException(ex);
        }

        Log.writeLog(this, "MAKE URI LISTENER PUBLIC AGAIN. Thank you :)");
        this.asapPeer.addASAPMessageReceivedListener(
                SharkNetMessengerComponent.SHARK_MESSENGER_FORMAT,
                this);
    }


    private void checkComponentRunning() throws SharkNetMessengerException {
        if(this.asapPeer == null || this.sharkPKIComponent == null)
            throw new SharkNetMessengerException("peer not started an/or pki not initialized");
    }

    @Override
    public void sendSharkMessage(String contentType, byte[] content, CharSequence uri, boolean sign) throws IOException, SharkNetMessengerException {
        HashSet<CharSequence> set = new HashSet<>();
        this.sendSharkMessage(contentType, content, uri, set, sign, false);
    }

    @Override
    public void sendSharkMessage(String contentType, byte[] content, CharSequence uri,
                                 CharSequence receiver, boolean sign,
                                 boolean encrypt) throws IOException, SharkNetMessengerException {
        HashSet<CharSequence> set = new HashSet<>();
        if(receiver != null) set.add(receiver);
        this.sendSharkMessage(contentType, content, uri, set, sign, encrypt);
    }

    @Override
    public void sendSharkMessage(String contentType, byte[] content, CharSequence uri,
                                 Set<CharSequence> selectedRecipients, boolean sign,
                                 boolean encrypt)
            throws SharkNetMessengerException, IOException {

        this.checkComponentRunning();

        // lets serialize and send asap messages.
        try {
            if (encrypt && selectedRecipients != null && selectedRecipients.size() > 1) {
                // more than one receiver and encrypted. Send one message for each.
                for(CharSequence receiver : selectedRecipients) {
                    this.asapPeer.sendASAPMessage(SHARK_MESSENGER_FORMAT, uri,
                            // we have at most one receiver - this method can handle all combinations
                            InMemoSharkNetMessage.serializeMessage(
                                    contentType,
                                    content,
                                    this.asapPeer.getPeerID(),
                                    receiver,
                                    sign, encrypt,
                                    this.sharkPKIComponent.getASAPKeyStore()));
                }
            } else {
                this.asapPeer.sendASAPMessage(SHARK_MESSENGER_FORMAT, uri,
                    // we have at most one receiver - this method can handle all combinations
                    InMemoSharkNetMessage.serializeMessage(
                            contentType,
                            content,
                            this.asapPeer.getPeerID(),
                            selectedRecipients,
                            sign, encrypt,
                            this.sharkPKIComponent.getASAPKeyStore()));
            }
        } catch (ASAPException e) {
            throw new SharkNetMessengerException("when serialising and sending message: " + e.getLocalizedMessage(), e);
        }
    }

    public SharkNetMessengerClosedChannel createClosedChannel(CharSequence uri, CharSequence name)
            throws IOException, SharkNetMessengerException {

        this.checkComponentRunning();
        throw new SharkNotSupportedException("not yet implemented");
    }

    public SharkNetMessengerChannel getChannel(CharSequence uri) throws SharkNetMessengerException, IOException {
        try {
            ASAPStorage asapStorage =
                    this.asapPeer.getASAPStorage(SharkNetMessengerComponent.SHARK_MESSENGER_FORMAT);

            ASAPChannel channel = asapStorage.getChannel(uri);

            return new SharkNetMessengerChannelImpl(this.asapPeer, this.sharkPKIComponent, channel);
        }
        catch(ASAPException asapException) {
            throw new SharkNetMessengerException(asapException);
        }
    }

    public SharkNetMessengerChannel getChannel(int position) throws SharkNetMessengerException, IOException {
        try {
            CharSequence uri = this.getChannelUris().get(position);
            return this.getChannel(uri);
        }
        catch(IndexOutOfBoundsException ioe) {
            throw new SharkNetMessengerException("channel position is out of bound: " +  + position);
        }
    }

    public SharkNetMessengerChannel createChannel(CharSequence uri, CharSequence name)
            throws SharkNetMessengerException, IOException {
        return this.createChannel(uri, name, true);
    }

    public SharkNetMessengerChannel createChannel(CharSequence uri, CharSequence name, boolean mustNotAlreadyExist)
            throws SharkNetMessengerException, IOException {

        this.checkComponentRunning();

        try {
            this.getChannel(uri); // already exists ?
            // yes exists
            if(mustNotAlreadyExist) throw new SharkNetMessengerException("channel already exists");
        }
        catch(SharkNetMessengerException asapException) {
            // does not exist yet - or it is okay
        }

        // create
        try {
            ASAPStorage asapStorage =
                    this.asapPeer.getASAPStorage(SharkNetMessengerComponent.SHARK_MESSENGER_FORMAT);

            asapStorage.createChannel(uri);
            ASAPChannel channel = asapStorage.getChannel(uri);

            return new SharkNetMessengerChannelImpl(this.asapPeer, this.sharkPKIComponent, channel, name);
        }
        catch(ASAPException asapException) {
            throw new SharkNetMessengerException(asapException);
        }
    }


    @Override
    public List<CharSequence> getChannelUris() throws IOException, SharkNetMessengerException {
        try {
            return this.asapPeer.getASAPStorage(SharkNetMessengerComponent.SHARK_MESSENGER_FORMAT).getChannelURIs();
        } catch(ASAPException asapException) {
                throw new SharkNetMessengerException(asapException);
        }
    }

    @Override
    public void removeChannel(CharSequence uri) throws IOException, SharkNetMessengerException {
        try {
            this.asapPeer.getASAPStorage(SharkNetMessengerComponent.SHARK_MESSENGER_FORMAT).removeChannel(uri);
        } catch(ASAPException asapException) {
            throw new SharkNetMessengerException(asapException);
        }
    }

    public int size() throws IOException, SharkNetMessengerException {
        try {
            ASAPStorage asapStorage =
                    this.asapPeer.getASAPStorage(SharkNetMessengerComponent.SHARK_MESSENGER_FORMAT);

            return asapStorage.getChannelURIs().size();
        }
        catch(ASAPException asapException) {
            throw new SharkNetMessengerException(asapException);
        }
    }

    @Override
    public SharkPKIComponent getSharkPKI() {
        return this.sharkPKIComponent;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                     act on received messages                            //
    /////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages,
                                     String senderE2E, // E2E part
                                     List<ASAPHop> asapHops) {
        CharSequence uri = asapMessages.getURI();
        Log.writeLog(this, "MAKE URI LISTENER PUBLIC AGAIN. Thank you :)");

        this.notifySharkMessageReceivedListener(uri);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                       backdoor - remove it when finished implementing                   //
    /////////////////////////////////////////////////////////////////////////////////////////////

    public ASAPStorage getASAPStorage() throws IOException, ASAPException {
        return this.asapPeer.getASAPStorage(SHARK_MESSENGER_FORMAT);
    }
}
