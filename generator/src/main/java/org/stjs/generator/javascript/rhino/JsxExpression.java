package org.stjs.generator.javascript.rhino;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

/**
 * @author Christophe Pollet
 */
public class JsxExpression extends AstNode {
	private final String jsx;

	public JsxExpression(String jsx) {
		super();
		this.jsx = jsx;
	}

	@Override
	public String toSource(int i) {
		return "[JSX]";
	}

	@Override
	public void visit(NodeVisitor nodeVisitor) {
		// do nothing
	}

	public String getJsx() {
		return jsx;
	}
}
