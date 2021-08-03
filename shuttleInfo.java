package shuttleGuidance;

public class shuttleInfo {

	private double AOA; // pitch
	private final double MAXALPHAMODULATION = 3d;
	private double heading;
	private double bankAngle; // roll around velocity
	private EntryConditions ec = EntryConditions.standby;
	
	
	public EntryConditions getEc()
	{
		return ec;
	}



	public void setEc(EntryConditions ec)
	{
		this.ec = ec;
	}



	public shuttleInfo() {
		// TODO Auto-generated constructor stub
	}
	
	
	
	enum EntryConditions {standby, deorbit, reentry, approach, landing} 

}
