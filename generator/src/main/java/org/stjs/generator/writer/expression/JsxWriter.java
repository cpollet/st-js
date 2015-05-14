package org.stjs.generator.writer.expression;

import com.sun.source.tree.MethodTree;
import org.stjs.generator.GenerationContext;
import org.stjs.generator.javac.InternalUtils;
import org.stjs.generator.writer.WriterContributor;
import org.stjs.generator.writer.WriterVisitor;

import javax.lang.model.element.Element;

/**
 * @author Christophe Pollet
 */
public class JsxWriter<JS> implements WriterContributor<MethodTree, JS> {
	@Override
	public JS visit(WriterVisitor<JS> visitor, MethodTree tree, GenerationContext<JS> context) {
		Element element = InternalUtils.symbol(tree);
		if (element != null) {
			String comment = context.getElements().getDocComment(element);
			if (comment != null) {
				return context.js().newJsxExpression(comment);
			}
		}

		return context.js().newJsxExpression("<div>[empty]</div>");
	}
}
