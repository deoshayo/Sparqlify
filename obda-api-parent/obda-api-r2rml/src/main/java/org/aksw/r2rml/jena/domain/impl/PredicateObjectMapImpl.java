/**
 * 
 */
package org.aksw.r2rml.jena.domain.impl;

import java.util.Set;

import org.aksw.r2rml.jena.domain.api.GraphMap;
import org.aksw.r2rml.jena.domain.api.ObjectMap;
import org.aksw.r2rml.jena.domain.api.PredicateMap;
import org.aksw.r2rml.jena.domain.api.PredicateObjectMap;
import org.aksw.r2rml.jena.vocab.RR;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;

/**
 * @author sherif
 * 
 */
public class PredicateObjectMapImpl
	extends AbstractMappingComponent
	implements PredicateObjectMap
{
	public PredicateObjectMapImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}

	@Override
	public Set<Resource> getPredicates() {
		Set<Resource> result = new SetFromResourceAndProperty<>(this, RR.predicate, Resource.class);
		return result;
	}

	@Override
	public Set<PredicateMap> getPredicateMaps() {
		Set<PredicateMap> result = new SetFromResourceAndProperty<>(this, RR.predicateMap, PredicateMap.class);
		return result;
	}

	@Override
	public Set<ObjectMap> getObjectMaps() {
		Set<ObjectMap> result = new SetFromResourceAndProperty<>(this, RR.objectMap, ObjectMap.class);
		return result;
	}
	
	@Override
	public Set<GraphMap> getGraphMaps() {
		Set<GraphMap> result = new SetFromResourceAndProperty<>(this, RR.graphMap, GraphMap.class);
		return result;
	}


/*
	@Override
	public Resource getPredicate() {
		Resource result = getPropertyResourceValue(RR.predicate);
		return result;
	}

	@Override
	public PredicateObjectMap setPredicate(Resource predicate) {
		setProperty(this, RR.predicate, predicate);
		return this;
	}

	@Override
	public TermMap getPredicateMap() {
		TermMap result = getObjectAs(this, RR.predicateMap, TermMap.class).orElse(null);
		return result;
	}

	@Override
	public PredicateObjectMap setPredicateMap(TermMap termMap) {
		setProperty(this, RR.predicateMap, termMap);
		return this;
	}

	@Override
	public TermMap getObjectMap() {
		TermMap result = getObjectAs(this, RR.objectMap, TermMap.class).orElse(null);
		return result;
	}

	@Override
	public PredicateObjectMap setObjectMap(TermMap termMap) {
		setProperty(this, RR.objectMap, termMap);
		return this;
	}
*/

}