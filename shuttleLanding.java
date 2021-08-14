package shuttleGuidance.reentry;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.partitioning.Side;
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
import krpc.client.services.SpaceCenter.VesselSituation;
import shuttleGuidance.reentry.shuttleInfo.EntryConditions;

public class shuttleLanding {

	private static final double SECTIONOFCIRCLE = 360.;

	private Connection connection;
	private Vessel vessel;
	private MechJeb mj;
	private SpaceCenter spaceCenter;
	private ReferenceFrame referenceFrame;
	private CelestialBody host;
	private Node node;
	private LandingFacility selectedLandingSite;
	public shuttleLanding() {
		initConnections();
		run();
	}

	public static void main(String[] args)
	{
		new shuttleLanding();
	}

	private void run()
	{
		try
		{
			shuttleControl sc = new shuttleControl(vessel, mj, referenceFrame);
			shuttleInfo sInfo = new shuttleInfo(connection, vessel, referenceFrame);

			findShortestDistance();
			System.out.println(sInfo.getDeorbitDistance());
			LandingFacility lf = selectedLandingSite;
			boolean go = true;
			while (go)
			{
				System.out.println("deorbitDistance," + sInfo.getDeorbitDistance());
				System.out.println(sInfo.getDistance(sInfo.getShuttleLatitude(), sInfo.getShuttleLongitude(),
						FastMath.toDegrees(lf.getLatitude()), FastMath.toDegrees(lf.getLongitude()),
						host.getEquatorialRadius()));
				if (sInfo.getDistance(sInfo.getShuttleLatitude(), sInfo.getShuttleLongitude(),
						FastMath.toDegrees(lf.getLatitude()), FastMath.toDegrees(lf.getLongitude()),
						host.getEquatorialRadius()) < sInfo.getDeorbitDistance()
						&& vessel.getOrbit().getPeriapsisAltitude() > sInfo.getDeorbitPE())
				{
					go = false;
					System.out.println("debug");
					spaceCenter.setRailsWarpFactor(0);
				}
				TimeUnit.MILLISECONDS.sleep(100);
			}

			// sInfo.setEc(EntryConditions.standby);
//			sc.execute(connection, sInfo, spaceCenter);
//			TimeUnit.SECONDS.sleep(5);
//
//			sInfo.setEc(EntryConditions.deorbit);
//			sInfo.setNode(vessel.getControl().getNodes().get(0));
//
//			spaceCenter.warpTo(sInfo.node.getUT() - 60, 1000, 4);
//			TimeUnit.SECONDS.sleep(60);
//			sc.execute(connection, sInfo, spaceCenter);
//
//			sInfo.setEc(EntryConditions.reentry);
//			TimeUnit.SECONDS.sleep(30);
//			sc.execute(connection, sInfo, spaceCenter);

		} catch (RPCException | InterruptedException e1)
		{
			e1.printStackTrace();
		}
		try
		{
			connection.close();
		} catch (IOException e)
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

	//TODO: add return of landing site for multiple sites
	private void findShortestDistance()
	{
		try
		{

			double now = spaceCenter.getUT();
			List<Node> listOfNodes = vessel.getControl().getNodes();
			if (!listOfNodes.isEmpty())
			{
				for (Node node : listOfNodes)
				{
					if (node != null)
					{
						node.remove();

					}
				}

			}
			foo bar = new foo();
			bar.distance = Double.MAX_VALUE;

			node = vessel.getControl().addNode(now, 0, 0, 0);

			Orbit orbit = vessel.getOrbit();
			double period = orbit.getPeriod();

			TimeUnit.SECONDS.sleep(1);

			for (int orbitCount = 0; orbitCount < ShuttleLandingSitesConstants.MaxNumberOfOrbitsToCheck; ++orbitCount)
			{
				foo b2 = null;
				for (Entry<String, LandingFacility> landingFacilityEntry : ShuttleLandingSitesConstants
						.getLandingSites().entrySet())
				{
					b2 = findMinFooForOrbit(orbitCount, period, now, orbit, landingFacilityEntry.getValue());

					if (bar.distance > b2.distance)
					{
						bar = b2;

						node.setUT(bar.time);
						System.out.println(bar.getLf().getName());
						TimeUnit.SECONDS.sleep(1);
						// System.out.println(bar.time);

					}
				}
				if (bar.getDistance() >= ShuttleLandingSitesConstants.MAXIMUMLANDINGSITEDIF)
				{
					System.out.println(bar.getDistance());
					if (bar.getDistance() < 2e6)
					{
						spaceCenter.warpTo(now + (period / 3), 1000, 5);
					}
					else 
					{
						spaceCenter.warpTo(now + (period / 2), 1000, 5);
					}
					findShortestDistance();
				} else
				{
					node.setUT(bar.time);
					System.out.println("Found runway");
					System.out.println(bar.time);
					System.out.println(bar.distance);
					System.out.println(bar.getLf());
					selectedLandingSite = bar.getLf();
					break;
				}

			}

			// node.setUT(bar.getTime() - (60. * 60.));

		} catch (Exception e)
		{

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
		return FastMath.sqrt(FastMath.pow(v2.getX() - v1.getX(), 2) + FastMath.pow(v2.getY() - v1.getY(), 2)
				+ FastMath.pow(v2.getZ() - v1.getZ(), 2));

	}

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
