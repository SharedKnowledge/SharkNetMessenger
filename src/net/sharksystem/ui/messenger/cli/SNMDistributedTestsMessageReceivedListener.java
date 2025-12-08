package net.sharksystem.ui.messenger.cli;

import net.sharksystem.app.messenger.SharkNetMessagesReceivedListener;

public class SNMDistributedTestsMessageReceivedListener implements SharkNetMessagesReceivedListener {
    private final SharkNetMessengerAppSupportingDistributedTesting sharkMessengerAppTestingVersion;

    public SNMDistributedTestsMessageReceivedListener(
            SharkNetMessengerAppSupportingDistributedTesting sharkMessengerApp) {
        this.sharkMessengerAppTestingVersion = sharkMessengerApp;
    }

    @Override
    public void sharkMessagesReceived(CharSequence uri) {
        // if release label received
        if(uri.toString().equalsIgnoreCase(
                SharkNetMessengerAppSupportingDistributedTesting.TEST_BLOCK_RELEASE_CHANNEL.toString())) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SNMDistributedTestsMessageReceivedListener.this.sharkMessengerAppTestingVersion.
                            releaseReceived(SharkNetMessengerAppSupportingDistributedTesting.TEST_BLOCK_RELEASE_CHANNEL);
                }
            }).start();
        }
        else if(uri.toString().equalsIgnoreCase(
                SharkNetMessengerAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL.toString())) {
            // no need for a thread - is already created in asap core
            SNMDistributedTestsMessageReceivedListener.this.sharkMessengerAppTestingVersion.
                    receivedScriptRQ(SharkNetMessengerAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL);
        }
        else if(uri.toString().equalsIgnoreCase(
                SharkNetMessengerAppSupportingDistributedTesting.TEST_SCRIPT_CHANNEL.toString())) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SNMDistributedTestsMessageReceivedListener.this.sharkMessengerAppTestingVersion.
                            receivedTestScript(SharkNetMessengerAppSupportingDistributedTesting.TEST_SCRIPT_CHANNEL);
                }
            }).start();
        }
    }

}
