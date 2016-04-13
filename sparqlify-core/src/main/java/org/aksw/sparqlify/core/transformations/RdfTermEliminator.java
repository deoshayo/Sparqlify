package org.aksw.sparqlify.core.transformations;

import org.aksw.sparqlify.algebra.sparql.expr.E_RdfTerm;

import org.apache.jena.sparql.expr.Expr;

public interface RdfTermEliminator {
	E_RdfTerm _transform(Expr expr);
}
