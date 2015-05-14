package org.stjs.generator.writer.jsx;

import org.junit.Test;

import static org.stjs.generator.utils.GeneratorTestHelper.generate;

/**
 * @author Christophe Pollet
 */
public class JSXTest {
	@Test
	public void test() {
		System.out.println(generate(JSX1.class));
	}
}
