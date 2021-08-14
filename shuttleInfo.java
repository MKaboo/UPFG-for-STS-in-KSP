package shuttleGuidance.reentry;

import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter.Control;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.Node;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;

public class shuttleInfo {

	double AOA = 40; // pitch
	final double MAXALPHAMODULATION = 3d;
	final double PitchDownAlt = 35000d;

	private double heading;
	private double bankAngle = 0; // roll around velocity
	private EntryConditions ec = null;
	private Flight flight;
	private Vessel vessel;
	private ReferenceFrame referenceFrame;
	private Stream<Double> latitudeStream;
	private Connection connection;
	private Stream<Double> longitudeStream;
	private Stream<Double> altitudeStream;
	private Control vesselControl;
	private double deorbitPE;
	private double deorbitDistance;
	private boolean headCorrect = false;
	private double degDist;
	
	
	private double bounce = 0;
	
	protected Node node;
	protected boolean sTurn = true;
	private Stream<Double> inclinationStream;

	/**
	 * @param vessel
	 * @param referenceFrame
	 */
	protected shuttleInfo(Connection connection, Vessel vessel, ReferenceFrame referenceFrame) {
		this.connection = connection;
		this.vessel = vessel;
		this.referenceFrame = referenceFrame;
		setEc(EntryConditions.standby);
		setFlight();
		try
		{
			vesselControl = vessel.getControl();
			deorbitPE = 30000. - (vessel.getMass() * 400.);

			deorbitDistance = 17960 + (7.5 * FastMath.toDegrees(vessel.getOrbit().getInclination()))
					+ ((vessel.getMass() - 83) * 25.);
			System.out.println(FastMath.toDegrees(vessel.getOrbit().getInclination()));
			System.out.println(deorbitDistance);
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected EntryConditions getEc()
	{
		return ec;
	}

	private void setFlight()
	{
		try
		{
			flight = vessel.flight(referenceFrame);

			latitudeStream = connection.addStream(flight, "getLatitude");
			latitudeStream.start();
			longitudeStream = connection.addStream(flight, "getLongitude");
			longitudeStream.start();
			altitudeStream = connection.addStream(flight, "getMeanAltitude");
			altitudeStream.start();
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (RPCException | StreamException | InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void setNode(Node thisNode)
	{
		node = thisNode;
	}

	protected void setEc(EntryConditions ec)
	{
		this.ec = ec;
	}

	protected double getShuttleLatitude()
	{
		try
		{
			return latitudeStream.get();
		} catch (RPCException | StreamException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	protected double getShuttleLongitude()
	{
		try
		{
			return longitudeStream.get();
		} catch (RPCException | StreamException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	protected double getShuttleAltitude()
	{
		try
		{
			return altitudeStream.get();
		} catch (RPCException | StreamException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	protected double getDeorbitPE()
	{
		return deorbitPE;
	}

	protected double getDirection(double lat1, double lon1, double lat2, double lon2)
	{
		double φ1 = lat1 * FastMath.PI / 180.0;
		double φ2 = lat2 * FastMath.PI / 180.0;
		double λ1 = lon1 * FastMath.PI / 180.0;
		double λ2 = lon2 * FastMath.PI / 180.0;

		double y = FastMath.sin(λ1 - λ1) * FastMath.cos(φ2);
		double x = FastMath.cos(φ1) * FastMath.sin(φ2) - FastMath.sin(φ1) * FastMath.cos(φ2) * FastMath.cos(λ2 - λ1);
		double θ = FastMath.atan2(y, x);
		double brng = (θ * 180.0 / FastMath.PI + 360.0) % 360.0; // in degrees

		return brng;
	}

	protected double getDistance(double lat1, double lon1, double lat2, double lon2, double rad)
	{
		double φ1 = lat1 * FastMath.PI / 180.0;
		double φ2 = lat2 * FastMath.PI / 180.0;
		double Δφ = (lat2 - lat1) * FastMath.PI / 180.0;
		double Δλ = (lon2 - lon1) * FastMath.PI / 180.0;
		double a = FastMath.sin(Δφ / 2.0) * FastMath.sin(Δφ / 2.0)
				+ FastMath.cos(φ1) * FastMath.cos(φ2) * FastMath.sin(Δλ / 2.0) * FastMath.sin(Δλ / 2.0);
		double c = 2.0 * FastMath.atan2(FastMath.sqrt(a), FastMath.sqrt(1 - a));
		double d = rad * c;
		return d;

	}

	protected Vector3D eastFor()
	{
		Vector3D vector3d = new Vector3D(0, 0, 1.0);
		return vector3d;
		
	}
	protected double pitchFor() 
	{
		Vector3D foreZAxis = new Vector3D(0, 1.0, 0);
		return 90 - FastMath.toDegrees(Vector3D.angle(foreZAxis,foreZAxis));
	}
	protected double compassFor()
	{

		Vector3D pointing = new Vector3D(0, 1.0, 0);
		Vector3D east = eastFor();
		
		double trigX = Vector3D.dotProduct(pointing, pointing);
		double trigY = Vector3D.dotProduct(east, pointing);

		double result = FastMath.atan2(trigY, trigX);
		
		if (result < 0) { 
			return 360 + result;
		} else {
			return result;
		}
	}
	
	protected void directCalc()
	{
		try
		{
			double currentPitch = flight.getPitch();
			//double currentYaw = flight.
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	public Control getVesselControl()
	{
		return vesselControl;
	}

	public double getDeorbitDistance()
	{
		return deorbitDistance;
	}

	/**
	 * @return the bounce
	 */
	public double getBounce()
	{
		return bounce;
	}

	/**
	 * @param bounce the bounce to set
	 */
	public void setBounce(double bounce)
	{
		this.bounce = bounce;
	}

	/**
	 * @return the headCorrect
	 */
	public boolean isHeadCorrect()
	{
		return headCorrect;
	}

	/**
	 * @param headCorrect the headCorrect to set
	 */
	public void setHeadCorrect(boolean headCorrect)
	{
		this.headCorrect = headCorrect;
	}

	enum EntryConditions {
		standby, deorbit, reentry, approach, landing
	}

}
