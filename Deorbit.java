package shuttleGuidance.reentry;

import java.io.IOException;
import java.util.HashMap;
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
import shuttleGuidance.reentry.shuttleInfo.EntryConditions;

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
		run();
	}

	public static void main(String[] args)
	{
		new Deorbit();
	}

	private void run()
	{

		shuttleControl sc = new shuttleControl(vessel, mj, referenceFrame);
		shuttleInfo sInfo = new shuttleInfo(connection, vessel, referenceFrame);

		EntryConditions ecc = sInfo.getEc();

		findShortestDistance();
		//setup re-entry info here
		Reentry reentryControl = new Reentry(connection,spaceCenter,vessel,host, mj);
		reentryControl.start();
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
						// TODO fix the angle in radians

						if (theta > FastMath.PI - FastMath.toRadians(1))
						{
							System.out.println("Acceptible deorbit point found");
							System.out.println("Landing site is " + landingFacilityEntry.getKey() );
							System.out.println("Time to deorbit: " + (time-now) + " in seconds");
							
							node.setUT(time);
							return;
						}
					}
				}
			}

		} catch (Exception e)
		{
			// TODO: handle exception
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

	



//	static class foo {
//		private double distance;
//		private double time;
//		private int orbitNumber;
//		private int sliceNumber;
//		private LandingFacility lf;
//
//		protected double getDistance()
//		{
//			return distance;
//		}
//
//		protected void setDistance(double distance)
//		{
//			this.distance = distance;
//		}
//
//		protected double getTime()
//		{
//			return time;
//		}
//
//		protected void setTime(double time)
//		{
//			this.time = time;
//		}
//
//		protected int getOrbitNumber()
//		{
//			return orbitNumber;
//		}
//
//		protected void setOrbitNumber(int orbitNumber)
//		{
//			this.orbitNumber = orbitNumber;
//		}
//
//		protected int getSliceNumber()
//		{
//			return sliceNumber;
//		}
//
//		protected void setSliceNumber(int sliceNumber)
//		{
//			this.sliceNumber = sliceNumber;
//		}
//
//		/**
//		 * @return the lf
//		 */
//		protected LandingFacility getLf()
//		{
//			return lf;
//		}
//
//		/**
//		 * @param lf the lf to set
//		 */
//		protected void setLf(LandingFacility lf)
//		{
//			this.lf = lf;
//		}
//
//		@Override
//		public String toString()
//		{
//			return "foo [distance=" + distance + ", time=" + time + ", orbitNumber=" + orbitNumber + ", sliceNumber="
//					+ sliceNumber + ", lf=" + lf + "]";
//		}
//
//	}
}
