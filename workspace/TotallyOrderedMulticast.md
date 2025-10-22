
## Static Group
### How do processes constitute a group ?

Chacun se connecte au helper pour rejoindre un groupe,
Donnant son broker name et un port libre

Une fois N arrivés, le helper répond à chacun son ID unique, et une liste des personn   es dans le groupe (ID + broker name + port libre)


Le premier process se connecte à tout les autres (et lui meme), le deuxieme accept le premier et se connect au suivant, etc.



###  Why does the protocol work? When a process P is about to deliver a message at the top of the queue of received messages, with all the necessary acknowledgments, why is it correct to do so? In other words, how can the process P be sure that it will not received any message in the future that would have a smaller timestamp that the one it is about to deliver?

[preuve](total_order.plantuml)


### Process failures


