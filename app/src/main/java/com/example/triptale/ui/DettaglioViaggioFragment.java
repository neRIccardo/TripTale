package com.example.triptale.ui;
import android.app.AlertDialog;
import android.os.Bundle;
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

public class DettaglioViaggioFragment extends Fragment {
    private Viaggio viaggioCorrente;
    private LinearLayout contenitoreTappe;
    private HorizontalScrollView scrollMeteo;
    private LinearLayout contenitoreMeteo;
    private TextView textErroreMeteo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Controlliamo se ci è stato passato un Bundle
        if (getArguments() != null) {
            // Estraiamo l'oggetto serializzato usando la stessa chiave definita prima
            viaggioCorrente = getArguments().getParcelable("viaggio_selezionato");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dettaglio_viaggio, container, false);
    }

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

        if (viaggioCorrente != null) {
            // Popoliamo l'interfaccia
            textTitolo.setText(viaggioCorrente.titolo);
            String testoData = viaggioCorrente.dataInizio + getString(R.string.trattino) + viaggioCorrente.dataFine;
            textDate.setText(testoData);
        }

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
                                        fotoDaCancellare.delete();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        contenitoreTappe = null;
        scrollMeteo = null;
        contenitoreMeteo = null;
        textErroreMeteo = null;
    }

    // =========================================================================
    // METODO PER CARICARE IL METEO
    // =========================================================================
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

    // =========================================================================
    // METODO PER CARICARE LE TAPPE E POPOLARE IL LINEAR LAYOUT
    // =========================================================================
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
                                        fotoDaCancellare.delete();
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