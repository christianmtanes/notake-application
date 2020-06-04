package com.tanxe.buggoff.notake.NotebookTable;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.tanxe.buggoff.notake.Utils.Converters;

import java.util.List;


@Database(entities = {Notebook.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class NotebookRoomDatabase extends RoomDatabase {
    public abstract NotebookDao notebookDao();

    private static NotebookRoomDatabase INSTANCE;
    private static final String TAG = "NotebookRoomDatabase";

    public static NotebookRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            Log.d(TAG, " NotebookRoomDatabase");

            synchronized (NotebookRoomDatabase.class) {
                if (INSTANCE == null) {
                    Log.d(TAG, " NotebookRoomDatabase.");

                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            NotebookRoomDatabase.class, "notebook_database")
                            .fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }

    public void insertNotebook(Notebook notebook) {
        new insertAsyncTask().execute(notebook);
    }

    public void deleteNotebook(Notebook notebook) {
        new deleteAsyncTask().execute(notebook);
    }

    public LiveData<List<Notebook>> getAllNotebooks() {
        return notebookDao().getAllNotebooks();
    }

    public List<Notebook> getAll() {
        return notebookDao().getAll();
    }

    public Notebook getNotebook(String id ) {
        return notebookDao().getNotebook(id);
    }


    public void updateDocId(final String id, final String docID) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                notebookDao().updateDocId(id, docID);
            }
        });
    }

    public void deleteAllNotebooks() {
        new deleteAllNotebooksAsyncTask().execute();
    }

    private class insertAsyncTask extends AsyncTask<Notebook, Void, Void> {
        @Override
        protected Void doInBackground(Notebook... notebook) {
            notebookDao().insert(notebook[0]);
            return null;
        }
    }

    private class deleteAsyncTask extends AsyncTask<Notebook, Void, Void> {
        @Override
        protected Void doInBackground(Notebook... notebook) {
            notebookDao().delete(notebook[0]);
            return null;
        }
    }

    private class deleteAllNotebooksAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            notebookDao().deleteAll();
            return null;
        }
    }
}
