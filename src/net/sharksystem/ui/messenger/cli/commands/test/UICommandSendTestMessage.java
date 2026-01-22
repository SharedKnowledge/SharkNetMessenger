//package net.sharksystem.ui.messenger.cli.commands.test;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import net.sharksystem.SharkException;
//import net.sharksystem.ui.messenger.cli.commands.messenger.UICommandSendMessage;
//import net.sharksystem.app.messenger.SharkNetMessengerComponent;
//import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
//import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
//import net.sharksystem.ui.messenger.cli.UICommand;
//import net.sharksystem.ui.messenger.cli.commandarguments.*;
//
///**
// * This command is for batch test purposes. It adds a test run specific ID to
// * each message and has two additional parameters:
// * <ul>
// *  <li>repetitions - defines how often the message will be sent</li>
// *  <li>delayInMillis - defines how much time will pass until a message is sent</li>
// * </ul>
// */
//public class UICommandSendTestMessage extends UICommand {
//    private final UICommandIntegerArgument repetitions;
//    private final UICommandLongArgument delayInMillis;
//    private final UICommandIntegerArgument channelIndex;
//    private final UICommandBooleanArgument sign;
//    private final UICommandBooleanArgument encrypt;
//    private final UICommandStringArgument message;
//    private final UICommandStringArgument receivers;
//    private final String peerName;
//
//    public UICommandSendTestMessage(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
//                                    String identifier, boolean rememberCommand) throws SharkException {
//        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
//
//        // additional parameters for tests
//        this.repetitions = new UICommandIntegerArgument(sharkMessengerApp);
//        this.delayInMillis = new UICommandLongArgument(sharkMessengerApp);
//
//        // standard send message command parameters
//        this.channelIndex = new UICommandIntegerArgument(sharkMessengerApp);
//        this.sign = new UICommandBooleanArgument(sharkMessengerApp);
//        this.encrypt = new UICommandBooleanArgument(sharkMessengerApp);
//        this.message = new UICommandStringArgument(sharkMessengerApp);
//        this.receivers = new UICommandStringArgument(sharkMessengerApp);
//
//        // allow broadcast msg
//        this.receivers.setEmptyStringAllowed(true);
//
//        // used for getting the right instance of the SentMessageCounter
//        this.peerName = this.getSharkMessengerApp().getSharkPeer().getPeerID().toString();
//    }
//
//    /**
//     * Prepares the command by parsing the arguments.
//     * @param arguments in following order:
//     * <ol>
//     *  <li>repetitions - int</li>
//     *  <li>delayInMillis - int</li>
//     *  <li>channelIndex - int</li>
//     *  <li>sign - boolean</li>
//     *  <li>encrypt - boolean</li>
//     *  <li>message - String</li>
//     *  <li>receivers - String [comma seperated]</li>
//     * </ol>
//     */
//    @Override
//    protected boolean handleArguments(List<String> arguments) {
//        if (arguments.size() < 7) {
//            return false;
//        }
//
//        boolean isParsable = this.repetitions.tryParse(arguments.get(0))
//                && this.delayInMillis.tryParse(arguments.get(1))
//                && this.channelIndex.tryParse(arguments.get(2))
//                && this.sign.tryParse(arguments.get(3))
//                && this.encrypt.tryParse(arguments.get(4))
//                && this.message.tryParse(arguments.get(5))
//                && this.receivers.tryParse(arguments.get(6));
//
//        if (this.repetitions.getValue() < 0) {
//            this.repetitions.setValue(0);
//        }
//
//        if (this.delayInMillis.getValue() < 0) {
//            this.delayInMillis.setValue(0L);
//        }
//
//        return isParsable;
//    }
//
//    @Override
//    protected void execute() throws Exception {
//        UICommand sendCommand = new UICommandSendMessage(getSharkMessengerApp(),
//                getSharkMessengerUI(), getIdentifier(), false);
//
//        // messageCounter provides message IDs
//        SentMessageCounter messageCounter = SentMessageCounter.getInstance(this.peerName);
//
//        // prepare argument list for sendMessage command
//        List<String> arguments = new ArrayList<>();
//        arguments.add(this.channelIndex.getValue().toString());
//        arguments.add(this.sign.getValue().toString());
//        arguments.add(this.encrypt.getValue().toString());
//        arguments.add("");
//        arguments.add(this.receivers.getValue());
//
//        // information for the messageCounter
//        SharkNetMessengerComponent messenger = getSharkMessengerApp().getSharkMessengerComponent();
//        String channelUri = messenger.getChannel(this.channelIndex.getValue()).getURI().toString();
//        String content = this.message.getValue();
//        String receivers = this.receivers.getValue();
//
//        for (int i = 0; i < this.repetitions.getValue(); i++) {
//            Thread.sleep(this.delayInMillis.getValue());
//
//            // add ID to the message and put it into the argument list for the sendMessage command
//            int msgID = messageCounter.nextMessage(receivers, channelUri, content);
//            String msg = msgID
//                    + System.lineSeparator()
//                    + this.message.getValue();
//            arguments.set(3, msg);
//
//            sendCommand.execute(arguments);
//        }
//    }
//
//    @Override
//    protected UICommandQuestionnaire specifyCommandStructure() {
//        // TODO Auto-generated method stub
//        throw new UnsupportedOperationException("Unimplemented method 'specifyCommandStructure'");
//    }
//
//    @Override
//    public String getDescription() {
//        return "Send a specified amount of test messages with a specified delay.";
//    }
//
//}
