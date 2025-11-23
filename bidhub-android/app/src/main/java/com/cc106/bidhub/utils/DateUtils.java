package com.cc106.bidhub.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
    private static final String API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String DISPLAY_DATE_FORMAT = "MMM dd, yyyy HH:mm";
    
    public static String formatDate(String dateString) {
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.US);
            apiFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = apiFormat.parse(dateString);
            
            SimpleDateFormat displayFormat = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.US);
            return displayFormat.format(date);
        } catch (ParseException e) {
            return dateString;
        }
    }
    
    public static String formatDateShort(String dateString) {
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.US);
            apiFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = apiFormat.parse(dateString);
            
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            return displayFormat.format(date);
        } catch (ParseException e) {
            return dateString;
        }
    }
}

