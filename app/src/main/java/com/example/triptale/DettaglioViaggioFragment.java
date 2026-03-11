package com.example.triptale;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class DettaglioViaggioFragment extends Fragment {
    private Viaggio viaggioCorrente;

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
        LinearLayout contenitoreTappe = view.findViewById(R.id.contenitoreTappe);
        ImageButton btnModifica = view.findViewById(R.id.btnModificaViaggio);

        // Elementi del meteo
        android.widget.HorizontalScrollView scrollMeteo = view.findViewById(R.id.scrollMeteo);
        LinearLayout contenitoreMeteo = view.findViewById(R.id.contenitoreMeteo);
        TextView textErroreMeteo = view.findViewById(R.id.textErroreMeteo);

        // Controlliamo se ci è stato passato un Bundle
        if (getArguments() != null) {
            // Estraiamo l'oggetto serializzato usando la stessa chiave definita prima
            viaggioCorrente = (Viaggio) getArguments().getSerializable("viaggio_selezionato");
            if (viaggioCorrente != null) {
                // Popoliamo l'interfaccia
                textTitolo.setText(viaggioCorrente.titolo);
                textDate.setText(viaggioCorrente.dataInizio + " - " + viaggioCorrente.dataFine);
                caricaTappe(contenitoreTappe);

                // --- CHIAMATA METEO TRAMITE MANAGER ESTERNO E VOLLEY ---
                if (viaggioCorrente.cittaDestinazione != null && !viaggioCorrente.cittaDestinazione.trim().isEmpty()) {

                    // Mostriamo all'utente che stiamo caricando
                    textErroreMeteo.setVisibility(View.VISIBLE);
                    textErroreMeteo.setText("Caricamento meteo in corso...");
                    scrollMeteo.setVisibility(View.GONE);

                    // Richiamiamo il Manager
                    MeteoManager.ottieniPrevisioni(requireContext(), viaggioCorrente.cittaDestinazione, viaggioCorrente.dataInizio, viaggioCorrente.dataFine, new MeteoManager.MeteoCallback() {

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
                                textTemp.setText(Math.round(prev.tempMin) + "° / " + Math.round(prev.tempMax) + "°");
                                contenitoreMeteo.addView(itemMeteo);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (!isAdded()) return; // Protezione ciclo di vita

                            scrollMeteo.setVisibility(View.GONE);
                            textErroreMeteo.setVisibility(View.VISIBLE);
                            textErroreMeteo.setText("🌥️\n" + errorMessage);
                        }
                    });
                } else {
                    // Se la città non è stata inserita affatto
                    textErroreMeteo.setVisibility(View.VISIBLE);
                    textErroreMeteo.setText("Inserisci la città di destinazione nel pannello di modifica per vedere il meteo.");
                    scrollMeteo.setVisibility(View.GONE);
                }
            }
        }

        // --- GESTIONE BOTTONE ELIMINA VIAGGIO ---
        btnElimina.setOnClickListener(v -> {
            // Creiamo la finestra di dialogo (il pop-up)
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Elimina Viaggio")
                    .setMessage("Sei sicuro di voler eliminare questo viaggio e tutte le sue tappe?\nL'azione è irreversibile.")
                    .setPositiveButton("Elimina", (dialog, which) -> {
                        // Se l'utente clicca "Elimina", facciamo partire il Thread di eliminazione
                        new Thread(() -> {
                            List <Tappa> tappeDelViaggio = AppDatabase.getInstance(requireContext()).tappaDao().ottieniTappeDelViaggio(viaggioCorrente.id);
                            for(Tappa tappa : tappeDelViaggio) {
                                // Cancelliamo l'eventuale foto della tappa dalla memoria
                                if (tappa.imagePath != null) {
                                    java.io.File fotoDaCancellare = new java.io.File(tappa.imagePath);
                                    if (fotoDaCancellare.exists()) {
                                        fotoDaCancellare.delete();
                                    }
                                }
                            }
                            AppDatabase.getInstance(requireContext()).viaggioDao().eliminaViaggio(viaggioCorrente);
                            // Cancellazione notifiche "orfane"
                            androidx.core.app.NotificationManagerCompat.from(requireContext()).cancel(viaggioCorrente.id);

                            if (!isAdded()) return; // Protezione ciclo di vita

                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Viaggio eliminato!", Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(view).popBackStack();
                            });
                        }).start();
                    })
                    .setNegativeButton("Annulla", null) // Se clicca annulla, la finestra si chiude da sola
                    .show(); // Mostriamo la finestra a schermo
        });

        // --- GESTIONE BOTTONE MODIFICA VIAGGIO ---
        btnModifica.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("viaggio_selezionato", viaggioCorrente);
            Navigation.findNavController(view).navigate(R.id.action_dettaglioViaggioFragment_to_modificaViaggioFragment, bundle);
        });

        // --- GESTIONE BOTTONE AGGIUNGI TAPPA  ---
        fabAggiungiTappa.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("id_del_viaggio", viaggioCorrente.id);
            Navigation.findNavController(view).navigate(R.id.action_dettaglioViaggioFragment_to_aggiungiTappaFragment, bundle);
        });
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
                        bundle.putSerializable("tappa_selezionata", tappa);
                        Navigation.findNavController(itemTappa).navigate(R.id.action_dettaglioViaggioFragment_to_modificaTappaFragment, bundle);
                    });

                    // --- GESTIONE BOTTONE ELIMINA TAPPA ---
                    btnElimina.setOnClickListener(v -> {
                        new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Elimina Tappa")
                                .setMessage("Sei sicuro di voler eliminare questa tappa?\nL'azione è irreversibile.")
                                .setPositiveButton("Elimina", (dialog, which) -> {

                                    new Thread(() -> {
                                        // Cancelliamo l'eventuale foto della tappa dalla memoria
                                        if (tappa.imagePath != null) {
                                            java.io.File fotoDaCancellare = new java.io.File(tappa.imagePath);
                                            if (fotoDaCancellare.exists()) {
                                                fotoDaCancellare.delete();
                                            }
                                        }
                                        // Cancelliamo la tappa dal database Room
                                        AppDatabase.getInstance(requireContext()).tappaDao().eliminaTappa(tappa);

                                        if (!isAdded()) return; // Protezione ciclo di vita

                                        // Aggiorniamo la grafica
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(requireContext(), "Tappa eliminata!", Toast.LENGTH_SHORT).show();
                                            // Rigeneriamo la lista di item
                                            caricaTappe(contenitore);
                                        });
                                    }).start();
                                })
                                .setNegativeButton("Annulla", null) // Chiude il pop-up
                                .show();
                    });
                    // Attacchiamo la tappa completa allo schermo
                    contenitore.addView(itemTappa);
                }
            });
        }).start();
    }
}