package com.example.triptale;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Questa classe è una tabella del database
@Entity(tableName = "tabella_viaggi")
public class Viaggio {

    // Ogni viaggio deve avere un ID unico. "autoGenerate = true" significa che
    // Room conterà da solo (1, 2, 3...) ogni volta che aggiungiamo un viaggio
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Colonne della nostra tabella
    public String titolo;
    public String dataInizio;
    public String dataFine;

    // Per definire un nuovo viaggio
    public Viaggio(String titolo, String dataInizio, String dataFine) {
        this.titolo = titolo;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
    }
}
