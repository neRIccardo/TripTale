package com.example.triptale;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AggiungiTappaFragment extends Fragment {
    private int idViaggioCorrente = -1; // Variabile per ricordare l'ID del viaggio corrente a cui associare la tappa
    private ImageView imageAnteprima;
    private String percorsoFotoAttuale = null;
    private Uri uriFotoTemporanea = null;
    private String percorsoNuovaFotoTemp = null; // Il file da 0 byte in attesa
    private boolean salvataggioCompletato = false;
    private String cloudIdViaggioCorrente = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_aggiungi_tappa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Recuperiamo l'id del viaggio dal bundle
        if (getArguments() != null) {
            idViaggioCorrente = getArguments().getInt("id_del_viaggio", -1);
            cloudIdViaggioCorrente = getArguments().getString("cloud_id_viaggio", null);
        }
        // Controllo di sicurezza
        if (idViaggioCorrente == -1) {
            Toast.makeText(requireContext(), "Errore: Viaggio non trovato!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        EditText editTitolo = view.findViewById(R.id.editTitoloTappa);
        EditText editNote = view.findViewById(R.id.editNoteTappa);
        Button btnScattaFoto = view.findViewById(R.id.btnScattaFoto);
        Button btnSalva = view.findViewById(R.id.btnSalvaTappa);
        imageAnteprima = view.findViewById(R.id.imageAnteprimaFoto);

        // --- GESTIONE BOTTONE SCATTO FOTO ---
        btnScattaFoto.setOnClickListener(v -> {
            try {
                // Creiamo il file vuoto
                File fileImmagine = creaFileImmagine();
                percorsoNuovaFotoTemp = fileImmagine.getAbsolutePath();

                // Chiediamo al FileProvider di creare un Uri sicuro per questo file
                uriFotoTemporanea = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        fileImmagine
                );
                // Lanciamo la fotocamera passandole l'Uri sicuro
                scattaFotoLauncher.launch(uriFotoTemporanea);

            } catch (IOException e) {
                Toast.makeText(requireContext(), "Errore nella creazione del file", Toast.LENGTH_SHORT).show();
            }
        });

        // --- GESTIONE BOTTONE SALVATGGIO TAPPA ---
        btnSalva.setOnClickListener(v -> {
            salvataggioCompletato = true;

            String titoloInserito = editTitolo.getText().toString().trim();
            String noteInserite = editNote.getText().toString().trim();
            editTitolo.setError(null);

            if (titoloInserito.isEmpty()) {
                editTitolo.requestFocus();
                editTitolo.setError("Il titolo è obbligatorio!");
                return; // Blocchiamo il salvataggio
            }

            Tappa nuovaTappa = new Tappa(idViaggioCorrente, titoloInserito, noteInserite);
            nuovaTappa.imagePath = percorsoFotoAttuale;
            new Thread(() -> {
                long idGenerato = AppDatabase.getInstance(requireContext()).tappaDao().inserisciTappa(nuovaTappa);
                nuovaTappa.id = (int) idGenerato;
                FirebaseManager.aggiungiTappa(requireContext(), nuovaTappa, cloudIdViaggioCorrente);

                if (!isAdded()) return;

                // Torniamo sul Main Thread per la grafica
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Tappa salvata!", Toast.LENGTH_SHORT).show();
                    // Torniamo al cruscotto del viaggio
                    Navigation.findNavController(view).popBackStack();
                });
            }).start();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Se sta uscendo SENZA salvare e aveva scattato una foto, cancelliamola
        if (!salvataggioCompletato && percorsoFotoAttuale != null) {
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

        return File.createTempFile(nomeFile,".jpg", cartellaStorage);
    }

    // =========================================================================
    // OGGETTO PER GESTIRE L'INTENT DELLA FOTOCAMERA
    // =========================================================================
    private final ActivityResultLauncher<Uri> scattaFotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            esitoPositivo -> {
                if (esitoPositivo) {
                    if (percorsoNuovaFotoTemp != null) {
                        ridimensionaEApplicaWatermark(percorsoNuovaFotoTemp);
                        // Se l'utente aveva GIA' scattato una foto in questa sessione
                        // e ha deciso di rifarla, cancelliamo quella precedente
                        if (percorsoFotoAttuale != null) {
                            new File(percorsoFotoAttuale).delete();
                        }
                        // Promuoviamo la foto temporanea a foto ufficiale
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
        // Leggiamo SOLO le dimensioni della foto (zero impatto sulla RAM)
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