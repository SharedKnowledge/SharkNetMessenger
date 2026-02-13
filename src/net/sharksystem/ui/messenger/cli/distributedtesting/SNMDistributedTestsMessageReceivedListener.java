package net.sharksystem.ui.messenger.cli.distributedtesting;

import net.sharksystem.app.messenger.SharkNetMessagesReceivedListener;

public class SNMDistributedTestsMessageReceivedListener implements SharkNetMessagesReceivedListener {
    private final SNMAppSupportingDistributedTesting sharkMessengerAppTestingVersion;

    public SNMDistributedTestsMessageReceivedListener(
            SNMAppSupportingDistributedTesting sharkMessengerApp) {
        this.sharkMessengerAppTestingVersion = sharkMessengerApp;
    }

    @Override
    public void sharkMessagesReceived(CharSequence uri) {
        // if release label received
        if(uri.toString().equalsIgnoreCase(
                SNMAppSupportingDistributedTesting.TEST_BLOCK_RELEASE_CHANNEL.toString())) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SNMDistributedTestsMessageReceivedListener.this.sharkMessengerAppTestingVersion.
                            releaseReceived(SNMAppSupportingDistributedTesting.TEST_BLOCK_RELEASE_CHANNEL);
                }
            }).start();
        }
        else if(uri.toString().equalsIgnoreCase(
                SNMAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL.toString())) {
            // no need for a thread - is already created in asap core
            SNMDistributedTestsMessageReceivedListener.this.sharkMessengerAppTestingVersion.
                    receivedScriptRQ(SNMAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL);
        }
        else if(uri.toString().equalsIgnoreCase(
                SNMAppSupportingDistributedTesting.TEST_SCRIPT_CHANNEL.toString())) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SNMDistributedTestsMessageReceivedListener.this.sharkMessengerAppTestingVersion.
                            receivedTestScript(SNMAppSupportingDistributedTesting.TEST_SCRIPT_CHANNEL);
                }
            }).start();
        }
    }

}
