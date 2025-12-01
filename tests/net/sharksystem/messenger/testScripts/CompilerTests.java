package net.sharksystem.messenger.testScripts;

import net.sharksystem.ui.messenger.cli.testlanguage.TestLanguageCompiler;
import net.sharksystem.ui.messenger.cli.testlanguage.TestLanguageCompilerException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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

    @Test
    public void x1() throws TestLanguageCompilerException {
        List<String> strings = new ArrayList<>();
        strings.add("hi");
        strings.remove(0);

        int i = 42;
    }
}
