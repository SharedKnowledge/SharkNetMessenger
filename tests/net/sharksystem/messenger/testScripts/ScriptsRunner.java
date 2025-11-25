package net.sharksystem.messenger.testScripts;

import net.sharksystem.SharkException;
import net.sharksystem.ui.messenger.cli.ProductionUI;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptsRunner {
    private static String getFriendlyPeerName(String peerName) {
        if(peerName.length() != 1) return peerName; // already friendly - hopefully

        switch (peerName) {
            case "A": return "Alice";
            case "B": return "Bob";
            case "C": return "Clara";
            case "D": return "David";
            case "E": return "Eve";
            default: return peerName;
        }
    }

    private class ScriptRunnerThread extends Thread {
        String peerName;
        String testName;
        String script;

        ScriptRunnerThread(String peerName, String testName, String script) {
            this.peerName = getFriendlyPeerName(peerName);
            this.testName = testName;
            this.script = script;
        }

        public void run() {
            ByteArrayOutputStream commandStream = new ByteArrayOutputStream();
            try {
                commandStream.write(script.getBytes());
                ByteArrayInputStream commandInStream = new ByteArrayInputStream(commandStream.toByteArray());
                System.setIn(commandInStream);

                System.out.println("start CLI with " + peerName + "_" + testName);
                ProductionUI.main(new String[]{peerName + "_" + testName});
                System.out.println("done with CLI with " + peerName + "_" + testName);
            } catch (IOException | SharkException e) {
                System.err.println("failed: " + testName);
                e.printStackTrace();
            }
        }
    }

    @Test
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
                    Field declaredField = this.getClass().getDeclaredField(scriptName);
                    //declaredField.setAccessible(true);
                    String script = (String) declaredField.get(this);
                    scriptRunner.add(new ScriptRunnerThread(party, testCase, script));
                }
                // run scripts
                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                System.out.print("Launch test " + testCase);
                for(Thread testRunner : scriptRunner) {
                    testRunner.start();
                }
                System.out.println(": " + scriptRunner.size() + "threads started");
                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
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

    // testname - names of involved parties
    private static Map<String, String[]> version1Tests = new HashMap();
    {
        version1Tests.put("CS", new String[]{"A","B"});
    }


    // Basics - always includes two parties
//    CS, CS1k, CS100k, CS100M, SC, SC1k, SC100k, SC100M
    static String CS_A = "sendMessage HiItsAlice; exit;";
    static String CS_B = "sendMessage HiItsBob; exit;";

    // Complex
    // Chain4_1k, ChainLT4_1k, Star4_1k
    // Hub
    // Hub3_1k, Hub3_100k, Hub3_100M, HubStalling_1s, HubStalling_1min, HubStalling_10min
}
