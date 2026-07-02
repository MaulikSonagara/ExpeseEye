package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.Trip;

import java.util.List;

@Dao
public interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTimestamp DESC")
    LiveData<List<Trip>> getAllTrips();

    @Query("SELECT * FROM trips ORDER BY startTimestamp DESC")
    List<Trip> getAllTripsSync();

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    LiveData<Trip> getActiveTrip();

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    Trip getActiveTripSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Trip trip);

    @Update
    void update(Trip trip);

    @Delete
    void delete(Trip trip);

    @Query("UPDATE trips SET isActive = 0")
    void deactivateAllTrips();

    @Query("UPDATE trips SET isActive = 1 WHERE id = :tripId")
    void activateTrip(int tripId);

    @Query("SELECT * FROM trips WHERE id = :tripId")
    LiveData<Trip> getTripById(int tripId);

    @Query("SELECT * FROM trips WHERE id = :tripId")
    Trip getTripByIdSync(int tripId);
}
