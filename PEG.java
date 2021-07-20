package flightComputer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.FastMath;
import org.javatuples.Triplet;

import flightComputer.FlightUI.FlightMode;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.KRPC;
import krpc.client.services.MechJeb;
import krpc.client.services.MechJeb.ManeuverPlanner;
import krpc.client.services.MechJeb.OperationCircularize;
import krpc.client.services.MechJeb.SmartASS;
import krpc.client.services.MechJeb.SmartASSAutopilotMode;
import krpc.client.services.MechJeb.SmartASSInterfaceMode;
import krpc.client.services.MechJeb.TimeReference;
import krpc.client.services.MechJeb.TimeSelector;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Control;
import krpc.client.services.SpaceCenter.Engine;
import krpc.client.services.SpaceCenter.Node;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;

public class PEG {

	private static final float MAX_G_FORCE = 3.0f;

	private static final float G_MARGIN = 0.1f;

	private static final float THROTTLE_STEP = 0.01f;

	private static double T = 400., T_MINUS;
	private static double A = 0, A_MINUS = 0;
	private static double B = 0, B_MINUS = 0;
	private static double burnoutRadius;
	private static Vector3D rVec = new Vector3D(0, 0, 0), vVec = new Vector3D(0, 0, 0);
	private static Connection connection;
	private double exhaustVelo = 0;
	private double tau;
	private static double cutoffVecticalSpeed;
	private static double cutoffTangentSpeed;
	private static Vessel vessel;
	private static double parkingApoM = 0;
	private boolean upfgConvergedFlag;

	private boolean firstGo = true;

	private boolean steadyPitchDown = false;

	private boolean allowPitch = true;

	private Control vesselControl;

	private double accel;

	private boolean direction = true;
	private static double mu;

	private static double rMag;
	private static double oldMET = 0;
	private static double newMET;
	private static double pitch;// , roll, heading;
	private static MechJeb mj;

	private static SmartASS smartASS;

	private static SpaceCenter spaceCenter;

	private FlightUI flightUI;

	public PEG(double apoAlt, final FlightUI flightUI) {
		this.flightUI = flightUI;
		this.flightUI.setFlightMode(FlightMode.upfg);

		run(apoAlt);

//		String[] str = { Double.toString(apoAlt) };

//		try
//		{
//			main(str);
//		} catch (RPCException | IOException | InterruptedException | StreamException e)
//		{
//			e.printStackTrace();
//		}
	}

	public PEG(Vessel vessel, ReferenceFrame referenceFrame)
			throws RPCException, InterruptedException, StreamException, IOException {

		vesselControl = vessel.getControl();
		Stream<Triplet<Double, Double, Double>> positionStream = connection.addStream(vessel, "position",
				referenceFrame);
		Stream<Triplet<Double, Double, Double>> velocityStream = connection.addStream(vessel, "velocity",
				referenceFrame);

		rVec = toV3D(positionStream.get());
		vVec = toV3D(velocityStream.get());
		rMag = FastMath.sqrt(Vector3D.dotProduct(rVec, rVec));

		vehiclePreformance();

		ReferenceFrame obtFrame = vessel.getOrbit().getBody().getNonRotatingReferenceFrame();

		Stream<Double> metStream = connection.addStream(vessel, "getMET");

		double deltaV = 0;
		while (true)
		{
			newMET = metStream.get();
			double deltaT = newMET - oldMET;

			// guidance

			if (firstGo)
			{
				deltaT = 0;
				firstGo = false;
			}

			if (deltaT >= 0.125)
			{

				if (T > 36)
				{

					setAandB();

					// update
					A_MINUS = A + deltaT * B;
					B_MINUS = B;
					T_MINUS = T - deltaT;

				} else
				{
					if (upfgConvergedFlag)
					{
						if (!vessel.getControl().getRCS())
						{
							vessel.getControl().setRCS(true);
						}
						allowPitch = false;
						terminalGuidance(vessel, referenceFrame, positionStream, velocityStream, deltaV);

						newMET = metStream.get();
						oldMET = newMET;
						while (FastMath.abs(newMET - oldMET) <= 10)
						{
							newMET = metStream.get();
							TimeUnit.MILLISECONDS.sleep(100);
						}
						evasionAndCircularization();
						break;
					}

				}

				rVec = toV3D(positionStream.get());
				vVec = toV3D(velocityStream.get());

				rMag = FastMath.sqrt(Vector3D.dotProduct(rVec, rVec));

				double[] flightInfo = calcDeltaVandT();

				deltaV = flightInfo[0];

				double Tnew = flightInfo[1];

				double fTheta = flightInfo[2];
				if (FastMath.abs(Tnew - T) < deltaT * 2.0)
				{
					upfgConvergedFlag = true;
				}

				oldMET = newMET;
				T = Tnew;

				steering(fTheta, obtFrame);

				flightUI.setTGo(T);
				flightUI.updateGUI();
			}

			if (vessel.getControl().getAbort())
			{
				break;
			}

			smartASS.update(false);
			TimeUnit.MILLISECONDS.sleep(100);
		}
	}

	private void fly(ReferenceFrame referenceFrame)
	{
		try
		{
			vesselControl = vessel.getControl();
			Stream<Triplet<Double, Double, Double>> positionStream = connection.addStream(vessel, "position",
					referenceFrame);
			Stream<Triplet<Double, Double, Double>> velocityStream = connection.addStream(vessel, "velocity",
					referenceFrame);
			// init
			rVec = toV3D(positionStream.get());
			vVec = toV3D(velocityStream.get());
			rMag = FastMath.sqrt(Vector3D.dotProduct(rVec, rVec));

			vehiclePreformance();

			ReferenceFrame obtFrame = vessel.getOrbit().getBody().getNonRotatingReferenceFrame();

			Stream<Double> metStream = connection.addStream(vessel, "getMET");

			double deltaV = 0;
			while (true)
			{
				newMET = metStream.get();
				double deltaT = newMET - oldMET;
				// guidance

				if (firstGo)
				{
					deltaT = 0;
					firstGo = false;
				}

				if (deltaT >= 0.125)
				{

					if (T > 36)
					{

						setAandB();

						// update
						A_MINUS = A + deltaT * B;
						B_MINUS = B;
						T_MINUS = T - deltaT;

					} else
					{
						if (upfgConvergedFlag)
						{
							if (!vessel.getControl().getRCS())
							{
								vessel.getControl().setRCS(true);
							}
							allowPitch = false;
							terminalGuidance(vessel, referenceFrame, positionStream, velocityStream, deltaV);

							newMET = metStream.get();
							oldMET = newMET;
							while (FastMath.abs(newMET - oldMET) <= 10)
							{
								newMET = metStream.get();
								TimeUnit.MILLISECONDS.sleep(100);
							}
							evasionAndCircularization();
							break;
						}

					}

					rVec = toV3D(positionStream.get());
					vVec = toV3D(velocityStream.get());

					rMag = FastMath.sqrt(Vector3D.dotProduct(rVec, rVec));

					double[] flightInfo = calcDeltaVandT();

					deltaV = flightInfo[0];

					double Tnew = flightInfo[1];

					double fTheta = flightInfo[2];
					if (FastMath.abs(Tnew - T) < deltaT * 2.0)
					{
						upfgConvergedFlag = true;
					}

					oldMET = newMET;
					T = Tnew;

					steering(fTheta, obtFrame);

					flightUI.setTGo(T);
					flightUI.updateGUI();
				}

				if (vessel.getControl().getAbort())
				{
					// connection.close();
					break;
				}

				smartASS.update(false);
				TimeUnit.MILLISECONDS.sleep(100);
			}
		} catch (RPCException | StreamException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	private void steering(double fTheta, ReferenceFrame obtFrame) throws RPCException
	{
		double targetPitch = FastMath.toDegrees(FastMath.acos(fTheta));

		if (targetPitch <= 1.0)
		{
			if (pitch > 0)
			{
				direction = false;
			} else
			{
				direction = true;
			}
			targetPitch = 0;
		}

		double obtSpeed = vessel.flight(obtFrame).getSpeed();
		if (obtSpeed >= 3400. && obtSpeed <= 6500.)
		{
			steadyPitchDown = true;
		} else
		{
			steadyPitchDown = false;
		}
		if (obtSpeed >= 7000.)
		{
			steadyPitchDown = false;
			direction = true;
		}

		if (upfgConvergedFlag)
		{
			if (!direction || steadyPitchDown)
			{
				targetPitch *= -1.0;
			}

			if (allowPitch)
			{
				pitch = targetPitch;
				smartASS.setSurfacePitch(targetPitch);
			}
		} else
		{
			System.err.println("UPFG NOT CONVERGED");
		}
	}

	private void setAandB()
	{
		double rdot = rVec.dotProduct(vVec) / rMag;
		double b0 = bSubN(0, exhaustVelo, T, tau);
		double b1 = bSubN(1, exhaustVelo, T, tau);
		double c0 = cSubN(0, exhaustVelo, T, tau);
		double c1 = cSubN(1, exhaustVelo, T, tau);

		double[][] arraytomat = { { b0, b1 }, { c0, c1 } };

		RealMatrix Ma = new Array2DRowRealMatrix(arraytomat);

		double[][] arraytomat2 = { { cutoffVecticalSpeed - rdot }, { burnoutRadius - rMag - rdot * T } };

		RealMatrix Mb = new Array2DRowRealMatrix(arraytomat2);
		RealMatrix MaPrime = new LUDecomposition(Ma).getSolver().getInverse();

		// solve for A and B
		RealMatrix Mx = MaPrime.multiply(Mb);
		A = Mx.getEntry(0, 0);
		B = Mx.getEntry(1, 0);
	}

	private void evasionAndCircularization() throws RPCException, InterruptedException, StreamException
	{
		Stream<Double> metStream = connection.addStream(vessel, "getMET");
		vesselControl.setThrottle(0);
		vesselControl.setActionGroup(1, true);

		smartASS.setSurfacePitch(0);
		smartASS.setSurfaceRoll(180);
		smartASS.update(false);

		vesselControl.setUp(-0.5f);
		rcsHoldLoop(metStream, vessel, 6, 100);

		vesselControl.setUp(0);
		stage();
		vesselControl.setActionGroup(0, true);
		stage();

		vesselControl.setUp(1f);
		rcsHoldLoop(metStream, vessel, 6, 100);
		vesselControl.setForward(1f);
		rcsHoldLoop(metStream, vessel, 4, 100);
		vesselControl.setUp(0f);
		vesselControl.setThrottle(1.0f);
		vesselControl.setForward(0f);

		rcsHoldLoop(metStream, vessel, 10, 100);
		vesselControl.setThrottle(0f);

		rcsHoldLoop(metStream, vessel, 10, 1000);
		vesselControl.toggleActionGroup(2);
		smartASS.setSurfaceRoll(0);
		smartASS.update(false);

		rcsHoldLoop(metStream, vessel, 10, 1000);
//		while(FastMath.abs(currentTime-oldTime) <= 10) 
//		{
//			
//			currentTime = vessel.getMET();
//			TimeUnit.MILLISECONDS.sleep(100);
//			
//		}
//		oldTime = currentTime;

//		NodeExecuterComputer nec = new NodeExecuterComputer(connection);
		// OperationCircularize oc = new OperationCircularize(connection, 0);

		ManeuverPlanner maneuverPlanner = mj.getManeuverPlanner();

		OperationCircularize oc = maneuverPlanner.getOperationCircularize();

		TimeSelector ts = oc.getTimeSelector();
		ts.setCircularizeAltitude(parkingApoM);
		TimeReference reference = TimeReference.ALTITUDE;
		ts.setTimeReference(reference);
		oc.makeNodes();
		//rcsHoldLoop(metStream, vessel, (5 * 60), 1000);

		// nec.executeNode();

		if (exeCircNode(metStream))
		{
			System.out.println("Welcome to orbit!");
		}

	}

	private boolean exeCircNode(Stream<Double> stream)
	{

		try
		{
			ArrayList<Engine> omsEngines = new ArrayList<>();
			for (Engine engine : vessel.getParts().getEngines())
			{
				double thrust = engine.getMaxThrust();

				if (thrust > 1.0e4 && thrust < 1.0e5)
				{

					omsEngines.add(engine);

				}
			}

			double currentEnginesCumlativeThrust = 0;
			for (int i = 0; i < omsEngines.size(); i++)
			{
				currentEnginesCumlativeThrust += omsEngines.get(i).getMaxThrust();
			}

			double vesselMass = vessel.getMass();

			double acceleration = currentEnginesCumlativeThrust / vesselMass;

			Node node = vessel.getControl().getNodes().get(0);

			double nodeDeltaV = node.getDeltaV();

			double burnTime = nodeDeltaV / acceleration;

			double halfBT = burnTime / 2.0;
			double halfBTPlusRotationMargin = halfBT + 30.0;

			double nodeTime = node.getUT();
			double nodeTimeBurn = nodeTime - halfBT;
			double nodeTimePlusMargin = nodeTime - halfBTPlusRotationMargin;
			Stream<Double> ut = connection.addStream(SpaceCenter.class, "getUT");

			boolean allowWarp = true;

			if (allowWarp)
			{
				spaceCenter.warpTo(nodeTimePlusMargin - 10, 1, 3);
			}

			while (ut.get() < nodeTimePlusMargin)
			{
				TimeUnit.MILLISECONDS.sleep(500);
			}

			smartASS.setAutopilotMode(SmartASSAutopilotMode.NODE);
			smartASS.setInterfaceMode(SmartASSInterfaceMode.SURFACE);
			smartASS.update(false);

			rcsHoldLoop(stream, vessel, 5, 500);

			while (ut.get() < nodeTimeBurn)
			{
				TimeUnit.MILLISECONDS.sleep(250);
			}

			vesselControl.setThrottle(1.0f);

			while (node.getRemainingDeltaV() >= 5.0)
			{

				TimeUnit.MILLISECONDS.sleep(500);

			}
			node.remove();
			vesselControl.setThrottle(0);
			smartASS.setAutopilotMode(SmartASSAutopilotMode.SURFACE_PROGRADE);
			smartASS.setInterfaceMode(SmartASSInterfaceMode.SURFACE);
			smartASS.setSurfaceVelRoll(0);
			smartASS.update(false);
			rcsHoldLoop(stream, vessel, 10, 500);

			smartASS.setAutopilotMode(SmartASSAutopilotMode.PROGRADE);
			smartASS.setInterfaceMode(SmartASSInterfaceMode.ORBITAL);
			smartASS.update(false);
			rcsHoldLoop(stream, vessel, 20, 500);
			vesselControl.setRCS(false);

		} catch (RPCException e)
		{
			e.printStackTrace();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		} catch (StreamException e)
		{
			e.printStackTrace();
		}

		return true;
	}

	private void rcsHoldLoop(final Stream<Double> stream, final Vessel vessel, final double timeDif, final long sleepAmount) throws RPCException, InterruptedException, StreamException
	{
		double currentTime = stream.get();
		double oldTime = currentTime;
		while (FastMath.abs(currentTime - oldTime) <= timeDif)
		{

			currentTime = stream.get();
			TimeUnit.MILLISECONDS.sleep(sleepAmount);

		}

	}

	private void terminalGuidance(final Vessel vessel, final ReferenceFrame referenceFrame, final Stream<Triplet<Double, Double, Double>> positionStream, final Stream<Triplet<Double, Double, Double>> velocityStream, final double deltaV) throws RPCException, StreamException, InterruptedException
	{
		stage();

		smartASS.setSurfacePitch(25.0);

		smartASS.update(false);
		while (vessel.getOrbit().getApoapsisAltitude() < parkingApoM)
		{
			TimeUnit.MILLISECONDS.sleep(100);
		}
		vesselControl.setThrottle(0.01f);
		smartASS.setSurfacePitch(0);
		smartASS.update(false);
		vesselControl.setThrottle(0);

	}

	private double[] calcDeltaVandT() throws RPCException
	{
		Vector3D rHat = rVec.normalize();
		Vector3D hVec = rVec.crossProduct(vVec);
		Vector3D hHat = hVec.normalize();

		double h = FastMath.sqrt(hVec.dotProduct(hVec));
		Vector3D thetaHat = hHat.crossProduct(rHat);
		double hT = cutoffTangentSpeed * burnoutRadius;
		double deltaH = hT - h;
		double rBar = (burnoutRadius + rMag) / 2.0;
		// double rdot = vVec.dotProduct(rHat);
		// double vTheta = vVec.dotProduct(thetaHat);

		vehiclePreformance();

		double omega = vVec.dotProduct(thetaHat) / rMag;
		double omegaT = vVec.dotProduct(thetaHat) / burnoutRadius;
		double C = (mu / FastMath.pow(rMag, 2) - FastMath.pow(omega, 2) * rMag) / accel;
		double fR = A_MINUS + C;
		double Ct = (mu / FastMath.pow(burnoutRadius, 2) - FastMath.pow(omegaT, 2) * burnoutRadius);
		Ct /= accelerationAtT(T_MINUS, vessel, accel, tau);

		double fRT = A_MINUS + B_MINUS * T_MINUS + Ct;
		double fRDot = (fRT - fR) / T_MINUS;

		double fTheta = 1.0 - (FastMath.pow(fR, 2) / 2.0);

		double fThetaDot = -1.0 * fR * fRDot;
		double fThetaDotDot = -1.0 * FastMath.pow(fThetaDot, 2) / 2.0;

		double deltaV = ((deltaH / rBar) + (exhaustVelo * T * (fThetaDot + fThetaDotDot * tau))
				+ ((fThetaDotDot * exhaustVelo * FastMath.pow(T, 2)) / 2.0));
		deltaV /= (fTheta + fThetaDot * tau + fThetaDotDot * FastMath.pow(tau, 2));

		double Tnew = tau * (1.0 - FastMath.exp(-1.0 * deltaV / exhaustVelo));

		return new double[] { deltaV, Tnew, fTheta, fRT };
	}

	private void vehiclePreformance() throws RPCException
	{
		double isp = 0.;
		ArrayList<Engine> mainEngines = new ArrayList<>();
		for (Engine engine : vessel.getParts().getEngines())
		{
			double thrust = engine.getMaxThrust();
			if (thrust > 1.0e6 && thrust < 1.0e7)
			{
				isp = engine.getSpecificImpulse();
				mainEngines.add(engine);

			}
		}
		exhaustVelo = isp * vessel.getOrbit().getBody().getSurfaceGravity();
		double currentEnginesCumlativeThrust = 0;
		for (int i = 0; i < mainEngines.size(); i++)
		{
			currentEnginesCumlativeThrust += mainEngines.get(i).getThrust();
		}

		accel = acceleration(vessel, currentEnginesCumlativeThrust);
		maintain3G(accel);

		tau = calcTau(exhaustVelo, vessel, accel);

	}

	private void stage() throws RPCException
	{
		vesselControl.activateNextStage();
	}

	private void run(double apoAlt)
	{
		try
		{
			connection = Connection.newInstance("PEG");
			KRPC.newInstance(connection);

			spaceCenter = SpaceCenter.newInstance(connection);
			vessel = spaceCenter.getActiveVessel();

			targetOrbitSetup(apoAlt, 90., 110., true);

			mu = vessel.getOrbit().getBody().getGravitationalParameter();

			ReferenceFrame referenceFrame = vessel.getOrbit().getBody().getReferenceFrame();

			mj = MechJeb.newInstance(connection);
			smartASS = mj.getSmartASS();
			smartASS.setForcePitch(true);
			smartASS.setForceYaw(true);
			smartASS.setForceRoll(true);
			smartASS.setInterfaceMode(SmartASSInterfaceMode.SURFACE);
			smartASS.setAutopilotMode(SmartASSAutopilotMode.SURFACE);

			pitch = smartASS.getSurfacePitch();
			smartASS.update(false);

			fly(referenceFrame);

			connection.close();
		} catch (IOException | RPCException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	private static Vector3D toV3D(Triplet<Double, Double, Double> triplet)
	{
		return new Vector3D(triplet.getValue0().doubleValue(), triplet.getValue1().doubleValue(),
				triplet.getValue2().doubleValue());

	}

	private static double acceleration(Vessel vessel, double thrust) throws RPCException
	{
		// F = ma
		// A = F/m
		return thrust / vessel.getMass();
	}

	private static double calcTau(double exhaustVelo, Vessel vessel, double accel) throws RPCException
	{
		return exhaustVelo / accel;
	}

	private static double accelerationAtT(double time, Vessel vessel, double accel, double tau)
	{
		return accel / (1 - time / tau);
	}

	private static double bSubN(int n, double exhaustVelo, double bigT, double tau)
	{
		if (n > 0)
		{
			return (bSubN(n - 1, exhaustVelo, bigT, tau) * tau) - ((exhaustVelo * FastMath.pow(bigT, n)) / (double) n);
		} else
		{
			return -1. * exhaustVelo * FastMath.log(1 - (bigT / tau));
		}
	}

	private static double cSubN(int n, double exhaustVelo, double bigT, double tau)
	{
		if (n > 0)
		{
			return (cSubN(n - 1, exhaustVelo, bigT, tau) * tau)
					- ((exhaustVelo * FastMath.pow(bigT, n + 1)) / ((double) n * (n + 1)));
		} else
		{
			return bSubN(0, exhaustVelo, bigT, tau) * bigT - bSubN(1, exhaustVelo, bigT, tau);
		}
	}

	private void maintain3G(double accel) throws RPCException
	{
		maintain3G();

	}

	private void maintain3G() throws RPCException
	{
		ReferenceFrame referenceFrame = vessel.getOrbit().getBody().getReferenceFrame();
		double gs = vessel.flight(referenceFrame).getGForce();
		float throttle = vesselControl.getThrottle();

		if (gs > MAX_G_FORCE - G_MARGIN)
		{
			throttle -= THROTTLE_STEP;
		} else
		{
			throttle += THROTTLE_STEP;
		}

		if (throttle <= 0f)
		{
			throttle = 0.1f;
		}
		vesselControl.setThrottle(throttle);
	}

	public static void targetOrbitSetup(final double targetApo, final double targetPer, final double cutoffAlt, final boolean debug) throws RPCException, InterruptedException
	{
		final double earthRadii = vessel.getOrbit().getBody().getEquatorialRadius();
		final double targetPerigee = (targetPer * 1000) + earthRadii;
		parkingApoM = targetApo;
		final double parkingApoMplusRadii = targetApo + earthRadii;
		final double cutoffAltM = (cutoffAlt * 1000) + earthRadii;

		mu = vessel.getOrbit().getBody().getGravitationalParameter();

		final double ellipseCenterOffset = (parkingApoMplusRadii - targetPerigee) / 2d;
		double delta = 1000;
		double a = ellipseCenterOffset + targetPerigee;
		double b = earthRadii;
		double c = FastMath.sqrt(FastMath.pow(a, 2) - FastMath.pow(b, 2));

		double tolerance = 1.0e6;
		for (int i = 0; i < 10; ++i)
		{
			delta = delta / 10.;
			tolerance = tolerance / 10.;

			while (true)
			{

				double newDif = FastMath.abs(ellipseCenterOffset - c);

				if (b + delta >= a)
				{
					delta /= 2d;

				}

				b += delta;
				c = FastMath.sqrt(FastMath.pow(a, 2) - FastMath.pow(b, 2));
				if (newDif <= tolerance)
				{
					break;
				}

			}
		}

		double v = FastMath.sqrt(mu * ((2d / cutoffAltM) - (1d / a)));
		double e = c / a;
		double h = FastMath.sqrt(a * (1 - FastMath.pow(e, 2)) * mu);
		double phi = FastMath.acos(h / (cutoffAltM * v));

		double vx = v * FastMath.cos(phi);
		double vy = v * FastMath.sin(phi);

		cutoffTangentSpeed = vx;
		cutoffVecticalSpeed = vy;
		burnoutRadius = cutoffAltM;

	}
}
