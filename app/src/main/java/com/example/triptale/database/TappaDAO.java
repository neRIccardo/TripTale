package com.example.triptale.database;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.triptale.model.Tappa;
import java.util.List;

/**
 * Interfaccia Data Access Object (DAO) per l'entità Tappa.
 * Gestisce l'interazione con la tabella_tappe nel database locale.
 */
@Dao
public interface TappaDAO {

    /**
     * Inserisce una nuova tappa associandola a un viaggio esistente.
     *
     * @param tappa L'oggetto Tappa da salvare.
     * @return L'ID univoco generato per la nuova tappa.
     */
    @Insert
    long inserisciTappa(Tappa tappa);

    /**
     * Aggiorna le informazioni di una tappa esistente.
     *
     * @param tappa L'oggetto Tappa con i dati modificati.
     */
    @Update
    void aggiornaTappa(Tappa tappa);

    /**
     * Recupera tutte le tappe associate esclusivamente a un viaggio specifico.
     *
     * @param idDelViaggio L'ID del viaggio padre (Foreign Key).
     * @return Una lista di Tappe appartenenti a quel viaggio.
     */
    @Query("SELECT * FROM tabella_tappe WHERE viaggioId = :idDelViaggio")
    List<Tappa> ottieniTappeDelViaggio(int idDelViaggio);

    /**
     * Recupera una singola tappa tramite il suo ID.
     *
     * @param tappaId L'ID univoco della tappa.
     * @return L'oggetto Tappa corrispondente.
     */
    @Query("SELECT * FROM tabella_tappe WHERE id = :tappaId LIMIT 1")
    Tappa ottieniTappaPerId(int tappaId);

    /**
     * Elimina una specifica tappa dal database.
     *
     * @param tappa L'oggetto Tappa da rimuovere.
     */
    @Delete
    void eliminaTappa(Tappa tappa);
}
