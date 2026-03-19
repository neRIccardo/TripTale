package com.example.triptale.database;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.triptale.model.Tappa;
import com.example.triptale.model.Viaggio;

/**
 * Classe astratta principale del database Room dell'applicazione.
 * Definisce la configurazione del database locale e fa da punto di accesso
 * per i Data Access Objects (DAO). Utilizza il pattern Singleton.
 */
@Database(entities = {Viaggio.class, Tappa.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    // Questa variabile terrà in memoria il nostro database aperto
    private static volatile AppDatabase INSTANCE;

    /**
     * Fornisce il DAO per le operazioni sull'entità Viaggio.
     *
     * @return L'istanza di ViaggioDAO.
     */
    public abstract ViaggioDAO viaggioDao();

    /**
     * Fornisce il DAO per le operazioni sull'entità Tappa.
     *
     * @return L'istanza di TappaDAO.
     */
    public abstract TappaDAO tappaDao();

    /**
     * Restituisce l'istanza Singleton del database, garantendo che
     * ne esista solo una in tutta l'applicazione per evitare memory leak e conflitti.
     *
     * @param context Il contesto dell'applicazione.
     * @return L'istanza univoca di AppDatabase.
     */
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
