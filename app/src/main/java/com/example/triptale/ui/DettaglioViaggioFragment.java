package com.example.triptale.ui;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.triptale.database.AppDatabase;
import com.example.triptale.network.FirebaseManager;
import com.example.triptale.network.MeteoManager;
import com.example.triptale.R;
import com.example.triptale.model.Tappa;
import com.example.triptale.model.Viaggio;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;

/**
 * Fragment che funge da "cruscotto" principale per visualizzare e gestire i dettagli di un singolo Viaggio.
 * Integra la visualizzazione delle tappe (lette dal database locale), le previsioni meteo
 * dinamiche (recuperate tramite chiamate di rete) e la logica di eliminazione/modifica
 * sincronizzata con il database in cloud Firebase.
 */
public class DettaglioViaggioFragment extends Fragment {
    private Viaggio viaggioCorrente;
    private LinearLayout contenitoreTappe;
    private HorizontalScrollView scrollMeteo;
    private LinearLayout contenitoreMeteo;
    private TextView textErroreMeteo;

    /**
     * Metodo del ciclo di vita chiamato alla creazione iniziale del Fragment.
     * Estrae in background l'oggetto Viaggio serializzato (Parcelable) passato dal Fragment
     * precedente tramite Bundle, rendendo i dati immediatamente disponibili per la UI.
     *
     * @param savedInstanceState L'eventuale stato precedentemente salvato del Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Controlliamo se ci è stato passato un Bundle
        if (getArguments() != null) {
            // Estraiamo l'oggetto serializzato usando la stessa chiave definita prima
            viaggioCorrente = getArguments().getParcelable("viaggio_selezionato");
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
        return inflater.inflate(R.layout.fragment_dettaglio_viaggio, container, false);
    }

    /**
     * Collega i componenti grafici alle variabili e configura i listener per i pulsanti di azione.
     * Gestisce la logica complessa di eliminazione del viaggio in modo a cascata,
     * assicurandosi di cancellare anche le foto salvate su disco, i record in Room, in Firebase
     * e le eventuali notifiche pendenti.
     *
     * @param view La View radice restituita da onCreateView().
     * @param savedInstanceState L'eventuale stato salvato in precedenza.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView textTitolo = view.findViewById(R.id.textTitoloDettaglio);
        TextView textDate = view.findViewById(R.id.textDateDettaglio);
        ImageButton btnElimina = view.findViewById(R.id.btnEliminaViaggio);
        FloatingActionButton fabAggiungiTappa = view.findViewById(R.id.fabAggiungiTappa);
        contenitoreTappe = view.findViewById(R.id.contenitoreTappe);
        ImageButton btnModifica = view.findViewById(R.id.btnModificaViaggio);
        scrollMeteo = view.findViewById(R.id.scrollMeteo);
        contenitoreMeteo = view.findViewById(R.id.contenitoreMeteo);
        textErroreMeteo = view.findViewById(R.id.textErroreMeteo);
        ImageButton btnApriMappa = view.findViewById(R.id.btnApriMappa);

        if (viaggioCorrente != null) {
            // Popoliamo l'interfaccia
            textTitolo.setText(viaggioCorrente.titolo);
            String testoData = viaggioCorrente.dataInizio + getString(R.string.trattino) + viaggioCorrente.dataFine;
            textDate.setText(testoData);
        }

        // --- GESTIONE BOTTONE APRI MAPPA ---
        btnApriMappa.setOnClickListener(v ->{
            // Controlla se la città è nulla o vuota
            if (viaggioCorrente.cittaDestinazione == null || viaggioCorrente.cittaDestinazione.trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.manca_citta_btnApriMappa, Toast.LENGTH_SHORT).show();
                return;
            }

            Uri gmUri= Uri.parse("geo:0,0?q=" + Uri.encode(viaggioCorrente.cittaDestinazione));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmUri);
            mapIntent.setPackage("com.google.android.apps.maps"); // Forziamo l'apertura in Google Maps

            // Controllo di sicurezza: verifica che ci sia un'app di mappe installata
            if(mapIntent.resolveActivity(requireActivity().getPackageManager()) != null){
                startActivity(mapIntent);
            }
            else{
                Toast.makeText(getContext(), R.string.errore_geolocalizzazione, Toast.LENGTH_SHORT).show();
            }
        });

        // --- GESTIONE BOTTONE ELIMINA VIAGGIO ---
        btnElimina.setOnClickListener(v -> {
            // Creiamo la finestra di dialogo (il pop-up)
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.elimina_viaggio)
                    .setMessage(R.string.msg_elimina_viaggio)
                    .setPositiveButton(R.string.elimina, (dialog, which) -> {
                        // Se l'utente clicca "Elimina", facciamo partire il Thread di eliminazione
                        new Thread(() -> {
                            List <Tappa> tappeDelViaggio = AppDatabase.getInstance(requireContext()).tappaDao().ottieniTappeDelViaggio(viaggioCorrente.id);
                            for(Tappa tappa : tappeDelViaggio) {
                                // Cancelliamo l'eventuale foto della tappa dalla memoria
                                if (tappa.imagePath != null) {
                                    File fotoDaCancellare = new File(tappa.imagePath);
                                    if (fotoDaCancellare.exists()) {
                                        if (!fotoDaCancellare.delete()) {
                                            Log.w("TripTale", "Impossibile eliminare il file o file già assente");
                                        }
                                    }
                                }
                                if (tappa.cloudId != null && !tappa.cloudId.isEmpty()) {
                                    FirebaseManager.eliminaTappa(tappa.cloudId);
                                }
                            }
                            AppDatabase.getInstance(requireContext()).viaggioDao().eliminaViaggio(viaggioCorrente);
                            if (viaggioCorrente.cloudId != null && !viaggioCorrente.cloudId.isEmpty()) {
                                FirebaseManager.eliminaViaggio(viaggioCorrente.cloudId);
                            }
                            // Cancellazione notifiche "orfane"
                            NotificationManagerCompat.from(requireContext()).cancel(viaggioCorrente.id);

                            if (!isAdded()) return; // Protezione ciclo di vita

                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), R.string.viaggio_eliminato, Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(view).popBackStack();
                            });
                        }).start();
                    })
                    .setNegativeButton(R.string.annulla, null) // Se clicca annulla, la finestra si chiude da sola
                    .show(); // Mostriamo la finestra a schermo
        });

        // --- GESTIONE BOTTONE MODIFICA VIAGGIO ---
        btnModifica.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable("viaggio_selezionato", viaggioCorrente);
            Navigation.findNavController(view).navigate(R.id.action_dettaglioViaggioFragment_to_modificaViaggioFragment, bundle);
        });

        // --- GESTIONE BOTTONE AGGIUNGI TAPPA  ---
        fabAggiungiTappa.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("id_del_viaggio", viaggioCorrente.id);
            bundle.putString("cloud_id_viaggio", viaggioCorrente.cloudId);
            Navigation.findNavController(view).navigate(R.id.action_dettaglioViaggioFragment_to_aggiungiTappaFragment, bundle);
        });
    }

    /**
     * Metodo del ciclo di vita chiamato ogni volta che il Fragment torna in primo piano (es. tornando
     * indietro dalla schermata di modifica o di aggiunta tappa).
     * Assicura che i dati a schermo (titolo, date, lista delle tappe e meteo) siano sempre
     * aggiornati interrogando in tempo reale il database locale.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (viaggioCorrente != null && contenitoreTappe != null) {

            // Ricarichiamo le tappe aggiornate in tempo reale
            caricaTappe(contenitoreTappe);

            // Ricarichiamo le informazioni del viaggio (es. se abbiamo cambiato titolo o date)
            new Thread(() -> {
                Viaggio v = AppDatabase.getInstance(requireContext()).viaggioDao().ottieniViaggioPerId(viaggioCorrente.id);

                if (v != null) {
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        viaggioCorrente = v; // Aggiorniamo il viaggio

                        // Aggiorniamo le scritte a schermo
                        TextView textTitolo = requireView().findViewById(R.id.textTitoloDettaglio);
                        TextView textDate = requireView().findViewById(R.id.textDateDettaglio);
                        textTitolo.setText(v.titolo);
                        String testoData = v.dataInizio + getString(R.string.trattino) + v.dataFine;
                        textDate.setText(testoData);
                        caricaMeteo(v);
                    });
                }
            }).start();
        }
    }

    /**
     * Sgancia i riferimenti alle View globali per prevenire l'occupazione inutile
     * di memoria (Memory Leak) quando il Fragment non è più visibile a schermo.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        contenitoreTappe = null;
        scrollMeteo = null;
        contenitoreMeteo = null;
        textErroreMeteo = null;
    }

    /**
     * Gestisce la richiesta asincrona delle previsioni meteorologiche tramite MeteoManager.
     * In caso di successo (onSuccess), "gonfia" dinamicamente dei blocchi XML per ogni
     * giorno di previsione e li inserisce in uno scorrimento orizzontale. In caso di fallimento
     * o assenza di città, mostra un messaggio di errore all'utente.
     *
     * @param viaggio L'oggetto Viaggio contenente la città e le date necessarie per le API.
     */
    private void caricaMeteo(Viaggio viaggio) {
        if (viaggio.cittaDestinazione != null && !viaggio.cittaDestinazione.trim().isEmpty()) {

            // Mostriamo all'utente che stiamo caricando
            textErroreMeteo.setVisibility(View.VISIBLE);
            textErroreMeteo.setText(R.string.meteo_caricamento);
            scrollMeteo.setVisibility(View.GONE);

            // Richiamiamo il Manager
            MeteoManager.ottieniPrevisioni(requireContext(), viaggio.cittaDestinazione, viaggio.dataInizio, viaggio.dataFine, new MeteoManager.MeteoCallback() {

                @Override
                public void onSuccess(List<MeteoManager.Previsione> previsioni) {
                    if (!isAdded()) return; // Protezione ciclo di vita

                    textErroreMeteo.setVisibility(View.GONE);
                    scrollMeteo.setVisibility(View.VISIBLE);
                    contenitoreMeteo.removeAllViews();

                    // Creiamo un quadratino per ogni giorno
                    for (MeteoManager.Previsione prev : previsioni) {
                        View itemMeteo = getLayoutInflater().inflate(R.layout.item_meteo, contenitoreMeteo, false);

                        TextView textData = itemMeteo.findViewById(R.id.textDataMeteo);
                        TextView textIcona = itemMeteo.findViewById(R.id.textIconaMeteo);
                        TextView textTemp = itemMeteo.findViewById(R.id.textTempMeteo);

                        // Inseriamo i dati (arrotondiamo i gradi per togliere i decimali)
                        textData.setText(prev.data);
                        textIcona.setText(prev.iconaEmoji);
                        textTemp.setText(getString(R.string.formato_temperatura, Math.round(prev.tempMin), Math.round(prev.tempMax)));
                        contenitoreMeteo.addView(itemMeteo);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (!isAdded()) return; // Protezione ciclo di vita

                    scrollMeteo.setVisibility(View.GONE);
                    textErroreMeteo.setVisibility(View.VISIBLE);
                    textErroreMeteo.setText(getString(R.string.errore_meteo_prefisso, errorMessage));
                }
            });
        } else {
            // Se la città non è stata inserita affatto
            textErroreMeteo.setVisibility(View.VISIBLE);
            textErroreMeteo.setText(R.string.meteo_citta_mancante);
            scrollMeteo.setVisibility(View.GONE);
        }
    }

    /**
     * Interroga in background il database locale per ottenere tutte le tappe relative al viaggio corrente.
     * Genera dinamicamente a runtime i blocchi dell'interfaccia grafica (item_tappa.xml) e li aggiunge
     * a cascata al layout principale, agganciando ad ognuno i rispettivi pulsanti di modifica ed eliminazione.
     *
     * @param contenitore Il LinearLayout in cui verranno inseriti (iniettati) i blocchi grafici delle tappe.
     */
    private void caricaTappe(LinearLayout contenitore) {
        new Thread(() -> {
            // Chiediamo al TappaDAO di darci SOLO le tappe di QUESTO viaggio
            List<Tappa> tappeSalvate = AppDatabase.getInstance(requireContext())
                    .tappaDao().ottieniTappeDelViaggio(viaggioCorrente.id);

            if (!isAdded()) return; // Protezione ciclo di vita

            requireActivity().runOnUiThread(() -> {
                contenitore.removeAllViews(); // Svuotiamo prima di ricaricare per evitare duplicati

                for (Tappa tappa : tappeSalvate) {
                    // Gonfiamo il nostro nuovo rettangolino (item_tappa.xml)
                    View itemTappa = getLayoutInflater().inflate(R.layout.item_tappa, contenitore, false);

                    // Colleghiamo gli elementi grafici del singolo item
                    TextView textTitolo = itemTappa.findViewById(R.id.textTitoloTappa);
                    TextView textNote = itemTappa.findViewById(R.id.textNoteTappa);
                    ImageButton btnModifica = itemTappa.findViewById(R.id.btnModificaTappa);
                    ImageButton btnElimina = itemTappa.findViewById(R.id.btnEliminaTappa);
                    ImageView imageMiniatura = itemTappa.findViewById(R.id.imageMiniaturaTappa);

                    // Scriviamo i dati
                    textTitolo.setText(tappa.titolo);
                    textNote.setText(tappa.note);

                    // --- GESTIONE IMMAGINE DELLA TAPPA ---
                    if (tappa.imagePath != null) {
                        try {
                            // Carichiamo la foto scattata
                            imageMiniatura.setImageURI(android.net.Uri.parse(tappa.imagePath));
                        } catch (Exception e) {
                            imageMiniatura.setImageResource(android.R.drawable.ic_menu_camera);
                        }
                    } else {
                        // Se non c'è foto, usiamo una icona di default
                        imageMiniatura.setImageResource(android.R.drawable.ic_menu_camera);
                    }

                    // --- GESTIONE BOTTONE MODIFICA TAPPA ---
                    btnModifica.setOnClickListener(v -> {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("tappa_selezionata", tappa);
                        bundle.putString("cloud_id_viaggio", viaggioCorrente.cloudId);
                        Navigation.findNavController(itemTappa).navigate(R.id.action_dettaglioViaggioFragment_to_modificaTappaFragment, bundle);
                    });

                    // --- GESTIONE BOTTONE ELIMINA TAPPA ---
                    btnElimina.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.titolo_elimina_tappa)
                            .setMessage(R.string.msg_elimina_tappa)
                            .setPositiveButton(R.string.elimina, (dialog, which) -> new Thread(() -> {
                                // Cancelliamo l'eventuale foto della tappa dalla memoria
                                if (tappa.imagePath != null) {
                                    File fotoDaCancellare = new File(tappa.imagePath);
                                    if (fotoDaCancellare.exists()) {
                                        if (!fotoDaCancellare.delete()) {
                                            Log.w("TripTale", "Impossibile eliminare il file o file già assente");
                                        }
                                    }
                                }
                                // Cancelliamo la tappa dal database Room
                                AppDatabase.getInstance(requireContext()).tappaDao().eliminaTappa(tappa);
                                if (tappa.cloudId != null && !tappa.cloudId.isEmpty()) {
                                    FirebaseManager.eliminaTappa(tappa.cloudId);
                                }

                                if (!isAdded()) return; // Protezione ciclo di vita

                                // Aggiorniamo la grafica
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), R.string.tappa_eliminata, Toast.LENGTH_SHORT).show();
                                    // Rigeneriamo la lista di item
                                    caricaTappe(contenitore);
                                });
                            }).start())
                            .setNegativeButton(R.string.annulla, null) // Chiude il pop-up
                            .show());
                    // Attacchiamo la tappa completa allo schermo
                    contenitore.addView(itemTappa);
                }
            });
        }).start();
    }
}