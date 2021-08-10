package shuttleGuidance.reentry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

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
import shuttleGuidance.reentry.testMain.foo;

public class shuttleLanding {

	private Connection connection;
	private Vessel vessel;
	private MechJeb mj;
	private SpaceCenter spaceCenter;
	private ReferenceFrame referenceFrame;
	private CelestialBody host;
	private Node node;

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

		shuttleControl sc = new shuttleControl(vessel, mj, referenceFrame);
		shuttleInfo sInfo = new shuttleInfo(connection, vessel, referenceFrame);

		EntryConditions ecc = sInfo.getEc();

		findShortestDistance();
		sc.startLoop(connection);
		sc.deorbit(sInfo);
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

	private void findShortestDistance()
	{
		try
		{
			HashMap<String, foo> timeAndDistance = new HashMap<String, foo>();
			double now = spaceCenter.getUT();
			node = vessel.getControl().addNode(now, 0, 0, 0);

			Orbit orbit = vessel.getOrbit();
			double period = orbit.getPeriod();

			// double then = now + period;

			double utMargin = period / 360.;

			for (int i = 0; i < 360; i++)
			{
				final double time = (i * utMargin) + now;
				node.setUT(time);
				final Vector3D position = toV3D(orbit.positionAt(time, referenceFrame));
				for (Entry<String, LandingFacility> landingFacilityEntry : ShuttleLandingSitesConstants
						.getLandingSites().entrySet())
				{
					LandingFacility landingFacility = landingFacilityEntry.getValue();
					Vector3D landingSitePosition = geodeticToECEF(landingFacility, host.getEquatorialRadius());

					foo bar = timeAndDistance.get(landingFacilityEntry.getKey());
					if (bar == null)
					{
						bar = new foo();
						bar.setDistance(Double.MAX_VALUE);
						bar.setTime(Double.MAX_VALUE);
						timeAndDistance.put(landingFacilityEntry.getKey(), bar);
					}

					double dis = distance(position, landingSitePosition);

					if (bar.getDistance() > dis)
					{
						bar.setDistance(dis);
						bar.setTime(time);
					}
				}

			}

			foo bar = null;
			String runwayName = null;
			for (Entry<String, foo> entry : timeAndDistance.entrySet())
			{
				if (bar == null || (bar.getDistance() > entry.getValue().getDistance()))
				{
					bar = entry.getValue();
					runwayName = entry.getKey();
				}

			}

			node.setUT(bar.getTime());

			node.setUT(node.getUT() - (period / 2.));

			try
			{
				connection.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// spaceCenter.warpTo(shortestDistAt, 1000, 4);

		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void checkInclination(Node node, double time)
	{

		try
		{
			final Vector3D position = toV3D(node.getOrbit().positionAt(time, referenceFrame));
			
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	private static double distance(Vector3D v1, Vector3D v2)
	{

		return Vector3D.distance(v1, v2);
	}

	static class foo {
		private double distance;
		private double time;

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
	}
}
