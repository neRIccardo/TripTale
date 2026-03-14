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
 * Classe di utilità per la gestione, il ridimensionamento
 * e l'applicazione di watermark alle immagini scattate.
 */
public class ImageUtils {

    // =========================================================================
    // METODO PER CREARE UN FILE VUOTO PER LA FOTOCAMERA
    // =========================================================================
    public static File creaFileImmagine(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nomeFile = "JPEG_" + timeStamp + "_";
        // Prendiamo la cartella "Pictures" segreta della nostra app
        File cartellaStorage = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(nomeFile, ".jpg", cartellaStorage);
    }

    // ==========================================================
    // METODO MANIPOLAZIONE IMMAGINI: RIDIMENSIONAMENTO E WATERMARK
    // ==========================================================
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

    // ==========================================================
    // METODO PER CALCOLARE IL FATTORE DI RIDUZIONE
    // ==========================================================
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