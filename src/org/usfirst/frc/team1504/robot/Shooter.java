package org.usfirst.frc.team1504.robot;

import java.nio.ByteBuffer;

import org.usfirst.frc.team1504.robot.Update_Semaphore.Updatable;

import com.ctre.CANTalon;
import com.ctre.CANTalon.FeedbackDevice;
import com.ctre.CANTalon.TalonControlMode;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Shooter implements Updatable
{	
	private static Shooter _instance;
	private static DriverStation _driver_station = DriverStation.getInstance();
	private static Preferences _preferences = Preferences.getInstance();
	private static Logger _logger = Logger.getInstance();
	
	private CANTalon _shooter_motor = new CANTalon(30);
	private CANTalon _hopper_motor = new CANTalon(31);
	//private CANTalon _turret_motor;
	
	private boolean _enabled  = false;
	private boolean _override = false;
	private boolean _clear_block = false;
	private int _shot_estimate = 0;
	private int _shot_speed_follower;
	private char _shot_current_follower;
	private int _shot_number_follower;
	
//	private double PID_values[][] = {{.03, .00015}, {.05, .00017}};
	private static double PID_DEADZONE = 150;
	private static double SHOT_DETECTION_THRESHOLD = 6.5;
	
	
	protected Shooter()
	{
		//if(_shooter_motor.isSensorPresent(FeedbackDevice.CtreMagEncoder_Relative) == CANTalon.FeedbackDeviceStatus.FeedbackStatusPresent)
		{
			_shooter_motor.setFeedbackDevice(FeedbackDevice.CtreMagEncoder_Relative);
			_shooter_motor.changeControlMode(TalonControlMode.Speed);
			//_shooter_motor.setP(Map.SHOOTER_GAIN_P);
			_shooter_motor.setP(0.03);
			_shooter_motor.setI(0.00015);
			_shooter_motor.setD(0.10);//0.09);
			//_shooter_motor.setI(Map.SHOOTER_GAIN_I);
			_shooter_motor.reverseSensor(false);
		}
		
		// Retain last set value across robot reboots
		SmartDashboard.putNumber("Shooter Target Speed", getTargetSpeed());
		
		Update_Semaphore.getInstance().register(this);
		System.out.println("Shooter Initialized");
	}
	
	/**
	 * Get the Shooter singleton class instance
	 * @return the Shooter instance
	 */
	public static Shooter getInstance()
	{
		if(_instance == null)
			_instance = new Shooter();
		return _instance;
	}
	
	/**
	 * Called to initialize the Shooter singleton class
	 */
	public static void initialize()
	{
		getInstance();
	}
	
	public static boolean initialized()
	{
		return (_instance == null);
	}
	
	/**
	 * Enable or Disable the shooter.
	 * @param enabled - True to enable the shooter, false to disable.
	 */
	public void setEnabled(boolean enabled)
	{
		_enabled = enabled;
	}
	
	/**
	 * Set the target shooter speed. Persistent across reboots.
	 * @param speed - the speed we want the shooter to run at (in RPM)
	 */
	public void setTargetSpeed(double speed)
	{
		SmartDashboard.putNumber("Shooter Target Speed", speed);
		_preferences.putDouble("Shooter Target Speed", speed);
	}
	
	/**
	 * Returns the current target speed of the shooter.
	 * @return Shooter target speed (in RPM)
	 */
	public double getTargetSpeed()
	{
		return _preferences.getDouble("Shooter Target Speed", 0.0);
	}
	
	/**
	 * Returns the current actual speed of the shooter
	 * @return Shooter speed (in RPM)
	 */
	public double getCurrentSpeed()
	{
		return _shooter_motor.getSpeed();
	}
	
	/**
	 * Returns a boolean signifying whether or not the shooter is at its target speed
	 * @return Boolean signifying if the shooter is at its target speed
	 */
	public boolean getSpeedGood()
	{
		return (Math.abs(_shooter_motor.getSpeed() + getTargetSpeed()) < PID_DEADZONE);
	}
	
	/**
	 * Override the vision system to force the shooter to fire when at speed and ignore the camera
	 * @param override - True to override the camera input and fire when at speed
	 */
	public void setOverride(boolean override)
	{
		_override = override;
	}
	
	private void update_dashboard()
	{
		SmartDashboard.putNumber("Shooter Speed", getCurrentSpeed());
		SmartDashboard.putBoolean("Shooter At Speed", getSpeedGood());
		SmartDashboard.putBoolean("Shooter Clearing Block", _clear_block);
		SmartDashboard.putNumber("Shooter Current", _shooter_motor.getOutputCurrent());
		SmartDashboard.putNumber("Shooter Hopper Current", _hopper_motor.getOutputCurrent());
		SmartDashboard.putNumber("Shooter Shot Estimate", _shot_estimate);
		SmartDashboard.putNumber("Shooter Shot Speed Follower", _shot_speed_follower);
		SmartDashboard.putNumber("Shooter Shot Current Follower", _shot_current_follower);
		SmartDashboard.putNumber("Shooter Shot Number Follower", _shot_number_follower);
	}
	
	private void dump()
	{
		byte[] output = new byte[1+4*7];
		output[0] = (byte) ((_enabled ? 8 : 0) + (_override ? 4 : 0) + (getSpeedGood() ? 2 : 0)+ (_clear_block ? 1 : 0));
		ByteBuffer.wrap(output, 1, 4).putFloat((float)(getCurrentSpeed()));
		ByteBuffer.wrap(output, 5, 4).putFloat((float)(_shooter_motor.getOutputCurrent()));
		ByteBuffer.wrap(output, 9, 4).putFloat((float)(_hopper_motor.getOutputCurrent()));
		ByteBuffer.wrap(output, 13, 4).putInt(_shot_estimate);
		ByteBuffer.wrap(output, 17, 4).putInt(_shot_speed_follower);
		ByteBuffer.wrap(output, 21, 4).putInt(_shot_current_follower);
		ByteBuffer.wrap(output, 25, 4).putInt(_shot_number_follower);
		
		_logger.log(Map.LOGGED_CLASSES.SHOOTER, output);
	}
	
	public void semaphore_update()
	{
		if(_driver_station.isOperatorControl())
		{
			setEnabled(IO.shooter_enable());
			setOverride(IO.operator_override());
		}
		
		// Update stored speed value if changed from the DS.
		if(SmartDashboard.getNumber("Shooter Target Speed", 0.0) != getTargetSpeed())
			setTargetSpeed(SmartDashboard.getNumber("Shooter Target Speed", 0.0));
		
		if(_enabled)
		{
			_shooter_motor.set(-getTargetSpeed());
			
			if(_shot_number_follower > 50 || _clear_block)
			{
				_clear_block = true;
				if(_shot_number_follower >= 50)
					_shot_number_follower = 25;
				_shot_number_follower--;
				
				if(_shot_number_follower <= 0)
					_clear_block = false;
			}
			else if(getSpeedGood())
				_shot_number_follower++;
			
			if(_clear_block)
				_hopper_motor.set(1.0);
			else if(getSpeedGood() || _override)
			{
				//_shooter_motor.setP(PID_values[1][0]);
				//_shooter_motor.setI(PID_values[1][1]);
				_hopper_motor.set(-1.0);
			}
			else
			{
				//_shooter_motor.setP(PID_values[0][0]);
				//_shooter_motor.setI(PID_values[0][1]);
				_hopper_motor.set(0.0);
			}
		}
		else
		{
			//_shooter_motor.setP(PID_values[0][0]);
			//_shooter_motor.setI(PID_values[0][1]);
			_shooter_motor.set(0);
			_hopper_motor.set(0.0);
			_shot_number_follower = 0;
		}
		
		// Speed follower checks if the shooter has recently been at speed (in the last 360ms)
		// Current follower checks for current spikes
		// If the speed follower > 0 (shooter has been at speed in the last 360ms) and on the rising
		// edge of a current spike, we add one to the shot estimate. This ensures we detect a current
		// spike when a ball goes through but we ignore the spike that happens when the shooter spins up.
		_shot_speed_follower = ((_shot_speed_follower << 1) + (getSpeedGood() ? 1 : 0)) & 262143;
		_shot_current_follower = (char) (((_shot_current_follower << 1) + (_shooter_motor.getOutputCurrent() > SHOT_DETECTION_THRESHOLD ? 1 : 0)) & 3);
		if(_shot_speed_follower > 0 && _shot_current_follower == 1)
		{
			_shot_estimate++;
			_shot_number_follower = 0;
		}
//		else if(!_clear_block && _shot_number_follower > 18)
//		{
//			_shot_number_follower = (_shot_number_follower & 262143) >> 2;
//			if(_shot_number_follower > 0)
//				_shot_number_follower += 262144;
//		}
//		else if(!_clear_block && getSpeedGood())
//			_shot_number_follower++;
		
		update_dashboard();
		dump();
	}

}
