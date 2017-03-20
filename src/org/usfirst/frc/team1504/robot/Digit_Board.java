package org.usfirst.frc.team1504.robot;

import org.usfirst.frc.team1504.robot.DigitBoard;
import edu.wpi.first.wpilibj.DriverStation;

public class Digit_Board
{
	private DriverStation _ds;	
	private DigitBoard _board;
	
	private long _thread_sleep_delay = 100;
	private int _thread_sleep_counter;
	private static int _thread_sleep_period = 10;
	
	private String _current_display_text;

	//Setting up a separate thread for the Digit Board
	private static class Board_Task implements Runnable
	{
		private Digit_Board _b;

		Board_Task(Digit_Board b)
		{
			_b = b;
		}

		public void run()
		{
			_b.board_task();
		}
	}


	private Thread _task_thread;
	private boolean _run = false;

	protected Digit_Board()
	{
		_task_thread = new Thread(new Board_Task(this), "1504_Display_Board");
		_task_thread.setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2);

		_ds = DriverStation.getInstance(); 
		_board = DigitBoard.getInstance();
		
		start();

		System.out.println("1504 Digit Board Encapsulation Successful.");
	}
	public void start()
	{
		if(_run)
			return;
		_run = true;
		_task_thread = new Thread(new Board_Task(this));
		_task_thread.start();
	}
	public void stop()
	{
		_run = false;
	}
	
	//getInstance() is a function used when instantiating the Digit_Board class in other project classes.
	private static Digit_Board instance = new Digit_Board();
	
	public static Digit_Board getInstance()
	{
		return Digit_Board.instance;
	}
	
	public static void initialize()
	{
		getInstance();
	}
	
	public void write(String value)
	{
		_current_display_text = value;
		_thread_sleep_counter = 0;
	}
	
	//Writes the values to the digit board.
	private void flush()
	{
		if (_thread_sleep_counter < _thread_sleep_period)
		{
			_board.writeDigits(_current_display_text);
			_thread_sleep_counter++;
		}
		else
		{
			_board.writeDigits(Double.toString(_ds.getBatteryVoltage()).substring(0, 4) + "V");
		}
	}
	
	//The loop for the separate thread, where all functions are called.
	private void board_task()
	{	
		double last_pot, current_pot;
		last_pot = current_pot = 0;
		while (_run)
		{
			// Track potentiometer
			current_pot = _board.getPotentiometer();
			if (current_pot != last_pot)
				write("  " + Double.toString(current_pot));
			last_pot = current_pot;
			
			// Flush data buffers to digit board
			flush();
			try
			{
				Thread.sleep(_thread_sleep_delay); // wait a while because people can't read that fast
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
}
