package org.stjs.generator.writer.jsx;

import org.stjs.generator.JSX;

/**
 * @author Christophe Pollet
 */
public class JSX1 {
	public JSX someMethod() {
		return new JSX() {
			/**
			 * <div>
			 *     <strong>
			 *         Ca marche :)
			 *     </strong>
			 * </div>
			 */
			@Override
			public void output() {
			}
		};
	}

	public String getName() {
		return "name";
	}
}
