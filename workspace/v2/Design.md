# Design

## RDV

Pour associer un `connect` et un `accept`, chaque broker maintient, par port, un rendez‑vous basé sur une SynchronousQueue.

- Par broker: `rdvs : Map<port, SynchronousQueue<Channel>>`.
- `accept(port)` bloque sur `take()` jusqu’à ce qu’un `connect` publie une extrémité de channel. Il n’y a pas de file d’attente: un seul appariement à la fois.
- `connect(remote, port)` construit les deux extrémités du channel (A↔B) et publie l’extrémité distante dans le RDV du broker ciblé via `offer(...)` avec un timeout borné (5s). Si aucun `accept` n’est présent dans ce délai, `connect` retourne `null`.

Conséquences de ce choix:

- Plusieurs `connect` peuvent viser le même port en parallèle, mais sans buffering: seul celui qui se synchronise effectivement avec un `accept` aboutit; les autres expirent (timeout) si aucun `accept` n’arrive à temps.
- Plusieurs `accept` peuvent également attendre en parallèle; chacun sera servi par un `connect` futur. La SynchronousQueue assure un appariement 1‑pour‑1 sans file interne.

Ainsi, le RDV n’utilise ni sémaphore dédié ni moniteur avec file d’attente: la SynchronousQueue par port réalise la synchronisation directe entre un `connect` et un `accept`.

---

## Double Buffers

Un channel est full-duplex, il doit donc gérer la lecture/écriture dans les deux sens indépendamment.
Pour cela, on utilisera deux buffers circulaires :

* le premier pour les données allant de A vers B,
* le second pour les données allant de B vers A.

Chaque extrémité du channel possède donc un buffer de lecture et un buffer d’écriture.

Les opérations de `read` et `write` sont bloquantes :

* `read` bloque tant qu’aucun octet n’est disponible,
* `write` bloque tant qu’aucune place n’est disponible.

Ces opérations doivent se débloquer si le channel est déconnecté.

---

## Disconnect

Chaque channel doit gérer une déconnexion initiée localement ou à distance.

* Localement, lorsqu’un endpoint appelle `disconnect`, il n’est plus possible d’appeler `read` ou `write` de son côté. Les appels futurs lanceront une erreur.
* À distance, les octets déjà envoyés avant la déconnexion doivent rester lisibles. Une fois épuisés, les lectures signalent la déconnexion.
* Les écritures effectuées après que le remote ait déconnecté ne sont pas garanties : elles peuvent être acceptées mais perdues.

Pour modéliser l’état, chaque endpoint possède deux booléens :

* `localDisconnected` pour sa propre déconnexion,
* `remoteDisconnected` pour celle de l’autre côté.

Ces deux états sont partagés entre les extrémités pour garantir une propagation correcte.

---

## Connect sur autre Broker

Un `connect` peut cibler un broker différent de celui de la tâche locale.

* Tous les brokers créés sont enregistrés dans une table partagée accessible par leur nom.
* Lors d’un `connect`, si le broker distant n’existe pas, l’appel retourne immédiatement `null`.
* Sinon, le `connect` est enregistré dans le rendez-vous du port distant et bloquera jusqu’à ce qu’un `accept` se présente.

Cette table partagée doit être thread-safe afin que plusieurs brokers puissent y accéder simultanément.

---

## Concurrence et sécurité

* Deux endpoints d’un channel peuvent lire et écrire en parallèle (full-duplex).
* En revanche, plusieurs lectures concurrentes sur le même endpoint, ou plusieurs écritures concurrentes sur le même endpoint, ne sont pas sûres et doivent être évitées par les tâches appelantes.
* Un seul `accept` est possible par port et par broker, mais plusieurs `connect` peuvent cibler le même port en parallèle.

