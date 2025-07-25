package com.example.soyabean_disease;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface PredictionDao {

    @Insert
    void insert(PredictionEntry entry);

    @Query("SELECT * FROM prediction_table ORDER BY timestamp DESC")
    List<PredictionEntry> getAllPredictions();

    @Query("DELETE FROM prediction_table")
    void clearAll(); // just one delete method is enough
}


