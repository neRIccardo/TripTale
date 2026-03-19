package com.example.triptale.utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import com.example.triptale.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Classe di utilità per la gestione della fotocamera e l'elaborazione delle immagini.
 * Si occupa della creazione di file temporanei sicuri per l'acquisizione, del ridimensionamento
 * intelligente per ottimizzare l'uso della RAM e dell'applicazione di watermark (timbro testuale).
 */
public class ImageUtils {

    /**
     * Crea un file temporaneo univoco all'interno della cartella privata dell'app (Pictures),
     * pronto per ricevere il flusso di dati binari catturato dalla fotocamera di sistema.
     *
     * @param context Il contesto dell'applicazione, necessario per accedere allo storage locale.
     * @return Un oggetto File univoco, inizialmente vuoto (0 byte).
     * @throws IOException Se si verifica un errore durante la creazione fisica del file sul disco.
     */
    public static File creaFileImmagine(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nomeFile = "JPEG_" + timeStamp + "_";
        // Prendiamo la cartella "Pictures" segreta della nostra app
        File cartellaStorage = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(nomeFile, ".jpg", cartellaStorage);
    }

    /**
     * Ottimizza un'immagine salvata su disco: per prevenire OutOfMemoryError (OOM), legge prima
     * solo i metadati (bounds) dell'immagine, calcola il fattore di riduzione ottimale e poi
     * la carica in RAM ridimensionata. Successivamente, utilizza Canvas e Paint per sovrimprimere
     * un watermark testuale. Infine, sovrascrive il file originale comprimendolo in JPEG all'85%.
     *
     * @param context Il contesto per accedere alle risorse testuali (il prefisso del watermark).
     * @param percorsoFile Il percorso assoluto del file immagine originale da elaborare.
     */
    public static void ridimensionaEApplicaWatermark(Context context, String percorsoFile) {
        // Leggiamo SOLO le dimensioni della foto dai metadati (zero impatto sulla RAM)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(percorsoFile, options);

        // Calcoliamo il fattore di riduzione (es. da 4000px a max 1024px)
        options.inSampleSize = calcolaFattoreRiduzione(options, 1024, 1024);

        // Carichiamo la foto VERA in RAM, ma già rimpicciolita
        options.inJustDecodeBounds = false;
        Bitmap bitmapOriginale = BitmapFactory.decodeFile(percorsoFile, options);

        if (bitmapOriginale == null) return; // Sicurezza extra

        // Per poterci "disegnare" sopra, la Bitmap deve essere di tipo "Mutable" (modificabile)
        Bitmap bitmapModificabile = bitmapOriginale.copy(Bitmap.Config.ARGB_8888, true);

        // Prepariamo Canvas e Paint
        Canvas canvas = new Canvas(bitmapModificabile);
        Paint pennello = new Paint();
        pennello.setColor(Color.WHITE); // Testo bianco
        pennello.setTextSize(70f); // Grandezza testo
        pennello.setAntiAlias(true); // Rende i bordi del testo morbidi
        // Mettiamo un'ombreggiatura nera al testo
        pennello.setShadowLayer(10f, 5f, 5f, Color.BLACK);

        // Creiamo il nostro testo (es. TripTale - 06/03/2026)
        String testoWatermark = context.getString(R.string.watermark_prefisso) + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        // Disegniamo il testo in basso a sinistra (x=50, y=altezzaFoto - 50)
        canvas.drawText(testoWatermark, 50, bitmapModificabile.getHeight() - 50, pennello);

        // Sovrascriviamo il file originale gigante con questa versione ridimensionata e "timbrata"
        try (FileOutputStream out = new FileOutputStream(percorsoFile)) {
            // Comprimiamo in JPEG all'85% di qualità per salvare un sacco di spazio su disco
            bitmapModificabile.compress(Bitmap.CompressFormat.JPEG, 85, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calcola il fattore matematico (potenza di 2) di riduzione della risoluzione dell'immagine
     * basandosi sulle dimensioni originali e su quelle massime desiderate. Un inSampleSize pari a 2
     * restituisce un'immagine con larghezza e altezza dimezzate (quindi 1/4 dei pixel totali).
     *
     * @param options L'oggetto BitmapFactory.Options contenente i metadati (larghezza/altezza originali).
     * @param reqWidth La larghezza massima desiderata per l'output.
     * @param reqHeight L'altezza massima desiderata per l'output.
     * @return Il fattore inSampleSize ottimale da applicare in fase di decodifica reale.
     */
    private static int calcolaFattoreRiduzione(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calcola il valore inSampleSize (potenza di 2) che mantiene la foto sopra la dimensione richiesta
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}