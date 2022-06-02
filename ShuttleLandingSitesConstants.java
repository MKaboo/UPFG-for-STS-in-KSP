package shuttleGuidance.reentry;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.util.FastMath;

import krpc.client.Connection;
import krpc.client.services.KRPC;
import krpc.client.services.MechJeb;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.CelestialBody;
import krpc.client.services.SpaceCenter.Vessel;
import krpc.client.services.SpaceCenter.Waypoint;
import krpc.client.services.SpaceCenter.WaypointManager;

class ShuttleLandingSitesConstants {

	private static HashMap<String, LandingFacility> landingSites;
	private static ArrayList<String> facilitiesNames;
	final  static double MAXIMUMLANDINGSITEDIF = 700000.;
	final protected static int MaxNumberOfOrbitsToCheck = 5;

	
	static
	{
		//LandingFacility KSC15 = new LandingFacility("SLF-15",FastMath.toRadians(28.615), FastMath.toRadians(-80.6945), 3, 150, 4600);
		LandingFacility KSC33 = new LandingFacility("SLF-33", FastMath.toRadians(28.615), FastMath.toRadians(-80.6945), 3, 330, 4600);
		//LandingFacility EAFB22 = new LandingFacility("EAFB-22", FastMath.toRadians(34.905798), FastMath.toRadians(-117.883003), 704.3, 220, 4579);

		
		landingSites = new HashMap<>();
		facilitiesNames = new ArrayList<>();
		
		//addLocation(KSC15);
		addLocation(KSC33);
		//addLocation(EAFB22);
		
		
		try
		{
			Connection connection = Connection.newInstance("Init");
			KRPC.newInstance(connection);
			SpaceCenter spaceCenter = SpaceCenter.newInstance(connection);
			WaypointManager wm = spaceCenter.getWaypointManager();
			Vessel vessel = spaceCenter.getActiveVessel();
			CelestialBody host = vessel.getOrbit().getBody();
			
			for (String string : facilitiesNames)
			{
				LandingFacility lf = landingSites.get(string);
				Waypoint toadd = wm.addWaypointAtAltitude(lf.getLatitude(),lf.getLongitude(),lf.getAltitude(),host,lf.getName());
			}
			connection.close();
		}
		catch (Exception e) {
			// TODO: handle exception
			System.err.println(e.getStackTrace());
		}
	}
	
	private static void addLocation(LandingFacility lf)
	{
		landingSites.put(lf.getName(), lf);
		facilitiesNames.add(lf.getName());

	}
	
	protected static HashMap<String, LandingFacility> getLandingSites()
	{
		return landingSites;
	}

	protected static ArrayList<String> getFacilitiesNames()
	{
		return facilitiesNames;
	}
	

	


}
