/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.common.geogebra3D.kernel3D.algos;

import geogebra.common.geogebra3D.kernel3D.geos.GeoPoint3D;
import geogebra.common.kernel.Construction;
import geogebra.common.kernel.Path;
import geogebra.common.kernel.algos.AlgoPointOnPath;
import geogebra.common.kernel.arithmetic.NumberValue;

public class AlgoPoint3DOnPath extends AlgoPointOnPath {

	public AlgoPoint3DOnPath(Construction cons, String label, Path path,
			double x, double y, double z) {

		super(cons, label, path, x, y, z);

	}

	public AlgoPoint3DOnPath(Construction cons, String label, Path path,
			double x, double y, double z, NumberValue param) {

		super(cons, label, path, x, y, param);
	}

	@Override
	protected void createPoint(Path path, double x, double y, double z) {

		P = new GeoPoint3D(cons, path);
		P.setCoords(x, y, z, 1.0);

	}

}
