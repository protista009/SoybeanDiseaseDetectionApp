package com.example.soyabean_disease;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PredictionEntry.class}, version = 1)
public abstract class PredictionDatabase extends RoomDatabase {

    private static PredictionDatabase instance;

    public abstract PredictionDao predictionDao();

    public static synchronized PredictionDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            PredictionDatabase.class, "prediction_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
