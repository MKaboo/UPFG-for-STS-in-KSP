package shuttleGuidance.reentry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import krpc.client.RPCException;
import krpc.client.StreamException;

public class testMain {

//	private static MechJeb mj;
//	private static SmartASS smartASS;

	public testMain() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException, RPCException, InterruptedException, StreamException
	{

		HashMap<String, LandingFacilities> test = ShuttleLandingSitesConstants.getLandingSites();
		ArrayList<String> runwayNames = ShuttleLandingSitesConstants.getFacilitiesNames();

		for (String string : runwayNames)
		{
			System.out.println(test.get(string).toString());
		}

		System.out.println("test");

	}

}