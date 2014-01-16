package com.kuxhausen.huemore.timing;

public class AlarmState {
	public String mood;
	public String group;
	public Integer brightness;
	public Boolean scheduledForFuture;

	/** 7 booleans which days {Sunday, ... ,Saturday} to repeat on } **/
	private Boolean[] repeats;

	/** if nonrepeating, size = 1. If repeating, size = 7 **/
	private Long[] scheduledTimes;

	
	public boolean isRepeating() {
		boolean result = false;
		if (repeats == null)
			return result;
		for (Boolean b : repeats)
			if (b != null && b == true)
				result = true;

		return result;
	}

	public boolean[] getRepeatingDays() {
		boolean[] result = new boolean[7];
		if (repeats == null)
			return result;
		for (int i = 0; i < 7; i++)
			result[i] = repeats[i];
		return result;
	}

	/**
	 * @param day
	 *            an array of 7 booleans indicated which days Sunday...Saturday
	 *            that the alarm should repeat on
	 **/
	public void setRepeatingDays(boolean[] day) {
		repeats = new Boolean[7];
		for (int i = 0; i < 7; i++)
			repeats[i] = day[i];
	}

	public long getTime() {
		if (scheduledTimes == null || scheduledTimes.length != 1
				|| scheduledTimes[0] == null)
			return -1; // TODO better error handling
		return scheduledTimes[0];
	}

	/** only valid if isRepeating == false **/
	public void setTime(long time) {
		scheduledTimes = new Long[1];
		scheduledTimes[0] = time;
	}

	/**
	 * only valid if isRepeating == true. If getRepeatingDays[i] is false,
	 * getRepeatingScheduledTimes[i] is undefined
	 **/
	public long[] getRepeatingTimes() {
		long[] result = new long[7];
		if (scheduledTimes == null || scheduledTimes.length != 7) {
			return result;
		}
		for (int i = 0; i < 7; i++)
			if (scheduledTimes[i] != null)
				result[i] = scheduledTimes[i];
		return result;
	}

	/** only valid if isRepeating == true **/
	public void setRepeatingTimes(long[] repeatingTimes) {
		scheduledTimes = new Long[7];
		for (int i = 0; i < 7; i++)
			scheduledTimes[i] = repeatingTimes[i];
	}

	public AlarmState() {
	}

}
