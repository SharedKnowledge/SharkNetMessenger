package net.sharksystem.ui.messenger.cli.commands.testing;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Run a script with in a fresh CLI thread.
 */
public class ScriptsRunner {
    // assume scripts come as member
    private String getScript(String scriptName) throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = this.getClass().getDeclaredField(scriptName);
        return (String) declaredField.get(this);
    }

    // testname and names of involved parties
    private static Map<String, String[]> version1Tests = new HashMap();
    {
        version1Tests.put("CS", new String[]{"A","B"});
    }

    // Basics - always includes two parties
//    CS, CS1k, CS100k, CS100M, SC, SC1k, SC100k, SC100M
    String CS_A = "sendMessage HiItsAlice; exit;";
    String CS_B = "sendMessage HiItsBob; exit;";

    // Complex
    // Chain4_1k, ChainLT4_1k, Star4_1k
    // Hub
    // Hub3_1k, Hub3_100k, Hub3_100M, HubStalling_1s, HubStalling_1min, HubStalling_10min

    public void runScripts() {
        String scriptName = null;
        for(String testCase : version1Tests.keySet()) {
            String[] parties = version1Tests.get(testCase);
            if(parties == null || parties.length == 0) {
                System.err.println("ERROR testcase has no scripts: " + testCase);
            }
            try {
                List<ScriptRunnerThread> scriptRunner = new ArrayList<>();
                for(String party : parties) {
                    scriptName = testCase + "_" + party;
                    scriptRunner.add(new ScriptRunnerThread(party, testCase, this.getScript(scriptName)));
                }
                // run scripts
                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                System.out.println("Launch test " + testCase);
                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                for(Thread testRunner : scriptRunner) {
                    testRunner.start();
                }
                for(Thread testRunner : scriptRunner) {
                    try {
                        testRunner.join();
                    } catch (InterruptedException e) {
                        // won't happen
                    }
                }

                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                System.out.println("Test ended " + testCase);
                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                try {
                    Thread.sleep(100); // give a moment to settle - necessary? Threads already done.
                } catch (InterruptedException e) {
                    // won't happen
                }

            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
                System.err.println("ERROR missing script: " + scriptName + "cannot execute test " + testCase);
                continue;
            }
        }
    }
}
