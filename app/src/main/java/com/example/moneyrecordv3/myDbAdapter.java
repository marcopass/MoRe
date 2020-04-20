package com.example.moneyrecordv3;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.ScrollView;
import android.widget.Toast;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class myDbAdapter {
    myDbHelper myHelper;
    public myDbAdapter(Context context) {
        myHelper = new myDbHelper(context);
    }

    public long insertData(String date, Float amount, String method, String category, String comments) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(myDbHelper.DATE, date);
        contentValues.put(myDbHelper.AMOUNT, amount);
        contentValues.put(myDbHelper.METHOD, method);
        contentValues.put(myDbHelper.CATEGORY, category);
        contentValues.put(myDbHelper.COMMENTS, comments);
        long id = db.insert(myDbHelper.TABLE_NAME, null, contentValues);
        return id;
    }

    public String getDataOrderedByDate() {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        String[] columns = {myDbHelper.UID,
                            myDbHelper.DATE,
                            myDbHelper.AMOUNT,
                            myDbHelper.METHOD,
                            myDbHelper.CATEGORY,
                            myDbHelper.COMMENTS};
        Cursor cursor = db.query(myDbHelper.TABLE_NAME, columns, null, null, null, null, myDbHelper.DATE + " DESC, " + myDbHelper.UID + " DESC");
        StringBuffer buffer = new StringBuffer();
        while (cursor.moveToNext()) {
            int cid = cursor.getInt(cursor.getColumnIndex(myDbHelper.UID));
            String date = cursor.getString(cursor.getColumnIndex(myDbHelper.DATE));
            float amount = cursor.getFloat(cursor.getColumnIndex(myDbHelper.AMOUNT));
            String method = cursor.getString(cursor.getColumnIndex(myDbHelper.METHOD));
            String category = cursor.getString(cursor.getColumnIndex(myDbHelper.CATEGORY));
            String comments = cursor.getString(cursor.getColumnIndex(myDbHelper.COMMENTS));
            buffer.append(cid + " " + date + " " + amount + " " + method + " " + category + " " + comments + "\n");
        }
        return buffer.toString();
    }

    public int deleteById(String id) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        String[] whereArgs = {id};
        int count = db.delete(myDbHelper.TABLE_NAME, myDbHelper.UID + " =?", whereArgs);
        return count;
    }

    // Output: Totale <value>
    //         <category> <value>
    public String getTotalPerMonthPerCategory(String year, String month, boolean withSpecial) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        String[] whereArgs = {year, month};
        Cursor cursor;
        StringBuffer buffer = new StringBuffer();

        String withSpecialString = " AND abs(" + myDbHelper.AMOUNT + ") < " + Config.SPECIAL_TRANS_THRESHOLD;
        if (withSpecial) {
            withSpecialString = "";
        }

        cursor = db.rawQuery("SELECT SUM(" + myDbHelper.AMOUNT + ") FROM " + myDbHelper.TABLE_NAME
                + " WHERE strftime('%Y', " + myDbHelper.DATE + ") = ? AND strftime('%m', " + myDbHelper.DATE + ") = ?" + withSpecialString, whereArgs);
        cursor.moveToFirst();
        buffer.append("Totale " + String.valueOf(cursor.getFloat(0)) + "\n");

        cursor = db.rawQuery("SELECT SUM(" + myDbHelper.AMOUNT + ") AS Total, " + myDbHelper.CATEGORY + " FROM " + myDbHelper.TABLE_NAME
                + " WHERE strftime('%Y', " + myDbHelper.DATE + ") = ? AND strftime('%m', " + myDbHelper.DATE + ") = ?" + withSpecialString + " GROUP BY " + myDbHelper.CATEGORY
                + " ORDER BY Total DESC", whereArgs);

        while (cursor.moveToNext()) {
            String category = cursor.getString(cursor.getColumnIndex(myDbHelper.CATEGORY));
            String total = String.valueOf(cursor.getFloat(cursor.getColumnIndex("Total")));
            buffer.append(category + " " + total + "\n");
        }
        return  buffer.toString();
    }

    public String customSearch(String amount, String date, String method, String category, String comments) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        Cursor cursor;
        StringBuffer buffer = new StringBuffer();

        String amountQuery = "";
        if (!amount.isEmpty()) {
            if (amount.indexOf('<') == 0) {
                amountQuery = myDbHelper.AMOUNT + " < " + amount.substring(1);
            } else if (amount.indexOf('>') == 0) {
                amountQuery = myDbHelper.AMOUNT + " > " + amount.substring(1);
            } else if (amount.indexOf('-') > 0) {
                amountQuery = myDbHelper.AMOUNT + " > " + amount.substring(0, amount.indexOf('-')) + " AND " + myDbHelper.AMOUNT + " < " + amount.substring(amount.indexOf('-')+1);
            } else {
                amountQuery = myDbHelper.AMOUNT + " = " + amount;
            }
        }

        String dateQuery = "";
        if (!date.isEmpty()) {
            if (date.indexOf('<') == 0) {
                if (date.substring(1).matches("xx/xx/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") <= julianday('20" + date.substring(7,9) + "-12-31')";
                } else if (date.substring(1).matches("xx/\\d{2}/\\d{2}")) {
                    YearMonth yearMonth = YearMonth.of(Integer.valueOf("20" + date.substring(7,9)), Integer.valueOf(date.substring(4,6)));
                    dateQuery = "julianday(" + myDbHelper.DATE + ") <= julianday('20" + date.substring(7,9) + "-" + date.substring(4,6) + "-" + String.valueOf(yearMonth.lengthOfMonth()) + "')";
                } else if (date.substring(1).matches("\\d{2}/\\d{2}/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") <= julianday('20" + date.substring(7,9) + "-" + date.substring(4,6) + "-" + date.substring(1,3) + "')";
                }
            } else if (date.indexOf('>') == 0) {
                if (date.substring(1).matches("xx/xx/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") >= julianday('20" + date.substring(7,9) + "-01-01')";
                } else if (date.substring(1).matches("xx/\\d{2}/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") >= julianday('20" + date.substring(7,9) + "-" + date.substring(4,6) + "-01')";
                } else if (date.substring(1).matches("\\d{2}/\\d{2}/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") >= julianday('20" + date.substring(7,9) + "-" + date.substring(4,6) + "-" + date.substring(1,3) + "')";
                }
            } else if (date.indexOf('-') > 0) {
                if (date.substring(0,8).matches("xx/xx/\\d{2}") && date.substring(9,17).matches("xx/xx/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") >= julianday('20" + date.substring(6,8) + "-01-01') AND julianday(" + myDbHelper.DATE + ") <= julianday('20" + date.substring(15,17) + "-12-31')";
                } else if (date.substring(0,8).matches("xx/\\d{2}/\\d{2}") && date.substring(9,17).matches("xx/\\d{2}/\\d{2}")) {
                    YearMonth yearMonth = YearMonth.of(Integer.valueOf("20" + date.substring(15,17)), Integer.valueOf(date.substring(12,14)));
                    dateQuery = "julianday(" + myDbHelper.DATE + ") >= julianday('20" + date.substring(6,8) + "-" + date.substring(3,5) + "-01') AND julianday(" + myDbHelper.DATE + ") <= julianday('20" + date.substring(15,17) + "-" + date.substring(12,14) + "-" + String.valueOf(yearMonth.lengthOfMonth()) + "')";
                } else if (date.substring(0,8).matches("\\d{2}/\\d{2}/\\d{2}") && date.substring(9,17).matches("\\d{2}/\\d{2}/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") >= julianday('20" + date.substring(6,8) + "-" + date.substring(3,5) + "-" + date.substring(0,2) + "') AND julianday(" + myDbHelper.DATE + ") <= julianday('20" + date.substring(15,17) + "-" + date.substring(12,14) + "-" + date.substring(9,11) + "')";
                }
            } else {
                if (date.matches("xx/xx/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") >= julianday('20" + date.substring(6,8) + "-01-01') AND julianday(" + myDbHelper.DATE + ") <= julianday('20" + date.substring(6,8) + "-12-31')";
                } else if (date.matches("xx/\\d{2}/\\d{2}")) {
                    YearMonth yearMonth = YearMonth.of(Integer.valueOf("20" + date.substring(6,8)), Integer.valueOf(date.substring(3,5)));
                    dateQuery = "julianday(" + myDbHelper.DATE + ") >= julianday('20" + date.substring(6,8) + "-" + date.substring(3,5) + "-01') AND julianday(" + myDbHelper.DATE + ") <= julianday('20" + date.substring(6,8) + "-" + date.substring(3,5) + "-" + String.valueOf(yearMonth.lengthOfMonth()) + "')";
                } else if (date.matches("\\d{2}/\\d{2}/\\d{2}")) {
                    dateQuery = "julianday(" + myDbHelper.DATE + ") = julianday('20" + date.substring(6,8) + "-" + date.substring(3,5) + "-" + date.substring(0,2) + "')";
                }
            }
        }

        String methodQuery = "";
        switch (method) {
            case "Cash":
                methodQuery = myDbHelper.METHOD + " = 'Cash'";
                break;
            case "Card":
                methodQuery = myDbHelper.METHOD + " = 'Card'";
                break;
        }

        String categoryQuery = myDbHelper.CATEGORY + " LIKE '%" + category.replace(" ", "_") + "%'";

        String commentsQuery = myDbHelper.COMMENTS + " LIKE '%" + comments.replace(" ", "_") + "%'";

        String whereQuery = " WHERE ";
        whereQuery += amountQuery.isEmpty() ? "" : (amountQuery + " AND ");
        whereQuery += dateQuery.isEmpty() ? "" : (dateQuery + " AND ");
        whereQuery += methodQuery.isEmpty() ? "" : (methodQuery + " AND ");
        whereQuery += categoryQuery.isEmpty() ? "" : (categoryQuery + " AND ");
        whereQuery += commentsQuery.isEmpty() ? "" : (commentsQuery + " AND ");

        whereQuery = whereQuery.substring(0, whereQuery.length()-5);

        cursor = db.rawQuery("SELECT COUNT(*) AS Count, SUM(" + myDbHelper.AMOUNT + ") AS Sum, AVG(" + myDbHelper.AMOUNT + ") AS Avg FROM " + myDbHelper.TABLE_NAME + whereQuery + " ORDER BY " + myDbHelper.DATE + " DESC", new String[] {});

        cursor.moveToFirst();
        String count = cursor.getString(cursor.getColumnIndex("Count"));
        String sum = cursor.getString(cursor.getColumnIndex("Sum"));
        String avg = cursor.getString(cursor.getColumnIndex("Avg"));
        buffer.append("QuickSummary " + count + " " + sum + " " + avg + "\n");

        cursor = db.rawQuery("SELECT * FROM " + myDbHelper.TABLE_NAME + whereQuery + " ORDER BY " + myDbHelper.DATE + " DESC", new String[] {});

        while (cursor.moveToNext()) {
            int cid = cursor.getInt(cursor.getColumnIndex(myDbHelper.UID));
            String dateRes = cursor.getString(cursor.getColumnIndex(myDbHelper.DATE));
            float amountRes = cursor.getFloat(cursor.getColumnIndex(myDbHelper.AMOUNT));
            String methodRes = cursor.getString(cursor.getColumnIndex(myDbHelper.METHOD));
            String categoryRes = cursor.getString(cursor.getColumnIndex(myDbHelper.CATEGORY));
            String commentsRes = cursor.getString(cursor.getColumnIndex(myDbHelper.COMMENTS));
            buffer.append(cid + " " + dateRes + " " + amountRes + " " + methodRes + " " + categoryRes + " " + commentsRes + "\n");
        }

        return buffer.toString();
    }

    // Output: <method> <value>
    public String getTotalPerMonthPerMethod(String year, String month) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        String[] whereArgs = {year, month};
        Cursor cursor = db.rawQuery("SELECT SUM(" + myDbHelper.AMOUNT + ") AS Total, " + myDbHelper.METHOD + " FROM " + myDbHelper.TABLE_NAME
                + " WHERE strftime('%Y', " + myDbHelper.DATE + ") = ? AND strftime('%m', " + myDbHelper.DATE + ") = ? AND abs(" + myDbHelper.AMOUNT + ") < " + Config.SPECIAL_TRANS_THRESHOLD + " GROUP BY " + myDbHelper.METHOD
                + " ORDER BY " + myDbHelper.METHOD + " DESC", whereArgs);
        StringBuffer buffer = new StringBuffer();
        while (cursor.moveToNext()) {
            String method = cursor.getString(cursor.getColumnIndex(myDbHelper.METHOD));
            String total = String.valueOf(cursor.getFloat(cursor.getColumnIndex("Total")));
            buffer.append(method + " " + total + "\n");
        }
        return  buffer.toString();
    }

    // Output: <category>
    public String getTotalPerCategory() {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + myDbHelper.AMOUNT + ") AS Total, " + myDbHelper.CATEGORY + " FROM " + myDbHelper.TABLE_NAME
                                    + " GROUP BY " + myDbHelper.CATEGORY
                                    + " ORDER BY Total DESC", null);
        StringBuffer buffer = new StringBuffer();
        while (cursor.moveToNext()) {
            String category = cursor.getString(cursor.getColumnIndex(myDbHelper.CATEGORY));
            String total = String.valueOf(cursor.getFloat(cursor.getColumnIndex("Total")));
            buffer.append(category + " " + total + "\n");
        }
        return  buffer.toString();
    }

    // Output: Totale <value>
    //         Stipendio <value>
    public String getTotalAndRevenueAll() {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        String[] whereArgs = {};
        Cursor cursor;
        StringBuffer buffer = new StringBuffer();

        cursor = db.rawQuery("SELECT SUM(" + myDbHelper.AMOUNT + ") FROM " + myDbHelper.TABLE_NAME, whereArgs);
        cursor.moveToFirst();
        buffer.append("Totale " + String.valueOf(cursor.getFloat(0)) + "\n");

        String[] whereArgs2 = {"Stipendio"};
        cursor = db.rawQuery("SELECT SUM(" + myDbHelper.AMOUNT + ") FROM " + myDbHelper.TABLE_NAME
                            + " WHERE " + myDbHelper.CATEGORY + " = ?", whereArgs2);
        cursor.moveToFirst();
        buffer.append("Stipendio " + String.valueOf(cursor.getFloat(0)) + "\n");

        return buffer.toString();
    }

    // Output: Totale <value>
    //         Stipendio <value>
    public String getTotalAndRevenueYear(String year) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        String[] whereArgs = {year};
        Cursor cursor;
        StringBuffer buffer = new StringBuffer();

        cursor = db.rawQuery("SELECT SUM(" + myDbHelper.AMOUNT + ") FROM " + myDbHelper.TABLE_NAME
                        + " WHERE strftime('%Y', " + myDbHelper.DATE + ") = ? ", whereArgs);
        cursor.moveToFirst();
        buffer.append("Totale " + String.valueOf(cursor.getFloat(0)) + "\n");

        String[] whereArgs2 = {year, "Stipendio"};
        cursor = db.rawQuery("SELECT SUM(" + myDbHelper.AMOUNT + ") FROM " + myDbHelper.TABLE_NAME
                + " WHERE strftime('%Y', " + myDbHelper.DATE + ") = ? AND " + myDbHelper.CATEGORY + " = ?", whereArgs2);
        cursor.moveToFirst();
        buffer.append("Stipendio " + String.valueOf(cursor.getFloat(0)) + "\n");

        return buffer.toString();
    }

    // Output: <category>
    public String getAllCategories() {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        String[] whereArgs = {};
        StringBuffer buffer = new StringBuffer();
        Cursor cursor = db.rawQuery("SELECT " + myDbHelper.CATEGORY + " FROM " + myDbHelper.TABLE_NAME
                                    + " GROUP BY " + myDbHelper.CATEGORY, whereArgs);
        while (cursor.moveToNext()) {
            String category = cursor.getString(cursor.getColumnIndex(myDbHelper.CATEGORY));
            buffer.append(category + "\n");
        }
        return  buffer.toString();
    }

    static class myDbHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "MoneyRecordDB";
        private static final String TABLE_NAME = "Transactions";
        private static final int DATABASE_VERSION = 1;
        private static final String UID = "_id";
        private static final String DATE = "Date";
        private static final String AMOUNT = "Amount";
        private static final String METHOD = "Method";
        private static final String CATEGORY = "Category";
        private static final String COMMENTS = "Comments";

        private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
                + " (" + UID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DATE + " DATE, "
                + AMOUNT + " DECIMAL(8,2), "
                + METHOD + " VARCHAR(16), "
                + CATEGORY + " VARCHAR(255), "
                + COMMENTS + " VARCHAR(255));";
        private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

        private Context context;

        public myDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            this.context = context;
        }

        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(CREATE_TABLE);
            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                Toast.makeText(context, "onUpgrade", Toast.LENGTH_SHORT).show();
                db.execSQL(DROP_TABLE);
                onCreate(db);
            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
