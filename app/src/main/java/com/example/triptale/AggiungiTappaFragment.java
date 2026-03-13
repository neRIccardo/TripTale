package com.example.triptale;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import java.io.FileOutputStream;
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
            Toast.makeText(requireContext(), getString(R.string.viaggio_non_trovato), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), R.string.errore_creazione_file, Toast.LENGTH_SHORT).show();
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
                editTitolo.setError(getString(R.string.errore_titolo_obbligatorio));
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
                    Toast.makeText(requireContext(), R.string.tappa_salvata, Toast.LENGTH_SHORT).show();
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
        String testoWatermark = getString(R.string.watermark_prefisso) + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

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
    private int calcolaFattoreRiduzione(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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