package shuttleGuidance.reentry;

public class shuttleInfo {

	private double AOA; // pitch
	private final double MAXALPHAMODULATION = 3d;
	private double heading;
	private double bankAngle; // roll around velocity
	private EntryConditions ec = null;
	
	
	protected EntryConditions getEc()
	{
		return ec;
	}



	protected void setEc(EntryConditions ec)
	{
		this.ec = ec;
	}



	protected shuttleInfo() {
		ec = EntryConditions.standby;
	}
	
	
	
	enum EntryConditions {standby, deorbit, reentry, approach, landing} 

}
