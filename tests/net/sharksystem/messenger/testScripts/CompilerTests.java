package net.sharksystem.messenger.testScripts;

import net.sharksystem.ui.messenger.cli.testlanguage.TestLanguageCompiler;
import net.sharksystem.ui.messenger.cli.testlanguage.TestLanguageCompilerException;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class CompilerTests {

    @Test
    public void sentenceTest1() throws TestLanguageCompilerException {
        TestLanguageCompiler tlc = new TestLanguageCompiler();
        Map<String, String> scripts = tlc.compile("Cab;Cac");

        for(String peerName : scripts.keySet()) {
            System.out.println("script for " + peerName + ": ");
            System.out.println(scripts.get(peerName) + "\n");
        }
    }
}
