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

    // =========================================================================
    // OGGETTO PER GESTIRE L'INTENT DELLA FOTOCAMERA
    // =========================================================================
    private final ActivityResultLauncher<Uri> scattaFotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            esitoPositivo -> {
                if (esitoPositivo) {
                    //Se c'era già una vecchia foto, la eliminiamo fisicamente
                    // per non consumare memoria inutilmente sul telefono
                    if (tappaCorrente.imagePath != null && !tappaCorrente.imagePath.equals(percorsoFotoAttuale)) {
                        File vecchiaFoto = new File(tappaCorrente.imagePath);
                        if (vecchiaFoto.exists()) {
                            vecchiaFoto.delete();
                        }
                    }
                    imageAnteprima.setImageURI(uriFotoTemporanea);
                } else {
                    // Se annulla, annulliamo il nuovo percorso e manteniamo quello vecchio
                    percorsoFotoAttuale = tappaCorrente.imagePath;
                }
            }
    );

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
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Tappa aggiornata!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                });
            }).start();
        });
    }

    // =========================================================================
    // METODO PER CREARE UN FILE VUOTO PER LA FOTOCAMERA
    // =========================================================================
    private File creaFileImmagine() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nomeFile = "JPEG_" + timeStamp + "_";
        File cartellaStorage = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File fileCreato = File.createTempFile(nomeFile, ".jpg", cartellaStorage);
        percorsoFotoAttuale = fileCreato.getAbsolutePath();
        return fileCreato;
    }
}