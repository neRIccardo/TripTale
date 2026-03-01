package com.example.triptale;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface ViaggioDAO {

    // Comando per SALVARE un nuovo viaggio
    @Insert
    void inserisciViaggio(Viaggio viaggio);

    // Comando per LEGGERE tutti i viaggi salvati
    @Query("SELECT * FROM tabella_viaggi")
    List<Viaggio> ottieniViaggi();

    // Comando per CANCELLARE un viaggio
    @Delete
    void eliminaViaggio(Viaggio viaggio);
}
