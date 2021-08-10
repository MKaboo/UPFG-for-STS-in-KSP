package shuttleGuidance.reentry;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.util.FastMath;

class ShuttleLandingSitesConstants {

	private static HashMap<String, LandingFacility> landingSites;
	private static ArrayList<String> facilitiesNames;
	final  static double MAXIMUMLANDINGSITEDIF = 5.e5;
	final protected static int MaxNumberOfOrbitsToCheck = 3;

	
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
