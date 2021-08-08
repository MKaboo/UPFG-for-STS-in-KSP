package shuttleGuidance.reentry;

import java.util.concurrent.TimeUnit;

import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.MechJeb;
import krpc.client.services.UI;
import krpc.client.services.MechJeb.SmartASS;
import krpc.client.services.MechJeb.SmartASSAutopilotMode;
import krpc.client.services.SpaceCenter.Control;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;
import krpc.client.services.UI.Canvas;
import krpc.client.services.UI.MessagePosition;

class shuttleControl {
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

	protected void setMechjebConditions(SmartASSAutopilotMode assAutopilotMode)
	{
		try
		{
			smartASS.setAutopilotMode(assAutopilotMode);
			try
			{
				smartASS.setForcePitch(true);
				smartASS.setForceRoll(true);
				smartASS.setForceYaw(true);

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

	protected void deorbit(shuttleInfo si)
	{

		try
		{
			Control vesselControl = si.getVesselControl();
			vesselControl.setThrottle(1f);
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

	protected void startLoop(Connection connection)
	{
		try
		{
			UI ui = UI.newInstance(connection);
			Canvas canvas = ui.getStockCanvas();
			ui.message("Activate Action Group 3 to Close Cargobay Doors", 10, MessagePosition.TOP_CENTER,
					new Triplet<Double, Double, Double>(100., 100., 100.), 100f);
			setMechjebConditions(SmartASSAutopilotMode.SURFACE_RETROGRADE);
			try
			{
				smartASS.setSurfaceRoll(180);
			} catch (RPCException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (RPCException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
