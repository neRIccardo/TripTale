package com.example.triptale.model;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Rappresenta l'entità "Tappa" all'interno del database locale (Room).
 * Mantiene una forte relazione di chiave esterna (ForeignKey) con l'entità Viaggio.
 * La relazione è impostata con l'opzione CASCADE: se un viaggio viene eliminato,
 * tutte le tappe ad esso associate verranno eliminate automaticamente dal database.
 * Implementa Parcelable per la navigazione tra i Fragment.
 */
@Entity (tableName = "tabella_tappe",
        foreignKeys = @ForeignKey(
                entity = Viaggio.class,
                parentColumns = "id", // Id tabella Viaggio
                childColumns = "viaggioId",
                onDelete = ForeignKey.CASCADE // Se elimino un viaggio, elimino tutte le tappe
        ))

public class Tappa implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int viaggioId; // Collegamento a Viaggio
    public String titolo;
    public String note;
    public String imagePath;
    public String cloudId; // Collegamento tra Room e Firebase


    /**
     * Costruttore principale per creare una nuova istanza di Tappa.
     *
     * @param viaggioId L'ID del viaggio padre a cui questa tappa appartiene (Foreign Key).
     * @param titolo Il nome o l'intestazione della tappa.
     * @param note Appunti o descrizioni testuali aggiuntive inserite dall'utente.
     */
    public Tappa(int viaggioId, String titolo, String note) {
        this.viaggioId = viaggioId;
        this.titolo = titolo;
        this.note = note;
    }

    /**
     * Costruttore protetto utilizzato dal sistema Android per "rimontare" (deserializzare)
     * l'oggetto Tappa a partire da un pacchetto Parcel ricevuto da un'altra schermata.
     *
     * @param in Il pacchetto Parcel contenente i dati serializzati.
     */
    protected Tappa(Parcel in) {
        id = in.readInt();
        viaggioId = in.readInt();
        titolo = in.readString();
        note = in.readString();
        imagePath = in.readString();
        cloudId = in.readString();
    }

    /**
     * Generatore standard richiesto obbligatoriamente dall'interfaccia Parcelable.
     * Il sistema Android lo utilizza dietro le quinte per ricostruire (deserializzare)
     * l'oggetto a partire dal pacchetto Parcel ricevuto.
     */
    public static final Creator<Tappa> CREATOR = new Creator<>() {
        @Override
        public Tappa createFromParcel(Parcel in) {
            return new Tappa(in);
        }

        @Override
        public Tappa[] newArray(int size) {
            return new Tappa[size];
        }
    };

    /**
     * Descrive la tipologia di oggetti speciali contenuti all'interno della classe.
     * Di norma restituisce 0, a meno che non si stiano gestendo risorse particolari
     * come i File Descriptor.
     *
     * @return Un intero che rappresenta una maschera di bit (nel nostro caso 0).
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Metodo fondamentale che "smonta" (serializza) l'oggetto,
     * scrivendo tutti i suoi singoli attributi all'interno del pacchetto Parcel di destinazione,
     * in modo che possano essere trasportati in un'altra schermata.
     *
     * @param dest Il Parcel in cui scrivere i dati dell'oggetto.
     * @param flags Flag aggiuntivi per definire il comportamento di scrittura (solitamente 0).
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(viaggioId);
        dest.writeString(titolo);
        dest.writeString(note);
        dest.writeString(imagePath);
        dest.writeString(cloudId);
    }
}
