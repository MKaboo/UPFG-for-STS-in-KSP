package shuttleGuidance.reentry;

import java.util.concurrent.TimeUnit;

import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.MechJeb;
import krpc.client.services.UI;
import krpc.client.services.MechJeb.SmartASS;
import krpc.client.services.MechJeb.SmartASSAutopilotMode;
import krpc.client.services.MechJeb.SmartASSInterfaceMode;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Control;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;
import krpc.client.services.UI.Canvas;
import krpc.client.services.UI.MessagePosition;
import shuttleGuidance.reentry.shuttleInfo.EntryConditions;

class shuttleControl {
	private Vessel vessel;

	private MechJeb mj;
	private Control vControl; 
	private ReferenceFrame referenceFrame = null;
	private SmartASS smartASS;

	shuttleControl(final Vessel vessel, final MechJeb mj, final ReferenceFrame referenceFrame) {
		super();
		this.vessel = vessel;
		this.mj = mj;
		this.referenceFrame = referenceFrame;
		try
		{
			vControl = vessel.getControl();
			smartASS = mj.getSmartASS();
			TimeUnit.SECONDS.sleep(2);
			
		} catch (RPCException | InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
//	protected void mainGuidanceLoop()
//	{
//		if()
//	}
	

	protected void setMechjebConditions(SmartASSAutopilotMode assAutopilotMode, SmartASSInterfaceMode interfaceMode)
	{
		try
		{
		
			smartASS.setAutopilotMode(assAutopilotMode);
			smartASS.setInterfaceMode(interfaceMode);

			try
			{
				smartASS.setForcePitch(true);
				smartASS.setForceRoll(true);
				smartASS.setForceYaw(true);
				smartASS.update(false);
			} catch (Exception e)
			{
				// TODO: handle exception
			}
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void deorbit(shuttleInfo si, SpaceCenter spaceCenter)
	{

		try
		{
			while(spaceCenter.getUT() < si.node.getUT()) 
			{
				TimeUnit.MILLISECONDS.sleep(100);
			}
			Control vesselControl = si.getVesselControl();
			vesselControl.setThrottle(1f);
			System.out.println(si.getDeorbitPE());
			while (vessel.getOrbit().getPeriapsisAltitude() > si.getDeorbitPE())
			{
				TimeUnit.MILLISECONDS.sleep(100);
			}
			vesselControl.setThrottle(0);

		} catch (RPCException | InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	private void reentry(shuttleInfo si)
	{
		
		try
		{
			setMechjebConditions(SmartASSAutopilotMode.SURFACE_PROGRADE, SmartASSInterfaceMode.SURFACE);
			smartASS.setSurfaceVelPitch(si.AOA);
			smartASS.setSurfaceVelRoll(0);
			smartASS.update(false);
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void execute(Connection connection, shuttleInfo si, SpaceCenter spaceCenter)
	{
		try
		{
			EntryConditions ecc = si.getEc();
			
			System.out.println(ecc);
			switch (ecc) {
			case standby:
				UI ui = UI.newInstance(connection);
				ui.message("Activate Action Group 3 to Close Cargobay Doors", 10, MessagePosition.TOP_CENTER,
						new Triplet<Double, Double, Double>(100., 100., 100.), 15f);
				setMechjebConditions(SmartASSAutopilotMode.SURFACE_RETROGRADE, SmartASSInterfaceMode.SURFACE);
				
				smartASS.setSurfaceVelRoll(180);
				smartASS.update(false);
				TimeUnit.SECONDS.sleep(1);

				break;
				
			case  deorbit:
				deorbit(si, spaceCenter);
				break;
				
			case  reentry:
				reentry(si);
				break;
			case  approach:
				break;
			case  landing:
				break;			
				
			default:
				throw new IllegalArgumentException();
			}
		} catch (RPCException | InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
