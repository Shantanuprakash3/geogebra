/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.common.geogebra3D.kernel3D.algos;

import geogebra.common.geogebra3D.kernel3D.geos.GeoLine3D;
import geogebra.common.geogebra3D.kernel3D.geos.GeoPoint3D;
import geogebra.common.kernel.Construction;
import geogebra.common.kernel.Kernel;
import geogebra.common.kernel.Matrix.Coords;
import geogebra.common.kernel.algos.AlgoTangentPointND;
import geogebra.common.kernel.geos.GeoLine;
import geogebra.common.kernel.kernelND.AlgoIntersectND;
import geogebra.common.kernel.kernelND.GeoConicND;
import geogebra.common.kernel.kernelND.GeoLineND;
import geogebra.common.kernel.kernelND.GeoPointND;

/**
 * Two tangents through point P to conic section c
 */
public class AlgoTangentPoint3D extends AlgoTangentPointND {

	public AlgoTangentPoint3D(Construction cons, String[] labels, GeoPointND P,
			GeoConicND c) {
		super(cons, labels, P, c);
	}

	@Override
	protected void setPolar() {

		polarCoords = new double[3];

		// the tangents are computed by intersecting the
		// polar line of P with c
		polar = new GeoLine(cons);
		// updatePolarLine();
		algoIntersect = new AlgoIntersectLineIncludedConic3D(cons, polar, c);
		// this is only an internal Algorithm that shouldn't be in the
		// construction list
		cons.removeFromConstructionList(algoIntersect);
		tangentPoints = algoIntersect.getIntersectionPoints();
	}

	private double[] polarCoords;

	private Coords polarOrigin, polarDirection;

	@Override
	protected void setTangentFromPolar(int i) {

		if (i == 0) { // for second tangent, calculations are already done
			polar.getCoords(polarCoords);
			polarDirection = c.getCoordSys().getVector(-polarCoords[1],
					polarCoords[0]);
			if (Kernel.isZero(polarCoords[0])) {
				polarOrigin = c.getCoordSys().getPoint(0,
						-polarCoords[2] / polarCoords[1]);
			} else {
				polarOrigin = c.getCoordSys().getPoint(
						-polarCoords[2] / polarCoords[0], 0);
			}
		}

		((GeoLine3D) tangents[i]).setCoord(polarOrigin, polarDirection);

	}

	@Override
	protected void setTangents() {
		tangents = new GeoLine3D[2];
		tangents[0] = new GeoLine3D(cons);
		tangents[1] = new GeoLine3D(cons);
		((GeoLine3D) tangents[0]).setStartPoint(P);
		((GeoLine3D) tangents[1]).setStartPoint(P);
	}

	@Override
	protected boolean checkUndefined() {

		if (super.checkUndefined()) {
			return true;
		}

		coords2D = c.getCoordSys().getNormalProjection(P.getInhomCoordsInD3())[1];
		if (!Kernel.isZero(coords2D.getZ())) {
			return true;
		}

		// now it's a 2D point in coord sys
		coords2D.setZ(1);

		return false;
	}

	@Override
	protected void updatePolarLine() {
		c.polarLine(coords2D, polar);
	}

	private Coords coords2D;

	@Override
	protected boolean isIntersectionPointIncident() {
		return c.isIntersectionPointIncident(coords2D, Kernel.MIN_PRECISION); // ||
																				// P.getIncidenceList().contains(c);
	}

	@Override
	protected void updateTangents() {
		// calc tangents through tangentPoints
		if (!tangentPoints[0].isDefined()) {
			tangents[0].setUndefined();
		} else {
			((GeoLine3D) tangents[0]).setCoord(P, tangentPoints[0]);
		}
		if (!tangentPoints[1].isDefined()) {
			tangents[1].setUndefined();
		} else {
			((GeoLine3D) tangents[1]).setCoord(P, tangentPoints[1]);
		}
	}

	/**
	 * Inits the helping interesection algorithm to take the current position of
	 * the lines into account. This is important so the the tangent lines are
	 * not switched after loading a file
	 */
	@Override
	public void initForNearToRelationship() {
		// if first tangent point is not on first tangent,
		// we switch the intersection points

		initForNearToRelationship(tangentPoints, tangents[0], algoIntersect);
	}

	/**
	 * Inits the helping interesection algorithm to take the current position of
	 * the lines into account. This is important so the the tangent lines are
	 * not switched after loading a file
	 *
	 * @param tangentPoints
	 *            tangent points
	 * @param tangent
	 *            tangent line
	 * @param algoIntersect
	 *            algo used
	 */
	public static final void initForNearToRelationship(
			GeoPointND[] tangentPoints, GeoLineND tangent,
			AlgoIntersectND algoIntersect) {
		Coords firstTangentPoint = tangentPoints[0].getInhomCoordsInD3();

		if (!((GeoLine3D) tangent).isOnFullLine(firstTangentPoint,
				Kernel.MIN_PRECISION)) {
			algoIntersect.initForNearToRelationship();

			// first = second
			algoIntersect.setIntersectionPoint(0, tangentPoints[1]);

			// second = first
			((GeoPoint3D) tangentPoints[1]).setCoords(firstTangentPoint);
			algoIntersect.setIntersectionPoint(1, tangentPoints[1]);
		}
	}

}
