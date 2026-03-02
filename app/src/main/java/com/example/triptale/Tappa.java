package com.example.triptale;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity (tableName = "tabella_tappe",
        foreignKeys = @ForeignKey(
                entity = Viaggio.class,
                parentColumns = "id", // Id tabella Viaggio
                childColumns = "viaggioId",
                onDelete = ForeignKey.CASCADE // Se elimino un viaggio, elimino tutte le tappe
        ))

public class Tappa implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int viaggioId; // Collegamento a Viaggio
    public String titolo;
    public String note;
    public String imagePath;

    // Per definire una nuova Tappa
    public Tappa(int viaggioId, String titolo, String note) {
        this.viaggioId = viaggioId;
        this.titolo = titolo;
        this.note = note;
    }
}
