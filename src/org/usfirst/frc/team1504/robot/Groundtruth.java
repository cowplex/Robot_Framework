package org.usfirst.frc.team1504.robot;

import java.util.ArrayList;
import java.util.List;

import org.usfirst.frc.team1504.robot.Update_Semaphore.Updatable;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Groundtruth implements Updatable {
	private static final Groundtruth instance = new Groundtruth();
	
	private static final int DATA_MAP[] = {2, -1, 3, -5, 4, 6};
	
	private Logger _logger = Logger.getInstance();
	private Arduino _arduino = Arduino.getInstance();
	private DriverStation _driver_station = DriverStation.getInstance();
	//private volatile byte[] _raw_data = null;
	private volatile List<Byte> _raw_data = new ArrayList<Byte>();
	private volatile byte[] _current_data = new byte[6];
	private double[] _position = {0.0, 0.0, 0.0};
	private double[] _position_error = {0.0, 0.0, 0.0};
	
	private boolean _data_good = false;
	private double[] _speed = {0.0, 0.0, 0.0};
	private double[] _acceleration = {0.0, 0.0, 0.0};
	private double[][] _speed_samples = new double[Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES][3];
	private double[][] _acceleration_samples = new double[Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES][3];
	private int _sample_index = 0;
	private long _last_update;
	
	protected Groundtruth()
	{
		double[] initializer = {0.0, 0.0, 0.0};
		for(int i = 0; i < Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES; i++)
		{
			_speed_samples[i] = initializer;
			if(i < (Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES - 1))
				_acceleration_samples[i] = initializer;
		}
		
		_raw_data.add((byte) 0);
		
		Update_Semaphore.getInstance().register(this);
		
		System.out.println("Groundtruth Initialized");
	}
	
	public static Groundtruth getInstance()
	{
		return instance;
	}
	
	/**
	 * Gets if the data is good from the groundtruth sensor
	 * @return Boolean - if we should trust the Groundtruth sensor
	 */
	public boolean getDataGood()
	{
		return _data_good;
	}
	
	/**
	 * Gets the robot's current position.
	 * @return double[] array containing: {Forward position, Right position, Anticlockwise position}
	 */
	public double[] getPosition()
	{
		return _position;
	}
	
	/**
	 * Sets the robot's current position index.
	 * Useful for setting at the start of the match or resetting at a known point on the field.
	 * @param p - forward, right, and anticlockwise position values.
	 */
	public void setPosition(double[] p)
	{
		_position = p;
	}
	
	/**
	 * Gets the robot's current speed
	 * @return double[] array containing: {Forward speed, Right speed, Anticlockwise speed}
	 */
	public double[] getSpeed()
	{
		return _speed;
	}
	
	/**
	 * Gets the robot's current acceleration
	 * @return double[] array containing: {Forward acceleration, Right acceleration, Anticlockwise acceleration}
	 */
	public double[] getAcceleration()
	{
		return _acceleration;
	}
	
	/**
	 * Input the current data from the ground truth sensors
	 * @param data - Data format: LEFT_X, LEFT_Y, LEFT_SQUAL, RIGHT_X, RIGHT_Y, RIGHT_SQUAL
	 */
	public void getData()
	{
		if(DriverStation.getInstance().isOperatorControl())
		{
			_data_good = false;
			return;
		}
		byte[] data = _arduino.getSensorData();
		for(int i = 0; i < DATA_MAP.length; i++)
			_current_data[i] = (byte) (Math.signum(DATA_MAP[i]) * data[Math.abs(DATA_MAP[i]) - 1]);
		
		//if(_driver_station.isEnabled())
			compute(_current_data);
		
	}
	
	//public void getData(/*byte[] data*/)
	private void compute(byte[] data)
	{
		//byte[] data = _arduino.getSensorData();
		
		// Data format: LEFT_X LEFT_Y LEFT_SQUAL RIGHT_X RIGHT_Y RIGHT_SQUAL
		//_raw_data.set(0, (byte) (_raw_data.get(0) + 1));
		for(byte b : data)
			_raw_data.add(b);
		
		if(data[2] < Map.GROUNDTRUTH_QUALITY_MINIMUM || data[5] < Map.GROUNDTRUTH_QUALITY_MINIMUM)
		{
			_data_good = false;
			return;
		}
		_data_good = true;
		
		double[] normalized_data = new double[6];
		double[] motion = new double[3];
		
		// Normalize from raw counts to distance
		for(int i = 0; i < data.length; i++)
			normalized_data[i] = data[i] * Map.GROUNDTRUTH_DISTANCE_PER_COUNT;
		
		// Forward
		motion[0] = (normalized_data[1] + normalized_data[4]) / 2.0;
		// Right
		motion[1] = (normalized_data[0] + normalized_data[3]) / 2.0;
		// Anticlockwise
		motion[2] = (normalized_data[4] - normalized_data[1]) / (2.0 * Map.GROUNDTRUTH_TURN_CIRCUMFERENCE);
		
		// TODO: Compute rolling positional error
		for(int i = 0; i < _position.length; i++)
		{
			_position[i] += motion[i];
			_position_error[i] += 1.0 / Map.GROUNDTRUTH_DISTANCE_PER_COUNT;
		}
		
		// Update global speed and acceleration values
		long update_time = System.currentTimeMillis();
		double elapsed_time = (update_time - _last_update) / 1000.0; // Convert to seconds
		_last_update = update_time;
		
		// Put current speed value into averaging array
		for(int i = 0; i < motion.length; i++)
			_speed_samples[_sample_index][i] = motion[i] / elapsed_time;
		// Compute acceleration array
		for(int i = 0; i < motion.length; i++)
			_acceleration_samples[_sample_index][i] = (_speed_samples[_sample_index][i] - _speed_samples[(_sample_index + Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES - 1) % Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES][i]) / elapsed_time;
		
		// Find average speed
		double[] speed = {0.0, 0.0, 0.0};
		for(int i = 0; i < _speed_samples.length; i++)
		{
			for(int j = 0; j < 3; j++)
				speed[j] += _speed_samples[i][j];
		}
		for(int i = 0; i < speed.length; i++)
			speed[i] /= Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES;
		
		// Find average acceleration
		double[] acceleration = {0.0, 0.0, 0.0};
		for(int i = 0; i < _acceleration_samples.length; i++)
		{
			for(int j = 0; j < 3; j++)
				acceleration[j] += _acceleration_samples[i][j];
		}
		for(int i = 0; i < acceleration.length; i++)
			acceleration[i] /= Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES;
		
		// Set global variables
		_speed = speed;
		_acceleration = acceleration;
		
		_sample_index = (_sample_index + 1) % Map.GROUNDTRUTH_SPEED_AVERAGING_SAMPLES;
	}

	public void semaphore_update()
	{
		if(_raw_data.get(0) == 0)
			return;
		
		byte[] data = new byte[_raw_data.size()];
		for(int index = 0; index < _raw_data.size(); index++)
			data[index] = _raw_data.get(index);
		
		data[0] = (byte) _raw_data.size();

		_raw_data.clear();
		_raw_data.add((byte) 0);
		
		_logger.log(Map.LOGGED_CLASSES.GROUNDTRUTH, data);
		
		
		/*if(_raw_data != null)
			_logger.log(Map.LOGGED_CLASSES.GROUNDTRUTH, _raw_data);
		_raw_data = null;*/
	}
	
	public void dashboard_update()
	{
		// SmartDashboard output code
		SmartDashboard.putNumber("Groundtruth position Y", _position[0]);
		SmartDashboard.putNumber("Groundtruth position X", _position[1]);
		SmartDashboard.putNumber("Groundtruth position W", _position[2]);
		
		SmartDashboard.putNumber("Groundtruth speed Y", _speed[0]);
		SmartDashboard.putNumber("Groundtruth speed X", _speed[1]);
		SmartDashboard.putNumber("Groundtruth speed W", _speed[2]);
		
		SmartDashboard.putNumber("Groundtruth acceleration Y", _acceleration[0]);
		SmartDashboard.putNumber("Groundtruth acceleration X", _acceleration[1]);
		SmartDashboard.putNumber("Groundtruth acceleration W", _acceleration[2]);
		
		SmartDashboard.putBoolean("Groundtruth data good", _data_good);
		SmartDashboard.putNumber("Groundtruth raw left X", _current_data[0]);
		SmartDashboard.putNumber("Groundtruth raw left Y", _current_data[1]);
		SmartDashboard.putNumber("Groundtruth raw left SQUAL", _current_data[2]);
		SmartDashboard.putNumber("Groundtruth raw right X", _current_data[3]);
		SmartDashboard.putNumber("Groundtruth raw right Y", _current_data[4]);
		SmartDashboard.putNumber("Groundtruth raw right SQUAL", _current_data[5]);
	}
}
