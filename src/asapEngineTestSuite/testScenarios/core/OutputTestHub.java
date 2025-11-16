package asapEngineTestSuite.testScenarios.core;

import java.util.Arrays;

public class OutputTestHub {

	public static void main(String[] args) {
		CoreScenariosHub hub = new CoreScenariosHub(7000);
		CoreScenariosTCPChain chain = new CoreScenariosTCPChain();

		var a1 = hub.hubDisA1Commands();
		var a2 = hub.hubDisA2Commands();

		var b1 = hub.hubDisB1Commands();
		var b2 = hub.hubDisB2Commands();


		var c1 = chain.coreADisCommandLists(1);
		System.out.println(c1[0]);
		System.out.println("========");
		System.out.println(c1[1]);
		System.out.println("========");

		System.out.println(Arrays.stream(a1).reduce("", (a,b) -> a + System.lineSeparator() + "========" + System.lineSeparator() + b));
//		System.out.println(Arrays.stream(a2).reduce("", (a,b) -> a + System.lineSeparator() + "========" + System.lineSeparator() + b));
//		System.out.println(Arrays.stream(b1).reduce("", (a,b) -> a + System.lineSeparator() + "========" + System.lineSeparator() + b));
//		System.out.println(Arrays.stream(b2).reduce("", (a,b) -> a + System.lineSeparator() + "========" + System.lineSeparator() + b));

	}
}
