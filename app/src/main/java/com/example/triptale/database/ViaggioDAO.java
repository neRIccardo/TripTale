package com.example.triptale.database;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;
import com.example.triptale.model.Viaggio;
import java.util.List;

@Dao
public interface ViaggioDAO {

    // Comando per SALVARE un nuovo viaggio
    @Insert
    long inserisciViaggio(Viaggio viaggio);

    @Update
    void aggiornaViaggio(Viaggio viaggio);

    // Comando per LEGGERE tutti i viaggi salvati
    @Query("SELECT * FROM tabella_viaggi")
    List<Viaggio> ottieniViaggi();

    // Comando per cancellare tutti i viaggi
    @Query("DELETE FROM tabella_viaggi")
    void eliminaViaggi();

    // Comando per leggere un viaggio specifico
    @Query("SELECT * FROM tabella_viaggi WHERE id = :viaggioId LIMIT 1")
    Viaggio ottieniViaggioPerId(int viaggioId);

    // Comando per CANCELLARE un viaggio
    @Delete
    void eliminaViaggio(Viaggio viaggio);
}
