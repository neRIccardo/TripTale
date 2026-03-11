package com.example.triptale;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ModificaTappaFragment extends Fragment {

    private Tappa tappaCorrente;
    private ImageView imageAnteprima;
    private String percorsoFotoAttuale = null; // Il percorso che salveremo nel DB
    private Uri uriFotoTemporanea = null;
    private String percorsoNuovaFotoTemp = null; // Il file da 0 byte in attesa
    private boolean salvataggioCompletato = false; // Ci dice se l'utente ha premuto "Salva"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_modifica_tappa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editTitolo = view.findViewById(R.id.editTitoloTappaModifica);
        EditText editNote = view.findViewById(R.id.editNoteTappaModifica);
        imageAnteprima = view.findViewById(R.id.imageAnteprimaTappaModifica);
        Button btnScatta = view.findViewById(R.id.btnScattaFotoTappaModifica);
        Button btnSalva = view.findViewById(R.id.btnSalvaTappaModifica);

        if (getArguments() != null) {
            tappaCorrente = (Tappa) getArguments().getSerializable("tappa_selezionata");
            if (tappaCorrente != null) {
                editTitolo.setText(tappaCorrente.titolo);
                editNote.setText(tappaCorrente.note);
                percorsoFotoAttuale = tappaCorrente.imagePath;

                if (tappaCorrente.imagePath != null) {
                    try {
                        imageAnteprima.setImageURI(Uri.parse(tappaCorrente.imagePath));
                    } catch (Exception e) {
                        imageAnteprima.setImageResource(android.R.drawable.ic_menu_camera);
                    }
                }
            }
        }

        //--- GESTIONE BOTTONE SCATTO FOTO ---
        btnScatta.setOnClickListener(v -> {
            try {
                File fileImmagine = creaFileImmagine();
                percorsoNuovaFotoTemp = fileImmagine.getAbsolutePath();
                uriFotoTemporanea = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        fileImmagine
                );
                scattaFotoLauncher.launch(uriFotoTemporanea);
            } catch (IOException e) {
                Toast.makeText(requireContext(), "Errore nell'apertura della fotocamera", Toast.LENGTH_SHORT).show();
            }
        });

        // --- GESTIONE BOTTONE SALVATAGGIO TAPPA ---
        btnSalva.setOnClickListener(v -> {
            salvataggioCompletato = true;
            if (tappaCorrente.imagePath != null && !tappaCorrente.imagePath.equals(percorsoFotoAttuale)) {
                File vecchiaFoto = new File(tappaCorrente.imagePath);
                if (vecchiaFoto.exists()) {
                    vecchiaFoto.delete();
                }
            }

            tappaCorrente.titolo = editTitolo.getText().toString().trim();
            tappaCorrente.note = editNote.getText().toString().trim();
            tappaCorrente.imagePath = percorsoFotoAttuale;

            if(tappaCorrente.titolo.isEmpty()) {
                editTitolo.setError("Il titolo è obbligatorio!");
                editTitolo.requestFocus();
                return;
            }

            // Aggiorniamo il Database
            new Thread(() -> {
                AppDatabase.getInstance(requireContext()).tappaDao().aggiornaTappa(tappaCorrente);

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Tappa aggiornata!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                });
            }).start();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Se l'utente sta uscendo dalla schermata SENZA aver premuto "Salva"...
        // e ha scattato una foto nuova...
        if (!salvataggioCompletato && percorsoFotoAttuale != null && !percorsoFotoAttuale.equals(tappaCorrente.imagePath)) {
            // Eliminiamo la foto nuova "orfana", mantenendo intatta quella vecchia sul telefono
            new File(percorsoFotoAttuale).delete();
        }
    }

    // =========================================================================
    // METODO PER CREARE UN FILE VUOTO PER LA FOTOCAMERA
    // =========================================================================
    private File creaFileImmagine() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nomeFile = "JPEG_" + timeStamp + "_";
        // Prendiamo la cartella "Pictures" segreta della nostra app
        File cartellaStorage = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(nomeFile, ".jpg", cartellaStorage);
    }

    // =========================================================================
    // OGGETTO PER GESTIRE L'INTENT DELLA FOTOCAMERA
    // =========================================================================
    private final ActivityResultLauncher<Uri> scattaFotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            esitoPositivo -> {
                if (esitoPositivo) {
                    if (percorsoNuovaFotoTemp != null) {
                        // Applichiamo il watermark e ridimensionamento alla nuova foto
                        ridimensionaEApplicaWatermark(percorsoNuovaFotoTemp);

                        // Se l'utente aveva scattato un'altra foto "nuova" ma
                        // ha deciso di rifarla, cancelliamo quella precedente per non intasare la memoria
                        if (percorsoFotoAttuale != null && !percorsoFotoAttuale.equals(tappaCorrente.imagePath)) {
                            new File(percorsoFotoAttuale).delete();
                        }
                        // Aggiorniamo la foto mostrata a schermo
                        percorsoFotoAttuale = percorsoNuovaFotoTemp;
                    }
                    imageAnteprima.setImageURI(uriFotoTemporanea);
                } else {
                    // L'utente ha premuto INDIETRO sulla fotocamera
                    // Cancelliamo il file da 0 byte
                    if (percorsoNuovaFotoTemp != null) {
                        new File(percorsoNuovaFotoTemp).delete();
                        percorsoNuovaFotoTemp = null;
                    }
                }
            }
    );

    // ==========================================================
    // METODO MANIPOLAZIONE IMMAGINI: RIDIMENSIONAMENTO E WATERMARK
    // ==========================================================
    private void ridimensionaEApplicaWatermark(String percorsoFile) {
        // Leggiamo SOLO le dimensioni della foto dai metadati (zero impatto sulla RAM)
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(percorsoFile, options);

        // Calcoliamo il fattore di riduzione (es. da 4000px a max 1024px)
        options.inSampleSize = calcolaFattoreRiduzione(options, 1024, 1024);

        // Carichiamo la foto VERA in RAM, ma già rimpicciolita
        options.inJustDecodeBounds = false;
        android.graphics.Bitmap bitmapOriginale = android.graphics.BitmapFactory.decodeFile(percorsoFile, options);

        if (bitmapOriginale == null) return; // Sicurezza extra

        // Per poterci "disegnare" sopra, la Bitmap deve essere di tipo "Mutable" (modificabile)
        android.graphics.Bitmap bitmapModificabile = bitmapOriginale.copy(android.graphics.Bitmap.Config.ARGB_8888, true);

        // Prepariamo Canvas e Paint
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmapModificabile);
        android.graphics.Paint pennello = new android.graphics.Paint();
        pennello.setColor(android.graphics.Color.WHITE); // Testo bianco
        pennello.setTextSize(70f); // Grandezza testo
        pennello.setAntiAlias(true); // Rende i bordi del testo morbidi
        // Mettiamo un'ombreggiatura nera al testo
        pennello.setShadowLayer(10f, 5f, 5f, android.graphics.Color.BLACK);

        // Creiamo il nostro testo (es. TripTale - 06/03/2026)
        String testoWatermark = "TripTale - " + new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

        // Disegniamo il testo in basso a sinistra (x=50, y=altezzaFoto - 50)
        canvas.drawText(testoWatermark, 50, bitmapModificabile.getHeight() - 50, pennello);

        // Sovrascriviamo il file originale gigante con questa versione ridimensionata e "timbrata"
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(percorsoFile)) {
            // Comprimiamo in JPEG all'85% di qualità per salvare un sacco di spazio su disco
            bitmapModificabile.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================================
    // METODO PER CALCOLARE IL FATTORE DI RIDUZIONE
    // ==========================================================
    private int calcolaFattoreRiduzione(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
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