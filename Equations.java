package shuttleGuidance.reentry;

import org.apache.commons.math3.util.FastMath;

import krpc.client.services.SpaceCenter.Orbit;

public class Equations {

	Orbit orbit;

	private double calcEcentricity(double semiMajorAxis, double semiMinorAxis)
	{
		return FastMath.sqrt(1 - (FastMath.pow(semiMinorAxis, 2) / FastMath.pow(semiMajorAxis, 2)));
	}

	private double calcY(double x, double h, double k, double semiMajor, double semiMinor)
	{
		return FastMath.sqrt((1 - (FastMath.pow(x - h, 2)) / FastMath.pow(semiMajor, 2)) * FastMath.pow(semiMinor, 2))
				+ k;
	}
}
