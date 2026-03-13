package com.example.triptale;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;
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

    // Comando per CANCELLARE un viaggio
    @Delete
    void eliminaViaggio(Viaggio viaggio);
}
