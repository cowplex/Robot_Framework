package org.usfirst.frc.team1504.robot;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;

public class Autonomus_Setup
{
	private static Autonomus_Setup instance = new Autonomus_Setup();
	
	private static DriverStation _driver_station = DriverStation.getInstance();
	private static Digit_Board _digit_display = Digit_Board.getInstance();
	private static DigitBoard _digit_hardware = DigitBoard.getInstance(); // TODO: fix this garbage
	private static Groundtruth _groundtruth = Groundtruth.getInstance();
	
	private static enum STARTING_POSITIONS {CENTER, LEFT, RIGHT { @Override public STARTING_POSITIONS next() { return values()[0]; }; }; public STARTING_POSITIONS next() { return values()[ordinal() + 1]; }}
	private static enum AUTO_TYPES {TIMED, GROUNDTRUTH  { @Override public AUTO_TYPES next() { return values()[0]; }; }; public AUTO_TYPES next() { return values()[ordinal() + 1]; }}
	private static enum SHOOTING_OPTION {ONLY_GEAR, SHOOT { @Override public SHOOTING_OPTION next() { return values()[0]; }; }; public SHOOTING_OPTION next() { return values()[ordinal() + 1]; }}
	
	private STARTING_POSITIONS _starting_position = STARTING_POSITIONS.CENTER;
	private AUTO_TYPES _auto_type = AUTO_TYPES.TIMED;
	private SHOOTING_OPTION _shooting_option = SHOOTING_OPTION.ONLY_GEAR;
		
	public static Autonomus_Setup getInstance()
	{
//		if(!initialized())
//			instance = new Autonomus_Setup();
		return Autonomus_Setup.instance;
	}
	
	public static void initialize()
	{
		getInstance();
	}
	
	public static boolean initialized()
	{
		return instance == null;
	}
	
	protected Autonomus_Setup()
	{
		new Thread(new Runnable() {
			public void run() {
				
				int active = 0;
				int active_counter = 0;
				
				while(true)
				{
					if(_driver_station.isDisabled())
					{
						if(_digit_hardware.getAOnRisingEdge())
						{
							if(++active % 5 == 0)
								active = 1;
							active_counter = 0;
						}
						
						if(active > 0)
						{
							if(active == 4)
							{
								_digit_display.write(Double.toString(_groundtruth.getPosition()[0]));
								continue;
							}
							if(_digit_hardware.getBOnRisingEdge())
							{
								if(active == 1)
									_starting_position = _starting_position.next();
								if(active == 2)
									_auto_type = _auto_type.next();
								if(active == 3)
									_shooting_option = _shooting_option.next();
								active_counter = 0;
							}
							active_counter++;
							
							if(active_counter > 100)
								active = active_counter = 0;
							
							StringBuilder output = new StringBuilder( 
									_starting_position.toString().substring(0, 1) +
									_auto_type.toString().substring(0, 1) +
									_shooting_option.toString().substring(0, 1) +
									" "
									);
							// Blink active item
							if(active_counter > 0 && active_counter / 5 % 2 == 0)
								output.setCharAt(active - 1, ' ');
							
							_digit_display.write(output.toString());
						}
					}
					else
					{
						active = active_counter = 0;
					}
					
					Timer.delay(.02);
				}
			}
		}).start();
		
		System.out.println("Autonomus Setup Initialized");
	}
	
	public double[][] get_path()
	{
		switch(_starting_position)
		{
			case LEFT:
				//Move forward, turn right, move forward
				return new double[][] {{0.25, 0.0, 0.0, (_auto_type == AUTO_TYPES.GROUNDTRUTH ? 1 : 0), 3800}, {0.0, 0.0, -0.25, 0, 5000}, {0.25, 0.0, 0.0, 0, 7200}};
			case CENTER:
				//Move forward
				return new double[][] {{0.25, 0.0, 0.0, 0, 4000}};
			case RIGHT:
				//Move forward, turn left, move forward
				return new double[][] {{0.25, 0.0, 0.0, (_auto_type == AUTO_TYPES.GROUNDTRUTH ? 1 : 0), 3800}, {0.0, 0.0, 0.25, 0, 5000}, {0.25, 0.0, 0.0, 0, 7200}};
		}
		return new double[][] {{0.0, 0.0, 0.0, 0, 1}};
	}
}
