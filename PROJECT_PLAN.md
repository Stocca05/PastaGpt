# DroidMentor — Piano di Progetto e Timeline

**Autore:** Luca Raona (matricola 55603) — progetto individuale, nome PastaGPT mantenuto
**Corso:** Mobile Devices Programming (PDM) — Instituto Superior de Engenharia de Lisboa
**Anno accademico:** 2026/2027 — Semestre invernale
**Docenti:** Prof. Paulo Pereira, Prof. Diogo Cardoso
**Scadenza finale:** 12 dicembre 2026
**Documento:** Deliverable della Milestone 1 (tag `mentor_1`)

| Versione | Data | Autore | Stato | Note |
|---|---|---|---|---|
| 1.0 | 18/09/2026 | Luca Raona | Approvato per M1 | Versione iniziale consegnata con `mentor_1` |
| 1.0.1 | 09/10/2026 | Luca Raona | Decisioni M2 concordate | Cronologia completa, schema iniziale, invii ed errori; nessun task funzionale completato |
| 1.0.2 | 09/10/2026 | Luca Raona | M2-03 completato | Struttura package, tema Material 3 e verifica chiaro/scuro |
| 1.0.3 | 09/10/2026 | Luca Raona | M2-04 completato | Sei modelli di dominio; commenti didattici in inglese nel codice Kotlin dell’app |
| 1.0.4 | 09/10/2026 | Luca Raona | M2-11 completato | Entità Room, converter enum e sette test di persistenza delle entità superati |
| 1.0.5 | 09/10/2026 | Luca Raona | M2-12 completato | DAO dell’app, relazione ultimo messaggio, scritture atomiche e quattordici test DAO superati |
| 1.0.6 | 09/10/2026 | Luca Raona | M2-13 completato | Database dell’app v1, schema esportato e tracciato in Git, cinque test database superati |
| 1.0.7 | 10/10/2026 | Luca Raona | M2-14 completato | DataStore dedicato alla chiave, esiti tipizzati e dodici test impostazioni superati |
| 1.0.8 | 10/10/2026 | Luca Raona | M2-05 completato | Client Ktor configurato, log senza dati sensibili e otto test client superati in debug e release |
| 1.0.9 | 10/10/2026 | Luca Raona | M2-06 completato | DTO Gemini e conversione turni nel livello dati, dodici test del contratto JSON superati |
| 1.0.10 | 10/10/2026 | Luca Raona | M2-08 completato | Builder di cronologia nel dominio, rifiuti tipizzati e ventidue test di validazione superati |
| 1.0.11 | 10/10/2026 | Luca Raona | M2-10 completato | Persona del mentore centralizzata, tono didattico, esempi Kotlin commentati e vincoli del corso |
| 1.0.12 | 10/10/2026 | Luca Raona | M2-09 completato | Esiti API tipizzati, mapper HTTP/risposte, conversione errori persistibili e venti test superati |
| 1.0.13 | 11/10/2026 | Luca Raona | M2-07 completato | Data source Gemini stateless, gate connettività obbligatorio e tredici test di integrazione superati |
| 1.0.14 | 11/10/2026 | Luca Raona | M2-23 completato | Connettività Android validata, callback con cleanup, dieci test locali e tre su dispositivo superati |
| 1.0.15 | 11/10/2026 | Luca Raona | M2-15 completato | Repository Room offline-first, invii per chat, retry sulla stessa riga e ventitré test di integrazione superati |
| 1.0.16 | 11/10/2026 | Luca Raona | M2-01 completato | Application registrata, grafo manuale lazy condiviso, Context.app e quattro test del service locator superati |
| 1.0.17 | 11/10/2026 | Luca Raona | M2-02 completato | DependencyContainer implementato dall’Application, factory manuale AndroidX, sei test locali e due su dispositivo superati |
| 1.0.18 | 11/10/2026 | Luca Raona | M2-16 completato | Grafo delle cinque destinazioni conforme alla Figura 1, ritorni e stato salvato, nove test Android superati |
| 1.0.19 | 12/10/2026 | Luca Raona | M2-17 | Menu principale con logo, titolo e tre azioni; layout semplice e scorrevole. |
| 1.0.20 | 12/10/2026 | Luca Raona | M2-18 | Schermata Informazioni con versione Gradle, autore, corso, ISEL e crediti; frame Material condiviso. |
| 1.0.21 | 12/10/2026 | Luca Raona | M2-22 | Tre ViewModel manuali con StateFlow immutabile, eventi Channel e otto test di stato; Title/About statiche senza ViewModel vuoti. |
| 1.0.22 | 12/10/2026 | Luca Raona | M2-19 | Form della chiave con password, visibilità solo in memoria, salvataggio/cancellazione DataStore, stato configurato e snackbar tipizzate. |
| 1.0.23 | 12/10/2026 | Luca Raona | M2-20 | Lista Room con ID stabili, anteprima e data relativa, stato vuoto illustrato e FAB per creare una chat locale. |
| 1.0.24 | 12/10/2026 | Luca Raona | M2-21 | Chat Room con bubble per ruolo, input multilinea, invio protetto, attesa, retry sulla stessa riga, errori persistiti e gestione tastiera; eliminati i placeholder. |
| 1.0.25 | 12/10/2026 | Luca Raona | M2-24 | Banner offline persistente, invio/retry protetti da rete e chiave, bozze ancora modificabili e tre nuovi test sui prerequisiti. |
| 1.1 | *(da compilare)* | | Bozza | Revisione post-M2 |
| 1.2 | *(da compilare)* | | Bozza | Revisione post-M3 |

> **Nota di manutenzione.** Questo documento è vivo: al termine di ogni milestone va aggiornato lo stato dei task (`☐` → `☑`), le stime effettive e l'eventuale rischedulazione dei task non conclusi. La storia delle revisioni è tracciata nella tabella soprastante e nel log di Git.

---

## Indice

1. [Sintesi esecutiva](#1-sintesi-esecutiva)
2. [Autore e metodo di lavoro](#2-autore-e-metodo-di-lavoro)
3. [Vincoli tecnici non negoziabili](#3-vincoli-tecnici-non-negoziabili)
4. [Architettura di riferimento](#4-architettura-di-riferimento)
5. [Convenzioni operative e processo](#5-convenzioni-operative-e-processo)
6. [Legenda di stime, priorità e stato](#6-legenda-di-stime-priorità-e-stato)
7. [Timeline complessiva](#7-timeline-complessiva)
8. [Milestone 1 — Pianificazione e fondazioni (21/09/2026)](#8-milestone-1--pianificazione-e-fondazioni-21092026)
9. [Milestone 2 — Nucleo funzionante end-to-end (12/10/2026)](#9-milestone-2--nucleo-funzionante-end-to-end-12102026)
10. [Milestone 3 — Funzionalità avanzate e robustezza (16/11/2026)](#10-milestone-3--funzionalità-avanzate-e-robustezza-16112026)
11. [Final Milestone — Consolidamento e consegna (12/12/2026)](#11-final-milestone--consolidamento-e-consegna-12122026)
12. [Strategia di verifica](#12-strategia-di-verifica)
13. [Registro dei rischi](#13-registro-dei-rischi)
14. [Matrice di tracciabilità requisiti → task](#14-matrice-di-tracciabilità-requisiti--task)
15. [Appendici](#15-appendici)

---

## 1. Sintesi esecutiva

DroidMentor è un client Android **BYOK** (*Bring Your Own Key*) per Large Language Model, che espone all'utente un mentore conversazionale specializzato in sviluppo Android. La comunicazione avviene tramite l'API REST di Gemini, endpoint `generateContent`, **in modalità rigorosamente stateless**: non è ammesso alcun identificativo di sessione lato server, per cui è l'applicazione a ricostruire e trasmettere l'intera cronologia conversazionale a ogni singola interazione di rete.

L'applicazione è progettata come esperienza **offline-first**: la base dati locale (Room) è l'unica sorgente di verità per la UI, tutte le conversazioni restano consultabili in assenza di rete e la disponibilità della connessione viene verificata *prima* di ogni chiamata all'API, comunicando esplicitamente all'utente l'impossibilità di nuove interazioni quando si è offline.

Il piano si articola su **quattro milestone** e adotta un ciclo di vita incrementale a sprint settimanali. La strategia di fondo è la **verticalizzazione anticipata**: entro la Milestone 2 deve esistere una *slice* verticale completa (UI → ViewModel → Repository → Room + Ktor → Gemini → Room → UI) anche se minimale, in modo che i rischi integrativi più alti — serializzazione del payload stateless, service locator manuale, coerenza fra rete e persistenza — siano affrontati quando c'è ancora margine di manovra. Le funzionalità ad alta complessità logica (riscrittura della conversazione, ripristino della sessione attiva, allegati immagine) sono concentrate nella Milestone 3, mentre la Final Milestone è dedicata a integrazione, irrobustimento, regressione e consegna, con un **code freeze pianificato al 08/12/2026** che lascia quattro giorni di margine sulla scadenza.

**Obiettivi di qualità dichiarati**

| Obiettivo | Metrica verificabile |
|---|---|
| Stabilità | Zero crash (ANR/eccezioni non gestite) durante la regressione finale e la registrazione del video |
| Offline-first | Tutte le conversazioni leggibili con modalità aereo attiva; nessuna schermata vuota o in errore |
| Robustezza di rete | Gestione esplicita e testata di 400, 401/403, 429, 5xx, timeout, risposta malformata |
| Aderenza ai vincoli | Assenza totale di Hilt/Dagger, Retrofit, Gson/Moshi, Base64 persistito in Room |
| Copertura di test | ≥ 80 % sulla logica di costruzione del payload e sulla mappatura degli errori |
| Consegna | 4 tag Git (`mentor_1`, `mentor_2`, `mentor_3`, `mentor_f`) creati entro le rispettive scadenze |

---

## 2. Autore e metodo di lavoro

DroidMentor è un progetto individuale: lo realizzo io, **Luca Raona** (matricola 55603), sotto il nome di progetto **PastaGPT**.

**Organizzazione del lavoro.** Le tabelle WBS più sotto raggruppano i task per area tecnica (Rete, Persistenza, UI, Connettività, Verifica, Consegna) — è un raggruppamento tematico per orientarsi nel lavoro, non un'assegnazione a persone diverse. Un task che tocca più aree insieme va affrontato nella stessa sessione, senza interruzioni nel mezzo, così il ragionamento resta coerente dall'inizio alla fine.

**Metodo di verifica.** Sui task a rischio più alto (marcati 🔴 in §6) scrivo i test prima dell'implementazione, non dopo: è il modo più affidabile per non scoprire un errore concettuale a metà lavoro. Su ogni pull request, rileggo il diff per intero il giorno successivo a quando l'ho scritto, non subito dopo — a distanza di qualche ora si notano errori che a caldo restano invisibili.

**Gestione del rischio.** Il margine del code freeze (§7, §13) non è tempo extra per nuove funzionalità: è la riserva per un imprevisto — un'influenza, un altro esame che si sovrappone — perché non c'è nessun altro che possa assorbire un ritardo al posto mio.

**Ritmo di lavoro**

| Rito | Cadenza | Durata | Contenuto |
|---|---|---|---|
| Pianificazione sprint | Lunedì | 15 min | Scelgo i task dello sprint dalla WBS, confermo che la stima regga ancora |
| Diario di avanzamento | Merc./Ven. | 5 min | Una riga scritta: fatto / in corso / bloccato — utile come materiale grezzo per la scaletta del video di milestone |
| Autoreview + retrospettiva | Domenica | 30 min | Rileggo a mente fredda i diff della settimana, li verifico contro la Definition of Done, aggiusto il piano della settimana successiva |
| Revisione di milestone | Fine milestone | 60 min | Eseguo la checklist di accettazione dall'inizio alla fine, registro il video, creo il tag |

---

## 3. Vincoli tecnici non negoziabili

Questi vincoli derivano direttamente dall'enunciato dell'assignment e hanno valore di **criterio di accettazione trasversale**: la loro violazione invalida la consegna indipendentemente dalla qualità funzionale del risultato. Ogni pull request deve essere verificata rispetto a questa tabella.

| # | Vincolo | Implicazione implementativa | Anti-pattern da evitare | Task che lo indirizzano |
|---|---|---|---|---|
| V1 | **Dependency injection manuale** tramite la classe `Application` usata come *service locator*. Hilt/Dagger **vietati**. | `DroidMentorApplication` espone i singleton (database, DataStore, `HttpClient`, repository) con inizializzazione *lazy*. I ViewModel vengono creati con `ViewModelProvider.Factory` scritte a mano (`viewModelFactory { initializer { … } }`). | Qualsiasi annotazione `@HiltAndroidApp`, `@Inject`, `@Module`, `@Provides`; plugin KSP di Hilt nel `build.gradle.kts`. | M2-01, M2-02 |
| V2 | **Rete esclusivamente con Ktor Client + Kotlinx Serialization.** | Plugin `ContentNegotiation` con `Json { ignoreUnknownKeys = true }`; DTO annotati `@Serializable`; engine OkHttp o CIO. | Retrofit, Volley, `HttpURLConnection`, Gson, Moshi, Jackson. | M2-05, M2-06, M2-07 |
| V3 | **Persistenza delle chat con Room.** | Entità `chats` e `messages` con chiave esterna e `onDelete = CASCADE`; DAO che espongono `Flow`; schema esportato e versionato. | Salvataggio della cronologia in `SharedPreferences`, in file JSON o solo in memoria. | M2-11, M2-12, M2-13 |
| V4 | **Chiave API BYOK salvata con DataStore.** | `Preferences DataStore` dedicato, separato da eventuali preferenze di UI; esposizione come `Flow<String?>`; possibilità di cancellazione. | Chiave in `SharedPreferences`, in Room, in una costante nel codice, in `local.properties` committato, nel `BuildConfig`. | M2-14, MF-06 |
| V5 | **Modalità stateless obbligatoria.** Vietato l'uso di ID di sessione multi-turn lato server. | Ogni richiesta trasporta l'intero array `contents` con alternanza rigorosa dei ruoli `user` / `model`. Lo stato conversazionale vive **solo** in Room. | Uso di API o endpoint con stato server-side (es. sessioni persistenti o *interaction id*); memorizzazione di un `conversationId` restituito dal servizio. | M2-08, M2-15 |
| V6 | **Persona impostata via `system_instruction`.** | Campo `system_instruction` presente in ogni richiesta, con testo centralizzato in un'unica costante. Non sono richieste tecniche RAG o di prompt engineering avanzato. | Iniezione della persona come primo messaggio `user` dell'array `contents`. | M2-10 |
| V7 | **Esperienza offline-first.** | Room è l'unica sorgente di verità per la UI; verifica della connettività *prima* della chiamata; messaggio esplicito quando si è offline. | Schermate che dipendono dalla risposta di rete per popolarsi; `try/catch` silenzioso che lascia la UI vuota. | M2-15, M2-23, M2-24 |
| V8 | **Gestione graziosa degli errori HTTP** (es. 429, 500). | Mappatura tipizzata `sealed interface` degli esiti; nessuna eccezione propagata alla UI; feedback comprensibile all'utente. | Crash su `ClientRequestException`; messaggi tecnici grezzi mostrati all'utente. | M2-09, M3-08, M3-09 |
| V9 | **Immagini (requisito opzionale valorizzato):** in Room si persiste **solo l'URI/percorso del file**. | File salvati in storage locale dell'app; codifica `inline_data` in Base64 costruita **in memoria** al solo momento della richiesta e mai scritta su DB. | Colonna `TEXT` contenente la stringa Base64; `BLOB` con i byte dell'immagine. | M3-11 … M3-15 |
| V10 | **Consegne tramite tag Git** `mentor_X` sul repository, con accesso completo ai docenti e `README.md` in radice con la tua identificazione. | Tag annotati e pushati; README aggiornato a ogni milestone con il link al video. | Tag leggeri creati localmente e mai pushati; README mancante o incompleto. | M1-01, M1-02, M1-09, M2-31, M3-18, MF-11 |

---

## 4. Architettura di riferimento

### 4.1 Stratificazione

Architettura a tre strati con flusso di dati unidirezionale (UDF), senza framework di DI.

```
┌──────────────────────────────────────────────────────────────────┐
│  UI  (Jetpack Compose + Navigation)                              │
│  TitleScreen · ChatHistoryScreen · ActiveChatScreen              │
│  AboutScreen · SettingsScreen                                    │
│        ▲ UiState (StateFlow)          │ eventi utente            │
├────────┴───────────────────────────────▼─────────────────────────┤
│  PRESENTATION  ViewModel + UiState immutabili                    │
│  creati da ViewModelProvider.Factory manuali                     │
├──────────────────────────────────────────────────────────────────┤
│  DOMAIN  Chat · Message · Role · HistoryPayloadBuilder           │
│          (Kotlin puro, nessuna dipendenza da Android)            │
├──────────────────────────────────────────────────────────────────┤
│  DATA                                                            │
│   ChatRepository ──► Room (SINGLE SOURCE OF TRUTH)               │
│        │                                                         │
│        └──────────► GeminiRemoteDataSource (Ktor)                │
│   SettingsRepository ──► DataStore (chiave BYOK)                 │
│   ConnectivityObserver ──► ConnectivityManager                   │
├──────────────────────────────────────────────────────────────────┤
│  DroidMentorApplication  ← SERVICE LOCATOR (singleton lazy)      │
└──────────────────────────────────────────────────────────────────┘
```

### 4.2 Principio cardine: Room come sorgente di verità

La UI **non osserva mai** direttamente il risultato di una chiamata di rete. Il ciclo è:

1. L'utente invia un messaggio → il repository controlla chiave, connessione e assenza di messaggi irrisolti; quindi lo inserisce in Room con stato `SENDING`.
2. Il `Flow` del DAO notifica la UI, che mostra immediatamente il messaggio (*optimistic update*).
3. Il repository verifica la connettività, costruisce il payload con **tutta** la cronologia e chiama Gemini.
4. In caso di successo, una transazione Room inserisce la risposta `MODEL` e aggiorna il messaggio utente a `SENT`. In caso di fallimento, il messaggio passa a `FAILED` con categoria di errore persistita.
5. La UI si aggiorna di nuovo osservando solo Room.

Questa scelta soddisfa contemporaneamente V3, V5 e V7 e rende banale il requisito di consultazione offline.

### 4.3 Stack tecnologico

| Ambito | Scelta | Motivazione |
|---|---|---|
| Linguaggio | Kotlin + Coroutines/Flow | Standard del corso; concorrenza strutturata |
| UI | Jetpack Compose + Material 3 | Dichiarativo, allineato alle pratiche moderne |
| Navigazione | Navigation Compose con rotte tipizzate | Grafo esplicito, deep link verso Settings |
| Rete | **Ktor Client** (engine OkHttp) | Vincolo V2 |
| Serializzazione | **Kotlinx Serialization** | Vincolo V2 |
| Persistenza | **Room** | Vincolo V3 |
| Preferenze/segreti | **DataStore (Preferences)** | Vincolo V4 |
| Immagini | Coil (solo rendering) | Caricamento asincrono da URI locale |
| Test | JUnit 4, `kotlinx-coroutines-test`, Ktor `MockEngine`, Room in-memory, Compose UI Test | Copertura dei tre livelli della piramide |
| CI | GitHub Actions | Build e test automatici su ogni PR |

### 4.4 Configurazione di progetto

| Parametro | Valore | Nota |
|---|---|---|
| `minSdk` | 26 | Consente `java.time` e API di storage moderne |
| `targetSdk` / `compileSdk` | Ultima stabile disponibile | Da fissare in `libs.versions.toml` |
| Moduli | Singolo modulo `:app` con package per layer | Modularizzazione non richiesta; evita complessità non necessaria |
| Permessi | `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA` (solo se si implementa il requisito opzionale) | Nessun permesso di storage grazie al Photo Picker |

---

## 5. Convenzioni operative e processo

### 5.1 Repository e branching

- **Repository:** `pastagpt-droidmentor` (privato), con accesso completo concesso ai Prof. Paulo Pereira e Prof. Diogo Cardoso.
- **Modello:** *trunk-based* con branch a vita breve. `main` è sempre compilabile e verde in CI.
- **Nomenclatura branch:** `feat/<id-task>-<slug>`, `fix/<slug>`, `docs/<slug>`, `chore/<slug>`.
  Esempio: `feat/m2-08-history-payload-builder`.
- **Commit:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`), messaggio in inglese, riferimento al task nel corpo.
- **Pull request:** usata come checkpoint di autoreview anche da solo — apri una PR dal branch della feature, lascia che il template con la checklist dei vincoli giri su di essa, e fai merge solo dopo aver riletto il diff una volta per intero, idealmente il giorno dopo. PR di dimensione massima consigliata: ~400 righe modificate.
- **Tag:** annotati e pushati esplicitamente.
  ```bash
  git tag -a mentor_1 -m "Milestone 1 - Project plan"
  git push origin mentor_1
  ```

### 5.2 Definition of Done (per singolo task)

Un task è *Done* solo se tutte le condizioni seguenti sono soddisfatte:

- ☐ Il codice compila senza warning nuovi e supera `./gradlew lint`.
- ☐ Sono presenti i test previsti dalla strategia di verifica per quel livello.
- ☐ La suite completa (`./gradlew test connectedAndroidTest`) è verde in locale e in CI.
- ☐ Il diff è stato riletto per intero, dopo una pausa, prima del merge (vedi §2 sull'autoreview).
- ☐ Nessun vincolo della sezione 3 è stato violato (verifica esplicita in fase di review).
- ☐ Nessun segreto, chiave o token è finito nel repository o nei log.
- ☐ La documentazione impattata (README, ADR, questo piano) è aggiornata nello stesso commit.

### 5.3 Definition of Done (per milestone)

- ☐ Tutti i task pianificati sono *Done* o formalmente rischedulati con motivazione scritta.
- ☐ L'applicazione si installa ed è utilizzabile su un dispositivo fisico e su emulatore.
- ☐ Il video (ove previsto) rispetta la durata di **5–7 minuti** e copre tutti i punti richiesti.
- ☐ Il `README.md` di radice contiene la tua identificazione e il link al video.
- ☐ Il tag `mentor_X` è creato **e pushato** entro la data di scadenza.
- ☐ La checklist di accettazione della milestone è compilata in questo documento.

---

## 6. Legenda di stime, priorità e stato

**Difficoltà**

| Simbolo | Livello | Significato operativo |
|---|---|---|
| 🟢 | Bassa | Attività lineare, API note, rischio tecnico trascurabile. Eseguibile in autonomia. |
| 🟡 | Media | Richiede progettazione o integrazione fra più componenti. Review attenta consigliata. |
| 🔴 | Alta | Logica critica, alto rischio di regressione o di errore concettuale. **Test scritti prima dell'implementazione obbligatori** (il sostituto solitario del pair programming — vedi §2). |

**Priorità**

| Sigla | Significato |
|---|---|
| **M** | *Must* — requisito esplicito dell'enunciato: la sua assenza compromette la valutazione |
| **S** | *Should* — qualità attesa, fortemente raccomandato |
| **C** | *Could* — requisito opzionale valorizzato o miglioria |

**Stima:** espressa in **ore-uomo ideali** (lavoro effettivo, escluse interruzioni). Fattore di conversione prudenziale consigliato: **1 ora ideale ≈ 1,4 ore di calendario**.

**Stato:** `☐` da fare · `◐` in corso · `☑` completato · `⊘` rischedulato.

---

## 7. Timeline complessiva

| Fase | Periodo | Sprint | Focus | Ore ideali stimate |
|---|---|---|---|---|
| **Milestone 1** | 18/09 → **21/09/2026** | Sprint 0 | Pianificazione, repository, fondazioni di progetto | ~11 h |
| **Milestone 2** | 22/09 → **12/10/2026** | Sprint 1–3 | Architettura, rete stateless, persistenza, slice verticale completa, strategia di verifica | ~92 h |
| **Milestone 3** | 13/10 → **16/11/2026** | Sprint 4–8 | Cancellazione, riscrittura conversazione, sessione attiva, resilienza, immagini | ~83 h |
| **Final Milestone** | 17/11 → **12/12/2026** | Sprint 9–12 | Integrazione, hardening, UX, regressione, consegna | ~62 h |
| | | | **Totale** | **~248 h, tutte da solo** (~21 h ideali/settimana su 12 settimane ≈ 29 h di calendario/settimana con il fattore 1,4×) |

> **Verifica di sostenibilità.** 248 ore ideali da solo sulle ~12 settimane fino al 12/12/2026 sono circa 21 ore ideali a settimana — circa 29 ore di calendario a settimana applicando il fattore 1,4× del §6. È un carico consistente sopra a qualunque altro corso tu abbia in questo semestre, e non è distribuito uniformemente: la Milestone 2 da sola è più pesante di questa media (vedi il suo totale più sotto). Se il ritmo non regge contro il tuo orario reale, il punto da tagliare sono i task *Could* (il requisito opzionale delle immagini, M3-11…M3-15, MF-01) e i *Should*, non i *Must* — vedi §13, rischio R4.

### 7.1 Obiettivi di sprint

| Sprint | Settimana | Obiettivo verificabile a fine sprint |
|---|---|---|
| **0** | 18–21/09 | Repository operativo, piano approvato, tag `mentor_1` pushato |
| **1** | 22–28/09 | Progetto Android che compila con tutte le dipendenze; service locator funzionante; DTO Gemini serializzano/deserializzano correttamente in test |
| **2** | 29/09–05/10 | Room + DataStore operativi; navigazione fra le 5 schermate; Settings salva e rilegge la chiave |
| **3** | 06–12/10 | **Slice verticale completa:** invio messaggio → risposta del modello → persistenza → rilettura offline. Video M2 registrato, tag `mentor_2` |
| **4** | 13–19/10 | Cancellazione conversazioni con CASCADE; ordinamento deterministico dei messaggi |
| **5** | 20–26/10 | Riscrittura della conversazione funzionante end-to-end (troncamento + reinvio) |
| **6** | 27/10–02/11 | Ripristino della sessione attiva al lancio; sopravvivenza a process death |
| **7** | 03–09/11 | Resilienza di rete: retry con backoff, stati per messaggio, azione "Riprova" |
| **8** | 10–16/11 | Requisito opzionale immagini in stato dimostrabile. Video M3, tag `mentor_3` |
| **9** | 17–23/11 | Chiusura del requisito immagini, ciclo di vita dei file, hardening errori |
| **10** | 24–30/11 | Revisione UX, adattività, accessibilità, performance |
| **11** | 01–07/12 | Regressione completa su matrice dispositivi; documentazione finale |
| **12** | 08–12/12 | **Code freeze 08/12.** Solo bugfix critici. Video finale, build release, tag `mentor_f` |

### 7.2 Diagramma di Gantt

```mermaid
gantt
    title DroidMentor - PastaGPT (da solo, Luca Raona) - Timeline 2026
    dateFormat YYYY-MM-DD
    axisFormat %d/%m

    section Milestone 1
    Repository, piano e fondazioni      :m1, 2026-09-18, 4d
    Consegna mentor_1                   :milestone, 2026-09-21, 0d

    section Milestone 2
    Service locator e fondamenta        :m2a, 2026-09-22, 7d
    Rete Ktor e DTO Gemini              :m2b, 2026-09-22, 10d
    Room e DataStore                    :m2c, 2026-09-28, 8d
    UI e navigazione                    :m2d, 2026-09-29, 10d
    Slice verticale e verifica          :m2e, 2026-10-06, 5d
    Video e consegna mentor_2           :milestone, 2026-10-12, 0d

    section Milestone 3
    Cancellazione e ordinamento         :m3a, 2026-10-13, 7d
    Riscrittura conversazione           :m3b, 2026-10-20, 7d
    Sessione attiva e process death     :m3c, 2026-10-27, 7d
    Resilienza di rete                  :m3d, 2026-11-03, 7d
    Immagini (opzionale)                :m3e, 2026-11-03, 14d
    Video e consegna mentor_3           :milestone, 2026-11-16, 0d

    section Final Milestone
    Chiusura immagini e hardening       :mfa, 2026-11-17, 7d
    UX, adattivita e performance        :mfb, 2026-11-24, 7d
    Regressione e documentazione        :mfc, 2026-12-01, 7d
    Code freeze                         :milestone, 2026-12-08, 0d
    Video finale e consegna mentor_f    :milestone, 2026-12-12, 0d
```

---

## 8. Milestone 1 — Pianificazione e fondazioni (21/09/2026)

> **Settimana 3 del semestre. Finestra effettiva: 4 giorni.**

### 8.1 Obiettivo

Consegnare il piano di progetto richiesto dall'enunciato e predisporre l'infrastruttura di lavoro. I task marcati *(anticipo su M2)* non sono richiesti dai criteri di accettazione della Milestone 1, ma vanno eseguiti ora perché rimuovono attrito all'avvio dello Sprint 1: creare lo scheletro del progetto Gradle il giorno prima della prima *feature* è la causa più comune di slittamento nelle prime settimane.

### 8.2 WBS — Work Breakdown Structure

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M1-01 | Creazione repository | Repo GitHub `pastagpt-droidmentor`, privato. Invito ai Prof. Paulo Pereira e Diogo Cardoso con **accesso completo**. `.gitignore` per Android/Kotlin/IDEA. | M | 🟢 | 0,5 h | — | ☑ |
| M1-02 | `README.md` di radice | La tua identificazione: nome completo, numero studente, indirizzo istituzionale, handle GitHub. Titolo del progetto e riferimento al corso. Segnaposto per i link ai video delle milestone. | M | 🟢 | 0,5 h | M1-01 | ☑ |
| M1-03 | Piano di progetto | Redazione e revisione di questo documento (`PROJECT_PLAN.md`), con timeline, task e criteri di accettazione. L'enunciato chiede anche "a chi sono assegnati" i task: essendo un progetto individuale, l'assegnazione è implicita — sono tutti miei. Link in evidenza dal README. | M | 🟡 | 3 h | M1-01 | ☑ |
| M1-04 | Convenzioni di lavoro | `CONTRIBUTING.md` con branching, commit convention, policy di review. `.github/pull_request_template.md` con la checklist dei vincoli della sezione 3. `CODEOWNERS`. | S | 🟢 | 1 h | M1-01 | ☑ |
| M1-05 | Board di progetto | GitHub Projects con colonne *Backlog / Sprint / In review / Done*. Import dei task di questo piano come issue, con label per milestone e difficoltà. | S | 🟢 | 1 h | M1-03 | ⊘ |
| M1-06 | Scaffolding progetto *(anticipo su M2)* | Progetto Android Studio: Kotlin, Compose, `minSdk 26`, package `pt.isel.pdm.droidmentor`. Verifica che l'app vuota si avvii su emulatore. | S | 🟡 | 2 h | M1-01 | ☑ |
| M1-07 | Version catalog *(anticipo su M2)* | `gradle/libs.versions.toml` con Compose BOM, Navigation, Ktor (core, engine, content-negotiation, logging), kotlinx-serialization, Room (runtime, ktx, compiler KSP), DataStore Preferences, Coil, librerie di test. Sincronizzazione riuscita. | S | 🟡 | 1,5 h | M1-06 | ☑ |
| M1-08 | Provisioning chiave Gemini | Una chiave API da Google AI Studio (idealmente una seconda di riserva da un account diverso). Verifica dei limiti del piano gratuito e annotazione delle quote (rilevante per i test del percorso HTTP 429). **Nessuna chiave committata.** | M | 🟢 | 0,5 h | — | ☑ |
| M1-09 | Tag `mentor_1` | Commit finale, tag annotato, push del tag. Verifica su GitHub che il tag sia visibile e che i docenti abbiano accesso. | M | 🟢 | 0,25 h | M1-02, M1-03 | ☑ |

**Totale stimato:** ~10,25 ore ideali.

### 8.3 Checklist di accettazione (Milestone 1)

Derivata letteralmente dagli *Acceptance criteria* dell'enunciato:

- ☑ Il repository è correttamente taggato **`mentor_1`** ed il tag è stato pushato sul remoto.
- ☑ Il repository contiene la **timeline del piano di progetto** con l'indicazione dei task pianificati (l'assegnazione richiesta dall'enunciato è implicita: progetto individuale, sono tutti miei).
- ☑ Il file **`README.md` in radice** contiene la tua identificazione.
- ☐ Entrambi i docenti dispongono di **accesso completo** al repository (verifica in *Settings → Collaborators*).
- ☑ I segnaposto `[email]` ed `[@handle]` nel template del README (Appendice C) sono stati sostituiti con i tuoi dati reali.

### 8.4 Rischi specifici della milestone

| Rischio | Mitigazione |
|---|---|
| Finestra di soli 4 giorni | Priorità assoluta a M1-01, M1-02, M1-03, M1-09: sono gli unici task valutati. Lo scaffolding può slittare allo Sprint 1 senza conseguenze sulla valutazione. |
| Invito ai docenti non accettato per tempo | Inviare gli inviti il **primo giorno** e verificare lo stato prima del tag; documentare la data di invio nel README. |

---

## 9. Milestone 2 — Nucleo funzionante end-to-end (12/10/2026)

> **Settimana 6 del semestre. 3 settimane, Sprint 1–3.**

### 9.1 Obiettivo

Costruire l'intera impalcatura architetturale e raggiungere una **slice verticale dimostrabile**: l'utente configura la propria chiave, apre una nuova conversazione, invia un messaggio, riceve la risposta del mentore, chiude l'app, attiva la modalità aereo e rilegge la conversazione. Tutti i vincoli tecnici della sezione 3 devono essere in vigore già a questo stadio, perché correggere a posteriori una scelta di dependency injection o di serializzazione ha un costo sproporzionato.

Attenzione particolare al fatto che la Milestone 2 richiede esplicitamente, nel video, la **descrizione della strategia di verifica adottata**: i task del workstream F non sono opzionali né rinviabili.

### 9.1.1 Decisioni M2 concordate — 09/10/2026

Queste decisioni correggono le ambiguità del piano iniziale. Definiscono il comportamento da implementare in M2; non attestano il completamento dei task.

1. **Cronologia completa.** Ogni richiesta include tutti gli scambi completati e il messaggio utente corrente, ordinati per `seq`. Nessun turno vecchio viene eliminato per risparmiare token. Dopo N scambi completati, la richiesta successiva contiene `2N + 1` elementi; i ruoli iniziano con `user`, si alternano e terminano con `user`.
2. **Schema Room iniziale.** La versione 1 contiene `chats` e `messages`, con `seq` e categoria di errore nullable già presenti. La coppia `(chatId, seq)` è unica; assegnazione del progressivo e inserimento sono atomici. `imagePath` è escluso dallo schema e dal dominio M2. La successiva evoluzione dello schema seguirà i cambiamenti realmente implementati, senza una migrazione dedicata all'aggiunta di `seq`.
3. **Un invio per chat.** Il pulsante di invio resta disabilitato durante la richiesta. Il repository applica lo stesso vincolo e rifiuta nuovi turni finché esiste un messaggio `SENDING` o `FAILED` irrisolto nella chat. Nessuna richiesta parte su una cronologia locale non valida.
4. **Stati e completamento atomico.** `SENDING` indica un messaggio utente in attesa di risposta; `SENT` uno scambio completato; `FAILED` un tentativo senza risposta utilizzabile. Il salvataggio della risposta `MODEL` e il passaggio del messaggio utente a `SENT` avvengono nella stessa transazione. I messaggi del modello persistiti hanno stato `SENT`.
5. **Recupero manuale minimo.** M2 include un'azione *Riprova* per riprendere un invio fallito. Riusa la stessa riga utente, che passa da `FAILED` a `SENDING`, azzera la categoria di errore e ricostruisce l'intera cronologia. Nessun messaggio fallito viene saltato per aggiungere un nuovo turno. La politica di backoff automatico resta fuori dall'implementazione M2.
6. **Prerequisiti dell'invio.** Chiave mancante o rete assente bloccano l'invio; la cronologia resta leggibile e la bozza resta modificabile. Il repository ricontrolla i prerequisiti e verifica la connettività immediatamente prima di ogni chiamata. Una perdita di rete durante la richiesta produce un fallimento persistito.
7. **Errori osservati da Room.** `ApiResult` rimane nel livello dati. HTTP, timeout, IO, JSON malformato, risposta vuota e risposta bloccata sono esiti tipizzati, convertiti in una categoria persistita nel messaggio. La UI traduce questa categoria in un testo comprensibile; nessun corpo grezzo dell'errore e nessuna chiave API vengono persistiti in Room.
8. **Dominio indipendente da Gemini.** `HistoryPayloadBuilder` produce turni di dominio con ruolo e testo. I DTO Gemini e la conversione dei turni appartengono a `data/remote`; il dominio non importa Android, Room, Ktor o DTO del livello dati.
9. **Modello e persona.** Un solo nome di modello centralizzato. Candidato iniziale: `gemini-3.5-flash-lite`, documentato da [Google](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite); disponibilità nel progetto BYOK da verificare prima delle prove reali. Si usa `generateContent` stateless e si ripete `system_instruction` a ogni chiamata.

### 9.1.2 Layout obbligatorio dei package

```text
pt/isel/pdm/droidmentor/
├── DroidMentorApplication.kt
├── MainActivity.kt
├── di/ — DependencyContainer.kt
├── domain/
├── data/
│   ├── local/ — entity/, dao/, converter/, relation/
│   ├── remote/ — dto/, HttpClientFactory.kt, GeminiRemoteDataSource.kt, ApiResult.kt
│   ├── preferences/
│   ├── repository/
│   └── connectivity/
└── ui/
    ├── theme/
    ├── navigation/
    ├── screens/<screen>/
    └── components/
```

> **Verifica M2-03 — 09/10/2026.** Package predisposti; aggiunti quattro file Kotlin per il tema; collegati contenuto iniziale, tema nativo di avvio e preview chiaro/scuro. `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:installDebug --console=plain`: `BUILD SUCCESSFUL in 3s`, lint 0 errori e 59 avvisi, test unitari `NO-SOURCE`. Avvio a freddo e cambio chiaro/scuro di sistema verificati sull’emulatore Android 14 (API 34). Le 12 coppie testo/sfondo di ciascuna palette superano 4,5:1 di contrasto; minimo 6,46:1 in chiaro e 5,49:1 in scuro.

> **Verifica M2-04 — 09/10/2026.** Aggiunti `Chat`, `Message`, `Role`, `MessageStatus`, `MessageErrorCategory` e `ConversationTurn`, con campi in sola lettura e KDoc pubblico. Il dominio non contiene import o annotazioni di framework. Verificati ID locali testuali, date e progressivo `Long`, errori opzionali tipizzati e turni limitati a ruolo e testo. Ogni riga di codice degli 11 file Kotlin dell’app ha ora una spiegazione semplice in inglese; la preferenza è registrata in `AGENTS.md`. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`: `BUILD SUCCESSFUL in 4s`, lint 0 errori e 59 avvisi, test unitari `NO-SOURCE`. Il task aggiunge modelli di dati; validazione della cronologia e transizioni di stato restano task di implementazione separati.

> **Verifica M2-11 — 09/10/2026.** Aggiunti `ChatEntity`, `MessageEntity` e `MessageConverters`: chiavi primarie locali, chiave esterna `CASCADE`, indice su `chatId` e indice unico `(chatId, seq)`, enum tipizzati con errore nullable preservato, nessuna colonna per credenziali o immagini. Superati sette test strumentati SQLite su Android 14 (API 34): lettura e ordinamento dello scambio completato, progressivo Long e errore null, tutte le categorie di errore, cascata isolata alla chat eliminata, rifiuto di progressivi duplicati, stesso progressivo in chat diverse e rifiuto di messaggi senza chat. Database in memoria e DAO minimo esistono solo in `androidTest`; M2-12, M2-13 e la suite completa dei DAO M2-28 restano da fare. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 31s`, 7 test / 0 fallimenti / 0 errori / 0 saltati, lint 0 errori e 59 avvisi, test unitari `NO-SOURCE`.

> **Verifica M2-12 — 09/10/2026.** Aggiunti `ChatDao`, `MessageDao` e `ChatWithLastMessage`, con commenti didattici riga per riga. Verificati cronologia completa ordinata, preview ultimo messaggio nullable, aggiornamenti Flow di chat e messaggi, cambio errore del figlio senza modificare la chat, venti inserimenti concorrenti con progressivo atomico, sequenze indipendenti per chat, fallimento/retry sulla stessa riga, completamento atomico, rifiuto di risposte ripetute, rollback su conflitto dell’ID risposta, rifiuto di ID duplicati e cascata. Il database solo per test espone i DAO dell’app; M2-13 e la verifica completa DAO M2-28 restano da fare. Dopo la rigenerazione di vecchie copie duplicate della build, `./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 17s`, 21 test superati (14 DAO + 7 entità), zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi, test unitari `NO-SOURCE`.

> **Verifica M2-13 — 09/10/2026.** Aggiunti `DroidMentorDatabase` versione 1, accessi ai due DAO e builder persistente con application context, nome stabile del file dell’app e nomi separati per i test. Query sul thread principale e fallback di migrazione distruttiva restano disabilitati. KSP esporta `app/schemas/pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase/1.json`, aggiunto ai file tracciati in Git e agli asset dei test strumentati. I 14 test DAO usano ora il database reale in memoria; altri 5 verificano conservazione su file dopo chiusura/riapertura, errori salvati e retry, tutela dei dati senza migrazione, protezione del main thread e vincoli dello schema esportato. `./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 17s`, 26 test su dispositivo superati (7 entità + 14 DAO + 5 database), zero fallimenti/errori/saltati, lint 0 errori e 55 avvisi, test unitari `NO-SOURCE`. Service locator M2-01 e verifica finale DAO M2-28 restano da fare.

> **Verifica M2-14 — 10/10/2026.** Aggiunti `ApiKeyDataStore` a istanza unica, interfaccia `SettingsRepository`, `DataStoreSettingsRepository` ed esiti tipizzati di lettura/scrittura. Verificati chiave assente come null, persistenza dopo salvataggio/riapertura, trim e sostituzione, rifiuto di input vuoto prima della scrittura, cancellazione selettiva persistita, emissioni Flow dopo save/clear, conservazione del file corrotto e riuso dell’istanza. Sei test unitari coprono errori di lettura/scrittura, eccezioni inattese, cancellazione delle coroutine e testo dei risultati senza credenziali; sei test impostazioni su dispositivo usano valori fittizi generati e file Preferences isolati. Nessun valore è loggato o confrontato con assert che lo stampino. `./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 30s`, 6 test unitari + 32 su dispositivo superati, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Collegamento al service locator M2-01 e schermata Settings M2-19 restano da fare.

> **Verifica M2-05 — 10/10/2026.** Aggiunto `HttpClientFactory` con engine OkHttp gestito dal client, JSON con campi sconosciuti accettati e null omessi, timeout connect 10 s/socket 60 s e base URL Gemini. Gli status HTTP di errore restano leggibili per il mapper futuro; i redirect automatici sono disabilitati. `BuildConfig.DEBUG` abilita `Logging` solo in debug: livello INFO senza header/corpi, logger che scarta il testo originale e scrive un messaggio fisso senza URL o dettagli delle eccezioni. Otto test verificano engine e chiusura, variante di build, URL relativo e JSON, timeout inoltrati al motore, status HTTP preservati, redirect bloccati, privacy dei log su successo/fallimento e assenza del plugin in release. Nessuna chiamata reale a Gemini; solo MockEngine e valori fittizi generati in memoria. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 57s`, 14 test unitari in debug + gli stessi 14 in release + 32 su dispositivo, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Collegamento al service locator M2-01, DTO M2-06 e mappatura tipizzata M2-09 restano da fare.

> **Verifica M2-06 — 10/10/2026.** Aggiunti DTO `@Serializable` per richiesta, contenuto, parte testuale, configurazione opzionale, risposta, candidati, uso token, feedback di blocco e involucro errore. `systemInstruction` e `contents` sono obbligatori; `@SerialName("system_instruction")` mantiene la persona separata dalla cronologia. `ConversationTurn.toContent()` converte esplicitamente i ruoli nel livello dati senza modificare testo o introdurre dipendenze API nel dominio. Campi risposta opzionali e liste vuote rappresentano assenza di testo; motivi di fine/blocco sono stringhe compatibili con valori futuri. I DTO conservano il marker thought; nessun allegato, chiave o sessione nel modello richiesta. `ApiError.toString()` nasconde tutti i dettagli grezzi del server. Dodici nuovi test controllano contratto JSON, ordine e testo dei turni, configurazione omessa, campi richiesta obbligatori, risposte multipart e metadati, campi assenti, blocchi, valori futuri, parti prive di testo, rifiuto di JSON malformato, diagnostica sicura e conversione con il client Ktor reale su MockEngine. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 41s`, 26 test unitari in debug + gli stessi 26 in release + 32 su dispositivo, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Nessuna chiamata reale a Gemini. Validazione cronologia M2-08, data source M2-07, esiti tipizzati M2-09 e persona effettiva M2-10 restano da fare.

> **Verifica M2-08 — 10/10/2026.** Aggiunti `HistoryPayloadBuilder`, `HistoryPayloadResult` e categorie `HistoryValidationError`, senza import nel dominio. Il builder riceve ID chat e cronologia completa salvata, ordina una copia per `seq`, conserva ogni ruolo/testo e restituisce tutti i turni solo dopo la validazione. Rifiuta input vuoto, identità vuote/duplicate o di altre chat, progressivi negativi/duplicati, ruoli non alternati da user, testi vuoti, fallimenti irrisolti, stati incoerenti ed errori residui. Tutti i turni precedenti devono essere SENT; l’ultimo è USER/SENDING senza errore. Il retry deve già riattivare la stessa riga tramite repository/DAO: il builder non modifica messaggi né scarta turni falliti. Progressivi Long distinti non devono essere contigui; nessun ordinamento per timestamp, troncamento o calcolo incrementale con rischio di overflow. Ventidue test coprono anche `2N + 1`, 2.001 turni e testo Unicode lungo, tutti i 27 stati combinati su tre turni, ogni categoria di errore residua, riuso ID/seq nel retry, immutabilità dell’input e chiamate indipendenti. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 31s`, 48 test unitari in debug + gli stessi 48 in release + 32 su dispositivo, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Collegamento al repository M2-15, data source M2-07, errori di rete M2-09 e persona M2-10 restano da fare.

> **Verifica M2-10 — 10/10/2026.** Aggiunto `domain/MentorPersona.kt` con un’unica costante `SYSTEM_INSTRUCTION`, KDoc e commenti didattici in inglese. Il testo definisce un mentore Android senior paziente e pratico, risposte nella lingua dello studente, spiegazioni semplici ed esempi Kotlin piccoli con commenti in inglese. Richiede riferimenti ufficiali Android/Kotlin, rispetto delle versioni esistenti e trasparenza sulle incertezze. Include i vincoli del corso su DI manuale, rete/serializzazione, persistenza, credenziali, Compose/coroutine e protocollo stateless. Nessun nome di modello o credenziale nella costante; dominio ancora senza import. Rieseguita la suite esistente, senza aggiungere test che replichino il testo di una costante. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 27s`, 48 test unitari in debug + gli stessi 48 in release + 32 su dispositivo, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Testo definito e revisionato; invio tramite `system_instruction` a ogni chiamata e verifica delle risposte reali dipendono dal data source M2-07.

> **Verifica M2-09 — 10/10/2026.** Aggiunti `ApiResult`, `GeminiResultMapper` e conversione esaustiva `toMessageErrorCategory()`. Il wrapper riceve una singola operazione Ktor e contiene invio, lettura e decodifica: HTTP 400, 401/403, 429, ogni 5xx, timeout request/connect/socket, IO, errori JSON/conversione e fallimenti inattesi diventano esiti senza corpo grezzo o eccezione. La cancellazione del chiamante viene propagata anche se il motore la segnala come IO. `Retry-After` supporta secondi decimali non negativi e data HTTP RFC 1123, clock iniettato, arrotondamento per eccesso e zero per date passate; valori assenti/invalidi/oltre Long restano null, senza retry automatico. Blocchi prompt/candidato precedono il testo; si usa solo il primo candidato e si uniscono parti testuali non thought senza trim o separatori aggiunti. Testo assente/vuoto è `EmptyResponse`, autore esplicito non model è malformato, motivi di fine sconosciuti sono `Unknown`; STOP/MAX_TOKENS o metadati assenti permettono testo utilizzabile. Diagnostica Success nasconde il valore; gli errori espongono solo categorie e metadati numerici. Venti nuovi test coprono anche tutti i 100 status 5xx, timer Ktor reale, errore nel canale risposta, validazione HTTP che lancia eccezioni, cancellazione, etichette Room e privacy con valori fittizi in memoria. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 21s`, 68 test unitari in debug + gli stessi 68 in release + 32 su dispositivo, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Endpoint/modello e uso del wrapper nel data source M2-07, salvataggio degli esiti nel repository M2-15 e gating di connettività restano da integrare; nessuna chiamata reale a Gemini.

> **Verifica M2-07 — 11/10/2026.** Aggiunto `GeminiRemoteDataSource` con client condiviso, callback `isConnected` obbligatoria e mapper degli esiti iniettati manualmente. Ogni invio usa POST `v1beta/models/{MODEL_NAME}:generateContent`; la singola costante sceglie `gemini-3.5-flash-lite`, riconfermato nella documentazione Google. La chiave resta nel solo header `x-goog-api-key` della chiamata, con trim in memoria e rifiuto di input vuoto o caratteri non sicuri. Richiesta fresca con tutti i turni e persona separata in `system_instruction` a ogni call, default `MentorPersona.SYSTEM_INSTRUCTION`; nessun ID sessione, key o cronologia conservati dal source. Ruoli wire invalidi, testo o persona vuoti bloccano HTTP; il controllo di rete è eseguito immediatamente prima di POST dentro il mapper e offline produce `NetworkUnavailable`. Cancellazione preservata, client mai chiuso dal source. Tredici test simulati verificano endpoint/header/JSON reali, persona ripetuta, chiavi diverse senza accumulo, 501 turni completi, guard locali, gate dinamico ed errori del provider, esiti HTTP/risposta/trasporto, cancellazione, log debug sicuri, trim e riuso client. La prima verifica mirata è stata interrotta mentre KSP attendeva la lettura di cache `dataless`; rigenerati i soli file di build, `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 1m 9s`, 81 test unitari in debug + gli stessi 81 in release + 32 su dispositivo, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Nessuna credenziale reale o chiamata reale a Gemini. Provider Android di rete M2-23, service locator M2-01 e integrazione repository/UI restano da fare; disponibilità e quote del modello nel progetto BYOK da verificare nelle prove reali.

> **Verifica M2-23 — 11/10/2026.** Aggiunti `ConnectivityStatus`, `ConnectivityObserver` e tracker interno dei callback. Si osserva la rete predefinita usata da HTTP con `registerDefaultNetworkCallback`; INTERNET e VALIDATED sono entrambi richiesti. Il Flow freddo registra un callback per collector e lo rimuove alla cancellazione, elimina duplicati e conserva il valore più recente. Snapshot iniziale e callback sono protetti da riordinamenti; eventi di vecchie reti non disconnettono una nuova rete. Il check sospendibile `isConnected()` legge uno snapshot attuale su IO anche senza collector e respinge una rete cambiata durante la lettura. Il gate di `GeminiRemoteDataSource` ora accetta la funzione sospendibile; collegamento reale verificato in un test Android con MockEngine. Dieci test locali coprono capability, passaggi di rete, eventi obsoleti, race dello snapshot e collector indipendenti. Tre test sul dispositivo verificano chiamante main, 110 sottoscrizioni successive con cleanup, offline reale e ripristino: zero HTTP offline e una chiamata simulata dopo riconnessione. I toggle sono limitati all’emulatore e ripristinano Wi-Fi/dati originali. Alias MockEngine esistente aggiunto ai soli test Android. Verifica clean iniziale riuscita in 50 s; dopo la sistemazione della chiusura dello stream shell nei test, `./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 20s`, 91 test unitari per variante già superati nella build clean e ancora validi, tutti i 35 test dispositivo rieseguiti e superati, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Wi-Fi e dati originali confermati ripristinati. Istanza nel service locator M2-01, integrazione repository M2-15 e banner/gating UI M2-24 restano da completare.

> **Verifica M2-15 — 11/10/2026.** Aggiunti contratto `ChatRepository`, implementazione `RoomChatRepository`, `ChatSummary` ed esiti locali `ChatReadResult`/`ChatCommandResult`. La UI riceve solo osservazioni Room e acknowledgement locali: `Stored` conferma il salvataggio dell’esito, senza restituire testo modello o `ApiResult`. Operazioni locali di creazione/rinomina/cancellazione funzionano offline. Mutex per chat rifiuta operazioni concorrenti senza bloccare altre chat; preparazione breve in transazione verifica esistenza, righe irrisolte e cronologia completa, poi inserisce SENDING o riattiva l’ultimo FAILED mantenendo ID/seq/testo/timestamp. Nessuna transazione resta aperta durante HTTP. Chiave e connettività vengono controllate prima della scrittura e rilette/ricontrollate prima del source; cambiamenti dopo la preparazione diventano fallimenti persistiti. Successo atomico con risposta MODEL e utente SENT; errori salvati solo come categorie. Testo contenente la chiave configurata non entra nel nuovo messaggio; risposta che la riecheggia è bloccata. Cancellazione propagata dopo cleanup locale NonCancellable della riga posseduta, ritentabile come FAILED/UNKNOWN; storage indisponibile produce esito locale esplicito e può impedire il cleanup. Flow catch gestisce soltanto errori upstream, conservando gli errori del collector. Ventitré test reali Room + source Ktor/MockEngine coprono osservazione ottimistica/atomica, cronologia intera, dieci categorie API, retry senza duplicati, concorrenza e guard persistito, prerequisiti iniziali e cambiati, target invalidi, cancellazione in due fasi, rollback della risposta, confini credenziali, errori storage, trasparenza Flow e un ciclo con Preferences DataStore reale isolato. Prima build fermata prima della compilazione da execution-history Gradle illeggibile: rigenerata solo quella cache, clean build riuscita in 1m 5s. Verifica finale `./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 39s`, 91 test unitari in debug + gli stessi 91 in release + 58 su dispositivo, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Schema Room v1 invariato; nessuna chiamata reale o credenziale reale. Service locator M2-01 e ViewModel/UI restano da completare.

> **Verifica M2-01 — 11/10/2026.** Aggiunta `DroidMentorApplication : Application`, registrata con `android:name` nel manifest. Espone con `by lazy` sincronizzato database Room, Preferences DataStore dedicato, HttpClient Ktor, ConnectivityObserver, SettingsRepository, GeminiRemoteDataSource e ChatRepository; i costruttori sono collegati manualmente e il check Android sospendibile è fornito a source e repository. Extension `Context.app` risolve l’applicationContext senza trattenere Activity. Nessun framework DI, onCreate con query/request o apertura preventiva del database. Quattro test su dispositivo verificano classe creata dal manifest, context wrapper, 24 accessi concorrenti allo stesso grafo/provider/engine/nome DB e un round-trip locale repository→database condiviso: creata e rimossa solo la propria riga temporanea, nessuna chiave letta/scritta e nessun HTTP. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 37s`, 91 test unitari in debug + gli stessi 91 in release + 62 su dispositivo, zero fallimenti/errori/saltati, lint 0 errori e 59 avvisi. Dopo i test l’APK target non era installato: `:app:installDebug` riuscito in 44 s e avvio Android confermato `Status: ok`, `LaunchState: COLD`, 395 ms. Il grafo possiede le risorse per la durata del processo; i callback di rete restano legati ai collector. Contratto DependencyContainer e factory ViewModel M2-02 restano da implementare.

> **Verifica M2-02 — 11/10/2026.** Aggiunto `DependencyContainer` con i contratti ChatRepository, SettingsRepository e Flow di ConnectivityStatus; `DroidMentorApplication` implementa l’interfaccia riusando gli stessi repository lazy e il Flow freddo dell’observer esistente. `dependencyViewModelFactory<VM>` costruisce `ViewModelProvider.Factory` con `viewModelFactory { initializer { … } }`: risolve il container dall’Application negli extras standard oppure usa un container esplicito per i test. Il costruttore è fornito manualmente dalla schermata, senza framework DI, cache globale di ViewModel o accesso a credenziali. Sei test locali verificano costruzione differita, fake senza Application, riuso nello stesso ViewModelStore e separazione tra proprietari, assenza di dipendenze, tipo non registrato ed errore del costruttore. Due test Android verificano il container reale attraverso APPLICATION_KEY e il rifiuto di un’Application incompatibile. I ViewModel usati sono solo fixture di test; ViewModel/UiState delle schermate M2-22 e navigazione M2-16 restano da implementare. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 37s`, 97 test unitari in debug + gli stessi 97 in release + 64 su dispositivo, zero fallimenti/errori/saltati. Lint: 0 errori e gli stessi 59 avvisi precedenti. Nessuna dipendenza aggiunta, credenziale reale o chiamata reale a Gemini. Reinstallazione debug riuscita in 4 s e avvio a freddo confermato `Status: ok`, `LaunchState: COLD`, 342 ms.

> **Verifica M2-16 — 11/10/2026.** Letta e ispezionata visivamente la Figura 1 dell’[enunciato ufficiale del corso](https://github.com/isel-leic-pdm/2627i/blob/main/assignments/PDM-2627-Assignment.pdf), pagina 2. Aggiunti `DroidMentorRoutes` e `DroidMentorNavHost`, collegato a MainActivity e alle preview. Cinque destinazioni: Title iniziale, ChatHistory, ActiveChat/{chatId}, About, Settings. Callback del grafo per Title→History/Settings/About e History→ActiveChat; i ritorni usano popBackStack verso la voce precedente, senza aggiungere nuove Title. launchSingleTop evita duplicati della destinazione superiore. L’ID locale obbligatorio è codificato con Uri.encode e decodificato dagli argomenti Navigation, senza trasportare messaggi o chiavi; una selezione vuota non cambia destinazione. rememberNavController conserva lo stato salvato della composizione e il back stack. Questo non implementa la sessione DataStore M3-06. Slot Compose consentono ai task delle schermate di aggiungere i propri contenuti ricevendo soltanto eventi e ID, senza controller: in questa verifica i controlli sono fixture di test; l’app mostra ancora etichette provvisorie, senza menu reale, lista Room, editor chiave o chat. Otto test del grafo coprono tutte le rotte e i ritorni Figura 1, Back Android, eventi duplicati, ID riservati, selezioni vuote e ripristino dello stato; un nono test verifica MainActivity reale e assenza del pulsante Back sulla Title iniziale. Prima prova completa fallita in 45 s per cinque interazioni con controlli sovrapposti nelle fixture; corretto il solo layout di test, otto test mirati passati in 23 s. Verifica finale `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 46s`, 97 test unitari in debug + gli stessi 97 in release + 73 su dispositivo, zero fallimenti/errori/saltati. Lint: 0 errori e gli stessi 59 avvisi precedenti. Nessuna nuova dipendenza, chiave o chiamata reale a Gemini. Reinstallazione debug riuscita in 4 s, avvio a freddo confermato `Status: ok`, `LaunchState: COLD`, 356 ms. Schermate M2-17..M2-21 e ViewModel M2-22 restano da implementare.

> **Verifica M2-17 — 12/10/2026.** Menu principale con logo, titolo e tre azioni; layout semplice e scorrevole. Local verification: BUILD SUCCESSFUL in 1m 23s; debug/release build, unit tests, lint and instrumentation APK compilation passed. The initial incremental resource build was interrupted while waiting for generated files; the clean build passed. No emulator/device run in this session, as requested. Device UI checks remain pending; no real Gemini request or credential was used.

> **Verifica M2-18 — 12/10/2026.** Schermata Informazioni con versione Gradle, autore, corso, ISEL e crediti; frame Material condiviso. Local verification: BUILD SUCCESSFUL in 1m 30s; debug build, unit tests, lint and instrumentation APK compilation passed. No emulator/device run in this session, as requested. Device UI checks remain pending; no real Gemini request or credential was used.

> **Verifica M2-22 — 12/10/2026.** Tre ViewModel manuali con StateFlow immutabile, eventi Channel e otto test di stato; Title/About statiche senza ViewModel vuoti. Build/APK compilation passed in 1m 2s. Final local unit/lint verification: BUILD SUCCESSFUL in 34s; eight new ViewModel tests passed. No emulator/device run in this session, as requested. Device UI checks remain pending; no real Gemini request or credential was used.

> **Verifica M2-19 — 12/10/2026.** Form della chiave con password, visibilità solo in memoria, salvataggio/cancellazione DataStore, stato configurato e snackbar tipizzate. Local verification: BUILD SUCCESSFUL in 58s; debug build, unit tests, lint and instrumentation APK compilation passed. No emulator/device run in this session, as requested. Device UI checks remain pending; no real Gemini request or credential was used.

> **Verifica M2-20 — 12/10/2026.** Lista Room con ID stabili, anteprima e data relativa, stato vuoto illustrato e FAB per creare una chat locale. Local verification: BUILD SUCCESSFUL in 58s; debug build, unit tests, lint and instrumentation APK compilation passed. No emulator/device run in this session, as requested. Device UI checks remain pending; no real Gemini request or credential was used.

> **Verifica M2-21 — 12/10/2026.** Chat Room con bubble per ruolo, input multilinea, invio protetto, attesa, retry sulla stessa riga, errori persistiti e gestione tastiera; eliminati i placeholder. Local build/unit/lint/APK verification: BUILD SUCCESSFUL in 58s. After adjustResize manifest configuration, debug build/lint passed in 29s. No emulator/device run in this session, as requested. Device UI checks remain pending; no real Gemini request or credential was used.

> **Verifica M2-24 — 12/10/2026.** Banner offline persistente, invio/retry protetti da rete e chiave, bozze ancora modificabili e tre nuovi test sui prerequisiti. Local verification: BUILD SUCCESSFUL in 1m 6s; debug build, unit tests, lint and instrumentation APK compilation passed. No emulator/device run in this session, as requested. Device UI checks remain pending; no real Gemini request or credential was used.

### 9.2 WBS — Workstream A: Fondazioni e dependency injection manuale

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M2-01 | Service locator | `DroidMentorApplication : Application` che espone, con `by lazy`, il database Room, il DataStore, l'`HttpClient` Ktor e i repository. Extension `val Context.app: DroidMentorApplication`. **Nessuna traccia di Hilt/Dagger (V1).** | M | 🟡 | 4 h | M1-07 | ☑ |
| M2-02 | Factory dei ViewModel | Interfaccia `DependencyContainer` implementata dalla `Application`, così da poterla sostituire con un fake nei test. `ViewModelProvider.Factory` costruite con `viewModelFactory { initializer { … } }`. | M | 🟡 | 3 h | M2-01 | ☑ |
| M2-03 | Struttura a package e design system | Layout obbligatorio della sezione 9.1.2. Tema Material 3, palette, tipografia, spaziature, supporto tema chiaro/scuro. | S | 🟢 | 2 h | M1-06 | ☑ |
| M2-04 | Modello di dominio | `Chat(id, title, createdAt, updatedAt)`, `Message(id, chatId, role, text, seq, status, createdAt, errorCategory?)`, `enum Role { USER, MODEL }`, `enum MessageStatus { SENDING, SENT, FAILED }` e categoria di errore tipizzata. Turni di dominio con ruolo e testo; Kotlin puro, senza dipendenze da Android o DTO Gemini. `imagePath` escluso da M2. | M | 🟢 | 2 h | — | ☑ |

### 9.3 WBS — Workstream B: Rete (Ktor + Kotlinx Serialization)

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M2-05 | Configurazione `HttpClient` | Engine OkHttp; `ContentNegotiation` con `Json { ignoreUnknownKeys = true; explicitNulls = false }`; `HttpTimeout` (connect 10 s, socket 60 s); `DefaultRequest` con base URL `https://generativelanguage.googleapis.com/`; plugin `Logging` **attivo solo in debug** e configurato per non stampare header né corpo contenenti la chiave. | M | 🟡 | 3 h | M2-01 | ☑ |
| M2-06 | DTO Gemini | `@Serializable` per `GenerateContentRequest(systemInstruction, contents, generationConfig)`, `Content(role, parts)`, `Part(text)`, `GenerateContentResponse(candidates, usageMetadata)`, `Candidate(content, finishReason)`, `ApiErrorEnvelope(error)`. Conversione dai turni di dominio nel livello dati. Uso di `@SerialName` per `system_instruction`; allegati esclusi da M2. | M | 🟡 | 4 h | M2-05 | ☑ |
| M2-07 | `GeminiRemoteDataSource` | `suspend fun generateContent(history, systemInstruction, apiKey): ApiResult<String>`. `POST /v1beta/models/{model}:generateContent` con header `x-goog-api-key`. Modello configurabile da costante unica. **Nessun identificativo di sessione (V5).** | M | 🔴 | 5 h | M2-06 | ☑ |
| M2-08 | `HistoryPayloadBuilder` | Componente di dominio puro che produce turni con ruolo e testo a partire da tutti gli scambi completati e dal messaggio utente corrente `SENDING`, ordinati per `seq`. Valida alternanza rigorosa `user`/`model` e ultimo ruolo `user`. Nessun troncamento dei turni vecchi; un `FAILED` irrisolto blocca nuovi turni e viene riattivato sulla stessa riga per il retry. Conversione in DTO Gemini nel livello dati. **Da testare in modo esaustivo.** | M | 🔴 | 5 h | M2-04 | ☑ |
| M2-09 | Mappatura degli errori | `sealed interface ApiResult<out T>` con `Success`, `InvalidApiKey` (401/403), `BadRequest` (400), `RateLimited(retryAfterSeconds)` (429), `ServerError` (5xx), `NetworkUnavailable` (`IOException`), `Timeout`, `MalformedResponse` (`SerializationException`), `EmptyResponse`, `BlockedResponse`, `Unknown`. Nessuna eccezione di rete oltrepassa il data source verso la UI; gli esiti vengono convertiti in categorie di errore persistite in Room (V8). | M | 🟡 | 4 h | M2-07 | ☑ |
| M2-10 | `system_instruction` della persona | Testo unico che definisce il mentore Android senior: tono, ambito di competenza, preferenza per le best practice ufficiali, richiesta di esempi in Kotlin. Centralizzato in `MentorPersona.kt` (V6). | M | 🟢 | 1 h | M2-06 | ☑ |

### 9.4 WBS — Workstream C: Persistenza (Room + DataStore)

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M2-11 | Entità Room | `ChatEntity(id, title, createdAt, updatedAt)` e `MessageEntity(id, chatId, role, text, seq, status, createdAt, errorCategory?)` nello schema versione 1, con `@ForeignKey(onDelete = CASCADE)`, indice su `chatId` e indice unico su `(chatId, seq)`. Converter per ruoli, stati e categorie di errore. `imagePath` escluso da M2. | M | 🟡 | 4 h | M2-04 | ☑ |
| M2-12 | DAO | `ChatDao`: `observeChats(): Flow<List<ChatWithLastMessage>>`, `insert`, `updateTitle`, `deleteById`. `MessageDao`: `observeMessages(chatId): Flow<List<MessageEntity>>`, inserimento con assegnazione atomica di `seq`, aggiornamento di stato/errore, completamento atomico dello scambio e lettura della cronologia. Query di relazione con `@Transaction` + `@Relation`. | M | 🟡 | 4 h | M2-11 | ☑ |
| M2-13 | Database | `DroidMentorDatabase : RoomDatabase` versione 1, export dello schema in `app/schemas` **versionato su Git** (indispensabile per testare le migrazioni successive). | M | 🟢 | 2 h | M2-12 | ☑ |
| M2-14 | `SettingsRepository` | Preferences DataStore dedicato: `apiKey: Flow<String?>`, `saveApiKey`, `clearApiKey`. La chiave non viene mai loggata né inclusa in report di crash (V4). | M | 🟡 | 3 h | M2-01 | ☑ |
| M2-15 | `ChatRepository` offline-first | Ciclo descritto in §4.2: controllo dei prerequisiti e del messaggio irrisolto, inserimento ottimistico, ricontrollo connettività, payload completo, chiamata, persistenza atomica del successo o del fallimento. Un invio per chat; retry manuale sulla stessa riga `FAILED`. La UI osserva **solo** Room per messaggi ed errori di rete (V7). | M | 🔴 | 5 h | M2-09, M2-12, M2-23 | ☑ |

### 9.5 WBS — Workstream D: Interfaccia utente e navigazione

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M2-16 | Grafo di navigazione | `NavHost` con rotte `Title`, `ChatHistory`, `ActiveChat/{chatId}`, `About`, `Settings`, conformi alla Figura 1 dell'enunciato (compresi i percorsi di ritorno). Gestione del back stack. | M | 🟡 | 4 h | M2-03 | ☑ |
| M2-17 | Title Screen | Menu principale: logo/titolo, voci *Conversazioni*, *Impostazioni*, *Informazioni*. Punto di ingresso dell'app quando non esiste una chat attiva. | M | 🟢 | 2 h | M2-16 | ☑ |
| M2-18 | About Screen | Nome dell'applicazione, versione, il tuo nome, corso e istituzione, crediti delle librerie di terze parti. | M | 🟢 | 1,5 h | M2-16 | ☑ |
| M2-19 | Settings Screen | Campo per la chiave API con `PasswordVisualTransformation` e toggle di visibilità, salvataggio, cancellazione, indicatore *chiave configurata / non configurata*, validazione di formato non vuoto, feedback tramite snackbar. | M | 🟡 | 4 h | M2-14, M2-16 | ☑ |
| M2-20 | Chat History Screen | `LazyColumn` alimentata dal `Flow` di Room: titolo, anteprima dell'ultimo messaggio, data relativa. Stato vuoto illustrato. FAB *Nuova conversazione*. | M | 🟡 | 4 h | M2-12, M2-16 | ☑ |
| M2-21 | Active Chat Screen | Lista dei messaggi con bubble differenziate per ruolo, barra di input, invio disabilitato durante una richiesta o finché un messaggio fallito resta irrisolto, azione minima *Riprova*, auto-scroll, indicatore di attesa e gestione di `imePadding`. Testo di errore derivato dalla categoria in Room. | M | 🔴 | 6 h | M2-15, M2-16 | ☑ |
| M2-22 | ViewModel e UiState | Un ViewModel per schermata con stato dinamico e `UiState` immutabile esposto come `StateFlow`; eventi one-shot (snackbar, navigazione) tramite `Channel`/`SharedFlow`. Nessuna logica di business nei Composable. | M | 🟡 | 4 h | M2-02, M2-15 | ☑ |

### 9.6 WBS — Workstream E: Connettività

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M2-23 | `ConnectivityObserver` | Wrapper sulla rete predefinita di `ConnectivityManager` con `registerDefaultNetworkCallback`, `Flow<ConnectivityStatus>`, verifica di INTERNET e VALIDATED e cleanup dei callback. Check attuale sospendibile su IO. Istanza da esporre nel service locator M2-01. | M | 🟡 | 3 h | M2-01 | ☑ |
| M2-24 | Gating offline | Verifica della connettività **prima** di ogni chiamata (V7) e controllo della chiave nel repository. Se offline o senza chiave: invio bloccato con feedback comprensibile; bozza modificabile e cronologia interamente consultabile. Banner offline persistente nella Active Chat. | M | 🟡 | 3 h | M2-21, M2-23 | ☑ |

### 9.7 WBS — Workstream F: Verifica *(richiesta esplicitamente nel video M2)*

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M2-25 | Documento `VERIFICATION.md` | Formalizzazione della strategia descritta nella sezione 12: livelli, strumenti, criteri di uscita, matrice dispositivi. È la fonte da cui si costruisce la parte del video dedicata alla verifica. | M | 🟡 | 2 h | — | ☐ |
| M2-26 | Test di `HistoryPayloadBuilder` | Casi: cronologia vuota, primo messaggio, N scambi completati più nuovo messaggio (`2N + 1`), ordine `seq`, alternanza e ultimo ruolo `user`, cronologia lunga conservata integralmente, rifiuto di cronologia non valida e retry del `FAILED` sulla stessa riga. Nessun identificativo di sessione nel payload. | M | 🟡 | 3 h | M2-08 | ☐ |
| M2-27 | Test di rete con `MockEngine` | Simulazione di 200 con payload valido, 400, 401, 403, 429 con `Retry-After`, 5xx, IO, timeout, JSON malformato, risposta vuota e risposta bloccata. Verifica degli esiti tipizzati e dell’assenza di eccezioni di rete propagate alla UI. | M | 🟡 | 4 h | M2-09 | ☐ |
| M2-28 | Test strumentati dei DAO | Room in-memory: inserimenti e osservazione dei `Flow`, cancellazione a cascata, unicità e monotonia di `seq` anche con inserimenti concorrenti, completamento atomico dello scambio, persistenza della categoria di errore e assenza di duplicati al retry. | M | 🟡 | 3 h | M2-12 | ☐ |
| M2-29 | Continuous Integration | Workflow GitHub Actions su `push` e `pull_request`: `assembleDebug`, `testDebugUnitTest`, `lint`. Badge di stato nel README. | S | 🟡 | 2 h | M1-07 | ☐ |

### 9.8 WBS — Workstream G: Consegna

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M2-30 | Video Milestone 2 | Registrazione **5–7 minuti** secondo la scaletta dell'Appendice D: dimostrazione delle funzionalità implementate, discussione delle decisioni più rilevanti (service locator manuale, stateless, offline-first), **descrizione della strategia di verifica**, stato attuale del progetto. Upload su piattaforma con link stabile e accesso verificato in incognito. | M | 🟡 | 4 h | tutti M2 | ☐ |
| M2-31 | Tag `mentor_2` | Inserimento del link al video nel `README.md`, commit, tag annotato, push. | M | 🟢 | 0,5 h | M2-30 | ☐ |

**Totale stimato Milestone 2:** ~92 ore ideali, tutte da solo — circa 31 ore ideali a settimana sulle 3 settimane (≈43 ore di calendario a settimana con il fattore 1,4×). È il blocco più pesante di tutto il piano; vedi la nota di sostenibilità al §7.

### 9.9 Checklist di accettazione (Milestone 2)

- ☐ Il repository è correttamente taggato **`mentor_2`** e il tag è stato pushato.
- ☐ Il `README.md` contiene il **link al video**, verificato come accessibile da un account esterno.
- ☐ Il video dura **tra 5 e 7 minuti**.
- ☐ Il video **dimostra le funzionalità implementate**.
- ☐ Il video **discute le decisioni più rilevanti**.
- ☐ Il video **descrive la strategia di verifica adottata**.
- ☐ Il video **illustra lo stato attuale del progetto**.

**Verifica interna aggiuntiva (non richiesta dall'enunciato, ma necessaria per non accumulare debito):**

- ☐ Ricerca in tutto il codice di `hilt`, `dagger`, `retrofit`, `gson`, `moshi`: **zero occorrenze**.
- ☐ Ispezione del payload effettivamente inviato: contiene `system_instruction` e l'intero array `contents` con ruoli alternati.
- ☐ Prova manuale con modalità aereo: le conversazioni restano leggibili e il messaggio di indisponibilità è chiaro.

---

## 10. Milestone 3 — Funzionalità avanzate e robustezza (16/11/2026)

> **Settimana 11 del semestre. 5 settimane, Sprint 4–8.**

### 10.1 Obiettivo

Implementare i requisiti a più alta complessità logica — cancellazione delle conversazioni, **riscrittura della conversazione**, ripristino della sessione attiva al lancio — e portare il requisito opzionale delle immagini a uno stato dimostrabile. Questa è la milestone con il maggior rischio di regressione: ogni task marcato 🔴 modifica invarianti già coperte da test esistenti.

### 10.2 WBS — Gestione delle conversazioni

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M3-01 | Cancellazione conversazioni | Gesto di swipe e/o menu contestuale nella Chat History, dialog di conferma, cancellazione con `CASCADE` sui messaggi, snackbar con azione *Annulla* (finestra di 5 s prima della cancellazione effettiva dei file associati). | M | 🟡 | 4 h | M2-12, M2-20 | ☐ |
| M3-02 | Titolazione delle conversazioni | Titolo generato automaticamente dai primi caratteri del primo messaggio utente, con possibilità di rinomina manuale. | S | 🟢 | 2 h | M3-01 | ☐ |
| M3-03 | **Riscrittura della conversazione** | L'utente modifica un proprio messaggio in **qualsiasi posizione**. Transazione Room atomica: aggiornamento del testo, `deleteFromSeq(chatId, seq + 1)` che invalida ed elimina tutti gli scambi successivi, ricostruzione del payload sulla cronologia troncata e reinvio all'API. In caso di fallimento di rete, lo stato precedente al reinvio deve restare coerente. | M | 🔴 | 8 h | M2-15, M3-05 | ☐ |
| M3-04 | UI di riscrittura | Long-press su una bubble utente → menu contestuale (*Modifica*, *Copia*, *Elimina*). In modifica: dialog o barra di input precompilata con avviso esplicito che i messaggi successivi verranno eliminati; conferma richiesta. | M | 🔴 | 5 h | M3-03 | ☐ |
| M3-05 | Ordinamento deterministico | Verifica dell’ordinamento deterministico già introdotto in M2 nello schema versione 1: progressivi monotoni per chat e transazioni coerenti durante la riscrittura. **Non affidarsi ai timestamp.** Nessuna migrazione per aggiungere `seq`, già presente. | M | 🟡 | 3 h | M2-13 | ☐ |

### 10.3 WBS — Gestione dello stato e ciclo di vita

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M3-06 | Ripristino della sessione attiva | `activeChatId` salvato in DataStore all'ingresso nella Active Chat e **cancellato solo** su uscita esplicita (back, chiusura della conversazione). Al lancio, routing condizionale: se presente → Active Chat, altrimenti → Title. Implementa la semantica dell'enunciato ("*un utente è considerato precedentemente impegnato se non ha esplicitamente chiuso o abbandonato la conversazione attiva prima della terminazione dell'applicazione*"). | M | 🔴 | 5 h | M2-14, M2-16 | ☐ |
| M3-07 | Sopravvivenza a process death | `SavedStateHandle` per la bozza di testo non inviata e per la posizione di scroll. Verifica con l'opzione *Non mantenere le attività* e con `adb shell am kill`. | S | 🟡 | 4 h | M2-22 | ☐ |
| M3-08 | Politica di ritentativi | Backoff esponenziale con jitter per 429 e 503, lettura dell'header `Retry-After` quando presente, massimo 3 tentativi, operazione annullabile dall'utente, nessun ritentativo su 400/401/403. | M | 🔴 | 5 h | M2-09 | ☐ |
| M3-09 | Stato per singolo messaggio | Rendering differenziato per `SENDING` (indicatore di attesa), `SENT`, `FAILED` (icona + azione *Riprova* sulla bubble). Il reinvio riusa il payload ricostruito dalla cronologia corrente. | M | 🟡 | 4 h | M3-08 | ☐ |
| M3-10 | Chiave mancante o non valida | Se la chiave è assente o l'API risponde 401/403: messaggio contestuale e azione diretta che porta alla schermata Settings, senza vicoli ciechi. | M | 🟡 | 3 h | M2-19, M2-09 | ☐ |

### 10.4 WBS — Requisito opzionale: immagini nella conversazione

> Requisito **facoltativo ma valorizzato**. Da avviare solo se i task *Must* della milestone sono a buon punto entro lo Sprint 6. Il vincolo V9 è tassativo.

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M3-11 | Selezione dalla galleria | `ActivityResultContracts.PickVisualMedia` (Photo Picker): non richiede permessi di runtime. Copia del file selezionato in `filesDir/images/` con nome univoco. | C | 🟡 | 4 h | M2-21 | ☐ |
| M3-12 | Cattura da fotocamera | `ActivityResultContracts.TakePicture` con `FileProvider` configurato in `file_paths.xml`; gestione del permesso `CAMERA` con relativa spiegazione (*rationale*) e del diniego permanente. | C | 🔴 | 6 h | M3-11 | ☐ |
| M3-13 | Persistenza conforme al vincolo | Colonna `imagePath` (percorso **relativo**, per resistere ai cambi di sandbox) in `messages`. Migrazione dalla versione dello schema effettivamente distribuita, con test; nessuna numerazione anticipata basata sull’aggiunta di `seq`. La codifica Base64 `inline_data` viene prodotta **in memoria** solo nella richiesta e non viene **mai** scritta nel database (V9). | C | 🔴 | 5 h | M3-11, M2-08 | ☐ |
| M3-14 | Rendering nella conversazione | Anteprima nella bubble con Coil, placeholder di caricamento, gestione del caso *file non più presente*, apertura a schermo intero. | C | 🟡 | 4 h | M3-13 | ☐ |
| M3-15 | Ciclo di vita dei file | Cancellazione del file su eliminazione del messaggio o della conversazione; eliminazione su riscrittura che invalida il messaggio; routine di pulizia degli orfani all'avvio; ridimensionamento/compressione prima dell'invio per contenere il payload. | C | 🟡 | 4 h | M3-13, M3-01 | ☐ |

### 10.5 WBS — Qualità e consegna

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| M3-16 | Accessibilità e rifinitura UI | `contentDescription` su tutti gli elementi non testuali, aree tattili ≥ 48 dp, contrasto verificato, tema scuro completo, comportamento corretto in rotazione. | S | 🟡 | 4 h | M2-21 | ☐ |
| M3-17 | Estensione della suite di test | Test della riscrittura (troncamento corretto, payload risultante), dei ritentativi, delle migrazioni Room effettivamente introdotte dopo lo schema M2, test di UI Compose sui flussi principali. | M | 🟡 | 5 h | M3-03, M3-08 | ☐ |
| M3-18 | Video Milestone 3 | Registrazione **5–7 minuti**: dimostrazione delle funzionalità (con enfasi su riscrittura, cancellazione, ripristino sessione, immagini se presenti), discussione delle decisioni rilevanti, stato del progetto. | M | 🟡 | 4 h | tutti M3 | ☐ |
| M3-19 | Tag `mentor_3` | Link al video nel `README.md`, commit, tag annotato, push. | M | 🟢 | 0,5 h | M3-18 | ☐ |

**Totale stimato Milestone 3:** ~83 ore ideali, tutte da solo — circa 17 ore ideali a settimana sulle 5 settimane (≈23 ore di calendario a settimana con il fattore 1,4×).

### 10.6 Checklist di accettazione (Milestone 3)

- ☐ Il repository è correttamente taggato **`mentor_3`** e il tag è stato pushato.
- ☐ Il `README.md` contiene il **link al video**, accessibile dall'esterno.
- ☐ Il video dura **tra 5 e 7 minuti**.
- ☐ Il video **dimostra le funzionalità implementate**.
- ☐ Il video **discute le decisioni più rilevanti**.
- ☐ Il video **illustra lo stato attuale del progetto**.

**Verifica interna aggiuntiva:**

- ☐ La riscrittura di un messaggio **in posizione intermedia** elimina correttamente tutti gli scambi successivi e ne genera di nuovi.
- ☐ Chiudendo l'app da dentro una conversazione e riaprendola, si torna in quella conversazione; uscendo prima con *back*, si torna alla Title.
- ☐ Ispezione del database (App Inspection): la colonna delle immagini contiene **percorsi**, non stringhe Base64.

---

## 11. Final Milestone — Consolidamento e consegna (12/12/2026)

> **~4 settimane, Sprint 9–12. Code freeze 08/12/2026.**

### 11.1 Obiettivo

Nessuna nuova funzionalità dopo lo Sprint 9, salvo il completamento del requisito opzionale. Il periodo è dedicato a integrazione, irrobustimento, rifinitura dell'esperienza d'uso, regressione sistematica e produzione dei materiali di consegna. L'enunciato richiede un'applicazione *completa e stabile, con tutte le schermate accessibili* e con *tutte le funzionalità operative come specificato nelle milestone precedenti*: la stabilità è quindi un criterio di valutazione esplicito, non un contorno.

### 11.2 WBS

| ID | Task | Descrizione operativa | Pri. | Diff. | Stima | Dip. | Stato |
|---|---|---|---|---|---|---|---|
| MF-01 | Chiusura del requisito immagini | Completamento di eventuali task M3-11…M3-15 rimasti aperti, con test dedicati. Se si decide di non includere il requisito, va rimosso ogni codice morto e riferimento nella UI. | C | 🔴 | 8 h | M3-15 | ☐ |
| MF-02 | Hardening della gestione errori | Revisione sistematica di ogni percorso di errore: chiave assente, chiave revocata, quota esaurita, risposta bloccata dai filtri di sicurezza (`finishReason = SAFETY`), risposta vuota, timeout, perdita di rete a metà chiamata. Ogni caso produce un messaggio comprensibile e uno stato coerente in Room. | M | 🟡 | 6 h | M3-08 | ☐ |
| MF-03 | Revisione UX | Audit ispirato a *Don't Make Me Think* di Steve Krug (riferimento suggerito dall'enunciato): riduzione del carico cognitivo, etichette non ambigue, eliminazione dei vicoli ciechi, obiettivi principali raggiungibili in ≤ 3 tap dalla Title. Test di usabilità con due persone che non hanno lavorato al progetto. | S | 🟡 | 5 h | — | ☐ |
| MF-04 | Adattività e orientamento | Comportamento corretto in portrait e landscape su tutte le schermate, gestione della tastiera, `WindowSizeClass` per tablet, nessun troncamento di testo. | S | 🟡 | 5 h | MF-03 | ☐ |
| MF-05 | Performance | Chiavi stabili nelle `LazyColumn`, audit delle ricomposizioni con Layout Inspector, verifica della fluidità su conversazioni con 300+ messaggi, eventuale paginazione. Nessuna operazione di I/O sul main thread (verifica con StrictMode). | S | 🔴 | 5 h | — | ☐ |
| MF-06 | Sicurezza e build di rilascio | Plugin `Logging` di Ktor disattivato in release; nessun log della chiave in alcun ambiente; regole R8/ProGuard con verifica del funzionamento della serializzazione dopo l'offuscamento; esclusione della chiave dal backup automatico (`dataExtractionRules`, `allowBackup`). | M | 🔴 | 5 h | — | ☐ |
| MF-07 | Regressione completa | Esecuzione integrale della matrice di test manuali (Appendice E) su almeno due livelli di API e su un dispositivo fisico. Registrazione degli esiti in una tabella di *test report*. | M | 🟡 | 6 h | MF-02 | ☐ |
| MF-08 | Documentazione finale | `README.md` completo: descrizione, screenshot, istruzioni di build, procedura per ottenere e configurare la propria chiave BYOK, architettura sintetica, elenco delle librerie, la tua identificazione, link ai quattro video. KDoc sui componenti pubblici. | M | 🟡 | 4 h | — | ☐ |
| MF-09 | Build di rilascio | APK firmato generato e installato *ex novo* su dispositivo pulito, per validare il primo avvio senza chiave configurata. Allegato alla release GitHub. | S | 🟡 | 3 h | MF-06 | ☐ |
| MF-10 | Code freeze (08/12) | Da questa data si accettano solo correzioni di bug bloccanti, ciascuna con PR, review e regressione mirata. | M | 🟢 | — | — | ☐ |
| MF-11 | Video finale | Registrazione **5–7 minuti** che mostri chiaramente l'applicazione in funzione: tutte le schermate, il flusso conversazionale completo, riscrittura, cancellazione, comportamento offline, gestione degli errori, immagini se implementate. | M | 🟡 | 5 h | MF-07 | ☐ |
| MF-12 | Consegna `mentor_f` | Link al video nel `README.md`, verifica puntuale di **ogni** criterio di accettazione, commit finale, tag annotato `mentor_f`, push. Conferma visiva su GitHub che il tag è presente sul remoto. | M | 🟢 | 1 h | MF-11 | ☐ |

**Totale stimato Final Milestone:** ~53 ore ideali, tutte da solo — circa 13–15 ore ideali a settimana sulle ~4 settimane, più il margine di riserva dello Sprint 12.

### 11.3 Checklist di accettazione (Final Milestone)

- ☐ L'applicazione è **completa e stabile**, con **tutte le schermate pianificate accessibili** (Title, Chat History, Active Chat, About, Settings).
- ☐ **Tutte le funzionalità operano come specificato** nelle milestone precedenti e sono integrate fra loro.
- ☐ Il **video dimostrativo mostra chiaramente le funzionalità** dell'applicazione e dura **5–7 minuti**.
- ☐ Il repository è correttamente taggato **`mentor_f`** e il tag è stato pushato.
- ☐ Il `README.md` contiene il **link al video** finale.

**Verifica finale dei vincoli (da eseguire prima del tag):**

- ☐ V1 — nessuna dipendenza da Hilt/Dagger in alcun file Gradle o sorgente.
- ☐ V2 — tutte le chiamate HTTP passano da Ktor; la serializzazione è interamente Kotlinx.
- ☐ V3 — tutte le conversazioni risiedono in Room.
- ☐ V4 — la chiave API risiede esclusivamente in DataStore.
- ☐ V5 — ispezione di una richiesta reale: cronologia completa, ruoli alternati, nessun ID di sessione.
- ☐ V6 — `system_instruction` presente in ogni richiesta.
- ☐ V7 — prova in modalità aereo superata.
- ☐ V8 — 429 e 500 simulati e gestiti senza crash.
- ☐ V9 — nessun Base64 nel database.
- ☐ V10 — quattro tag presenti sul remoto, README completo, docenti con accesso.

---

## 12. Strategia di verifica

> Da formalizzare in `VERIFICATION.md` (task M2-25) e **da esporre nel video della Milestone 2**, dove è un deliverable esplicito.

### 12.1 Piramide dei test

| Livello | Quota | Oggetto | Strumenti |
|---|---|---|---|
| **Unitari** | ~70 % | `HistoryPayloadBuilder`, mappatura degli errori, logica di troncamento per riscrittura, backoff, serializzazione dei DTO | JUnit 4, `kotlinx-coroutines-test`, `kotlinx.serialization` |
| **Integrazione** | ~20 % | Data source Ktor con `MockEngine`, DAO con Room in-memory, repository con fake data source, migrazioni Room | `ktor-client-mock`, `androidx.room:room-testing`, `androidx.test` |
| **UI / end-to-end** | ~10 % | Flussi principali: configurazione chiave, invio messaggio, riscrittura, cancellazione, comportamento offline | Compose UI Test, `createAndroidComposeRule` |

### 12.2 Casi di prova prioritari

**Requisito stateless (V5)**

1. Dopo N scambi completati, la richiesta con il nuovo messaggio utente contiene `2N + 1` elementi: ruoli alternati a partire da `user` e ultimo ruolo `user`; tutti gli scambi completati sono inclusi.
2. Nessun campo del payload contiene identificativi di sessione o di conversazione.
3. `system_instruction` presente e valorizzato in ogni richiesta.
4. Dopo una riscrittura in posizione intermedia, il payload riflette la cronologia **troncata**, non quella originale.

**Resilienza di rete (V8)**

| Scenario simulato | Comportamento atteso |
|---|---|
| HTTP 429 con `Retry-After: 30` | Messaggio di limite raggiunto, ritentativo differito, nessun crash |
| HTTP 500 | Messaggio di errore del servizio, azione *Riprova* disponibile |
| HTTP 401 | Invito esplicito a verificare la chiave nelle Impostazioni |
| Timeout | Messaggio di timeout, messaggio marcato `FAILED` |
| JSON malformato | `MalformedResponse` gestito, nessuna eccezione propagata |
| Rete assente | Invio bloccato a monte, banner offline, cronologia consultabile |

**Persistenza (V3, V9)**

1. La cancellazione di una chat elimina in cascata tutti i suoi messaggi.
2. `seq` è strettamente monotono anche con inserimenti concorrenti.
3. Le migrazioni successive allo schema M2, quando necessarie, preservano i dati (test con `MigrationTestHelper`); `seq` è già presente nella versione 1.
4. La colonna delle immagini contiene percorsi; nessun valore Base64 (asserzione automatizzata sulla lunghezza e sul formato).

### 12.3 Matrice di esecuzione manuale

| Configurazione | Dispositivo | API | Quando |
|---|---|---|---|
| Baseline di sviluppo | Emulatore Pixel | Ultima stabile | Ogni sprint |
| API minima | Emulatore | 26 | Fine M2, fine M3, MF-07 |
| Dispositivo fisico | `[il tuo modello]` | `[livello]` | Fine di ogni milestone |
| Tablet / landscape | Emulatore tablet | Recente | MF-04 |

### 12.4 Criteri di uscita

- Suite automatica interamente verde su `main`.
- Zero crash noti e zero *blocker* aperti.
- Tutti i punti della matrice manuale eseguiti e registrati.
- Ogni vincolo della sezione 3 verificato esplicitamente.

---

## 13. Registro dei rischi

| ID | Rischio | Prob. | Impatto | Strategia di mitigazione |
|---|---|---|---|---|
| R1 | Esaurimento della quota gratuita dell'API durante lo sviluppo o, peggio, durante la registrazione del video | Alta | Medio | Chiavi multiple (es. un secondo account Google) con rotazione; uso di `MockEngine` nello sviluppo quotidiano; **prova della demo con quota fresca e registrazione in orario a basso traffico**; sequenza di riserva registrata in anticipo |
| R2 | Modifica o deprecazione dell'endpoint/modello Gemini durante il semestre | Media | Alto | Nome del modello e versione dell'API in **un'unica costante**; DTO tolleranti (`ignoreUnknownKeys = true`); verifica mensile della documentazione ufficiale. **Restare su `generateContent`: l'enunciato vieta esplicitamente le varianti con stato lato server** |
| R3 | Sottostima della riscrittura della conversazione (M3-03) | Alta | Alto | Prototipo della sola logica di troncamento in test unitari **prima** di toccare la UI; test scritti prima dell'implementazione (§2); buffer di uno sprint |
| R4 | Sovrapposizione con esami e altri progetti | Alta | Alto | Stime in ore ideali con fattore 1,4×; code freeze anticipato al 08/12; task *Could* sacrificabili per primi in caso di ritardo |
| R5 | Regressioni introdotte dai task 🔴 della Milestone 3 | Media | Alto | Suite automatica verde come precondizione di merge; CI obbligatoria; nessun merge diretto su `main` |
| R6 | Dimenticare, settimane dopo, perché è stata presa una certa decisione — senza nessuno a cui chiederlo | Media | Medio | Conventional Commits con il *perché* nel corpo, non solo il *cosa*; tenere `AGENTS.md` e le note di questo piano aggiornate quando la decisione viene presa, non dopo |
| R7 | Perdita accidentale della chiave API in un commit | Bassa | Molto alto | `.gitignore` completo, `git-secrets` o scansione locale pre-commit, checklist nella PR template, revoca immediata della chiave in caso di esposizione |
| R8 | Video fuori durata o carente rispetto ai punti richiesti | Media | Medio | Scaletta cronometrata (Appendice D), almeno una prova a vuoto con il cronometro prima della registrazione vera |
| R9 | Malattia o indisponibilità vicino a una scadenza, senza nessun compagno di squadra ad assorbire il carico | Alta | Molto alto | Nessun backup è possibile da solo — l'unica mitigazione reale è il margine già previsto nella timeline (il code freeze dell'08/12 lascia quattro giorni prima di `mentor_f`). Tratta quel margine come riservato a questo rischio, non come tempo extra per nuove funzionalità, e applica la stessa logica prima di ogni tag precedente |
| R10 | Accesso dei docenti al repository non correttamente configurato | Bassa | Molto alto | Verifica esplicita al termine di **ogni** milestone, inserita nelle checklist di accettazione |

---

## 14. Matrice di tracciabilità requisiti → task

Ogni requisito dell'enunciato è associato ai task che lo realizzano e alla milestone in cui viene completato. Questa tabella va usata come strumento di autovalutazione prima di ogni consegna.

| Requisito dell'enunciato | Task | Milestone di completamento |
|---|---|---|
| Client BYOK per LLM con API REST di Gemini (`generateContent`) | M2-05, M2-06, M2-07 | M2 |
| Classe `Application` come service locator; Hilt/Dagger vietati | M2-01, M2-02 | M2 |
| Rete via Ktor Client e Kotlinx Serialization | M2-05, M2-06 | M2 |
| Cronologia delle chat in Room | M2-11, M2-12, M2-13 | M2 |
| Chiave API in DataStore | M2-14, M2-19 | M2 |
| Variante stateless; nessun session ID lato server | M2-07, M2-08, M2-15 | M2 |
| Payload con l'intera cronologia, ruoli `user`/`model` alternati | M2-08, M2-26 | M2 |
| Persona del mentore via `system_instruction` | M2-10 | M2 |
| Esperienza offline-first; conversazioni accessibili senza rete | M2-15, M2-24 | M2 |
| Verifica della connettività prima di ogni chiamata | M2-23, M2-24 | M2 |
| Gestione graziosa degli errori HTTP (429, 500, …) | M2-09, M2-15, M2-21, M2-27; successive estensioni M3-08, MF-02 | M2 per gestione tipizzata e recupero manuale |
| Schermata Title | M2-17 | M2 |
| Schermata Chat History | M2-20 | M2 |
| Schermata Active Chat | M2-21 | M2 |
| Schermata About | M2-18 | M2 |
| Schermata Settings | M2-19 | M2 |
| Navigazione conforme alla Figura 1 | M2-16 | M2 |
| Ripristino automatico dell'ultima conversazione attiva al lancio | M3-06 | M3 |
| Rimozione di conversazioni precedenti | M3-01 | M3 |
| Riscrittura della conversazione, in qualsiasi posizione, con invalidazione degli scambi successivi | M3-03, M3-04, M3-05 | M3 |
| *(Opzionale)* Immagini da galleria o fotocamera | M3-11, M3-12 | M3 |
| *(Opzionale)* Solo URI/percorso in Room, Base64 vietato | M3-13, M3-15 | M3 |
| `README.md` con la tua identificazione | M1-02 | M1 |
| Accesso completo concesso ai docenti | M1-01 | M1 |
| Tag `mentor_1` con piano e timeline | M1-03, M1-09 | M1 |
| Tag `mentor_2` con video 5–7 min (funzionalità, decisioni, verifica, stato) | M2-30, M2-31 | M2 |
| Tag `mentor_3` con video 5–7 min (funzionalità, decisioni, stato) | M3-18, M3-19 | M3 |
| Tag `mentor_f` con applicazione completa e video finale | MF-11, MF-12 | Final |

---

## 15. Appendici

### Appendice A — Struttura del payload stateless

Richiesta a `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`, header `x-goog-api-key: <chiave BYOK>`.

```json
{
  "system_instruction": {
    "parts": [
      { "text": "You are DroidMentor, a senior Android engineering mentor. …" }
    ]
  },
  "contents": [
    { "role": "user",  "parts": [{ "text": "Come gestisco lo stato in Compose?" }] },
    { "role": "model", "parts": [{ "text": "Esponi uno StateFlow dal ViewModel …" }] },
    { "role": "user",  "parts": [{ "text": "E per sopravvivere al process death?" }] }
  ],
  "generationConfig": { "temperature": 0.7 }
}
```

Con allegato immagine (requisito opzionale), la codifica Base64 è costruita **in memoria** a partire dal file su disco e non viene mai persistita:

```json
{
  "role": "user",
  "parts": [
    { "text": "Cosa non va in questo layout?" },
    { "inline_data": { "mime_type": "image/jpeg", "data": "<base64 generato a runtime>" } }
  ]
}
```

**Invarianti da rispettare sempre:**

1. L'array `contents` contiene **l'intera** cronologia rilevante della conversazione, non solo l'ultimo turno.
2. I ruoli si alternano rigorosamente `user` → `model` → `user`, e il primo elemento ha ruolo `user`.
3. Nessun campo trasporta identificativi di sessione o di conversazione lato server.
4. `system_instruction` è ripetuto in **ogni** richiesta, perché la chiamata è per definizione priva di stato.

La risposta utile si trova in `candidates[0].content.parts[0].text`; vanno comunque gestiti i casi in cui `candidates` è vuoto o `finishReason` indica un blocco.

> **Nota.** Google ha introdotto anche interfacce conversazionali con stato lato server. **Non vanno usate:** l'enunciato vieta esplicitamente le sessioni multi-turn server-side e richiede che sia il client a gestire tutto il contesto.

### Appendice B — Schema Room M2 (versione 1)

```
chats
├── id           TEXT     PK
├── title        TEXT     NOT NULL
├── createdAt    INTEGER  NOT NULL
└── updatedAt    INTEGER  NOT NULL

messages
├── id           TEXT     PK
├── chatId       TEXT     NOT NULL  FK → chats.id  ON DELETE CASCADE   [INDEX]
├── seq          INTEGER  NOT NULL   -- ordinamento deterministico per chat
├── role         TEXT     NOT NULL   -- USER | MODEL
├── text         TEXT     NOT NULL
├── status       TEXT     NOT NULL   -- SENDING | SENT | FAILED
├── errorCategory TEXT    NULL       -- stable typed error category
└── createdAt    INTEGER  NOT NULL
```

| Versione | Contenuto | Introdotta in |
|---|---|---|
| 1 | `chats` + `messages`, con `seq`, indice unico `(chatId, seq)` e `errorCategory` nullable; senza `imagePath` | M2-11, M2-13 |

Le versioni successive saranno definite dai reali cambiamenti di schema; `seq` non richiede una futura migrazione.

Ogni migrazione deve essere accompagnata da un test con `MigrationTestHelper` e dall'aggiornamento dello schema esportato in `app/schemas`.

### Appendice C — Struttura del `README.md` di radice

```markdown
# DroidMentor

Client Android BYOK per LLM, sviluppato per il corso di Mobile Devices Programming
(ISEL, semestre invernale 2026/2027).

## Autore

| Nome | Matricola | Email | GitHub |
|---|---|---|---|
| Luca Raona | 55603 | [email] | [@handle] |

Progetto individuale, sviluppato sotto il nome **PastaGPT**.

## Consegne

| Milestone | Tag | Data | Video |
|---|---|---|---|
| 1 — Piano di progetto | `mentor_1` | 21/09/2026 | — (vedi PROJECT_PLAN.md) |
| 2 | `mentor_2` | 12/10/2026 | [link] |
| 3 | `mentor_3` | 16/11/2026 | [link] |
| Finale | `mentor_f` | 12/12/2026 | [link] |

## Documentazione
- [Piano di progetto](PROJECT_PLAN.md)
- [Strategia di verifica](VERIFICATION.md)
- [Linee guida di contribuzione](CONTRIBUTING.md)

## Build e configurazione
1. Requisiti (Android Studio, JDK, minSdk 26)
2. Come ottenere una chiave Gemini da Google AI Studio
3. Inserimento della chiave nella schermata Impostazioni dell'app
   (la chiave NON va inserita nel codice né in file di configurazione)
```

### Appendice D — Scaletta cronometrata dei video (5–7 minuti)

| Tempo | Sezione | Contenuto | Presente in |
|---|---|---|---|
| 0:00–0:30 | Apertura | Il tuo nome, nome del progetto (PastaGPT), obiettivo del video | M2, M3, Finale |
| 0:30–3:00 | Dimostrazione | Uso reale dell'app sul dispositivo: flusso completo, funzionalità nuove rispetto alla milestone precedente, comportamento offline e in errore | M2, M3, Finale |
| 3:00–4:30 | Decisioni rilevanti | Service locator manuale al posto di Hilt, gestione stateless della cronologia, Room come sorgente di verità, gestione degli errori HTTP | M2, M3 |
| 4:30–5:30 | Strategia di verifica | Piramide dei test, `MockEngine`, Room in-memory, CI | **M2 (obbligatorio)** |
| 5:30–6:30 | Stato del progetto | Task completati, in corso, rischi aperti e piano per la milestone successiva | M2, M3 |
| ~6:30–7:00 | Chiusura | Riepilogo e riferimento al repository | Tutti |

**Regole operative:** registrazione dello schermo del dispositivo con audio di commento; prova a vuoto per verificare la durata; chiave API configurata e quota disponibile **prima** di premere *rec*; nessun dato sensibile visibile a schermo; upload su piattaforma con link stabile e verifica dell'accessibilità da un browser in incognito.

### Appendice E — Matrice di test manuali (regressione finale MF-07)

| # | Scenario | Esito atteso | Esito |
|---|---|---|---|
| 1 | Primo avvio senza chiave configurata | Nessun crash; invito esplicito a configurare la chiave in Impostazioni | ☐ |
| 2 | Configurazione, salvataggio e rilettura della chiave dopo riavvio | La chiave persiste; è mascherata a schermo | ☐ |
| 3 | Nuova conversazione e invio del primo messaggio | Risposta del mentore ricevuta e persistita | ☐ |
| 4 | Conversazione multi-turno (≥ 5 scambi) | Il contesto è mantenuto: il modello ricorda i turni precedenti | ☐ |
| 5 | Chiusura e riapertura dell'app da dentro una conversazione | Ritorno automatico alla Active Chat corretta | ☐ |
| 6 | Uscita esplicita con *back* e riapertura | Avvio dalla Title Screen | ☐ |
| 7 | Modalità aereo: lettura delle conversazioni | Tutte leggibili; nessun errore a schermo | ☐ |
| 8 | Modalità aereo: tentativo di invio | Invio bloccato con messaggio chiaro; nessun crash | ☐ |
| 9 | Chiave non valida | Messaggio comprensibile con scorciatoia verso Impostazioni | ☐ |
| 10 | Superamento della quota (429) | Messaggio di limite raggiunto; ritentativo differito | ☐ |
| 11 | Cancellazione di una conversazione | Rimossa dalla lista; messaggi eliminati in cascata; file associati eliminati | ☐ |
| 12 | Riscrittura dell'**ultimo** messaggio | Risposta rigenerata correttamente | ☐ |
| 13 | Riscrittura di un messaggio **intermedio** | Tutti gli scambi successivi eliminati e sostituiti | ☐ |
| 14 | Rotazione dello schermo in ogni schermata | Nessuna perdita di stato né di bozza di input | ☐ |
| 15 | Process death (*Non mantenere le attività*) | Stato e bozza ripristinati | ☐ |
| 16 | Invio di un'immagine dalla galleria *(opzionale)* | Immagine mostrata nella bubble; **percorso** salvato in Room | ☐ |
| 17 | Scatto e invio da fotocamera *(opzionale)* | Come sopra; permesso gestito correttamente, anche in caso di rifiuto | ☐ |
| 18 | Ispezione del database dopo l'invio di immagini | Nessuna stringa Base64 presente | ☐ |
| 19 | Navigazione completa fra tutte e cinque le schermate | Tutti i percorsi e i ritorni funzionanti, back stack coerente | ☐ |
| 20 | Conversazione lunga (300+ messaggi) | Scorrimento fluido; nessun ANR | ☐ |

---

*Documento redatto per la Milestone 1 del progetto pratico di Mobile Devices Programming — ISEL, semestre invernale 2026/2027. Luca Raona — PastaGPT.*
