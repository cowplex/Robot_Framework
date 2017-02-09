package org.usfirst.frc.team1504.vision;

public class Vision
{
	private static Vision _instance = new Vision();
	
	protected Vision()
	{
		
	}
	
	public static Vision getInstance()
	{
		return _instance;
	}
	
	public static void initialize()
	{
		getInstance();
	}
}
