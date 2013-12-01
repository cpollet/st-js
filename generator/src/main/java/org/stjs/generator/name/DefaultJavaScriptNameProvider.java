package org.stjs.generator.name;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.stjs.generator.GenerationContext;
import org.stjs.generator.utils.JavaNodes;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;

public class DefaultJavaScriptNameProvider implements JavaScriptNameProvider {

	@Override
	public String getTypeName(GenerationContext context, TypeMirror type) {
		// TODO fix anonymous and inner types names
		// String name = clazz.getSimpleName();
		// if (name.isEmpty()) {
		// return GeneratorConstants.SPECIAL_INLINE_TYPE;
		// }
		// for (Class<?> c = clazz.getDeclaringClass(); c != null && !c.isAnonymousClass(); c = c.getDeclaringClass()) {
		// name = c.getSimpleName() + "." + name;
		// }
		//
		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			String name = declaredType.asElement().getSimpleName().toString();
			Element rootTypeElement = declaredType.asElement();
			for (DeclaredType enclosingType = JavaNodes.getEnclosingType(declaredType); enclosingType != null; enclosingType = JavaNodes
					.getEnclosingType(enclosingType)) {
				rootTypeElement = enclosingType.asElement();
				name = rootTypeElement.getSimpleName() + "." + name;
			}

			String namespace = JavaNodes.getNamespace(rootTypeElement);
			return (namespace == null ? "" : namespace + ".") + name;
		}
		throw new IllegalArgumentException("Don't know what how to get the name of this type:" + type);
	}

	@Override
	public String getVariableName(GenerationContext context, IdentifierTree treeNode, TreePath path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMethodName(GenerationContext context, MethodTree tree, TreePath path) {
		return tree.getName().toString();
	}

	@Override
	public String getMethodName(GenerationContext context, MethodInvocationTree tree, TreePath path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTypeName(GenerationContext context, Element type) {
		return getTypeName(context, type.asType());
	}

}