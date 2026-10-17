# Milestone 2 — Scaletta del video

**Autore:** Luca Raona, 55603. **Applicazione:** DroidMentor (repository PastaGpt).

Durata prevista: **6 minuti e 30 secondi**, comprese le interazioni. Fai una prova con il cronometro: la registrazione finale deve durare **tra 5 e 7 minuti**. Leggi solo i paragrafi tra virgolette; le indicazioni «Mostra» guidano lo schermo.

## Prima di registrare

- Configura la tua chiave nelle Impostazioni prima di iniziare, poi lascia il campo vuoto e la visibilità disattivata. Non registrare l'inserimento della chiave.
- Verifica rete e quota. Il 14/10/2026 hai confermato due risposte reali consecutive; durante la registrazione usa soltanto risposte effettivamente ricevute.
- Prepara una conversazione dimostrativa, senza dati personali, e tieni aperti README, VERIFICATION.md e la verifica riuscita su GitHub.
- Usa una registrazione dello schermo con microfono. Parla con parole tue e lascia qualche secondo per leggere le schermate.
- Se Gemini non risponde, conserva e descrivi l'errore realmente mostrato. Rifai la prova quando l'accesso funziona; non presentare una risposta simulata come reale.

## 0:00–0:30 — Presentazione

**Mostra:** menu principale dell'app.

> «Ciao, sono Luca Raona, numero studente 55603. Questo è DroidMentor, il mio progetto per il corso di Programmazione dei Dispositivi Mobili a ISEL. L'app permette di parlare con un mentore di sviluppo Android attraverso Gemini, usando la propria chiave API. In questo video presento la seconda milestone: le funzionalità implementate, le principali scelte tecniche e il modo in cui ho verificato il progetto.»

## 0:30–1:10 — Navigazione e impostazioni

**Mostra:** About, ritorno al menu, Settings con campo vuoto, ritorno al menu. Mantieni nascosta ogni credenziale.

> «Dal menu posso aprire le conversazioni, le impostazioni e le informazioni sull'applicazione. Nella schermata About sono presenti versione, autore, corso e librerie utilizzate. Nelle impostazioni posso salvare o cancellare la chiave. La chiave è già configurata per questa dimostrazione: il campo resta vuoto e l'app mostra soltanto se è presente. Il valore viene conservato in Preferences DataStore e non nella cronologia, nel codice o nei log.»

## 1:10–2:20 — Conversazione con due turni

**Mostra:** Conversations, nuova chat, invio del primo messaggio, attesa e risposta. Poi invia la seconda domanda. Se l'attesa è lunga, taglia solo il tempo morto, conservando ordine e contenuto degli scambi.

Primo messaggio suggerito: `Explain StateFlow in an Android ViewModel in three short sentences.`

Secondo messaggio suggerito: `Using your previous explanation, why should the UI collect it with lifecycle awareness? Answer briefly.`

> «Creo una nuova conversazione e invio una domanda su StateFlow. Durante la richiesta l'app mostra l'attesa e impedisce un secondo invio contemporaneo. Quando arriva la risposta, lo scambio viene salvato e visualizzato nella chat. Ora faccio una domanda che richiama la spiegazione precedente. Per ogni richiesta l'app ricostruisce l'intera conversazione salvata, con ruoli user e model alternati. Non usa un identificativo di sessione sul server. In questo modo il contesto della seconda domanda comprende anche il primo scambio.»

## 2:20–3:00 — Cronologia e comportamento offline

**Mostra:** ritorno alla cronologia e riapertura della chat. Disattiva la connessione dell'emulatore o del telefono, attendi il banner offline, scrivi una bozza. Mostra invio disabilitato e messaggi ancora leggibili; ripristina la connessione.

> «La conversazione compare nella cronologia con l'anteprima dell'ultimo messaggio. Posso riaprirla e leggere i messaggi salvati. Ora tolgo la connessione: la cronologia resta disponibile, compare il banner offline e posso ancora scrivere una bozza. L'invio è disabilitato finché manca la rete. Il repository controlla comunque la connettività prima di chiamare Gemini, perché lo stato della rete può cambiare anche dopo l'aggiornamento della schermata.»

## 3:00–4:30 — Decisioni tecniche

**Mostra:** albero dei package e, brevemente, `DroidMentorApplication.kt`, `GenerateContentRequest.kt`, `HistoryPayloadBuilder.kt` e `RoomChatRepository.kt`. Non mostrare file di preferenze o log contenenti dati privati.

> «Ho mantenuto una struttura semplice, separando interfaccia, dominio e dati. Le dipendenze vengono create manualmente nella classe Application, che funziona come service locator. Le factory dei ViewModel ricevono queste dipendenze, senza un framework di dependency injection.
>
> Per la rete uso Ktor Client e per il JSON kotlinx.serialization. La persona del mentore viene inviata nel campo system_instruction a ogni chiamata, separatamente dai messaggi dell'utente. Il builder controlla ordine e alternanza della cronologia e non elimina i turni precedenti.
>
> Room è la sorgente dei messaggi mostrati dalla UI. Il repository salva prima il messaggio in attesa e poi registra l'esito. Il ViewModel osserva i dati salvati, quindi una risposta di rete non viene visualizzata direttamente.
>
> Gli errori HTTP, i timeout e le risposte malformate vengono convertiti in risultati tipizzati. La chat mostra un messaggio comprensibile e permette di riprovare l'ultimo invio fallito. Il retry riusa la stessa riga del messaggio dell'utente, evitando duplicati.»

## 4:30–5:40 — Strategia di verifica

**Mostra:** VERIFICATION.md e [la verifica GitHub riuscita](https://github.com/Stocca05/PastaGpt/actions/runs/38383593411). Per errori e retry mostra i casi documentati nei test; non descriverli come errori del provider osservati dal vivo.

> «Ho verificato il progetto su più livelli. I test unitari controllano la costruzione della cronologia, il JSON, gli errori di rete e lo stato dei ViewModel. Con Ktor MockEngine posso simulare risposte valide, errori come 401 e 429, timeout e JSON malformato senza consumare quota.
>
> I test Android usano Room e preferenze isolate. Controllano persistenza, transazioni, navigazione e interazioni con le schermate, compreso il retry senza duplicati. L'ultima verifica locale ha superato 109 test unitari in debug, gli stessi 109 in release e 80 test Android su API 34. Lint ha riportato zero errori e 59 avvisi ancora presenti.
>
> Anche la verifica su GitHub è passata: compila l'app, esegue i test unitari e lint e compila i test Android. L'esecuzione sul dispositivo è una verifica separata. Ho inoltre provato il menu con caratteri grandi, rotazione e tema scuro.»

## 5:40–6:30 — Stato del progetto

**Mostra:** checklist M2 nel piano; termina sul menu o sulla chat.

> «La parte di implementazione della seconda milestone è completata: ci sono le cinque schermate, la configurazione della chiave, le conversazioni persistenti, l'invio a Gemini, gli errori tipizzati e la lettura offline. I commit sono pubblicati sul ramo della milestone.
>
> Restano limiti espliciti nelle verifiche: non ho ancora eseguito la matrice completa su API 26, API 35 e telefono fisico. Le prove simulate verificano il comportamento dell'app, mentre l'accesso e la quota di Gemini dipendono dalla chiave utilizzata.
>
> Modifica dei messaggi, ripristino della sessione attiva e immagini sono pianificati nelle milestone successive. Per questa consegna aggiungerò il link a questo video nel README e pubblicherò il tag mentor_2 sul commit finale. Il repository contiene il piano e le prove di verifica consultabili.»

## Dopo la registrazione — chiusura M2-30 e M2-31

- [ ] Controlla durata effettiva di 5–7 minuti e audio comprensibile.
- [ ] Riguarda il video completo: nessuna chiave o dato personale deve essere visibile.
- [ ] Verifica che contenga funzionalità, decisioni, strategia di verifica e stato attuale.
- [ ] Caricalo su una piattaforma con link stabile e apri il link senza autenticazione per verificare l'accesso.
- [ ] Inserisci il link reale nella riga Milestone 2 del README e nell'appendice delle consegne dei due piani.
- [ ] Spunta M2-30 nei due piani soltanto dopo aver verificato la registrazione e il link.
- [ ] Esegui il controllo delle modifiche e delle verifiche GitHub, quindi crea il commit di consegna.
- [ ] Crea il tag annotato `mentor_2` sul commit contenente il link e pubblica commit e tag.
- [ ] Verifica su GitHub che il tag punti al commit corretto, contenga il README con il link e sia accessibile ai docenti. Spunta M2-31 nel commit di consegna soltanto quando gli altri prerequisiti sono pronti; la conferma del push viene riportata dopo l'esecuzione.

**Stato al 14/10/2026:** scaletta pronta; due risposte reali consecutive confermate dall'autore in chat. Registrazione, link pubblico e tag ancora da completare. La scaletta non sostituisce il video richiesto.

**Aggiornamento al 17/10/2026:** su richiesta dell’autore, il codice viene consegnato su `main` con tag annotato `mentor_2` prima del video, il cui upload è previsto per il 18/10/2026. Nella chiusura del video sostituisci «pubblicherò il tag mentor_2 sul commit finale» con «il codice della M2 è già pubblicato con il tag mentor_2; aggiungerò il link a questo video nel README». La checklist precedente descrive la sequenza originaria: ora restano da completare video, accessibilità e commit del link. Non occorre ripetere la pubblicazione del tag per caricare il video.
