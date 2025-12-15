package net.sharksystem.app.messenger.commands.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.commands.CommandNames;
import net.sharksystem.utils.json.SNM_JSON_Builder;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleInteger;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

import java.io.IOException;

/**
 * This command lists all known messages of a channel.
 */
public class UICommandListMessages extends AbstractCommandWithSingleInteger {
    public static final int DEFAULT_CHANNEL_INDEX = 1;

    public UICommandListMessages(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                 String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand, true, DEFAULT_CHANNEL_INDEX);
    }

    @Override
    public void execute() throws Exception {
        this.executeReturnJSON(true, false);
        /*
        String debug = this.executeReturnJSON(true, true);// don't return anything
        int i = 42;
         */
    }

    //@Override
    public String executeReturnJSON() throws Exception {
        return this.executeReturnJSON(false, true);// don't return anything
    }

    public String executeReturnJSON(boolean produceUIOutput, boolean produceJSONOutput) throws Exception {
        SNM_JSON_Builder snmJsonBuilder = new SNM_JSON_Builder(CommandNames.CLI_LIST_MESSAGES);

        try {
            int userChannelIndex = this.getIntegerArgument(); // we start with 1 in UI and 0 inside
            int channelIndex = this.getIntegerArgument() - 1; // we start with 1 in UI and 0 inside
            SharkNetMessengerComponent messenger = this.getSharkMessengerApp().getSharkMessengerComponent();
            SharkNetMessengerChannel channel = null;
            try {
                channel = messenger.getChannel(channelIndex);
            }
            catch (SharkException se) {
                String errorMessage = "there is no channel index " + userChannelIndex;
                if(produceUIOutput) this.getSharkMessengerApp().tellUI(errorMessage);
                snmJsonBuilder.error(errorMessage);
                return snmJsonBuilder.getJsonString();
            }
            SharkNetMessageList messages = channel.getMessages();
            if(messages == null || messages.size() <1) {
                String errorMessage = "no messages in channel " + userChannelIndex;
                if(produceUIOutput) this.getSharkMessengerApp().tellUI(errorMessage);
                snmJsonBuilder.error(errorMessage);
                return snmJsonBuilder.getJsonString();
            }

            ChannelPrinter channelPrinter = new ChannelPrinter();
            snmJsonBuilder.addChannel(channel);
            if(produceUIOutput) this.getSharkMessengerApp().tellUI(ChannelPrinter.getChannelDescription(channel));

            // json
            if(produceJSONOutput) {
                snmJsonBuilder.addMessages(
                        this.getSharkMessengerApp(),
                        this.getSharkMessengerApp().getSharkPKIComponent(),
                        messages);
            }

            // CLI output
            if(produceUIOutput) this.getSharkMessengerApp().tellUI(
                    channelPrinter.getMessagesASString(this.getSharkMessengerApp(),
                            this.getSharkMessengerApp().getSharkPKIComponent(),
                            channel.getURI().toString(), messages));
        } catch (SharkException | IOException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }

        if(!produceJSONOutput) return null;

        return snmJsonBuilder.getJsonString();
    }

    @Override
    public String getDescription() {
        return "List messages in channel (index default: 1)";
    }
}
