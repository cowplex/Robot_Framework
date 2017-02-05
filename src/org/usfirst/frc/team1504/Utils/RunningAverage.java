package org.usfirst.frc.team1504.Utils;

public class RunningAverage
{
	private int _num_samples;
	private int _index;
	private double[] _samples;
	private double _sum;
	private double _average;
	
	/**
	 * Convenience class to provide a running average of an input.
	 * @param samples - Number of samples to average over.
	 */
	RunningAverage(int samples)
	{
		_num_samples = samples;
		_samples = new double[_num_samples];
	}
	
	/**
	 * Update and recompute the running average with a new sample
	 * @param sample - The sample to be included.
	 */
	public void update(double sample)
	{
		_sum += sample - _samples[_index];
		_samples[_index] = sample;
		_average = _sum / (double)_num_samples;
		_index = (_index + 1) % _num_samples;
	}
	
	/**
	 * Get the current running average.
	 * @return The running average
	 */
	public double get()
	{
		return _average;
	}
}
