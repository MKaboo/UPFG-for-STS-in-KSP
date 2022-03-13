package shuttleGuidance.reentry;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.KRPC;
import krpc.client.services.MechJeb;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.CelestialBody;
import krpc.client.services.SpaceCenter.Node;
import krpc.client.services.SpaceCenter.Orbit;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;

public class Deorbit {

	private static final double SECTIONOFCIRCLE = 360.;
	private Connection connection;
	private SpaceCenter spaceCenter;
	private Vessel vessel;
	private MechJeb mj;
	private ReferenceFrame referenceFrame;
	private CelestialBody host;
	private Node node;
	// private static MechJeb mj;
	// private static SmartASS smartASS;

	public Deorbit() {
		initConnections();
		try
		{
			run();
		} catch (RPCException e)
		{
			
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		new Deorbit();
	}

	private void run() throws RPCException
	{


		//EntryConditions ecc = sInfo.getEc();

		findShortestDistance();
		setDeorbitNode();
		//setup re-entry info here
		Reentry reentryControl = new Reentry(connection,spaceCenter,vessel,host, mj);
		reentryControl.start();
	}
	private double visViva(double deorbitPeri) throws RPCException
	{
		Triplet<Double, Double, Double> tripletNodePosition = node.position(referenceFrame);
		//Vector3D v3dNodePosition = toV3D(tripletNodePosition);
		//double circularOrbitAltitudeApproximation = v3dNodePosition.dotProduct(v3dNodePosition);
		double circularOrbitAltitudeApproximation = host.altitudeAtPosition(tripletNodePosition, referenceFrame);
		System.out.println("Orbit height at the node is " + circularOrbitAltitudeApproximation);
		
		double r = circularOrbitAltitudeApproximation; 
		double a = (r+deorbitPeri)/2;
		
		try
		{
			double v = FastMath.sqrt(host.getGravitationalParameter() * ((2/r) - (1/a)));
			return v;
		} catch (RPCException e)
		{
			e.printStackTrace();
		}
		return 0;
	}
	private void setDeorbitNode() throws RPCException 
	{
		double requiredV = visViva(76200);
		try
		{
			node.setPrograde(-1 * requiredV);
			
		} catch (RPCException e)
		{
			
			e.printStackTrace();
		}
	}

	private void initConnections()
	{
		try
		{
			connection = Connection.newInstance("Reentry-And-Landing");
			KRPC.newInstance(connection);

			spaceCenter = SpaceCenter.newInstance(connection);
			vessel = spaceCenter.getActiveVessel();

			mj = MechJeb.newInstance(connection);
			referenceFrame = vessel.getOrbit().getBody().getReferenceFrame();
			host = vessel.getOrbit().getBody();
		} catch (IOException | RPCException e)
		{
			e.printStackTrace();
		}
	}

	private void clearNodes() throws RPCException
	{
		List<Node> listOfNodes = vessel.getControl().getNodes();
		if (!listOfNodes.isEmpty())
		{
			node = vessel.getControl().getNodes().get(0);
			if (node != null)
			{
				node.remove();

			}
		}
	}

	private void findShortestDistance()
	{
		try
		{
			clearNodes();

			double now = spaceCenter.getUT();
			

			node = vessel.getControl().addNode(now, 0, 0, 0);

			Orbit orbit = vessel.getOrbit();
			double period = orbit.getPeriod();

			double utMargin = period / SECTIONOFCIRCLE;
			for (int orbitCount = 0; orbitCount < ShuttleLandingSitesConstants.MaxNumberOfOrbitsToCheck; ++orbitCount)
			{
				for (int section = 0; section < SECTIONOFCIRCLE; ++section)
				{
					double time = now + (utMargin * section) + (period * orbitCount);
					node.setUT(time);
					Vector3D shuttlePosition = toV3D(orbit.positionAt(time, referenceFrame));
					shuttlePosition = shuttlePosition.normalize();

					for (Entry<String, LandingFacility> landingFacilityEntry : ShuttleLandingSitesConstants
							.getLandingSites().entrySet())
					{
						Vector3D lfPosition = geodeticToECEF(landingFacilityEntry.getValue(),
								host.getEquatorialRadius()).normalize();

						double theta = FastMath.acos(shuttlePosition.dotProduct(lfPosition));
						theta = FastMath.abs(theta);
						
						//TODO fix the angle in radians
						if (theta > FastMath.PI - FastMath.toRadians(1))
						{
							node.setUT(time);
							System.out.println("Acceptible deorbit point found");
							System.out.println("Landing site is " + landingFacilityEntry.getKey() );
							System.out.println("Time to deorbit node: " + (time-now) + " in seconds");
					
							
							return;
						}
					}
					
				}
				System.out.println("No deorbit found in #" + orbitCount + " orbit" );
			}
			System.err.println("Landing impossible due to no valid deorbit point within " + ShuttleLandingSitesConstants.MaxNumberOfOrbitsToCheck + " orbits");

		} catch (Exception e)
		{
		}
	}

	private static Vector3D toV3D(Triplet<Double, Double, Double> triplet)
	{
		return new Vector3D(triplet.getValue0().doubleValue(), triplet.getValue2().doubleValue(),
				triplet.getValue1().doubleValue());

	}

	private static Vector3D geodeticToECEF(LandingFacility facility, double radius)
	{
		double phi = facility.getLatitude();
		double lambda = facility.getLongitude();
		double h = facility.getAltitude();

		double NofPhi = (radius);

		double x = (NofPhi + h) * FastMath.cos(phi) * FastMath.cos(lambda);
		double y = (NofPhi + h) * FastMath.cos(phi) * FastMath.sin(lambda);
		double z = (NofPhi + h) * FastMath.sin(phi);

		return new Vector3D(x, y, z);

	}

}
