package shuttleGuidance.reentry;

final class LandingFacility
{
	private final String name;
	private final double latitude;
	private final double longitude;
	private final double altitude;
	private final double heading;
	private final double runWayLength;
	
	public LandingFacility(String name, double latitude, double longitude, double altitude, double heading,
			double runWayLength) {
		super();
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.heading = heading;
		this.runWayLength = runWayLength;
	}
	
	public String getName()
	{
		return name;
	}

	public double getLatitude()
	{
		return latitude;
	}

	public double getLongitude()
	{
		return longitude;
	}

	public double getAltitude()
	{
		return altitude;
	}

	public double getHeading()
	{
		return heading;
	}

	public double getRunWayLength()
	{
		return runWayLength;
	}

	@Override
	public String toString()
	{
		return "LandingFacilities [name=" + name + ", latitude=" + latitude + ", longitude=" + longitude + ", altitude="
				+ altitude + ", heading=" + heading + ", runWayLength=" + runWayLength + "]";
	}
	
	
	
	
}