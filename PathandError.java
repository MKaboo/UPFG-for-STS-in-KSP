package shuttleGuidance.reentry;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class PathandError 
{
	
	public static void main(String[] args)
	{
		Vector3D v1 = new Vector3D(133, 160, 198);
		Vector3D v2 = new Vector3D(0, 0, 0);
		
		while (true)
		{
			double currentDistance = distanceFromLandingStraightLine(v1, v2);
			System.out.println(v1.toString());
			System.out.println(currentDistance);
			if (currentDistance <= 0)
			{
				break;
			}
			
			if (v1.getX() > v2.getX())
			{
				v1 = v1.add(new Vector3D(-1, 0, 0));
			}
			if (v1.getY() > v2.getY())
			{
				v1 = v1.add(new Vector3D(0, -1, 0));
			}
			if (v1.getZ() > v2.getZ())
			{
				v1 = v1.add(new Vector3D(0, 0, -1));
			}
		}
	}
	
	private static double distanceFromLandingStraightLine(Vector3D movingObject, Vector3D location)
	{
		return Vector3D.distance(movingObject, location);

	}
	
	private static double parceDistanceFromAllLandingSites() 
	{
		return 0;
	}
	
	
}
