package com.example.triptale;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TappaDAO {
    @Insert
    void inserisciTappa(Tappa tappa);

    // Restituisci tappe di un certo Viaggio
    @Query("SELECT * FROM tabella_tappe WHERE viaggioId = :idDelViaggio")
    List<Tappa> ottieniTappeDelViaggio(int idDelViaggio);

    @Delete
    void eliminaTappa(Tappa tappa);
}
