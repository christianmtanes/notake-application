package com.tanxe.buggoff.notake.NotebookTable;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotebookDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Notebook notebook);

    @Query("DELETE FROM notebook_table")
    void deleteAll();

    @Query("SELECT * from notebook_table")
    LiveData<List<Notebook>> getAllNotebooks();

    @Query("SELECT * from notebook_table WHERE id = :notebookID")
    Notebook getNotebook(String notebookID);

    @Query("SELECT * from notebook_table")
    List<Notebook> getAll();

    @Delete
    void delete(Notebook notebook);

    @Query("UPDATE notebook_table SET docID = :docID WHERE id = :tid")
    void updateDocId(String tid, String docID);
}
