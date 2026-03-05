package com.example.triptale;
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Diciamo a Room che questo è il Database generale
// L'array "entities" elenca tutte le tabelle che abbiamo
// La "version = 1" serve se in futuro vorremo aggiungere nuove colonne (es. per il deep learning)
@Database(entities = {Viaggio.class, Tappa.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    // Diciamo al database chi sono i DAO
    public abstract ViaggioDAO viaggioDao();
    public abstract TappaDAO tappaDao();

    // Questa variabile terrà in memoria il nostro database aperto
    private static volatile AppDatabase INSTANCE;

    // =========================================================================
    // METODO PER CREARE UNA ISTANZA DEL DATABASE
    // =========================================================================
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Se non esiste ancora, costruiamo materialmente il file del database
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "triptale_database").build();
                }
            }
        }
        return INSTANCE;
    }
}
