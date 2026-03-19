package com.example.triptale.database;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;
import com.example.triptale.model.Viaggio;
import java.util.List;

/**
 * Interfaccia Data Access Object (DAO) per l'entità Viaggio.
 * Fornisce i metodi per eseguire le operazioni CRUD (Create, Read, Update, Delete)
 * sulla tabella dei viaggi nel database locale.
 */
@Dao
public interface ViaggioDAO {

    /**
     * Inserisce un nuovo viaggio nel database.
     *
     * @param viaggio L'oggetto Viaggio da salvare.
     * @return L'ID univoco (Primary Key) generato automaticamente per il nuovo viaggio.
     */
    @Insert
    long inserisciViaggio(Viaggio viaggio);

    /**
     * Aggiorna i dati di un viaggio esistente nel database.
     *
     * @param viaggio L'oggetto Viaggio con i dati aggiornati.
     */
    @Update
    void aggiornaViaggio(Viaggio viaggio);

    /**
     * Recupera la lista completa di tutti i viaggi salvati.
     *
     * @return Una lista contenente tutti gli oggetti Viaggio presenti nel database.
     */
    @Query("SELECT * FROM tabella_viaggi")
    List<Viaggio> ottieniViaggi();

    /**
     * Elimina definitivamente tutti i viaggi dal database.
     * Attenzione: a causa della Foreign Key in modalità CASCADE, questa operazione
     * eliminerà in automatico anche tutte le tappe associate ai viaggi.
     */
    @Query("DELETE FROM tabella_viaggi")
    void eliminaViaggi();

    /**
     * Interroga il database per recuperare un singolo viaggio specifico.
     *
     * @param viaggioId L'identificatore univoco (Primary Key) del viaggio da cercare.
     * @return L'oggetto Viaggio corrispondente, oppure null se non trovato.
     */
    @Query("SELECT * FROM tabella_viaggi WHERE id = :viaggioId LIMIT 1")
    Viaggio ottieniViaggioPerId(int viaggioId);

    /**
     * Elimina un singolo viaggio specifico dal database.
     *
     * @param viaggio L'oggetto Viaggio da eliminare.
     */
    @Delete
    void eliminaViaggio(Viaggio viaggio);
}
