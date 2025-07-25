package com.example.soyabean_disease;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "prediction_table")
public class PredictionEntry {

    @PrimaryKey(autoGenerate = true)

    public int id;
    public String imagePath;
    public String result;
    public float confidence;
    public long timestamp;

    public PredictionEntry(String imagePath, String result, float confidence, long timestamp) {
        this.imagePath = imagePath;
        this.result = result;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }
}
