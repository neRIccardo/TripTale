package com.example.triptale;
import android.app.AlertDialog;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ListaViaggiFragment extends Fragment {
    private ScrollView scrollView;
    private LinearLayout contenitoreViaggi;
    private TextView textEmptyState;
    private ImageButton btnProfiloLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_viaggi, container, false);
    }

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
                            new Thread(() -> {
                                AppDatabase db = AppDatabase.getInstance(requireContext());
                                db.viaggioDao().eliminaViaggi();

                                if (!isAdded()) return;

                                requireActivity().runOnUiThread(() -> {
                                    aggiornaColoreIcona(btnProfiloLogin);
                                    Toast.makeText(requireContext(), R.string.logout_successo, Toast.LENGTH_SHORT).show();
                                    caricaViaggiDalDatabase();
                                });
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

    // =====================================================================
    // METODO PER CARICARE I VIAGGI
    // =====================================================================
    private void caricaViaggiDalDatabase() {
        // Thread per recuperare i viaggi dal database
        new Thread(() -> {
            // Apriamo il Database e richiediamo la lista dei viaggi
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Viaggio> viaggiSalvati = db.viaggioDao().ottieniViaggi();

            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
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
        }).start();
    }

    // =====================================================================
    // METODO PER AGGIORNARE IL COLORE DELL'ICONA DEL PROFILO
    // =====================================================================
    private void aggiornaColoreIcona(ImageButton btnProfilo) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            // UTENTE LOGGATO: coloriamo l'icona di Verde
            btnProfilo.setColorFilter(android.graphics.Color.parseColor("#4CAF50"));
        } else {
            // NON LOGGATO: rimettiamo il colore originale Arancione
            btnProfilo.setColorFilter(android.graphics.Color.parseColor("#E73B18"));
        }
    }
}