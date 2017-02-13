package org.usfirst.frc.team1504.robot;

import org.usfirst.frc.team1504.robot.Update_Semaphore.Updatable;

import com.ctre.CANTalon;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;

public class Winch implements Updatable
{
	private static Winch _instance = new Winch();

	private DriverStation _driver_station = DriverStation.getInstance();
	
	private CANTalon _winch_motor_nancy = new CANTalon(Map.WINCH_TALON_PORT_NANCY);
	private CANTalon _winch_motor_mead = new CANTalon(Map.WINCH_TALON_PORT_MEAD);
	
	private volatile boolean _winch_deployed = false;
	
	protected Winch()
	{
		new Thread( new Runnable()
			{ 
				public void run()
				{
					char enable_state = 0;
					DriverStation ds = DriverStation.getInstance();
					
					double timeout = Map.WINCH_BRAKE_TIMEOUT;
					
					while(true)
					{
						enable_state = (char) (((enable_state << 1) + (ds.isEnabled() ? 1 : 0)) & 3);
						if(enable_state == 1) // On enable, set brake mode
						{
							_winch_motor_nancy.enableBrakeMode(true);
							_winch_motor_mead.enableBrakeMode(true);
							
							set_release(false);
							
							System.out.println("Winch brakes ON");
						}
						else if(enable_state == 2) // On disable, wait 5 seconds then disable the brakes
						{
							System.out.println("Winch brakes OFF in "+ timeout +" seconds.");
							new Thread( new Runnable()
							{
								public void run() 
								{
									Timer.delay(timeout);
									if(ds.isEnabled())
										return;
									_winch_motor_nancy.enableBrakeMode(false);
									_winch_motor_mead.enableBrakeMode(false);
									System.out.println("Winch brakes OFF");
								}
							}).start();
						}
						
						Timer.delay(.2);
					}
				}
			}
		).start();
		
		
		_winch_motor_nancy.enableBrakeMode(true);
		_winch_motor_mead.enableBrakeMode(true);
		
		set_release(false);
		
		Update_Semaphore.getInstance().register(this);
		System.out.println("Winch initialized");
	}
	
	public static Winch getInstance()
	{
		return _instance;
	}
	
	public static void initialize()
	{
		getInstance();
	}
	
	public boolean getWinchDeployed()
	{
		return _winch_deployed;
	}
	
	private void set_release(boolean deploy)
	{
		_winch_deployed = deploy;
		if(_winch_deployed)
			return;
	}
	
	public void semaphore_update()
	{
		if(_driver_station.getMatchTime() > 30.0 && !IO.operator_override())
			return;
		
		// Deploy winch out the side of the robot
		//TODO: What is the mechanism?
		if(IO.deploy_winch() || IO.operator_override())
		{
			Drive.getInstance().setFrontAngleDegrees(Map.ROBOT_WINCHSIDE_OFFSET_DEG);
			set_release(true);
		}
		
		if(!_winch_deployed)
			return;
		
		// Run that thang!
		_winch_motor_nancy.set(IO.winch_input());
		_winch_motor_mead.set(-1.0 * IO.winch_input());
	}
}
