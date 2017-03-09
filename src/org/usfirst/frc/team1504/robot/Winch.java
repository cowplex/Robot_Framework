package org.usfirst.frc.team1504.robot;

import java.nio.ByteBuffer;

import org.usfirst.frc.team1504.robot.Update_Semaphore.Updatable;

import com.ctre.CANTalon;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;

public class Winch implements Updatable
{
	private static Winch _instance = new Winch();

	private static DriverStation _driver_station = DriverStation.getInstance();
	private static Logger _logger = Logger.getInstance();
	
	private CANTalon _winch_motor_nancy = new CANTalon(Map.WINCH_TALON_PORT_NANCY);
	private CANTalon _winch_motor_mead = new CANTalon(Map.WINCH_TALON_PORT_MEAD);
	
	private boolean _override = false;
	private volatile boolean _winch_deployed = false;
	
	private Thread _winch_pulse;
	
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
							
							if(_winch_pulse == null || !_winch_pulse.isAlive())
							{
								_winch_pulse = new Thread(
											new Runnable()
											{
												public void run()
												{
													while(!_winch_deployed)
													{
														run_winch(-.2);
														Timer.delay(.01);
														run_winch(0.0);
														for(int i = 0; i < 50 && !_winch_deployed; i++)
															Timer.delay(.01);//.5);
													}
													_winch_deployed = false;
													
													run_winch(1.0);
													Timer.delay(1);
													run_winch(0.0);
													
													_winch_deployed = true;
												}
											}
										);
								_winch_pulse.start();
							}
						}
						else if(enable_state == 2) // On disable, wait some time then disable the brakes
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
		
		_winch_motor_nancy.setCurrentLimit(60);
		_winch_motor_mead.setCurrentLimit(60);
		_winch_motor_nancy.EnableCurrentLimit(true);
		_winch_motor_mead.EnableCurrentLimit(true);
		
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
	public static boolean initialized()
	{
		return _instance != null;
	}
	
	public boolean getWinchDeployed()
	{
		return _winch_deployed;
	}
	
	private void set_override(boolean override)
	{
		// Turn off the current limit if the override button is pressed
		if(_override != override)
		{
			_winch_motor_nancy.EnableCurrentLimit(!override);
			_winch_motor_mead.EnableCurrentLimit(!override);
		}
		_override = override;
	}
	
	public void set_release(boolean deploy)
	{
		_winch_deployed = deploy;
	}
	
	private void run_winch(double magnitude)
	{
		//magnitude = Math.abs(magnitude);
		
		_winch_motor_nancy.set(magnitude);
		_winch_motor_mead.set(-1.0 * magnitude);
	}
	
	private void dump()
	{
		byte[] data = new byte[1+4*3];
		data[0] = (byte) ((_override ? 2 : 0) + (_winch_deployed ? 2 : 0));
		ByteBuffer.wrap(data, 1, 4).putFloat((float)(_winch_motor_nancy.get()));
		ByteBuffer.wrap(data, 5, 4).putFloat((float)(_winch_motor_nancy.getOutputCurrent()));
		ByteBuffer.wrap(data, 9, 4).putFloat((float)(_winch_motor_mead.getOutputCurrent()));
		
		_logger.log(Map.LOGGED_CLASSES.WINCH, data);
	}
	
	public void semaphore_update()
	{
		set_override(IO.operator_override());
		dump();
		
		if(_driver_station.getMatchTime() > 30.0 && !_override && !_winch_deployed)
			return;
		
		// Deploy winch out the side of the robot
		if(IO.deploy_winch())
		{
			Drive.getInstance().setFrontAngleDegrees(Map.ROBOT_WINCHSIDE_OFFSET_DEG);
			set_release(true);
			//_winch_deployed = true;
		}
		
		if(!_winch_deployed)
			return;
		
		// Run that thang!
		run_winch(IO.winch_input());
		//_winch_motor_nancy.set(IO.winch_input());
		//_winch_motor_mead.set(-1.0 * IO.winch_input());
	}
}
