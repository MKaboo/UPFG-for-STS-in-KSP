package shuttleGuidance.reentry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

public class shuttleLanding {

	private Connection connection;
	private Vessel vessel;
	private MechJeb mj;
	private SpaceCenter spaceCenter;
	private ReferenceFrame referenceFrame;
	private CelestialBody host;

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

//		while (true)
//		//for (int i = 0; i < 20000; i++)
//		{
//	
//			System.out.println(sInfo.getShuttleLatitude() + " " + sInfo.getShuttleLongitude());
//			try
//			{
//				TimeUnit.MILLISECONDS.sleep(500);
//			} catch (InterruptedException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

//		try
//		{
//			connection.close();
//		} catch (IOException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
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
			double now = spaceCenter.getUT();
			Node node = vessel.getControl().addNode(now, 0, 0, 0);
			Orbit orbit = vessel.getOrbit();
			double period = orbit.getPeriod();
			//double then = now + period;
			

			double utMargin = period / 360.;

			Vector3D position = new Vector3D(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
			HashMap<String, Double> timeAndDistance = new HashMap<String, Double>();
			for (int i = 0; i < 360; i++)
			{
				
				node.setUT((i * utMargin) + now);
				position = toV3D(orbit.positionAt((i * utMargin) + now, referenceFrame));
				//System.out.println(orbit.positionAt(then, referenceFrame));
				for (String name : ShuttleLandingSitesConstants.getFacilitiesNames())
				{
					//System.out.println(name);
					LandingFacility landingFacility = ShuttleLandingSitesConstants.getLandingSites().get(name);
					Vector3D landingSitePosition = geodeticToECEF(landingFacility);
					System.out.println(landingSitePosition);
					double dis = distance(position, landingSitePosition);
					//System.out.println(dis);
					timeAndDistance.put(Double.toString((i * utMargin) + now), dis);
				}

//				System.out.println(orbit.positionAt(then, referenceFrame));
//				System.out.println(FastMath.sqrt(position.dotProduct(position)));
			}

			
			double shortestDistAt = Double.parseDouble(getMinimumFromHash(timeAndDistance));
			
			node.setUT(shortestDistAt);
			
			spaceCenter.warpTo(shortestDistAt, 1000, 4);
			
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Vector3D toV3D(Triplet<Double, Double, Double> triplet)
	{
		return new Vector3D(triplet.getValue0().doubleValue(), triplet.getValue1().doubleValue(),
				triplet.getValue2().doubleValue());

	}

	private String getMinimumFromHash(HashMap<String, Double> map)
	{
		Map.Entry<String, Double> min = null;
		for (Map.Entry<String, Double> entry : map.entrySet()) {
		    if (min == null || min.getValue() > entry.getValue()) {
		        min = entry;
		    }
		}

		System.out.println(min.getKey());
		return min.getKey();
	}

	private Vector3D geodeticToECEF(LandingFacility facility)
	{
		double phi = facility.getLatitude();
		double lambda = facility.getLongitude();
		double h = facility.getAltitude();

		try
		{
			double NofPhi = (host.getEquatorialRadius());

			double x = (NofPhi + h) * FastMath.cos(phi) * FastMath.cos(lambda);
			double y = (NofPhi + h) * FastMath.cos(phi) * FastMath.sin(lambda);
			double z = (NofPhi + h) * FastMath.sin(phi);

			return new Vector3D(x, y, z);
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	private double distance(Vector3D v1, Vector3D v2)
	{
		return FastMath.sqrt(FastMath.pow(v2.getX() - v1.getX(), 2) + FastMath.pow(v2.getY() - v1.getY(), 2)
				+ FastMath.pow(v2.getZ() - v1.getZ(), 2));
	}

}
