# Data Consistency

Quand un noeud recoit le token:
- entrer dans la section critique
  - si ecriture:
        envoyer le  changement au maillon suivant, jusqu'a revenir à soi même
- sortir section critique
- passer le token au suivant


# Pannes
Chaque noeud connait son suivant et son précedent 


lors d'une panne d'un maillon:
- le noeud qui ne peut pas joindre son suivant demande en broadcast qui a perdu son précedent (& = à son suivant)
- celui qui répond devient son suivant, devient son suivant
  -  (et doit recreer le token si le maillon faible le détenait)
  

- delai lors d'attente d'un token: demande aux autres pour verifier son existence:
  - puis demande à l'ordonanceur si plus de token   




# Fault-tolerant Distributed Mutual Exclusion


## Architecture client–serveur

* On a un serveur principal qui garde l’état de tous les verrous (*mutex*).
* Les clients demandent au serveur :

  * `mutex_enter(data-id)` → pour prendre un verrou
  * `mutex_leave(data-id)` → pour le libérer


---

## Système à état (stateful)

Le serveur garde en mémoire l’état de tous les mutex :

| Ressource | Verrouillée par | File d’attente |
| --------- | --------------- | -------------- |
| fichier1  | clientA         | [clientB]      |
| fichier2  | —               | []             |

Cet état est mis à jour à chaque `enter` ou `leave`.

---

## Gestion des pannes

### Si un client tombe en panne :

* Le détecteur de panne parfait avertit le serveur : “clientA est mort”.
* Le serveur :

  * Libère tous les verrous détenus par clientA.
  * Donne ces verrous au prochain client en attente.

Résultat : pas de blocage, tout continue normalement.

---

### Si le serveur tombe en panne :

* Il existe un serveur de secours (backup) qui garde une copie à jour de l’état.
* Le serveur principal envoie chaque mise à jour au backup (par exemple à chaque verrou ou déverrouillage).
* Si le principal meurt :

  * Le backup devient le nouveau serveur principal.
  * Il reprend directement avec le même état.

Résultat : le service continue sans perdre l’information sur les verrous.



