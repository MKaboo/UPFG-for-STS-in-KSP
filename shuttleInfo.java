package shuttleGuidance.reentry;

import java.util.concurrent.TimeUnit;

import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;

public class shuttleInfo {

	private double AOA; // pitch
	private final double MAXALPHAMODULATION = 3d;
	private double heading;
	private double bankAngle; // roll around velocity
	private EntryConditions ec = null;
	private Flight flight;
	private Vessel vessel;
	private ReferenceFrame referenceFrame;
	private Stream<Double> latitudeStream;
	private Connection connection;
	private Stream<Double> longitudeStream;
	private Stream<Double> altitudeStream;
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

	enum EntryConditions {
		standby, deorbit, reentry, approach, landing
	}

}
