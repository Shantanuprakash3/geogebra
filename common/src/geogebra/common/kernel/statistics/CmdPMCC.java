package geogebra.common.kernel.statistics;

import geogebra.common.kernel.Kernel;
import geogebra.common.kernel.commands.CmdOneOrTwoListsFunction;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.kernel.geos.GeoList;

/**
 * 
 * CorrelationCoefficient[&lt;List of points>] CorrelationCoefficient[&lt;List
 * of numbers>, &lt;List of numbers> ]
 *
 */
public class CmdPMCC extends CmdOneOrTwoListsFunction {

	/**
	 * Creates new command processor
	 * 
	 * @param kernel
	 *            kernel
	 */
	public CmdPMCC(Kernel kernel) {
		super(kernel);
	}

	@Override
	final protected GeoElement doCommand(String a, GeoList b) {
		AlgoListPMCC algo = new AlgoListPMCC(cons, a, b);
		return algo.getResult();
	}

	@Override
	final protected GeoElement doCommand(String a, GeoList b, GeoList c) {

		AlgoDoubleListPMCC algo = new AlgoDoubleListPMCC(cons, a, b, c);
		return algo.getResult();
	}

}
