package com.example.triptale.model;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

// Questa classe è una tabella del database
@Entity(tableName = "tabella_viaggi")
public class Viaggio implements Parcelable {

    // Ogni viaggio deve avere un ID unico. "autoGenerate = true" significa che
    // Room conterà da solo (1, 2, 3...) ogni volta che aggiungiamo un viaggio
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Colonne della nostra tabella
    public String titolo;
    public String cittaDestinazione;
    public String dataInizio;
    public String dataFine;
    public String imagePath;
    public String cloudId; // Collegamento tra Room e Firebase

    // Per definire un nuovo viaggio
    public Viaggio(String titolo, String cittaDestinazione, String dataInizio, String dataFine) {
        this.titolo = titolo;
        this.cittaDestinazione = cittaDestinazione;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
    }

    // =====================================
    // CODICE PARCELABLE
    // =====================================

    // Costruttore che "rimonta" l'oggetto estraendo i dati dal pacchetto
    protected Viaggio(Parcel in) {
        id = in.readInt();
        titolo = in.readString();
        cittaDestinazione = in.readString();
        dataInizio = in.readString();
        dataFine = in.readString();
        imagePath = in.readString();
        cloudId = in.readString();
    }

    // "Creator" che Android usa per ricostruire l'oggetto
    public static final Creator<Viaggio> CREATOR = new Creator<>() {
        @Override
        public Viaggio createFromParcel(Parcel in) {
            return new Viaggio(in);
        }

        @Override
        public Viaggio[] newArray(int size) {
            return new Viaggio[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // Serve solo in casi rari (es. se l'oggetto contiene File Descriptor)
    }

    // Metodo che "smonta" l'oggetto e lo impacchetta nel Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(titolo);
        dest.writeString(cittaDestinazione);
        dest.writeString(dataInizio);
        dest.writeString(dataFine);
        dest.writeString(imagePath);
        dest.writeString(cloudId);
    }
}
