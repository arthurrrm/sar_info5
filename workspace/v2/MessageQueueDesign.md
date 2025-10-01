# Message Queue Design (synchrone)

## Send

Send est bloquant, il renvoie uniquement lorsque l'intégralités du message est envoyé ou qu'une exception est levée.
On leve une exception si le channel est fermé.
On envoie d'abbord la taille du message via le channel (4 bits),
puis le message, le tout dans un boucle qui fini seulement quand `write` confirme avoir tout envoyé.


## Receive

Receive est bloquant, il renvoie uniquement lorsque l'intégralités du message est reçu ou qu'une exception est levée.
On leve une exception si le channel est fermé.
On recoit d'abbord la taille du message via le channel (4 bits),
puis on alloue un tableau de la taille du message,
et on recoit le message, le tout dans un boucle qui fini seulement quand `read` confirme avoir tout recu.


