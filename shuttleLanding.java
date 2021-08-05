package shuttleGuidance.reentry;

import java.io.IOException;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.KRPC;
import krpc.client.services.MechJeb;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Vessel;
import shuttleGuidance.reentry.shuttleInfo.EntryConditions;

public class shuttleLanding {

	private Connection connection;
	private Vessel vessel;
	private MechJeb mj;
	private SpaceCenter spaceCenter;

	public shuttleLanding() {
		run();
	}

	public static void main(String[] args)
	{
		new shuttleLanding();
	}

	private void run()
	{
		initConnections();

		shuttleControl sc = new shuttleControl(vessel, mj);
		shuttleInfo sInfo = new shuttleInfo();

		EntryConditions ecc = sInfo.getEc();
		
		while (true)
		{
			
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
		} catch (IOException | RPCException e)
		{
			e.printStackTrace();
		}
	}

}
