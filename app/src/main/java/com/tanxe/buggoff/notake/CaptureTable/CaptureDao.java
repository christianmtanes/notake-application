package com.tanxe.buggoff.notake.CaptureTable;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;


@Dao
public interface CaptureDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Capture capture);

    @Query("DELETE FROM capture_table")
    void deleteAll();

    @Query("SELECT * from capture_table")
    LiveData<List<Capture>> getAllCaptures();

    @Query("SELECT * from capture_table")
    List<Capture> getAll();

    @Query("SELECT * FROM capture_table WHERE NotebookID=:NotebookID ")
    LiveData<List<Capture>> getCapturesByID(String NotebookID);

    @Query("SELECT * FROM capture_table WHERE NotebookID=:NotebookID ")
    List<Capture> getAllCapturesByID(String NotebookID);

    @Delete
    void delete(Capture capture);

    @Query("UPDATE capture_table SET comments = :comments WHERE id = :tid")
    void updateNotes(String tid, String comments);

    @Query("UPDATE capture_table SET tags = :tags WHERE id = :tid")
    void updateTags(String tid, ArrayList<String> tags);

    @Query("UPDATE capture_table SET imageID = :imageId WHERE id = :tid")
    void updateImageID(String tid, String imageId);
    @Query("UPDATE capture_table SET docID = :docID WHERE id = :tid")
    void updateDocID(String tid, String docID);
}


