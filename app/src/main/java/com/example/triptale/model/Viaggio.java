package com.example.triptale.model;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Rappresenta l'entità "Viaggio" all'interno del database locale (Room).
 * Questa classe definisce la struttura della tabella "tabella_viaggi".
 * Implementa l'interfaccia Parcelable per consentire il passaggio efficiente
 * dell'intero oggetto tra i vari Fragment dell'interfaccia grafica tramite Bundle.
 */
@Entity(tableName = "tabella_viaggi")
public class Viaggio implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Colonne della nostra tabella
    public String titolo;
    public String cittaDestinazione;
    public String dataInizio;
    public String dataFine;
    public String imagePath;
    public String cloudId; // Collegamento tra Room e Firebase

    /**
     * Costruttore principale per creare una nuova istanza di Viaggio.
     * L'ID non è richiesto come parametro poiché viene autogenerato dal database
     * locale (autoGenerate = true), e il cloudId viene assegnato in un secondo momento da Firebase.
     *
     * @param titolo Il nome assegnato al viaggio dall'utente.
     * @param cittaDestinazione La città o il luogo di destinazione.
     * @param dataInizio La data di partenza (formato testuale).
     * @param dataFine La data di ritorno (formato testuale).
     */
    public Viaggio(String titolo, String cittaDestinazione, String dataInizio, String dataFine) {
        this.titolo = titolo;
        this.cittaDestinazione = cittaDestinazione;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
    }

    /**
     * Costruttore protetto utilizzato dal sistema Android per "rimontare" (deserializzare)
     * l'oggetto Viaggio a partire da un pacchetto Parcel ricevuto da un'altra schermata.
     *
     * @param in Il pacchetto Parcel contenente i dati serializzati.
     */
    protected Viaggio(Parcel in) {
        id = in.readInt();
        titolo = in.readString();
        cittaDestinazione = in.readString();
        dataInizio = in.readString();
        dataFine = in.readString();
        imagePath = in.readString();
        cloudId = in.readString();
    }

    /**
     * Generatore standard richiesto obbligatoriamente dall'interfaccia Parcelable.
     * Il sistema Android lo utilizza dietro le quinte per ricostruire (deserializzare)
     * l'oggetto a partire dal pacchetto Parcel ricevuto.
     */
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
        dest.writeString(titolo);
        dest.writeString(cittaDestinazione);
        dest.writeString(dataInizio);
        dest.writeString(dataFine);
        dest.writeString(imagePath);
        dest.writeString(cloudId);
    }
}
