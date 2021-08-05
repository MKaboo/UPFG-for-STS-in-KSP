package shuttleGuidance.reentry;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.KRPC;
import krpc.client.services.MechJeb;
import krpc.client.services.SpaceCenter;
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
			Orbit orbit = vessel.getOrbit();
			double period = orbit.getPeriod();
			double then = now + period;

			double utMargin = period / 360.;

			Vector3D position = new Vector3D(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

			for (int i = 0; i <= 360; i++)
			{
				then += (i * utMargin);
				position = toV3D(orbit.positionAt(then, referenceFrame));
				System.out.println(orbit.positionAt(then, referenceFrame));
				System.out.println(FastMath.sqrt(position.dotProduct(position)));
			}

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

}
