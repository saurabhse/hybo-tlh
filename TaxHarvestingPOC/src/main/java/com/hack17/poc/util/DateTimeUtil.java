package com.hack17.poc.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class DateTimeUtil {
	private static DateFormat sdf = DateFormat.getDateInstance(DateFormat.DEFAULT);
	
	public static Date getDate(String date){
		try {
			return sdf.parse(date);
		} catch (ParseException e) {			
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
