package com.example.triptale.ui;
import android.net.Uri;
import android.os.Bundle;
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
import com.example.triptale.database.AppDatabase;
import com.example.triptale.network.FirebaseManager;
import com.example.triptale.utils.ImageUtils;
import com.example.triptale.R;
import com.example.triptale.model.Tappa;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.File;
import java.io.IOException;

public class ModificaTappaFragment extends Fragment {

    private Tappa tappaCorrente;
    private ImageView imageAnteprima;
    private String percorsoFotoAttuale = null; // Il percorso che salveremo nel DB
    private Uri uriFotoTemporanea = null;
    private String percorsoNuovaFotoTemp = null; // Il file da 0 byte in attesa
    private boolean salvataggioCompletato = false; // Ci dice se l'utente ha premuto "Salva"
    private String cloudIdViaggioCorrente = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tappaCorrente = getArguments().getParcelable("tappa_selezionata");
            cloudIdViaggioCorrente = getArguments().getString("cloud_id_viaggio", null);
        }
    }

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

        // --- GESTIONE BOTTONE SCATTO FOTO ---
        btnScatta.setOnClickListener(v -> {
            try {
                File fileImmagine = ImageUtils.creaFileImmagine(requireContext());
                percorsoNuovaFotoTemp = fileImmagine.getAbsolutePath();
                uriFotoTemporanea = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        fileImmagine
                );
                scattaFotoLauncher.launch(uriFotoTemporanea);
            } catch (IOException e) {
                Toast.makeText(requireContext(), R.string.errore_apertura_fotocamera, Toast.LENGTH_SHORT).show();
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
                editTitolo.setError(getString(R.string.errore_titolo_obbligatorio));
                editTitolo.requestFocus();
                return;
            }

            // Aggiorniamo il Database
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                Tappa tappaAggiornata = db.tappaDao().ottieniTappaPerId(tappaCorrente.id);
                if (tappaAggiornata != null) {
                    tappaCorrente.cloudId = tappaAggiornata.cloudId;
                }
                db.tappaDao().aggiornaTappa(tappaCorrente);

                if (tappaCorrente.cloudId != null && !tappaCorrente.cloudId.isEmpty()) {
                    FirebaseManager.aggiornaTappa(tappaCorrente, cloudIdViaggioCorrente);
                } else {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        FirebaseManager.sincronizzaTutto(requireContext(), user.getUid(), null);
                    }
                }

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.tappa_aggiornata, Toast.LENGTH_SHORT).show();
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
        imageAnteprima = null;
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
                        ImageUtils.ridimensionaEApplicaWatermark(requireContext(), percorsoNuovaFotoTemp);

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
}