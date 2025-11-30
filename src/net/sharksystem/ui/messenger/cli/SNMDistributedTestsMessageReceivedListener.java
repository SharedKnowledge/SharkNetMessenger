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
    }

}
