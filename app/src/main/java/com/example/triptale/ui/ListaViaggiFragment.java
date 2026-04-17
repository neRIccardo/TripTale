package com.example.triptale.ui;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;
import com.example.triptale.database.AppDatabase;
import com.example.triptale.R;
import com.example.triptale.model.Viaggio;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

/**
 * Fragment principale dell'applicazione.
 * Si occupa di mostrare l'elenco completo dei viaggi salvati recuperandoli dal database locale.
 * Gestisce inoltra la navigazione verso la creazione di nuovi viaggi, la visualizzazione
 * dei dettagli di un viaggio specifico e il flusso di Login/Logout tramite Firebase Authentication.
 */
public class ListaViaggiFragment extends Fragment {
    private ScrollView scrollView;
    private LinearLayout contenitoreViaggi;
    private TextView textEmptyState;
    private ImageButton btnProfiloLogin;

    /**
     * Inizializza e restituisce la gerarchia delle view associata alla schermata principale.
     *
     * @param inflater Il LayoutInflater utilizzato per "gonfiare" il layout XML.
     * @param container Il ViewGroup padre a cui la UI del Fragment dovrebbe essere attaccata.
     * @param savedInstanceState Lo stato salvato in precedenza.
     * @return La View radice del layout del Fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_viaggi, container, false);
    }

    /**
     * Collega i componenti grafici alle variabili e imposta i listener per i pulsanti.
     * Include la logica per il pulsante del Profilo, che funge sia da accesso alla schermata
     * di Login (se l'utente è sloggato) sia da interruttore per il Logout e lo svuotamento
     * del database locale (se l'utente è già loggato).
     *
     * @param view La View radice restituita da onCreateView().
     * @param savedInstanceState L'eventuale stato salvato in precedenza.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = view.findViewById(R.id.fabAggiungiViaggio);
        btnProfiloLogin = view.findViewById(R.id.btnProfiloLogin);

        // Recuperiamo i nuovi elementi della lista
        scrollView = view.findViewById(R.id.scrollViewViaggi);
        contenitoreViaggi = view.findViewById(R.id.contenitoreViaggi);
        textEmptyState = view.findViewById(R.id.textEmptyState);

        // --- GESTIONE BOTTONE AGGIUNGI VIAGGIO ---
        fab.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_listaViaggiFragment_to_aggiungiViaggioFragment));

        // --- GESTIONE BOTTONE PROFILO / LOGIN ---
        btnProfiloLogin.setOnClickListener(v -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() != null) {
                // Mostriamo il popup di logout se l'utente è loggato
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.gestione_account)
                        .setMessage(getString(R.string.msg_logout, auth.getCurrentUser().getEmail()))
                        .setPositiveButton(R.string.logout, (dialog, which) -> {
                            auth.signOut();
                            // Cancella le notifiche dell'utente appena uscito
                            NotificationManagerCompat.from(requireContext()).cancelAll();
                            new Thread(() -> {
                                Context context = getContext();
                                if (context == null) return;

                                AppDatabase.getInstance(context).viaggioDao().eliminaViaggi();

                                FragmentActivity activity = getActivity();
                                if (activity != null && isAdded()) {
                                    activity.runOnUiThread(() -> {
                                        aggiornaColoreIcona(btnProfiloLogin);
                                        Toast.makeText(context, R.string.logout_successo, Toast.LENGTH_SHORT).show();
                                        caricaViaggiDalDatabase();
                                    });
                                }
                            }).start();
                        })
                        .setNegativeButton(R.string.annulla, null)
                        .show();
            } else {
                // Andiamo alla schermata di login se l'utente non è loggato
                Navigation.findNavController(view).navigate(R.id.action_listaViaggiFragment_to_loginFragment);
            }
        });
    }

    /**
     * Metodo del ciclo di vita chiamato ogni volta che la schermata torna in primo piano.
     * Assicura che la lista dei viaggi e l'icona dello stato di autenticazione siano
     * costantemente aggiornate se modificati in altre schermate.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            if (btnProfiloLogin != null) {
                aggiornaColoreIcona(btnProfiloLogin);
            }
            caricaViaggiDalDatabase();
        }
    }

    /**
     * Sgancia i riferimenti alle View globali al momento della distruzione della UI
     * per prevenire pesanti Memory Leak legati alla lista dinamica dei viaggi.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scrollView = null;
        contenitoreViaggi = null;
        textEmptyState = null;
        btnProfiloLogin = null;
    }

    /**
     * Esegue una query in background sul database Room per ottenere l'elenco dei viaggi.
     * In caso di database vuoto, mostra un testo esplicativo (Empty State).
     * In caso di dati presenti, genera dinamicamente le schede grafiche dei viaggi (item_viaggio.xml)
     * e le inietta nello ScrollView, associando a ciascuna la navigazione verso il proprio dettaglio.
     */
    private void caricaViaggiDalDatabase() {
        // Thread per recuperare i viaggi dal database
        new Thread(() -> {
            Context context = getContext();
            if (context == null) return;

            // Apriamo il Database e richiediamo la lista dei viaggi
            AppDatabase db = AppDatabase.getInstance(context);
            List<Viaggio> viaggiSalvati = db.viaggioDao().ottieniViaggi();

            FragmentActivity activity = getActivity();
            if (activity != null && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (contenitoreViaggi == null) return;
                    // Svuotiamo la lista per evitare duplicati se torniamo indietro
                    contenitoreViaggi.removeAllViews();

                    if (viaggiSalvati.isEmpty()) {
                        // Se la lista è vuota nascondiamo la lista e mostriamo il testo di "default"
                        scrollView.setVisibility(View.GONE);
                        textEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        textEmptyState.setVisibility(View.GONE);
                        scrollView.setVisibility(View.VISIBLE);

                        for (Viaggio viaggio : viaggiSalvati) {
                            // Creiamo il singolo rettangolino per questo viaggio
                            View itemViaggio = getLayoutInflater().inflate(R.layout.item_viaggio, contenitoreViaggi, false);
                            TextView textTitolo = itemViaggio.findViewById(R.id.textTitoloViaggio);
                            TextView textDate = itemViaggio.findViewById(R.id.textDateViaggio);
                            textTitolo.setText(viaggio.titolo);

                            String testoData = viaggio.dataInizio + getString(R.string.trattino) + viaggio.dataFine;
                            textDate.setText(testoData);

                            ImageView imageCopertina = itemViaggio.findViewById(R.id.imageCopertina);

                            if (viaggio.imagePath != null) {
                                try {
                                    // Proviamo a caricare la foto salvata
                                    imageCopertina.setImageURI(android.net.Uri.parse(viaggio.imagePath));
                                } catch (SecurityException e) {
                                    // Se i permessi sono saltati o la foto non esiste più,
                                    // non facciamo crashare l'app mettendo l'icona di default
                                    imageCopertina.setImageResource(android.R.drawable.ic_menu_camera);
                                }
                            } else {
                                // Se non c'è proprio il percorso, mettiamo l'icona di default
                                imageCopertina.setImageResource(android.R.drawable.ic_menu_camera);
                            }

                            // --- GESTIONE CLICK SUL SINGOLO VIAGGIO ---
                            itemViaggio.setOnClickListener(v -> {
                                // Creiamo il Bundle e serializziamo Viaggio
                                Bundle bundle = new Bundle();
                                bundle.putParcelable("viaggio_selezionato", viaggio);
                                Navigation.findNavController(v).navigate(R.id.action_listaViaggiFragment_to_dettaglioViaggioFragment, bundle);
                            });
                            // Aggiungiamo il rettangolino dentro il LinearLayout
                            contenitoreViaggi.addView(itemViaggio);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * Aggiorna visivamente il colore dell'icona del profilo utente per fornire un feedback
     * immediato sullo stato di autenticazione (verde per loggato, arancione per ospite).
     *
     * @param btnProfilo L'ImageButton da ricolorare.
     */
    private void aggiornaColoreIcona(ImageButton btnProfilo) {
        Context context = getContext();
        if (context == null || !isAdded()) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // UTENTE LOGGATO: coloriamo l'icona di verde
            btnProfilo.setColorFilter(Color.parseColor(getString(R.string.utente_loggato)));
        } else {
            // NON LOGGATO: rimettiamo il colore originale arancione
            btnProfilo.setColorFilter(Color.parseColor(getString(R.string.utente_non_loggato)));
        }
    }
}