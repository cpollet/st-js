package org.stjs.generator.writer.jsx;

import org.junit.Test;

import static org.stjs.generator.utils.GeneratorTestHelper.assertCodeContains;

/**
 * @author Christophe Pollet
 */
public class JSXTest {
	@Test
	public void testBasicJsx() {
		assertCodeContains(JSX1.class, "prototype.someMethod = function() { return <div>OK</div>; }; ");
	}

	@Test
	public void testJsxIsGeneratedOnlyOnJsxObjects() {
		assertCodeContains(JSX2.class, "/** * Javadoc, no JSX */ prototype.someMethod = function() { }; ");
	}

	@Test
	public void testJsxIsGeneratedOnlyOnJsxObjects2() {
		assertCodeContains(JSX3.class, "prototype.someMethod = function() { var jsx =  <div>JSX</div>;" +
				"new (stjs.extend(function JSX3$2() {}, Object, [], function(constructor, prototype) {" +
				"/** * Javadoc */\n" +
				"prototype.somethingUseful = function() {};" +
				"}, {}, {}))(); return jsx;};");
	}
}
