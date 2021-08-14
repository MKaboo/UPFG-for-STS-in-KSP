package shuttleGuidance.launch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.util.FastMath;
import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.KRPC;
import krpc.client.services.MechJeb;
import krpc.client.services.MechJeb.SmartASS;
import krpc.client.services.MechJeb.SmartASSAutopilotMode;
import krpc.client.services.MechJeb.SmartASSInterfaceMode;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.CelestialBody;
import krpc.client.services.SpaceCenter.Control;
import krpc.client.services.SpaceCenter.Engine;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;
import shuttleGuidance.launch.FlightUI.FlightMode;

public class ShuttleAscent2 {

	private static double MINIMUMPITCH = 26d;
	private static boolean debug = false;
	private Control vesselControl;
	private MechJeb mj;
	private SmartASS smartASS;
	private static float maxRollRate = 10f;
	private static float maxPitchRate = 0.65f;
	private SpaceCenter spaceCenter;
	private Vessel vessel;
	private double roll = 0, pitch = 90, heading = 90;
	private double targApoM;
	private double inclination;
	private CelestialBody body;
	private double SRBmaxThrust;
	private ReferenceFrame refFrame;
	private boolean keepRunning = true;
	private int SRBINDEX = Integer.MAX_VALUE;
	private Connection connection;
	private boolean maxQ = true;
	protected FlightUI flightUI;
	public ShuttleAscent2() throws InterruptedException, IOException, RPCException, StreamException {
		connection = Connection.newInstance("Launch");
		KRPC.newInstance(connection);

		spaceCenter = SpaceCenter.newInstance(connection);
		vessel = spaceCenter.getActiveVessel();

		mj = MechJeb.newInstance(connection);
		smartASS = mj.getSmartASS();
		
		flightUI = new FlightUI();
		
		TimeUnit.SECONDS.sleep(2);

		body = vessel.getOrbit().getBody();

		vesselControl = vessel.getControl();
		
		
		smartASS.setForcePitch(true);
		smartASS.setForceYaw(true);
		smartASS.setForceRoll(true);
		smartASS.setInterfaceMode(SmartASSInterfaceMode.SURFACE);
		smartASS.setAutopilotMode(SmartASSAutopilotMode.SURFACE);
		
		
		//smartASS.setSurfaceHeading(90);
		smartASS.setSurfacePitch(90);
		smartASS.setSurfaceRoll(0);

		smartASS.update(false);

		refFrame = ReferenceFrame.createHybrid(connection, vessel.getOrbit().getBody().getReferenceFrame(),
				vessel.getSurfaceReferenceFrame(), vessel.getOrbit().getBody().getReferenceFrame(),
				vessel.getOrbit().getBody().getReferenceFrame());


		flightUI.start();
		
		initializeTWR();
		findLaunchCorridor(300, 300, 35, false);
		countdown();
		launch();

		// vesselControl.setThrottle(0);

		// connection.close();

	}

	public static void main(String[] args) throws IOException, RPCException, InterruptedException, StreamException
	{
		new ShuttleAscent2();
	}

	private void initializeTWR() throws RPCException
	{
		for (Engine iterable_element : vessel.getParts().getEngines())
		{
			float thrust = iterable_element.getMaxThrust();
			if (thrust > 1.6e6)
			{

				if (thrust > SRBmaxThrust)
				{
					SRBmaxThrust = thrust;

				}

			}
		}

		
	}

	private double provideCurrentSRBThrust() throws RPCException
	{
		List<Engine> engines = vessel.getParts().getEngines();
		ArrayList<Float> thrusts = new ArrayList<Float>();
		for (int i = 0; i < engines.size(); i++)
		{
			thrusts.add(engines.get(i).getThrust());
		}
		if (SRBINDEX == Integer.MAX_VALUE)
		{
			Float srbThrust = Collections.max(thrusts);
			SRBINDEX = thrusts.indexOf(srbThrust);
			return srbThrust;
		} else
		{
			return thrusts.get(SRBINDEX);
		}

	}

	private void stage() throws RPCException
	{
		vesselControl.activateNextStage();
	}

	private boolean findLaunchCorridor(final double targOrbitApoKM, final double targOrbitPerKM, final double orbitInclination, final boolean timedLaunch) throws RPCException
	{

		targApoM = targOrbitApoKM * 1000;
		inclination = orbitInclination;
		double tempAngle = 0;

		if (timedLaunch)
		{
			Vessel targetVessel = spaceCenter.getTargetVessel();
			CelestialBody targetBody = spaceCenter.getTargetBody();

			if (targetVessel != null)
			{
				tempAngle = FastMath.cos(targetVessel.getOrbit().getInclination()
						/ FastMath.cos(vessel.flight(vessel.getOrbitalReferenceFrame()).getLatitude()));

				if (debug)
				{

				}

			} else if (targetBody != null)
			{

			}

			if (tempAngle > 1)
			{
				System.out.println("Direct Launch Impossible.");
				System.out.println("Will Launch at closest point.");

			} else
			{

			}
		}

		else
		{
			if (FastMath.abs(inclination) < vessel.flight(vessel.getOrbitalReferenceFrame()).getLatitude())
			{
				System.out.println("Error Inclination is below current");
				return false;
			}
			tempAngle = FastMath.cos(
					orbitInclination / FastMath.cos(vessel.flight(vessel.getOrbitalReferenceFrame()).getLatitude()));
			if (tempAngle > 1)
			{
				System.out.println("Direct Launch Impossible");
			} else
			{
				heading = FastMath.asin(tempAngle);

				double veq = (2 * FastMath.PI * body.getEquatorialRadius()) / body.getRotationalPeriod();

				// set ospeed to sqrt(constant:G*body:mass/(body:radius+tgtapo)).
				double ospeed = FastMath.sqrt(6.6743e-11 * body.getMass() / (body.getEquatorialRadius() + targApoM));
				double vx = ospeed * FastMath.sin(heading)
						- veq * FastMath.cos(vessel.flight(vessel.getOrbitalReferenceFrame()).getLatitude());

				double vy = ospeed * FastMath.cos(heading);
				heading = FastMath.atan(vx / vy);
				heading = FastMath.toDegrees(heading);
				vesselControl.setThrottle(0.90f);

			}
		}

		return true;
	}

	private void countdown() throws InterruptedException, RPCException
	{

		vesselControl.setLights(true);
		vesselControl.setAbort(false);
		vesselControl.setThrottle(0.90f);

		smartASS.update(true);
		for (int i = 10; i > 0; i--)
		{
			flightUI.updateGUI();
			System.out.println("T-minus " + i);
			vesselControl.setThrottle(0.90f);
			if (i == 7)
			{
				System.out.println("Main Engine Start");
				stage();
			}

			TimeUnit.SECONDS.sleep(1);
		}
		System.out.println("T " + 0);
		stage();

	}

	private void launch() throws RPCException, InterruptedException, IOException, StreamException
	{
		vesselControl.setAbort(false);
		vesselControl.setThrottle(1f);
		roll = 0;
		pitch = 90;
		Stream<Double> metStream = connection.addStream(vessel, "getMET");

		boolean headsDown = false;
		double oldMET = 0;
		double MET = metStream.get();
		boolean SRB = true;
		vesselState vs = vesselState.verticalAscent;

		while (keepRunning)
		{

			MET = metStream.get();

			if (vesselControl.getAbort())
			{
				break;
			}

			if (MET - oldMET >= 1)
			{
				flightUI.updateGUI();
				// findFlightDirectionalAngles();

				// shipStatus();
				smartASS.setSurfaceRoll(roll);

				switch (vs) {
				case verticalAscent: {
					if (MET > 6)
					{
						//smartASS.setSurfaceHeading(heading);
						vs = vesselState.rollOver;
					}
					break;
				}

				case rollOver: {
					if (roll < 180)
					{
						// smartASS.setSurfaceRoll(roll);
						roll += maxRollRate;
					} else
					{
						smartASS.setSurfaceRoll(180);
						smartASS.setSurfaceHeading(heading);
						roll = 180;
					}
					if (pitch > 78)
					{
						pitch -= maxPitchRate;
					} else
					{
						smartASS.setSurfacePitch(78);
						pitch = 78;
					}
					if (roll >= 180 && pitch <= 78)
					{
						smartASS.setSurfaceHeading(heading);
						vs = vesselState.pitchTo45;
						headsDown = true;
					}
					break;
				}

				case pitchTo45: {

					if (headsDown)
					{
						if (maxQ)
						{
							maxQThrustMod();
						} else
						{
							thrustChange(1.0f);
						}
						if (pitch > MINIMUMPITCH)
						{
							pitch -= maxPitchRate;
						} else
						{
							pitch = MINIMUMPITCH;
						}

					}

					break;
				}
				default:
					break;
				}

				if (MET > 7)
				{
					if (SRB)
					{

						if (provideCurrentSRBThrust() / SRBmaxThrust <= 0.03f)
						{

							stage();
							SRB = false;

						}
					} else
					{
//						//TODO prepare for handover
//						String[] str = { Double.toString(targApoM) };
//						connection.close();
//						PEG.main(str);
						
						
						//flightUI.setFlightMode(FlightMode.upfg);
						new PEG(targApoM, flightUI);
						keepRunning = false;
						break;
					}

				}
				smartASS.setSurfacePitch(pitch);
				smartASS.setSurfaceRoll(roll);

				oldMET = MET;

				smartASS.update(true);

			}
			TimeUnit.MILLISECONDS.sleep(125);
		}
		connection.close();
	}

	private enum vesselState {
		verticalAscent, rollOver, pitchTo45, pitchTo30, terminalApoRaise
	}

	private void maxQThrustMod() throws RPCException, StreamException
	{
		double oldMET = 0;

		Stream<Float> q = connection.addStream(vessel.flight(refFrame), "getDynamicPressure");
		
		
		
		if(q.get() > 2.9e4 && maxQ) 
		{
			flightUI.setFlightMode(FlightMode.maxQ);
			maxQ = false;
			thrustChange(0.1f);
			maxPitchRate = 1f;
		}
		while (q.get() > 2.9e4)
		{
			Stream<Double> metStream = connection.addStream(vessel, "getMET");

			double MET = metStream.get();


			if (MET - oldMET >= 0.125)
			{
				try
				{
					flightUI.updateGUI();
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				smartASS.setSurfacePitch(pitch);


				//thrustChange(0.1f);
				smartASS.update(false);
				pitch -= 0.1;

				oldMET = MET;
				
			}
		}
		flightUI.setFlightMode(FlightMode.preprogramed);
	}

	private void thrustChange(float throttle) throws RPCException
	{
		vesselControl.setThrottle(throttle);
	}

	private Triplet<Double, Double, Double> vectorScalarMultiplication(double scalar, Triplet<Double, Double, Double> vector)
	{

		Triplet<Double, Double, Double> toReturn = vector;

		toReturn.setAt0(vector.getValue0() * scalar).setAt1(vector.getValue1() * scalar)
		.setAt2(vector.getValue2() * scalar);

		return toReturn;

	}

	@SuppressWarnings({ "unused", "null" })
	private Triplet<Double, Double, Double> vectorSubtraction(Triplet<Double, Double, Double> v1, Triplet<Double, Double, Double> v2)
	{

		Triplet<Double, Double, Double> toReturn = null;

		double x1 = v1.getValue0();
		double y1 = v1.getValue1();
		double z1 = v1.getValue2();

		double x2 = v2.getValue0();
		double y2 = v2.getValue1();
		double z2 = v2.getValue2();

		toReturn.setAt0(x1 - x2).setAt1(y1 - y2).setAt2(z1 - z2);

		return toReturn;

	}

	@SuppressWarnings("unused")
	private Triplet<Double, Double, Double> makeIntoUnit(Triplet<Double, Double, Double> nonNormalized)
	{
		Triplet<Double, Double, Double> normalized = null;

		double vectorMagScaled = FastMath.sqrt(magnitude(nonNormalized));

		normalized = vectorScalarMultiplication(1 / vectorMagScaled, nonNormalized);

		return normalized;

	}

	private double magnitude(Triplet<Double, Double, Double> vector)
	{

		double magnitude = FastMath.sqrt(dotProduct(vector, vector));

		return magnitude;
	}

	private double dotProduct(Triplet<Double, Double, Double> v1, Triplet<Double, Double, Double> v2)
	{
		double xdot = v1.getValue0() * v2.getValue0();
		double ydot = v1.getValue1() * v2.getValue1();
		double zdot = v1.getValue2() * v2.getValue2();

		return xdot + ydot + zdot;

	}

}
