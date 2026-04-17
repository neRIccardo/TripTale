package com.example.triptale.ui;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import androidx.fragment.app.FragmentActivity;
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

/**
 * Fragment dedicato alla modifica di una Tappa preesistente.
 * Permette all'utente di aggiornare il titolo, le note e l'immagine associata (scattandone una nuova).
 * Gestisce in modo sicuro la pulizia e la sovrascrittura dei file immagine sul dispositivo e sincronizza
 * le modifiche apportate sia sul database locale (Room) sia su quello in cloud (Firebase).
 */
public class ModificaTappaFragment extends Fragment {

    private Tappa tappaCorrente;
    private ImageView imageAnteprima;
    private String percorsoFotoAttuale = null; // Il percorso che salveremo nel DB
    private Uri uriFotoTemporanea = null;
    private String percorsoNuovaFotoTemp = null; // Il file da 0 byte in attesa
    private boolean salvataggioCompletato = false; // Ci dice se l'utente ha premuto "Salva"
    private String cloudIdViaggioCorrente = null;

    /**
     * Metodo del ciclo di vita chiamato alla creazione iniziale del Fragment.
     * Recupera in background l'oggetto Tappa da modificare e l'ID cloud del viaggio padre,
     * passati tramite Bundle dal Fragment precedente, preparandoli per l'uso nella UI.
     *
     * @param savedInstanceState L'eventuale stato precedentemente salvato del Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tappaCorrente = getArguments().getParcelable("tappa_selezionata");
            cloudIdViaggioCorrente = getArguments().getString("cloud_id_viaggio", null);
        }
    }

    /**
     * Inizializza e restituisce la gerarchia delle view associata al Fragment.
     *
     * @param inflater Il LayoutInflater utilizzato per "gonfiare" il layout XML.
     * @param container Il ViewGroup padre a cui la UI del Fragment dovrebbe essere attaccata.
     * @param savedInstanceState Lo stato salvato in precedenza.
     * @return La View radice del layout del Fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_modifica_tappa, container, false);
    }

    /**
     * Collega i componenti grafici alle variabili e precompila i campi di testo e l'immagine
     * con i dati attuali della Tappa. Imposta inoltre i listener per l'acquisizione di una nuova foto
     * e per il salvataggio definitivo delle modifiche, gestendo l'eventuale cancellazione della vecchia foto.
     *
     * @param view La View radice restituita da onCreateView().
     * @param savedInstanceState L'eventuale stato salvato in precedenza.
     */
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

        // --- RIPRISTINO DELLO STATO DOPO LA ROTAZIONE ---
        if (savedInstanceState != null) {
            // Recuperiamo i percorsi
            percorsoFotoAttuale = savedInstanceState.getString("percorsoFotoAttuale");
            percorsoNuovaFotoTemp = savedInstanceState.getString("percorsoNuovaFotoTemp");
            String uriStr = savedInstanceState.getString("uriFotoTemporanea");

            if (uriStr != null) uriFotoTemporanea = Uri.parse(uriStr);

            // Se l'utente ha scattato una nuova foto (percorsoFotoAttuale diverso da quello nel DB)
            // o se il ripristino ha riportato la foto scattata, carichiamola
            if (percorsoFotoAttuale != null) {
                imageAnteprima.setImageURI(Uri.fromFile(new File(percorsoFotoAttuale)));
            }
        }

        // --- GESTIONE BOTTONE SCATTO FOTO ---
        btnScatta.setOnClickListener(v -> {
            try {
                Context context = getContext();
                if (context == null) return;

                File fileImmagine = ImageUtils.creaFileImmagine(context);
                percorsoNuovaFotoTemp = fileImmagine.getAbsolutePath();
                uriFotoTemporanea = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
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
                    if (!vecchiaFoto.delete()) {
                        Log.w("TripTale", "Impossibile eliminare il file o file già assente");
                    }
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
                Context context = getContext();
                if (context == null) return;

                AppDatabase db = AppDatabase.getInstance(context);
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
                        FirebaseManager.sincronizzaTutto(context, user.getUid(), null);
                    }
                }

                FragmentActivity activity = getActivity();
                if (activity != null && isAdded()) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(context, R.string.tappa_aggiornata, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).popBackStack();
                    });
                }
            }).start();
        });
    }

    /**
     * Metodo del ciclo di vita chiamato quando la vista (UI) del Fragment sta per essere distrutta.
     * Previene perdite di memoria (Memory Leak) sganciando il riferimento all'ImageView.
     * Svolge inoltre una funzione cruciale di pulizia del disco: se l'utente esce dalla schermata
     * senza salvare (ad es. premendo "Indietro"), elimina l'eventuale nuova foto scattata
     * per non occupare spazio inutilmente, mantenendo intatta la foto originale.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Controllo: entriamo solo se l'utente sta uscendo dalla schermata SENZA salvare.
        // Non dobbiamo fare nulla se l'utente sta solo ruotando il telefono
        if (!requireActivity().isChangingConfigurations()) {
            // Se l'utente sta uscendo dalla schermata SENZA aver premuto "Salva"...
            // e ha scattato una foto nuova...
            if (!salvataggioCompletato && percorsoFotoAttuale != null && !percorsoFotoAttuale.equals(tappaCorrente.imagePath)) {
                // Eliminiamo la foto nuova "orfana", mantenendo intatta quella vecchia sul telefono
                if (!new File(percorsoFotoAttuale).delete()) {
                    Log.w("TripTale", "Impossibile eliminare il file o file già assente");
                }
            }
        }
        imageAnteprima = null;
    }
    /**
     * Metodo del ciclo di vita chiamato quando il Fragment viene distrutto per salvare lo stato attuale.
     * @param outState L'eventuale stato salvato in precedenza.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Salviamo le variabili fondamentali come stringhe
        outState.putString("percorsoFotoAttuale", percorsoFotoAttuale);
        outState.putString("percorsoNuovaFotoTemp", percorsoNuovaFotoTemp);

        // Se c'è un'URI, la trasformiamo in stringa e la salviamo
        if (uriFotoTemporanea != null) {
            outState.putString("uriFotoTemporanea", uriFotoTemporanea.toString());
        }
    }

    /**
     * Gestore asincrono dell'intent della fotocamera tramite Activity Result API.
     * In caso di scatto completato con successo, applica il ridimensionamento e il watermark alla nuova foto,
     * elimina un'eventuale precedente foto temporanea scattata nella stessa sessione di modifica,
     * e aggiorna l'anteprima a schermo. Se lo scatto viene annullato, rimuove il file temporaneo vuoto (0 byte).
     */
    private final ActivityResultLauncher<Uri> scattaFotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            esitoPositivo -> {
                Context context = getContext();
                if (context == null || !isAdded()) return;

                if (esitoPositivo) {
                    if (percorsoNuovaFotoTemp != null) {
                        // Applichiamo il watermark e ridimensionamento alla nuova foto
                        ImageUtils.ridimensionaEApplicaWatermark(context, percorsoNuovaFotoTemp);

                        // Se l'utente aveva scattato un'altra foto "nuova" ma
                        // ha deciso di rifarla, cancelliamo quella precedente per non intasare la memoria
                        if (percorsoFotoAttuale != null && !percorsoFotoAttuale.equals(tappaCorrente.imagePath)) {
                            if (!new File(percorsoFotoAttuale).delete()) {
                                Log.w("TripTale", "Impossibile eliminare il file o file già assente");
                            }
                        }
                        // Aggiorniamo la foto mostrata a schermo
                        percorsoFotoAttuale = percorsoNuovaFotoTemp;
                    }
                    imageAnteprima.setImageURI(uriFotoTemporanea);
                } else {
                    // L'utente ha premuto INDIETRO sulla fotocamera
                    // Cancelliamo il file da 0 byte
                    if (percorsoNuovaFotoTemp != null) {
                        if (!new File(percorsoNuovaFotoTemp).delete()) {
                            Log.w("TripTale", "Impossibile eliminare il file o file già assente");
                        }
                        percorsoNuovaFotoTemp = null;
                    }
                }
            }
    );
}