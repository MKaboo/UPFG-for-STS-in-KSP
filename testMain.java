package shuttleGuidance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.KRPC;
import krpc.client.services.MechJeb;
import krpc.client.services.SpaceCenter;
import krpc.client.services.MechJeb.ManeuverPlanner;
import krpc.client.services.MechJeb.OperationCircularize;
import krpc.client.services.MechJeb.SmartASS;
import krpc.client.services.MechJeb.SmartASSAutopilotMode;
import krpc.client.services.MechJeb.SmartASSInterfaceMode;
import krpc.client.services.MechJeb.TimeReference;
import krpc.client.services.MechJeb.TimeSelector;
import krpc.client.services.SpaceCenter.CelestialBody;
import krpc.client.services.SpaceCenter.Engine;
import krpc.client.services.SpaceCenter.Node;
import krpc.client.services.SpaceCenter.Orbit;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;

public class testMain {

	private static MechJeb mj;
	private static SmartASS smartASS;

	public testMain() {
		// TODO Auto-generated constructor stub
	}

	  public static void main(String[] args)
        throws IOException, RPCException, InterruptedException, StreamException {
      Connection connection = Connection.newInstance("Vessel velocity");
        SpaceCenter spaceCenter = SpaceCenter.newInstance(connection);
        Vessel vessel = spaceCenter.getActiveVessel();
        
        
        spaceCenter = SpaceCenter.newInstance(connection);
		vessel = spaceCenter.getActiveVessel();

		mj = MechJeb.newInstance(connection);
		smartASS = mj.getSmartASS();
		
		
		FlightUI flightUI = new FlightUI();

		
		TimeUnit.SECONDS.sleep(2);

//		body = vessel.getOrbit().getBody();
//
//		vesselControl = vessel.getControl();
		
		
		smartASS.setForcePitch(true);
		smartASS.setForceYaw(true);
		smartASS.setForceRoll(true);
		smartASS.setInterfaceMode(SmartASSInterfaceMode.SURFACE);
		smartASS.setAutopilotMode(SmartASSAutopilotMode.SURFACE);
		smartASS.setSurfaceHeading(90);
		smartASS.setSurfacePitch(90);
		smartASS.setSurfaceRoll(0);

		smartASS.update(false);

		flightUI.start();
		
		for (int i = 0; i < 10000; i++)
		{
			flightUI.updateGUI();
		}
		
		flightUI.start();
		
		
		
	
		
		
		
	  }
	  

}