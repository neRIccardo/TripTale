package com.example.triptale;
import androidx.room.Database;
import androidx.room.RoomDatabase;

// Diciamo a Room che questo è il Database generale
// L'array "entities" elenca tutte le tabelle che abbiamo
// La "version = 1" serve se in futuro vorremo aggiungere nuove colonne (es. per il deep learning)
@Database(entities = {Viaggio.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    // Diciamo al database chi è il DAO
    public abstract ViaggioDAO viaggioDao();

}
