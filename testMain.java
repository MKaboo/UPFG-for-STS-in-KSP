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

public class testMain {

	private static final double SECTIONOFCIRCLE = 360.;
	private Connection connection;
	private SpaceCenter spaceCenter;
	private Vessel vessel;
	private MechJeb mj;
	private ReferenceFrame referenceFrame;
	private CelestialBody host;
	private Node node;
	//	private static MechJeb mj;
	//	private static SmartASS smartASS;

	public testMain() {
		initConnections();
		run();
	}

	public static void main(String[] args)
	{
		new testMain();
		//		LandingFacility KSC33 = ShuttleLandingSitesConstants.getLandingSites().get("SLF-33");
		//		Vector3D test = geodeticToECEF(KSC33, 6371*1000);
		//		System.out.println(test.toString());
		//		System.out.println(distance(test, test));
		// Vector3D test2 = test.;

	}

	private void run()
	{

		shuttleControl sc = new shuttleControl(vessel, mj, referenceFrame);
		shuttleInfo sInfo = new shuttleInfo(connection, vessel, referenceFrame);

		EntryConditions ecc = sInfo.getEc();

		findShortestDistance();

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

	
//		private void findShortestDistance()
//		{
//			try
//			{
//				// HashMap<String, foo> timeAndDistance = new HashMap<String, foo>();
//	
//				double now = spaceCenter.getUT();
//				List<Node> listOfNodes = vessel.getControl().getNodes();
//				if (!listOfNodes.isEmpty())
//				{
//					node = vessel.getControl().getNodes().get(0);
//					if (node != null)
//					{
//						node.remove();
//	
//					}
//				}
//				foo bar = new foo();
//				bar.distance = Double.MAX_VALUE;
//	
//				node = vessel.getControl().addNode(now, 0, 0, 0);
//	
//				Orbit orbit = vessel.getOrbit();
//				double period = orbit.getPeriod();
//	
//				double utMargin = period / SECTIONOFCIRCLE;
//	
//				TimeUnit.SECONDS.sleep(1);
//	
//				for (int orbitCount = 0; orbitCount < ShuttleLandingSitesConstants.MaxNumberOfOrbitsToCheck; ++orbitCount)
//				{
//					foo b2 = null;
//					for (Entry<String, LandingFacility> landingFacilityEntry : ShuttleLandingSitesConstants
//							.getLandingSites().entrySet())
//					{
//						b2 = findMinFooForOrbit(orbitCount, period, now, orbit, landingFacilityEntry.getValue());
//	
//						if (bar.distance > b2.distance)
//						{
//							bar = b2;
//	
//							node.setUT(bar.time);
//							System.out.println(bar.getLf().getName());
//							TimeUnit.SECONDS.sleep(1);
//							// System.out.println(bar.time);
//	
//						}
//					}
//					if (bar.getDistance() >= ShuttleLandingSitesConstants.MAXIMUMLANDINGSITEDIF)
//					{
//						System.out.println(bar.getDistance());
//						spaceCenter.warpTo(now + period, 1000, 5);
//						findShortestDistance();
//					} else
//					{
//						node.setUT(bar.time);
//						System.out.println("Found runway");
//						System.out.println(bar.time);
//						System.out.println(bar.distance);
//						System.out.println(bar.getLf());
//						break;
//					}
//	
//				}
//	
//				node.setUT(bar.getTime() - (period / 2.));
//				try
//				{
//	
//					connection.close();
//				} catch (IOException e)
//				{
//					// TODO Auto-generated catch block
//					// e.printStackTrace();
//				}
//	
//			} catch (RPCException e)
//			{
//				// TODO Auto-generated catch block
//				// e.printStackTrace();
//			} catch (InterruptedException e1)
//			{
//				// TODO Auto-generated catch block
//				// e1.printStackTrace();
//			}
//			
//		}
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
			// HashMap<String, foo> timeAndDistance = new HashMap<String, foo>();

			clearNodes();

			double now = spaceCenter.getUT();
			foo bar = new foo();
			bar.distance = Double.MAX_VALUE;

			node = vessel.getControl().addNode(now, 0, 0, 0);

			Orbit orbit = vessel.getOrbit();
			double period = orbit.getPeriod();

			double utMargin = period / SECTIONOFCIRCLE;
			for (int orbitCount = 0; orbitCount < ShuttleLandingSitesConstants.MaxNumberOfOrbitsToCheck; ++orbitCount)
			{
				for (int section = 0; section < SECTIONOFCIRCLE; ++section)
				{
					double time = now + (utMargin * section) + (period * orbitCount);
					Vector3D shuttlePosition = toV3D(orbit.positionAt(time, referenceFrame));
					shuttlePosition = shuttlePosition.normalize();

					for (Entry<String, LandingFacility> landingFacilityEntry : ShuttleLandingSitesConstants
							.getLandingSites().entrySet())
					{
						Vector3D lfPosition = geodeticToECEF(landingFacilityEntry.getValue(),
								host.getEquatorialRadius()).normalize();

						double theta = FastMath.acos(shuttlePosition.dotProduct(lfPosition));

						// TODO fix the angle in radians
						if (theta < 0)
						{

						}
					}
				}
			}

		} catch (Exception e)
		{
			// TODO: handle exception
		}
	}

	private foo findMinFooForOrbit(int orbitCount, double period, double now, Orbit orbit, LandingFacility lF) throws RPCException
	{
		foo bar = new foo();

		final double orbitStarttime = ((orbitCount) * period) + now;
		bar.time = orbitStarttime;

		double utMargin = period / SECTIONOFCIRCLE;
		bar.distance = Double.MAX_VALUE;
		for (int sliceOfCicle = 0; sliceOfCicle < SECTIONOFCIRCLE; ++sliceOfCicle)
		{

			final double time = (sliceOfCicle * utMargin) + orbitStarttime;
			node.setUT(time);
			final Vector3D position = toV3D(orbit.positionAt(time, referenceFrame));

			LandingFacility landingFacility = lF;

			Vector3D landingSitePosition = geodeticToECEF(landingFacility, host.getEquatorialRadius());
			double dis = distance(position, landingSitePosition);

			if (bar.distance > dis)
			{
				bar.distance = dis;
				bar.lf = lF;
				bar.time = time;
				// node.setUT(bar.time);

			}

		}

		return bar;
	}

	private foo findClosestLandingSite(final HashMap<String, foo> timeAndDistance)
	{
		foo bar = null;
		for (Entry<String, foo> entry : timeAndDistance.entrySet())
		{
			if (bar == null || (bar.getDistance() > entry.getValue().getDistance()))
			{
				bar = entry.getValue();
			}

		}
		return bar;
	}

	//	private boolean checkInclination(Vector3D v1, Vector3D v2)
	//	{
	//		double dis = getScaledDistance(v1, v2);
	//		if (dis <= ShuttleLandingSitesConstants.MAXIMUMLANDINGSITEDIF)
	//		{
	//			System.out.println("here");
	//			System.out.println(dis);
	//			return true;
	//
	//		}
	//		return false;
	//	}

	//	private double getScaledDistance(Vector3D v1, Vector3D v2)
	//	{
	//		Vector3D v1hat = getScaledVector(v1);
	//		Vector3D v2hat = getScaledVector(v2);
	//
	//		return distance(v1hat, v2hat);
	//
	//	}

	private Vector3D getScaledVector(Vector3D v)
	{
		double mag = FastMath.sqrt(v.dotProduct(v));
		double x = v.getX();
		double y = v.getY();
		double z = v.getZ();

		x /= mag;
		y /= mag;
		z /= mag;
		//
		//		try
		//		{
		//			x *= host.getEquatorialRadius();
		//			y *= host.getEquatorialRadius();
		//			z *= host.getEquatorialRadius();
		//		} catch (RPCException e)
		//		{
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

		return new Vector3D(x, y, z);

	}

	private static Triplet<Double, Double, Double> toTriplet(Vector3D v)
	{
		return new Triplet<Double, Double, Double>(v.getX(), v.getZ(), v.getY());
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

	private static double distance(Vector3D v1, Vector3D v2)
	{

		// return Vector3D.distance(v1, v2);
		//		double angle = Vector3D.angle(v1, v2);
		//
		//		double v1Mag = v1.dotProduct(v1);
		//		double v2Mag = v2.dotProduct(v2);
		//
		//		return FastMath.sqrt(v1Mag + v2Mag - (2 * FastMath.sqrt(v1Mag) * FastMath.sqrt(v1Mag) * FastMath.cos(angle)));
		//		
		return FastMath.sqrt(FastMath.pow(v2.getX() - v1.getX(), 2) + FastMath.pow(v2.getY() - v1.getY(), 2)
		+ FastMath.pow(v2.getZ() - v1.getZ(), 2));

	}

	//	private static double distance(Vector3D v1, Vector3D v2, SpaceCenter spaceCenter)
	//	{
	//		//return raycast
	//	}

	static class foo {
		private double distance;
		private double time;
		private int orbitNumber;
		private int sliceNumber;
		private LandingFacility lf;

		protected double getDistance()
		{
			return distance;
		}

		protected void setDistance(double distance)
		{
			this.distance = distance;
		}

		protected double getTime()
		{
			return time;
		}

		protected void setTime(double time)
		{
			this.time = time;
		}

		protected int getOrbitNumber()
		{
			return orbitNumber;
		}

		protected void setOrbitNumber(int orbitNumber)
		{
			this.orbitNumber = orbitNumber;
		}

		protected int getSliceNumber()
		{
			return sliceNumber;
		}

		protected void setSliceNumber(int sliceNumber)
		{
			this.sliceNumber = sliceNumber;
		}

		/**
		 * @return the lf
		 */
		protected LandingFacility getLf()
		{
			return lf;
		}

		/**
		 * @param lf the lf to set
		 */
		protected void setLf(LandingFacility lf)
		{
			this.lf = lf;
		}

		@Override
		public String toString()
		{
			return "foo [distance=" + distance + ", time=" + time + ", orbitNumber=" + orbitNumber + ", sliceNumber="
					+ sliceNumber + ", lf=" + lf + "]";
		}

	}
}
