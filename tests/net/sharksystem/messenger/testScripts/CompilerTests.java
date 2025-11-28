package net.sharksystem.messenger.testScripts;

import net.sharksystem.ui.messenger.cli.testtools.TestLanguageCompiler;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class CompilerTests {

    @Test
    public void sentenceTest1() {
        TestLanguageCompiler tlc = new TestLanguageCompiler();
        Map<String, String> scripts = tlc.compile("Cab");

        for(String peerName : scripts.keySet()) {
            System.out.println("script for " + peerName);
            System.out.println(scripts.get(peerName));
        }
    }
}
