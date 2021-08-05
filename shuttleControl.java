package shuttleGuidance.reentry;

import krpc.client.RPCException;
import krpc.client.services.MechJeb;
import krpc.client.services.MechJeb.SmartASS;
import krpc.client.services.MechJeb.SmartASSAutopilotMode;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;

class shuttleControl 
{
	private Vessel vessel;
	
	private MechJeb mj;
	
	private ReferenceFrame referenceFrame = null;
	private SmartASS smartASS;
	
	shuttleControl(final Vessel vessel, final MechJeb mj, final ReferenceFrame referenceFrame) {
		super();
		this.vessel = vessel;
		this.mj = mj;
		this.referenceFrame = referenceFrame;
		try
		{
			mj.getSmartASS();
			
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	private void setMechjebConditions(SmartASSAutopilotMode assAutopilotMode)
	{
		try
		{
			smartASS.setAutopilotMode(assAutopilotMode);
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	private void steering()
	{
		
	}
	
	
}
