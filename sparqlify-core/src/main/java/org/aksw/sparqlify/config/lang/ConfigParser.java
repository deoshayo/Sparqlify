package org.aksw.sparqlify.config.lang;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.aksw.sparqlify.config.syntax.Config;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.slf4j.Logger;


public class ConfigParser {
	//private static final Logger fallbackLogger = LoggerFactory.getLogger(ConfigParser.class);
	
	public Config parse(String str, Logger logger) throws RecognitionException {
		ByteArrayInputStream bais = new ByteArrayInputStream(str.getBytes());
		
		Config result;
		try {
			result = parse(bais, logger);
		} catch (IOException e) {
			// Should not happen - we are reading from a string
			throw new RuntimeException(e);
		} finally {
			try {
				bais.close();
			} catch (IOException e) {
				// Should still not happen
				throw new RuntimeException(e);
			}
		}
		
		return result;		
	}

	
	public Config parse(InputStream in, Logger logger)
			throws IOException, RecognitionException
	{
		CharStream cs = new ANTLRInputStream(in);
		
		SparqlifyConfigLexer lexer = new SparqlifyConfigLexer(cs);
		CommonTokenStream tokens = new CommonTokenStream();
		tokens.setTokenSource(lexer);
		

		SparqlifyConfigParser parser = new SparqlifyConfigParser(tokens);

		parser.setLogger(logger);
		
		CommonTree ast = (CommonTree)parser.sparqlifyConfig().getTree();

		//printAst(ast, 0);

		SparqlifyConfigTree treeParser = new SparqlifyConfigTree(new CommonTreeNodeStream(ast));

		Config config = treeParser.sparqlifyConfig();
		
		return config;
	}
}
