package shuttleGuidance.reentry;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Orbit {
	private double inclination;
	private double semiMajorAxis;
	private double eccentricity;
	private Vector3D startingPositon;
	private double LAN;//Longitude of Ascending Node
	private double aoP;//Argument of Perigee
	private double trueAnomaly;// 0 <= v < 360

	public Orbit() {
		// TODO Auto-generated constructor stub
	}
	
	

	public Orbit(double inclination, double semiMajorAxis, double eccentricity, double lAN, double aoP,
			double trueAnomaly) {
		super();
		this.inclination = inclination;
		this.semiMajorAxis = semiMajorAxis;
		this.eccentricity = eccentricity;
		LAN = lAN;
		this.aoP = aoP;
		this.trueAnomaly = trueAnomaly;
	}



	protected void defineCOES()
	{

	}
}
