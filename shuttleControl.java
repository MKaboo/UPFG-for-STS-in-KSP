package shuttleGuidance;

import krpc.client.RPCException;
import krpc.client.services.MechJeb;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;

class shuttleControl 
{
	private Vessel vessel;
	
	private MechJeb mj;
	private Flight flight;
	private ReferenceFrame referenceFrame = null;
	
	
	shuttleControl(final Vessel vessel, final MechJeb mj) {
		super();
		this.vessel = vessel;
		this.mj = mj;
		try
		{
			referenceFrame = vessel.getOrbit().getBody().getReferenceFrame();
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setFlight();
	}
	
	
	private void setFlight()
	{
		try
		{
			flight = vessel.flight(referenceFrame);
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setMechjebConditions()
	{
		
	}
	
	
	
	private void steering()
	{
		
	}
	
	
}
