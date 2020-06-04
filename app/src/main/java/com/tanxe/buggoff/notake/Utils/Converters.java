package com.tanxe.buggoff.notake.Utils;

import android.accounts.Account;

import androidx.room.TypeConverter;

import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class Converters {
    static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @TypeConverter
    public static Date fromTimestamp(String value) {
        if (value != null) {
            try {
                return df.parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            return null;
        }
    }

    @TypeConverter
    public static String dateToTimestamp(Date value) {

        return value == null ? null : df.format(value);
    }

    @TypeConverter
    public static String AccountToString(Account account) {
        Gson gson = new Gson();
        String json = gson.toJson(account);

        return json;
        // return date == null ? null : date.getTime();
    }
    @TypeConverter
    public static Account StringToAccount(String accountString) {
        Gson gson = new Gson();
        Account json = gson.fromJson(accountString, Account.class);

        return json;
        // return date == null ? null : date.getTime();
    }
    @TypeConverter
    public String fromArray(ArrayList<String> strings) {
        String string = "";
        for(String s : strings) string += (s + ",");

        return string;
    }

    @TypeConverter
    public ArrayList<String> toArray(String concatenatedStrings) {
        ArrayList<String> myStrings = new ArrayList<>();

        for(String s : concatenatedStrings.split(",")) myStrings.add(s);

        return myStrings;
    }


    @TypeConverter
    public String fromSet(HashSet<String> strings) {
        if (strings== null)
            return null;
        String string = "";
        for(String s : strings) string += (s + ",");

        return string;
    }

    @TypeConverter
    public HashSet<String> toSet(String concatenatedStrings) {
        if (concatenatedStrings==null)
            return null;
        HashSet<String> myStrings = new HashSet<>();

        for(String s : concatenatedStrings.split(",")) myStrings.add(s);

        return myStrings;
    }
}
