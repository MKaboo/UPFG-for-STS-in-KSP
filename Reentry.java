package shuttleGuidance.reentry;

import java.util.concurrent.TimeUnit;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.MechJeb;
import krpc.client.services.MechJeb.SmartASS;
import krpc.client.services.MechJeb.SmartASSAutopilotMode;
import krpc.client.services.MechJeb.SmartASSInterfaceMode;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.CelestialBody;
import krpc.client.services.SpaceCenter.Control;
import krpc.client.services.SpaceCenter.Vessel;

class Reentry {
	private Connection connection;
	private SpaceCenter spaceCenter;
	private Vessel shuttle;
	private CelestialBody earth;
	private Control vControl;
	private MechJeb mj;
	private SmartASS smartASS;
	
	public Reentry(Connection connection, SpaceCenter spaceCenter, Vessel shuttle, CelestialBody earth, MechJeb mj) {
		super();
		this.connection = connection;
		this.spaceCenter = spaceCenter;
		this.shuttle = shuttle;
		this.earth = earth;
		
		try
		{
			this.mj = mj;
			vControl = shuttle.getControl();
			smartASS = mj.getSmartASS();
			TimeUnit.SECONDS.sleep(2);
			
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Reentry() {
		
		
	}
	public void start()
	{
		try
		{
			setInitialControl(0, 0, 0);
			
		} catch (RPCException | InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void checkError() 
	{
		
	}
	private void setInitialControl(double initPitch, double initRoll, double initYaw) throws RPCException, InterruptedException
	{
		vControl.setRCS(true);
		
		smartASS.setForcePitch(true);
		smartASS.setForceYaw(true);
		smartASS.setForceRoll(true);
		smartASS.setInterfaceMode(SmartASSInterfaceMode.SURFACE);
		smartASS.setAutopilotMode(SmartASSAutopilotMode.SURFACE_PROGRADE);
		
		smartASS.setSurfaceVelRoll(initRoll);
		smartASS.setSurfaceVelPitch(initPitch);
		smartASS.setSurfaceVelYaw(initYaw);

		smartASS.update(false);
		
		while(smartASS.getSurfaceVelPitch() != initPitch && smartASS.getSurfaceVelRoll() != initRoll && smartASS.getSurfaceVelYaw() != initYaw) 
		{
			TimeUnit.MILLISECONDS.sleep(250);
		}
	}
}
