package com.example.triptale.database;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.triptale.model.Tappa;
import java.util.List;

@Dao
public interface TappaDAO {
    @Insert
    long inserisciTappa(Tappa tappa);

    @Update
    void aggiornaTappa(Tappa tappa);

    // Restituisci tappe di un certo Viaggio
    @Query("SELECT * FROM tabella_tappe WHERE viaggioId = :idDelViaggio")
    List<Tappa> ottieniTappeDelViaggio(int idDelViaggio);

    // Comando per leggere una tappa specifica
    @Query("SELECT * FROM tabella_tappe WHERE id = :tappaId LIMIT 1")
    Tappa ottieniTappaPerId(int tappaId);

    @Delete
    void eliminaTappa(Tappa tappa);
}
