package com.example.triptale.network;
import android.content.Context;
import android.util.Log;
import com.example.triptale.database.AppDatabase;
import com.example.triptale.model.Tappa;
import com.example.triptale.model.Viaggio;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;

public class FirebaseManager {

    // ========================================================================================
    // METODO PER LA SINCRONIZZAZIONE DI TUTTI I VIAGGI E LE TAPPE
    // ========================================================================================
    public static void sincronizzaTutto(Context context, String userId, Runnable alTermine) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        new Thread(() -> {
            try {
                AppDatabase roomDb = AppDatabase.getInstance(context);
                java.util.List<Viaggio> viaggiLocali = roomDb.viaggioDao().ottieniViaggi();

                // 1. UPLOAD VIAGGI OFFLINE
                for (Viaggio viaggio : viaggiLocali) {
                    if (viaggio.cloudId == null) {
                        Map<String, Object> viaggioMap = new HashMap<>();
                        viaggioMap.put("titolo", viaggio.titolo);
                        viaggioMap.put("cittaDestinazione", viaggio.cittaDestinazione);
                        viaggioMap.put("dataInizio", viaggio.dataInizio);
                        viaggioMap.put("dataFine", viaggio.dataFine);
                        viaggioMap.put("imagePath", viaggio.imagePath);

                        // Usiamo Tasks.await() per bloccare il Thread finché Firebase non risponde
                        DocumentReference docRef = Tasks.await(
                                db.collection("utenti").document(userId).collection("viaggi").add(viaggioMap)
                        );
                        // Salviamo il nuovo ID generato dal cloud nel nostro database locale
                        viaggio.cloudId = docRef.getId();
                        roomDb.viaggioDao().aggiornaViaggio(viaggio);
                    }
                }

                // 2. UPLOAD TAPPE OFFLINE
                for (Viaggio viaggio : viaggiLocali) {
                    if (viaggio.cloudId != null) { // Controlliamo i viaggi che esistono sul cloud
                        List<Tappa> tappeDelViaggio = roomDb.tappaDao().ottieniTappeDelViaggio(viaggio.id);
                        for (Tappa tappa : tappeDelViaggio) {
                            if (tappa.cloudId == null) {
                                Map<String, Object> tappaMap = new HashMap<>();
                                tappaMap.put("idViaggioCloud", viaggio.cloudId);
                                tappaMap.put("titolo", tappa.titolo);
                                tappaMap.put("note", tappa.note);
                                tappaMap.put("imagePath", tappa.imagePath);

                                DocumentReference docRefTappa = Tasks.await(
                                        db.collection("utenti").document(userId).collection("tappe").add(tappaMap)
                                );
                                tappa.cloudId = docRefTappa.getId();
                                roomDb.tappaDao().aggiornaTappa(tappa);
                            }
                        }
                    }
                }

                viaggiLocali = roomDb.viaggioDao().ottieniViaggi(); // Rileggiamo i dati freschi
                HashMap<String, Viaggio> mappaViaggiLocali = new HashMap<>();
                for (Viaggio v : viaggiLocali) {
                    if (v.cloudId != null) {
                        mappaViaggiLocali.put(v.cloudId, v); // Chiave: cloudId, Valore: l'intero Viaggio
                    }
                }


                // 3. DOWNLOAD VIAGGI DAL CLOUD
                QuerySnapshot viaggiSnap = Tasks.await(
                        db.collection("utenti").document(userId).collection("viaggi").get()
                );

                for (QueryDocumentSnapshot doc : viaggiSnap) {
                    String cloudId = doc.getId();
                    if (!mappaViaggiLocali.containsKey(cloudId)) {
                        Viaggio nuovoViaggio = new Viaggio(
                                doc.getString("titolo"), doc.getString("cittaDestinazione"),
                                doc.getString("dataInizio"), doc.getString("dataFine")
                        );
                        nuovoViaggio.cloudId = cloudId;
                        nuovoViaggio.imagePath = doc.getString("imagePath");
                        roomDb.viaggioDao().inserisciViaggio(nuovoViaggio);
                        mappaViaggiLocali.put(cloudId, nuovoViaggio);
                    }
                }

                // 4. DOWNLOAD TAPPE DAL CLOUD
                QuerySnapshot tappeSnap = Tasks.await(
                        db.collection("utenti").document(userId).collection("tappe").get()
                );

                List<Viaggio> tuttiViaggiFinali = roomDb.viaggioDao().ottieniViaggi();
                HashMap<String, Integer> mappaCloudIdToLocaleId = new HashMap<>();
                for (Viaggio v : tuttiViaggiFinali) {
                    if (v.cloudId != null) {
                        mappaCloudIdToLocaleId.put(v.cloudId, v.id);
                    }
                }

                for (QueryDocumentSnapshot doc : tappeSnap) {
                    String cloudIdTappa = doc.getId();
                    String idViaggioCloud = doc.getString("idViaggioCloud");

                    Integer viaggioLocaleId = mappaCloudIdToLocaleId.get(idViaggioCloud);

                    if (viaggioLocaleId != null) {
                        List<Tappa> tappeLocali = roomDb.tappaDao().ottieniTappeDelViaggio(viaggioLocaleId);
                        boolean tappaGiaPresente = false;
                        for (Tappa tLocale : tappeLocali) {
                            if (cloudIdTappa.equals(tLocale.cloudId)) {
                                tappaGiaPresente = true;
                                break;
                            }
                        }

                        if (!tappaGiaPresente) {
                            Tappa nuovaTappa = new Tappa(viaggioLocaleId, doc.getString("titolo"), doc.getString("note"));
                            nuovaTappa.cloudId = cloudIdTappa;
                            nuovaTappa.imagePath = doc.getString("imagePath");
                            roomDb.tappaDao().inserisciTappa(nuovaTappa);
                        }
                    }
                }

                // 5. FINE OPERAZIONI: Sblocchiamo la schermata
                if (alTermine != null) {
                    new Handler(Looper.getMainLooper()).post(alTermine);
                }

            } catch (Exception e) {
                Log.e("FirebaseSync", "Errore durante la sincronizzazione", e);
                // Fondamentale: effettuare lo sblocco anche in caso di errore (es. niente internet)
                // altrimenti l'app dell'utente rimane bloccata in attesa per sempre
                if (alTermine != null) {
                    new Handler(Looper.getMainLooper()).post(alTermine);
                }
            }
        }).start();
    }

    // ========================================================================================
    // CREATE: aggiunge un nuovo viaggio al Cloud (quando l'utente lo crea)
    // ========================================================================================
    public static void aggiungiViaggio(Context context, Viaggio viaggio) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return; // Se non è loggato, ci fermiamo qui. Ci penserà il Login a fare il merge

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> viaggioMap = new HashMap<>();
        viaggioMap.put("titolo", viaggio.titolo);
        viaggioMap.put("cittaDestinazione", viaggio.cittaDestinazione);
        viaggioMap.put("dataInizio", viaggio.dataInizio);
        viaggioMap.put("dataFine", viaggio.dataFine);
        viaggioMap.put("imagePath", viaggio.imagePath);

        db.collection("utenti").document(user.getUid()).collection("viaggi")
                .add(viaggioMap)
                .addOnSuccessListener(documentReference -> {
                    // Aggiorniamo il cloudId in Room
                    new Thread(() -> {
                        viaggio.cloudId = documentReference.getId();
                        AppDatabase.getInstance(context).viaggioDao().aggiornaViaggio(viaggio);
                    }).start();
                });
    }

    // ========================================================================================
    // UPDATE: aggiorna un viaggio esistente sul Cloud (quando l'utente lo modifica)
    // ========================================================================================
    public static void aggiornaViaggio(Viaggio viaggio) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || viaggio.cloudId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> viaggioMap = new HashMap<>();
        viaggioMap.put("titolo", viaggio.titolo);
        viaggioMap.put("cittaDestinazione", viaggio.cittaDestinazione);
        viaggioMap.put("dataInizio", viaggio.dataInizio);
        viaggioMap.put("dataFine", viaggio.dataFine);
        viaggioMap.put("imagePath", viaggio.imagePath);

        // Usiamo .set() per sovrascrivere il documento esatto usando il cloudId
        db.collection("utenti").document(user.getUid()).collection("viaggi")
                .document(viaggio.cloudId)
                .set(viaggioMap);
    }

    // ========================================================================================
    // DELETE: elimina un viaggio dal Cloud (quando l'utente lo elimina)
    // ========================================================================================
    public static void eliminaViaggio(String cloudId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cloudId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("utenti").document(user.getUid()).collection("viaggi")
                .document(cloudId)
                .delete();
    }

    // ========================================================================================
    // CREATE: aggiunge una nuova tappa al Cloud (quando l'utente la crea)
    // ========================================================================================
    public static void aggiungiTappa(Context context, Tappa tappa, String idViaggioCloud) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Se non è loggato, o se il viaggio padre non è ancora sul cloud, ci fermiamo
        if (user == null || idViaggioCloud == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> tappaMap = new HashMap<>();
        tappaMap.put("idViaggioCloud", idViaggioCloud); // Aggiungiamo il campo "idViaggioCloud" per il link tra Viaggio e Tappa
        tappaMap.put("titolo", tappa.titolo);
        tappaMap.put("note", tappa.note);
        tappaMap.put("imagePath", tappa.imagePath);

        // Salviamo le tappe in una collezione separata chiamata "tappe"
        db.collection("utenti").document(user.getUid()).collection("tappe")
                .add(tappaMap)
                .addOnSuccessListener(documentReference -> new Thread(() -> {
                    tappa.cloudId = documentReference.getId();
                    // Aggiorniamo la tappa nel DB locale con il suo nuovo cloudId
                    AppDatabase.getInstance(context).tappaDao().aggiornaTappa(tappa);
                }).start());
    }

    // ========================================================================================
    // UPDATE: aggiorna una tappa esistente sul Cloud (quando l'utente la modifica)
    // ========================================================================================
    public static void aggiornaTappa(Tappa tappa, String idViaggioCloud) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || tappa.cloudId == null || idViaggioCloud == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> tappaMap = new HashMap<>();
        tappaMap.put("idViaggioCloud", idViaggioCloud);
        tappaMap.put("titolo", tappa.titolo);
        tappaMap.put("note", tappa.note);
        tappaMap.put("imagePath", tappa.imagePath);

        db.collection("utenti").document(user.getUid()).collection("tappe")
                .document(tappa.cloudId)
                .set(tappaMap);
    }

    // ========================================================================================
    // DELETE: elimina una tappa dal Cloud (quando l'utente la elimina)
    // ========================================================================================
    public static void eliminaTappa(String cloudId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cloudId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("utenti").document(user.getUid()).collection("tappe")
                .document(cloudId)
                .delete();
    }
}