package shuttleGuidance.reentry;

import java.util.ArrayList;
import java.util.HashMap;

class ShuttleLandingSitesConstants {

	private static HashMap<String, LandingFacilities> landingSites;
	private static ArrayList<String> facilitiesNames;
	

	
	static
	{
		LandingFacilities KSC15 = new LandingFacilities("SLF-15", 28.36540, -80.41402, 3, 150, 4600);
		LandingFacilities KSC33 = new LandingFacilities("SLF-33", 28.36540, -80.41402, 3, 330, 4600);
		LandingFacilities EAFB22 = new LandingFacilities("EAFB-22", 34.905798, -117.883003, 704.3, 220, 4579);

		
		landingSites = new HashMap<>();
		facilitiesNames = new ArrayList<>();
		
		addLocation(KSC15);
		addLocation(KSC33);
		addLocation(EAFB22);
	}
	
	private static void addLocation(LandingFacilities lf)
	{
		landingSites.put(lf.getName(), lf);
		facilitiesNames.add(lf.getName());
	}
	
	protected static HashMap<String, LandingFacilities> getLandingSites()
	{
		return landingSites;
	}

	protected static ArrayList<String> getFacilitiesNames()
	{
		return facilitiesNames;
	}
	

	


}
