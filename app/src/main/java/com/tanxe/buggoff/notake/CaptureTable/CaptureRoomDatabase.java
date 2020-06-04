package com.tanxe.buggoff.notake.CaptureTable;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.tanxe.buggoff.notake.Utils.Converters;

import java.util.ArrayList;
import java.util.List;


@Database(entities = {Capture.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})

public abstract class CaptureRoomDatabase extends RoomDatabase {

    public abstract CaptureDao captureDao();

    private static CaptureRoomDatabase INSTANCE;

    public static CaptureRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (CaptureRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            CaptureRoomDatabase.class, "capture_database")
                            .fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }

    public void insertCapture(Capture capture) {
        new insertAsyncTask().execute(capture);
    }

    public void deleteCapture(Capture capture) {
        new deleteAsyncTask().execute(capture);
    }

    public LiveData<List<Capture>> getAllCaptures() {
        return captureDao().getAllCaptures();
    }

    public List<Capture> getALLCapturesByID(String id) {
        return captureDao().getAllCapturesByID(id);
    }

    public LiveData<List<Capture>> getCapturesByID(String id) {
        return captureDao().getCapturesByID(id);
    }

    public void updateNotes(final String id, final String notes) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                captureDao().updateNotes(id, notes);
            }
        });
    }

    public void updateTags(final String id, final ArrayList<String> tags) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                captureDao().updateTags(id, tags);
            }
        });
    }

    public void updateDocID(final String id, final String docID) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                captureDao().updateDocID(id, docID);
            }
        });
    }

    public void updateImageID(final String id, final String imageId) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                captureDao().updateImageID(id, imageId);
            }
        });
    }

    public void deleteAllCaptures() {
        new deleteAllCapturesAsyncTask().execute();
    }

    private class insertAsyncTask extends AsyncTask<Capture, Void, Void> {
        @Override
        protected Void doInBackground(Capture... capture) {
            captureDao().insert(capture[0]);
            return null;
        }
    }

    private class deleteAsyncTask extends AsyncTask<Capture, Void, Void> {
        @Override
        protected Void doInBackground(Capture... capture) {
            captureDao().delete(capture[0]);
            return null;
        }
    }

    private class deleteAllCapturesAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            captureDao().deleteAll();
            return null;
        }
    }
}
