package com.example.moneyrecordv3;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class Config {

    // All transactions above this value are considered to be special
    public static float SPECIAL_TRANS_THRESHOLD = 100f;

    // Column with for history table layout
    public static int COL_ID_WIDTH_DP = 50;
    public static int COL_DATE_WIDTH_DP = 80;
    public static int COL_AMOUNT_WIDTH_DP = 90;
    public static int COL_CATEGORY_WIDTH_DP = 100;
    public static int COL_COMMENTS_WIDTH_DP = 260;

    // Column width for month summary table layout
    public static int COL_SUBCATEGORY_WIDTH_DP = 95;
    public static int COL_VALUE_WIDTH_DP = 75;

    public static float TEXT_SIZE = 16f;
    public static int AMOUNT_PADDING_RIGHT = 25;

    // Colors for various charts
    public static String[] MY_CHART_COLORS = {"#4285F4", "#DB4437", "#F4B400", "#0F9D58", "#FF6D00", "#46BDC6"};
    public  static String[] MY_CHART_COLORS_2 = {"#0F9D58", "#DB4437", "#FF6D00"};

    static public String map(String month) {
        String m = "";
        switch (month) {
            case "Jan":
                m = "01";
                break;
            case "Feb":
                m = "02";
                break;
            case "Mar":
                m = "03";
                break;
            case "Apr":
                m = "04";
                break;
            case "May":
                m = "05";
                break;
            case "Jun":
                m = "06";
                break;
            case "Jul":
                m = "07";
                break;
            case "Aug":
                m = "08";
                break;
            case "Sep":
                m = "09";
                break;
            case "Oct":
                m = "10";
                break;
            case "Nov":
                m = "11";
                break;
            case "Dec":
                m = "12";
                break;
        }

        return m;
    }

    static public String inverseMap(String month) {
        String m = "";
        switch (month) {
            case "01":
                m = "Jan";
                break;
            case "02":
                m = "Feb";
                break;
            case "03":
                m = "Mar";
                break;
            case "04":
                m = "Apr";
                break;
            case "05":
                m = "May";
                break;
            case "06":
                m = "Jun";
                break;
            case "07":
                m = "Jul";
                break;
            case "08":
                m = "Aug";
                break;
            case "09":
                m = "Sep";
                break;
            case "10":
                m = "Oct";
                break;
            case "11":
                m = "Nov";
                break;
            case "12":
                m = "Dec";
                break;
        }

        return m;
    }

    public static String[] MONTH_ARRAY = new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
}
