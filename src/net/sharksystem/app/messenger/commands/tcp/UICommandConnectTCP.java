package net.sharksystem.app.messenger.commands.tcp;

import net.sharksystem.app.messenger.commands.commandarguments.*;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;

import java.io.IOException;
import java.util.List;

/**
 * This command conects to a peer over TCP/IP.
 */
public class UICommandConnectTCP extends UICommand {
    private final UICommandIntegerArgument portNumber;
    private final UICommandStringArgument hostName;

    private final UICommandIntegerArgument numberAttemptsParameter;
    private static final int DEFAULT_NUMBER_ATTEMPTS = 1;
    private int numberAttempts = DEFAULT_NUMBER_ATTEMPTS;

    private final UICommandIntegerArgument waitBetweenAttemptsInMSParameter;
    private static final int DEFAULT_WAIT_BETWEEN_ATTEMPTS = 5000;
    private int waitBetweenAttemptsInMS = DEFAULT_WAIT_BETWEEN_ATTEMPTS;

    private UICommandBooleanArgument blockedCommandParameter;
    private static final boolean DEFAULT_BLOCKED_COMMAND = true;
    private boolean blockedCommand = DEFAULT_BLOCKED_COMMAND;

    public UICommandConnectTCP(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI, String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);

        this.portNumber = new UICommandIntegerArgument(sharkMessengerApp);
        this.hostName = new UICommandStringArgument(sharkMessengerApp);
        this.numberAttemptsParameter = new UICommandIntegerArgument(sharkMessengerApp);
        this.waitBetweenAttemptsInMSParameter = new UICommandIntegerArgument(sharkMessengerApp);
        this.blockedCommandParameter = new UICommandBooleanArgument(sharkMessengerApp);
    }

    /**
     * @param arguments in following order:
     * <ol>
     *  <li>port - int</li>
     *  <li>host - String</li>
     * </ol>
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.size() < 2) {
            System.err.println("host(ip/name) and port number required");
            return false;
        }

        boolean isParsable = this.hostName.tryParse(arguments.get(0))
                && this.portNumber.tryParse(arguments.get(1));

        if(!isParsable) {
            System.err.println("failed to parse hostname and port number: "
                    + arguments.get(0) + " | " + arguments.get(1));
        }

        // optional
        if (arguments.size() > 2) {
            if(this.numberAttemptsParameter.tryParse(arguments.get(2))) {
                this.numberAttempts = this.numberAttemptsParameter.getValue();
                if(this.numberAttempts < 1) {
                    this.getSharkMessengerApp().tellUI("number of attempts < 1 makes no sense - take default instead:"
                    + DEFAULT_NUMBER_ATTEMPTS);
                    this.numberAttempts = DEFAULT_NUMBER_ATTEMPTS;
                }
            }
        }

        if (arguments.size() > 3) {
            if(this.waitBetweenAttemptsInMSParameter.tryParse(arguments.get(3))) {
                this.waitBetweenAttemptsInMS = this.waitBetweenAttemptsInMSParameter.getValue();
                if(this.waitBetweenAttemptsInMS < 1) {
                    this.getSharkMessengerApp().tellUI("wait in ms < 1 makes no sense - take default instead:"
                            + DEFAULT_NUMBER_ATTEMPTS);
                    this.waitBetweenAttemptsInMS = DEFAULT_WAIT_BETWEEN_ATTEMPTS;
                }
            }
        }

        if (arguments.size() > 4) {
            if(this.blockedCommandParameter.tryParse(arguments.get(4))) {
                this.blockedCommand = this.blockedCommandParameter.getValue();
            }
        }

        return isParsable;
    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'specifyCommandStructure'");
    }

    private void connectAgainLoop() {
        boolean success = false;
        while(UICommandConnectTCP.this.numberAttempts > 0) {
            try {
                Thread.sleep(UICommandConnectTCP.this.waitBetweenAttemptsInMS);
                UICommandConnectTCP.this.getSharkMessengerApp().tellUI(
                        "try connecting again (" + UICommandConnectTCP.this.numberAttempts + ")");
                UICommandConnectTCP.this.doExecute();
                success = true;
                return;
            } catch (IOException | InterruptedException ex) {
                // ignore
            }
        }
        if(!success) UICommandConnectTCP.this.getSharkMessengerApp().tellUI(
                "give up connecting attempt to " + UICommandConnectTCP.this.hostName.getValue()
                        + ":" + UICommandConnectTCP.this.portNumber.getValue());
    }

    @Override
    protected void execute() throws Exception {
        // first attempt - same thread
        try {
            this.doExecute();
        }
        catch(IOException e) {
            // try again?
            if (this.numberAttempts > 0) {
                // try again in background ?
                if (this.blockedCommand) {
                    this.connectAgainLoop();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            UICommandConnectTCP.this.connectAgainLoop();
                        }
                    }).start();
                }
            }
        }
    }

    private void doExecute() throws IOException {
        this.numberAttempts--;
        try {
            this.getSharkMessengerApp().connectTCP(this.hostName.getValue(), this.portNumber.getValue());
            this.getSharkMessengerApp().tellUI("connected to "
                    + this.hostName.getValue() + ":" + this.portNumber.getValue());
        } catch (IOException e) {
            this.getSharkMessengerApp().tellUIError(
                    "failed to not connect to " + this.hostName.getValue() + ":" + this.portNumber.getValue());
            this.printErrorMessage(e.getLocalizedMessage());
            throw e;
        }
    }

    @Override
    public String getDescription() {
        // append hint for how to use
        return "connectTCP <host/IP-address> <port> <number of attempts = 5> <wait in ms = 5000> <blocked = true>";
    }
}
