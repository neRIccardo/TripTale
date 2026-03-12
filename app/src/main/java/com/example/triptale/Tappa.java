package com.example.triptale;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

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

    // Per definire una nuova Tappa
    public Tappa(int viaggioId, String titolo, String note) {
        this.viaggioId = viaggioId;
        this.titolo = titolo;
        this.note = note;
    }

    // =====================================
    // CODICE PARCELABLE
    // =====================================

    // Costruttore che "rimonta" l'oggetto estraendo i dati dal pacchetto
    protected Tappa(Parcel in) {
        id = in.readInt();
        viaggioId = in.readInt();
        titolo = in.readString();
        note = in.readString();
        imagePath = in.readString();
    }

    // "Creator" che Android usa per ricostruire l'oggetto
    public static final Creator<Tappa> CREATOR = new Creator<Tappa>() {
        @Override
        public Tappa createFromParcel(Parcel in) {
            return new Tappa(in);
        }

        @Override
        public Tappa[] newArray(int size) {
            return new Tappa[size];
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
        dest.writeInt(viaggioId);
        dest.writeString(titolo);
        dest.writeString(note);
        dest.writeString(imagePath);
    }
}
