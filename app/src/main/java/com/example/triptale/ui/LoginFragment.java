package com.example.triptale.ui;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.triptale.R;
import com.example.triptale.network.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

/**
 * Fragment responsabile della gestione dell'autenticazione utente tramite Firebase.
 * Fornisce l'interfaccia unificata sia per il Login che per la Registrazione di un nuovo account.
 * Si occupa della validazione formale degli input, della gestione asincrona delle chiamate di rete
 * e dell'avvio automatico della sincronizzazione cloud-locale in caso di accesso convalidato.
 */
public class LoginFragment extends Fragment {
    private FirebaseAuth mAuth;

    /**
     * Inizializza e restituisce la gerarchia delle view associata al Fragment di login.
     *
     * @param inflater Il LayoutInflater utilizzato per "gonfiare" il layout XML.
     * @param container Il ViewGroup padre a cui la UI del Fragment dovrebbe essere attaccata.
     * @param savedInstanceState Lo stato salvato in precedenza.
     * @return La View radice del layout del Fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    /**
     * Associa i componenti grafici alle relative logiche di business.
     * Imposta i listener per i pulsanti di "Accedi" e "Registrati", intercettando
     * in modo puntuale le varie eccezioni restituite da Firebase (es. password troppo debole,
     * utente inesistente, email già in uso) per fornire un feedback testuale mirato all'utente.
     *
     * @param view La View radice restituita da onCreateView().
     * @param savedInstanceState L'eventuale stato salvato in precedenza.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        EditText editEmail = view.findViewById(R.id.editEmail);
        EditText editPassword = view.findViewById(R.id.editPassword);
        Button btnAccedi = view.findViewById(R.id.btnAccedi);
        Button btnRegistrati = view.findViewById(R.id.btnRegistrati);
        TextView textErrore = view.findViewById(R.id.textErrore);

        // --- GESTIONE BOTTONE ACCEDI ---
        btnAccedi.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            editEmail.setError(null);
            editPassword.setError(null);
            textErrore.setVisibility(View.GONE);

            if (email.isEmpty()){
                editEmail.setError(getString(R.string.errore_inserisci_email));
                editEmail.requestFocus();
                return;
            }
            if (password.isEmpty()){
                editPassword.setError(getString(R.string.errore_inserisci_password));
                editPassword.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Context context = getContext();
                        if (context != null && isAdded()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                // Otteniamo l'ID univoco dell'utente appena loggato
                                String userId = user.getUid();
                                FirebaseManager.sincronizzaTutto(context, userId, () -> {
                                    if (isAdded() && getContext() != null) {
                                        Toast.makeText(getContext(), R.string.login_successo, Toast.LENGTH_SHORT).show();
                                        Navigation.findNavController(view).popBackStack();
                                    }
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            if (e instanceof FirebaseAuthInvalidCredentialsException ||
                                    e instanceof FirebaseAuthInvalidUserException) {
                                mostraErrore(textErrore, getString(R.string.errore_credenziali_errate));
                            } else {
                                mostraErrore(textErrore, getString(R.string.errore_connessione));
                            }
                        }
                    });
        });

        // --- GESTIONE BOTTONE REGISTRATI ---
        btnRegistrati.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            editEmail.setError(null);
            editPassword.setError(null);
            textErrore.setVisibility(View.GONE);

            if (email.isEmpty()){
                editEmail.setError(getString(R.string.errore_inserisci_email));
                editEmail.requestFocus();
                return;
            }
            if (password.isEmpty()){
                editPassword.setError(getString(R.string.errore_inserisci_password));
                editPassword.requestFocus();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Context context = getContext();
                        if (context != null && isAdded()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                // Otteniamo l'ID univoco dell'utente appena registrato
                                String userId = user.getUid();
                                FirebaseManager.sincronizzaTutto(context, userId, () -> {
                                    if (isAdded() && getContext() != null) {
                                        Toast.makeText(getContext(), R.string.login_successo, Toast.LENGTH_SHORT).show();
                                        Navigation.findNavController(view).popBackStack();
                                    }
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            if (e instanceof FirebaseAuthWeakPasswordException) {
                                mostraErrore(textErrore, getString(R.string.err_password_debole));
                            } else if (e instanceof FirebaseAuthUserCollisionException) {
                                mostraErrore(textErrore, getString(R.string.err_email_esistente));
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                mostraErrore(textErrore, getString(R.string.err_email_invalida));
                            } else {
                                mostraErrore(textErrore, getString(R.string.err_generico_account));
                            }
                        }
                    });
        });
    }

    /**
     * Metodo di utilità per semplificare la visualizzazione a schermo dei messaggi di errore
     * generati durante il processo di autenticazione.
     *
     * @param textView La TextView dedicata a mostrare gli alert di errore all'utente.
     * @param messaggio Il contenuto testuale dell'errore da visualizzare.
     */
    private void mostraErrore(TextView textView, String messaggio) {
        textView.setText(messaggio);
        textView.setVisibility(View.VISIBLE);
    }
}