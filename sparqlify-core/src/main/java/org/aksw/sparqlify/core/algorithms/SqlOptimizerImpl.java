package org.aksw.sparqlify.core.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.commons.factory.Factory1;
import org.aksw.commons.util.jdbc.ColumnsReference;
import org.aksw.commons.util.jdbc.Index;
import org.aksw.sparqlify.algebra.sql.exprs2.S_ColumnRef;
import org.aksw.sparqlify.algebra.sql.exprs2.S_Equals;
import org.aksw.sparqlify.algebra.sql.exprs2.SqlExpr;
import org.aksw.sparqlify.algebra.sql.nodes.Projection;
import org.aksw.sparqlify.algebra.sql.nodes.SqlOp;
import org.aksw.sparqlify.algebra.sql.nodes.SqlOpJoin;
import org.aksw.sparqlify.algebra.sql.nodes.SqlOpJoinN;
import org.aksw.sparqlify.algebra.sql.nodes.SqlOpSelectBlock;
import org.aksw.sparqlify.algebra.sql.nodes.SqlOpTable;
import org.aksw.sparqlify.algebra.sql.nodes.SqlSortCondition;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.SortCondition;
import com.hp.hpl.jena.sdb.core.JoinType;


class SelfJoinResult {
	private List<SqlOp> ops;
	//private List<Collection<SqlExpr>> cnf;
	private Factory1<SqlExpr> aliasSubstitutor;
	
	public SelfJoinResult(List<SqlOp> ops, Factory1<SqlExpr> aliasSubstitutor) {
		super();
		this.ops = ops;
		this.aliasSubstitutor = aliasSubstitutor;
	}

	public List<SqlOp> getOps() {
		return ops;
	}

	public Factory1<SqlExpr> getAliasSubstitutor() {
		return aliasSubstitutor;
	}
}


/**
 * Substitutes aliases
 * 
 * Note: Optimization of the resulting expressions is a separate step
 * 
 * @author raven
 *
 */
class AliasSubstitutor
	implements Factory1<SqlExpr>
{
	private Map<String, String> aliasMap;
	
	public AliasSubstitutor(Map<String, String> aliasMap) {
		this.aliasMap = aliasMap;
	}
	
	@Override
	public SqlExpr create(SqlExpr a) {
		
		SqlExpr result;

		switch(a.getType()) {
		case Variable:
			S_ColumnRef cr = (S_ColumnRef)a;
			String alias = cr.getRelationAlias();
			
			// Check for existence of the alias in order to treat null values properly
			if(aliasMap.containsKey(alias)) {
				String newAlias = aliasMap.get(alias);
				
				result = new S_ColumnRef(cr.getDatatype(), cr.getColumnName(), newAlias);
			} else {
				result = a;
			}
			break;
		default:
			result = a;
		}
		
		return result;
	}
	
};

class EdgeJoin
	extends DefaultEdge
{
	private S_ColumnRef a;
	private S_ColumnRef b;
	
	public EdgeJoin() {
		
	}
	
	public EdgeJoin(S_ColumnRef a, S_ColumnRef b) {
		super();
		this.a = a;
		this.b = b;
	}
	
	public S_ColumnRef getA() {
		return a;
	}
	
	public void setA(S_ColumnRef a) {
		this.a = a;
	}
	
	public S_ColumnRef getB() {
		return b;
	}
	
	public void setB(S_ColumnRef b) {
		this.b = b;
	}
}

class EdgeSelfJoin
	extends DefaultEdge
{
	private String aliasA;
	private String aliasB;
	private Set<String> columnNames = new HashSet<String>(); // The set of columns on which a table joins with itself
	
	public EdgeSelfJoin() {
	}
	
	
	public EdgeSelfJoin(String aliasA, String aliasB) {
		this.aliasA = aliasA;
		this.aliasB = aliasB;
	}
	
	public String getAliasA() {
		return aliasA;
	}

	public void setAliasA(String aliasA) {
		this.aliasA = aliasA;
	}

	public String getAliasB() {
		return aliasB;
	}

	public void setAliasB(String aliasB) {
		this.aliasB = aliasB;
	}

	public Set<String> getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(Set<String> columnNames) {
		this.columnNames = columnNames;
	}

	
	/*
	public EdgeJoin(S) {
		super();
		this.a = a;
		this.b = b;
	}*/
}

// TODO Optimization is a transformation - so we should use some transformer base class
interface SqlOptimizer {
	SqlOp optimize(SqlOp op);
}



public class SqlOptimizerImpl
{

	private org.aksw.commons.util.jdbc.Schema dbSchema;
	
	public SqlOptimizerImpl(org.aksw.commons.util.jdbc.Schema dbSchema) {
		this.dbSchema = dbSchema;
	}
	
	/**
	 * Optimization result
	 * 
	 * The result of an optimization.
	 * Columns may have been renamed
	 * 
	 * @author raven
	 *
	 */
	class OptResult {
		private SqlOp op;
		private Map<S_ColumnRef, S_ColumnRef> replacement;
		
		public OptResult(SqlOp op, Map<S_ColumnRef, S_ColumnRef> replacement) {
			super();
			this.op = op;
			this.replacement = replacement;
		}

		public SqlOp getOp() {
			return op;
		}

		public Map<S_ColumnRef, S_ColumnRef> getReplacement() {
			return replacement;
		}

		@Override
		public String toString() {
			return "OptiStep [op=" + op + ", replacement=" + replacement + "]";
		}
	}
	
	public static void opt(SqlOpSelectBlock op) {

		//Projection proj = op.getProjection();
		//Set<String> references = proj.getNameToExpr().keySet();
		
	}

		

	/**
	 * 
	 * @param ops
	 * @param cnf cnf of the conditions
	 */
	public SelfJoinResult eliminateSelfJoins(List<SqlOp> ops, List<Collection<SqlExpr>> cnf) {
		
		

		// Index the tables - and keep track of the rest that is not a table
		// We only seek to eleminate self joins on tables
		// (otherwise we most likely won't know about unique constraints)
		Map<String, SqlOpTable> aliasToTable = new HashMap<String, SqlOpTable>();
		Multimap<String, SqlOpTable> nameToTable = HashMultimap.create();
		List<SqlOp> rests = new ArrayList<SqlOp>();
		
		for(SqlOp op : ops) {
			if(op instanceof SqlOpTable) {
				SqlOpTable opTable = (SqlOpTable)op;
				//String alias = opTable.getAliasName();
				String tableName = opTable.getTableName();
				nameToTable.put(tableName, opTable);
				
				String aliasName = opTable.getAliasName();
				aliasToTable.put(aliasName, opTable);
			} else {
				rests.add(op);
			}
		}
		

		
		//UndirectedGraph<String, EdgeJoin> joinGraph = new Multigraph<String, EdgeJoin>(EdgeJoin.class);
		UndirectedGraph<String, EdgeSelfJoin> selfJoinGraph = new SimpleGraph<String, EdgeSelfJoin>(EdgeSelfJoin.class);
		//joinGraph.edgesOf("a").
		
		// Create the join graph
		for(Collection<SqlExpr> clause : cnf) {
			// Skip disjunctions (empty clauses should not happen)
			if(clause.size() != 1) {
				continue;
			}
			
			SqlExpr expr = clause.iterator().next();
			
			if(!(expr instanceof S_Equals)) {
				continue;
			}
			
			S_Equals equals = (S_Equals)expr;
			
			SqlExpr ta = equals.getLeft();
			
			if(!(ta instanceof S_ColumnRef)) {
				continue;
			}
			
			SqlExpr tb = equals.getRight();
			if(!(tb instanceof S_ColumnRef)) {
				continue;
			}
			
			S_ColumnRef a = (S_ColumnRef)ta;
			S_ColumnRef b = (S_ColumnRef)tb;
			
			String colNameA = a.getColumnName();
			String colNameB = b.getColumnName();
			
			if(!colNameA.equals(colNameB)) {
				// Column names to equal - no self join candidate
				continue;
			}

			
			String aliasA = a.getRelationAlias();
			String aliasB = b.getRelationAlias();
			
			SqlOpTable tableA = aliasToTable.get(aliasA);
			SqlOpTable tableB = aliasToTable.get(aliasB);
			
			String tableNameA = tableA.getTableName();
			String tableNameB = tableB.getTableName();
			
			if(!tableNameA.equals(tableNameB)) {
				// No self join candidate
				continue;
			}
			
			selfJoinGraph.addVertex(aliasA);
			selfJoinGraph.addVertex(aliasB);
			
			EdgeSelfJoin edge = selfJoinGraph.getEdge(aliasA, aliasB);
			
			if(edge == null) {
				edge = new EdgeSelfJoin(aliasA, aliasB);
				
				selfJoinGraph.addEdge(aliasA, aliasB, edge);
			}
			
			edge.getColumnNames().add(colNameA);
		}

		//Set<String> open = selfJoinGraph.vertexSet();
		List<EdgeSelfJoin> openEdges = new ArrayList<EdgeSelfJoin>(selfJoinGraph.edgeSet());
		List<EdgeSelfJoin> nextEdges = new ArrayList<EdgeSelfJoin>();
		
		Set<String> affectedVertices = new HashSet<String>();
		
		Map<String, String> aliasRemap = new HashMap<String, String>();
		
		// We can incrementally skip edges referring to removed aliases
		while(!openEdges.isEmpty()) {
			for(EdgeSelfJoin edge : openEdges) {
				
				
				// Get the table that corresponds to the edge
				String aliasA = edge.getAliasA();
				String aliasB = edge.getAliasB();
				
				// We might already have an eliminated self join
				boolean isRemappedA = aliasRemap.containsKey(aliasA);
				boolean isRemappedB = aliasRemap.containsKey(aliasB);
				boolean isRemapped = isRemappedA || isRemappedB;
				
				// If one of the nodes was remapped, the edge is obsolete
				if(isRemapped) {
					continue;
				}
				
				SqlOpTable tableA = aliasToTable.get(aliasA);
				String tableName = tableA.getTableName();
				
				Collection<String> joinColumns = edge.getColumnNames();
				
				// Note: We made sure that no edge can refer to different tables
				// Get the set of unique indexes for the table
				
				Multimap<String, Index> tableToIndexes = dbSchema.getIndexes();
				Collection<Index> indexes = tableToIndexes.get(tableName);
				
				boolean isSelfJoin = false;
				// If our join columns contain all columns of any unique index, we have a self join
				for(Index index : indexes) {
					ColumnsReference ref = index.getColumns();
					
					List<String> indexCols = ref.getColumnNames();
	
					isSelfJoin = joinColumns.containsAll(indexCols);
					
					if(isSelfJoin) {
						break;
					}
				}
				
				
				if(isSelfJoin) {
					// B gets merged into A
					aliasRemap.put(aliasB, aliasA);
					
					Set<EdgeSelfJoin> edges = selfJoinGraph.edgesOf(aliasB);
					
					// For each edge of the node that is about to become removed...
					for(EdgeSelfJoin e : edges) {
						
						// ... get the vertex that is NOT a
						String mergeAlias;
						if(e.getAliasB().equals(aliasB)) {
							mergeAlias = e.getAliasA();
						} else {
							mergeAlias = e.getAliasB();
						}
						
						// ... and create a new edge to it - effectively making b obsolete
						// Don't merge with oneself
						if(!aliasB.equals(mergeAlias)) {
							continue;
						}
						
						EdgeSelfJoin update = selfJoinGraph.getEdge(aliasB, mergeAlias);
						if(update == null) {
							update = selfJoinGraph.addEdge(aliasA, mergeAlias);
						}
						
						// Add the update edge to the set of edges to process next
						nextEdges.add(update);
						
						// Remove the vertex from the graph
						update.getColumnNames().addAll(e.getColumnNames());
					}
					
					selfJoinGraph.removeVertex(aliasB);
				}
			}

			openEdges.clear();
			
			List<EdgeSelfJoin> tmp = nextEdges;
			nextEdges = openEdges;
			openEdges = tmp;			
		}
			
		// TODO Not sure if aliasRemap needs to be transitively accessed or not
		// I guess not - if self joins are not properly removed, this might be the reason

		Factory1<SqlExpr> aliasSubstitutor = new AliasSubstitutor(aliasRemap);
		

		List<SqlOp> newOps = new ArrayList<SqlOp>();
		
		for(SqlOpTable table : aliasToTable.values()) {
			
			String alias = table.getAliasName();
			boolean isRemapped = aliasRemap.containsKey(alias);
			
			if(!isRemapped) {
				newOps.add(table);
			}
		}
		
		
		newOps.addAll(rests);
		
		SelfJoinResult result = new SelfJoinResult(newOps, aliasSubstitutor);
		return result;
		
		//List<Collection<SqlExpr>> newCnf = SqlExprSubstitutor2.substitute(cnf, aliasSubstitutor);

		
		//SqlExprSubstitutor2

		//System.out.println(newCnf);
		
		//System.out.println("... and now?");
	}
	
	
	public static OptResult opt(SqlOp op, Set<S_ColumnRef> references) {
		return null;
	}

	public OptResult opt(SqlOpSelectBlock op, Set<S_ColumnRef> references) {
		List<Collection<SqlExpr>> cnf = SqlExprUtils.toCnf(op.getConditions());
		
		SqlExprUtils.optimizeNotNullInPlace(cnf);
		
		SqlOp subOp = op.getSubOp();
		List<SqlOp> subOps = collectJoins(subOp);
		
		System.out.println("Joins collected: " + subOps.size());
		eliminateSelfJoins(subOps, cnf);
		
		
		if(subOps.size() > 1) {
			for(SqlOp s : subOps) {
				//s.getSchema().
				System.out.println("     Member schema: " + s.getSchema());
			}
		}
		
		return null;
	}
	
//	List<SqlOp> subOps = op.getSubOps();
//	for(SqlOp subOp : subOps) {
//		optimize(subOp);
//	}
//
//	}
	
	
	public static void substituteProjectionInPlace(Projection proj, Factory1<SqlExpr> transformer) {
		for(Entry<String, SqlExpr> entry : proj.getNameToExpr().entrySet()) {
			
			SqlExpr expr = entry.getValue();
			
			SqlExpr newExpr = SqlExprSubstitutor2.substitute(expr, transformer);
			
			entry.setValue(newExpr);			
		}
	}
	
	public static List<SqlSortCondition> transformSortConditions(List<SqlSortCondition> sortConditions, Factory1<SqlExpr> transformer) {
		List<SqlSortCondition> result = new ArrayList<SqlSortCondition>(sortConditions.size());
		for(SqlSortCondition sc : sortConditions) {
			int direction = sc.getDirection();
			SqlExpr expr = sc.getExpression();
			SqlExpr newExpr = SqlExprSubstitutor2.substitute(expr, transformer);
			
			
			SqlSortCondition newSc = new SqlSortCondition(newExpr, direction);
			result.add(newSc);
		}

		return result;
	}
	
	public void optimize(SqlOp op) {
		if(op instanceof SqlOpSelectBlock) {
			SqlOpSelectBlock block = (SqlOpSelectBlock)op;
			
			List<Collection<SqlExpr>> cnf = SqlExprUtils.toCnf(block.getConditions());
		
			SqlExprUtils.optimizeNotNullInPlace(cnf);
			
			SqlOp subOp = block.getSubOp();
			List<SqlOp> subOps = collectJoins(subOp);
			
			System.out.println("Joins collected: " + subOps.size());
			
			for(SqlOp so : subOps) {
				optimize(so);
			}

			
			SelfJoinResult sjr = eliminateSelfJoins(subOps, cnf);
			List<SqlOp> newOps = sjr.getOps();
			
			Factory1<SqlExpr> aliasRemapper = sjr.getAliasSubstitutor();
			List<Collection<SqlExpr>> newCnf = SqlExprSubstitutor2.substitute(cnf, aliasRemapper);
			
			SqlExprUtils.optimizeEqualityInPlace(newCnf);

			
			List<SqlExpr> exprs = SqlExprUtils.cnfAsList(newCnf);
			block.getConditions().clear();
			block.getConditions().addAll(exprs);
			
			substituteProjectionInPlace(block.getProjection(), aliasRemapper);
			
			List<SqlSortCondition> newSortConditions = transformSortConditions(block.getSortConditions(), aliasRemapper);
			block.getSortConditions().clear();
			block.getSortConditions().addAll(newSortConditions);
			
			SqlOpJoinN newOp = new SqlOpJoinN(null, newOps);
			block.setSubOp(newOp);
			
			
			if(subOps.size() > 1) {
				for(SqlOp s : subOps) {
					//s.getSchema().
					System.out.println("     Member schema: " + s.getSchema());
				}
			}
			
		} else {
		
			List<SqlOp> subOps = op.getSubOps();
			for(SqlOp subOp : subOps) {
				optimize(subOp);
			}
		}		
	}
	
	public static List<SqlOp> collectJoins(SqlOp sqlOp) {
		List<SqlOp> result = new ArrayList<SqlOp>();
		
		collectJoins(sqlOp, result);
		
		return result;
	}
	
	public static void collectJoins(SqlOp sqlOp, List<SqlOp> result) {
		if(sqlOp instanceof SqlOpJoin) {
			
			SqlOpJoin opJoin = ((SqlOpJoin)sqlOp);
			
			if(opJoin.getJoinType().equals(JoinType.INNER)) {
			
				SqlOp a = opJoin.getLeft();
				SqlOp b = opJoin.getRight();
	
				collectJoins(a, result);
				collectJoins(b, result);
				
				return;
			}
		} 
		
		result.add(sqlOp);
	}
}

