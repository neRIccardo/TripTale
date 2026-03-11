package com.example.triptale;
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

import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

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
                editEmail.setError("Inserisci email");
                editEmail.requestFocus();
                return;
            }
            if (password.isEmpty()){
                editPassword.setError("Inserisci password");
                editPassword.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(requireContext(), "Accesso effettuato!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).popBackStack(); // Torna alla home
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ||
                                e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                            mostraErrore(textErrore, "Email o password errati. Riprova.");
                        } else {
                            mostraErrore(textErrore, "Errore di connessione. Riprova più tardi.");
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
                editEmail.setError("Inserisci email");
                editEmail.requestFocus();
                return;
            }
            if (password.isEmpty()){
                editPassword.setError("Inserisci password");
                editPassword.requestFocus();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(requireContext(), "Account creato con successo!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).popBackStack(); // Torna alla home
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
                            mostraErrore(textErrore, "La password è troppo debole (minimo 6 caratteri).");
                        } else if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            mostraErrore(textErrore, "Esiste già un account con questa email.");
                        } else if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            mostraErrore(textErrore, "Il formato dell'email non è valido.");
                        } else {
                            mostraErrore(textErrore, "Impossibile creare l'account. Riprova.");
                        }
                    });
        });
    }

    // =================================================
    // METODO PER MOSTRARE ERRORE
    // =================================================
    private void mostraErrore(TextView textView, String messaggio) {
        textView.setText(messaggio);
        textView.setVisibility(View.VISIBLE);
    }
}