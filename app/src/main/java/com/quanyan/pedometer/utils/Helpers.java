package com.quanyan.pedometer.utils;

public class Helpers
{
	public static float TryParse(String str, float defaultFloat){
		try{
			float f = Float.parseFloat(str);
			return f;
		} catch (NumberFormatException ex){
			return defaultFloat;
		}
	}
}
