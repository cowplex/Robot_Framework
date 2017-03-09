package org.usfirst.frc.team1504.vision;

public class Vision
{
	private static Vision _instance;
	
	protected Vision()
	{
		
	}
	
	public static Vision getInstance()
	{
		if(_instance == null)
			_instance = new Vision();
		return _instance;
	}
	
	public static void initialize()
	{
		getInstance();
	}
	public static boolean initialized()
	{
		return _instance != null;
	}
}
