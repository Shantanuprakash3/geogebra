package geogebra.common.kernel.parser;

import geogebra.common.kernel.arithmetic.ExpressionNode;
import geogebra.common.kernel.arithmetic.Function;
import geogebra.common.kernel.arithmetic.FunctionNVar;
import geogebra.common.kernel.arithmetic.ValidExpression;

/**
 * Interface for parser 
 *
 */
public interface ParserInterface {
	/**
	 * @param parseString string to parse
	 * @return expression
	 * @throws ParseException if parsing  fails
	 */
	public ExpressionNode parseExpression(String parseString) throws ParseException;

	/**
	 * @param string string to parse
	 * @return function
	 * @throws ParseException if parsing  fails
	 */
	public Function parseFunction(String string) throws ParseException;

	/**
	 * @param string string to parse
	 * @return multivariate function
	 * @throws ParseException if parsing fails
	 */
	public FunctionNVar parseFunctionNVar(String string)
			throws ParseException;

	/**
	 * @param str string to parse
	 * @return expression
	 * @throws ParseException if parsing fails
	 */
	public ValidExpression parseGeoGebraExpression(String str) throws ParseException;

	/**
	 * @param label potential label
	 * @return valid label
	 * @throws ParseException if parsing fails
	 */
	public String parseLabel(String label) throws ParseException;
}
