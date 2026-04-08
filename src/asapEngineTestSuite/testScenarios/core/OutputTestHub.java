package asapEngineTestSuite.testScenarios.core;

import asapEngineTestSuite.testScenarios.core.hub.CoreScenariosHub;

import java.util.Arrays;

@Deprecated
public class OutputTestHub {

	public static void main(String[] args) {
		CoreScenariosHub hub = new CoreScenariosHub(7000);
		CoreScenariosTCPChain chain = new CoreScenariosTCPChain();

		var a1 = hub.hubTStalling_Length(1000);
		var a2 = hub.hubTX_Length(3, 1000);


		System.out.println(Arrays.stream(a1).reduce("", (a,b) -> a + System.lineSeparator() + "========" + System.lineSeparator() + b));
		System.out.println(Arrays.stream(a2).reduce("", (a,b) -> a + System.lineSeparator() + "========" + System.lineSeparator() + b));
//		System.out.println(Arrays.stream(b1).reduce("", (a,b) -> a + System.lineSeparator() + "========" + System.lineSeparator() + b));
//		System.out.println(Arrays.stream(b2).reduce("", (a,b) -> a + System.lineSeparator() + "========" + System.lineSeparator() + b));

	}
}
