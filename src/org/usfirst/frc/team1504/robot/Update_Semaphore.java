package org.usfirst.frc.team1504.robot;

import java.util.List;
import java.nio.ByteBuffer;
import java.util.ArrayList;

//import org.usfirst.frc.team1504.robot.Latch_Joystick._button_mask;

public class Update_Semaphore
{
	interface Updatable
	{
		void semaphore_update();
	}
	
	private List<Updatable> _list = new ArrayList<Updatable>();
	private List<Thread> _thread_pool = new ArrayList<Thread>();
	private Logger _logger = Logger.getInstance();
	private long _last_update;
	
	private static final Update_Semaphore instance = new Update_Semaphore();
	//private int _clear_mask_rising_edge;
	//private int _clear_mask;
	//private int _button_mask;// = Latch_Joystick._button_mask;
	//private int _button_mask_rising_edge;// = Latch_Joystick._button_mask_rising;

	
	protected Update_Semaphore()
	{
		ClassLoader class_loader = Update_Semaphore.class.getClassLoader();
		for(int i = 1; i < Map.LOGGED_CLASSES.values().length; i++)
		{
			String subclass = Utils.toCamelCase(Map.LOGGED_CLASSES.values()[i].toString());
			try {
				System.out.println("Semaphore - Attempting to load org.usfirst.frc.team1504.robot." + subclass);
				class_loader.loadClass("org.usfirst.frc.team1504.robot." + subclass);
				//Class.forName("org.usfirst.frc.team1504.robot." + subclass);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Semaphore Initialized");
	}
	
	public static Update_Semaphore getInstance()
	{
		return instance;
	}
	
	public static void initialize()
	{
		getInstance();
	}
	
	public void register(Updatable update_class)
	{
		_list.add(update_class);
		System.out.println("\tSemaphore - registered " + update_class.getClass().getName());
		//obj.semaphore_update();
		
		// Let's see about this. Creating several threads at 20hz might be an overhead issue...
//		Thread t = new Thread();
//		_tlist.add(t);
		
		
		_thread_pool.add(
				new Thread(
						new Runnable()
						{
							public void run()
							{
								System.out.println("\tSemaphore - starting pool thread for " + update_class.getClass().getName());
								Update_Semaphore semaphore = Update_Semaphore.getInstance();
								while(true)
								{
									try {
										semaphore.wait(); // Will wait indefinitely until notified
										update_class.semaphore_update();
									} catch (InterruptedException error) {
										error.printStackTrace();
									}
								}
							}
						}
				)
		);
		_thread_pool.get(_thread_pool.size() - 1).start();
		
	}
	
	private void dump()
	{
		byte[] ret = new byte[4];
		ByteBuffer.wrap(ret).putInt((int)(_last_update - IO.ROBOT_START_TIME));
		
		_logger.log(Map.LOGGED_CLASSES.SEMAPHORE, ret);
	}
	
	public void newData()
	{
		_last_update = System.currentTimeMillis();
		dump();
		
		this.notifyAll();
	}
	/*public void newData()
	{
		_last_update = System.currentTimeMillis();
		dump();
		
		for(int i = 0; i < _list.size(); i++)
		{
			if(_thread_pool.get(i) == null || !_thread_pool.get(i).isAlive())
			{
				Updatable u = _list.get(i);
				Thread t = new Thread(new Runnable() {
					public void run() {
						u.semaphore_update();
					}
				});
				_thread_pool.set(i, t);
				_thread_pool.get(i).start(); //t.start()
				
			}
			else
			{
//				System.out.println("thread not updated!");
			}
		}
	}*/
}
