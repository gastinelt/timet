# TimeT

Mod NeoForge pour **Minecraft 1.21.1** qui synchronise l'heure du jeu avec l'**heure réelle** de la machine où tourne le jeu (ou le serveur).

## Commandes

| Commande | Effet |
|---|---|
| `/timet` | Affiche l'état, l'heure réelle, l'heure en jeu et le décalage |
| `/timet on` / `/timet off` | Active ou désactive la synchronisation (hors synchro : cycle Minecraft classique) |
| `/timet set <heure>` | Règle l'heure, par exemple `/timet set 15h21` |
| `/timet sync` (ou `reset`) | Recolle directement à l'heure réelle |
| `/timet status` | Affiche l'état de la synchronisation |

Les commandes sont accessibles à tous les joueurs (niveau de permission 0). La configuration est enregistrée dans `config/timet.json`.

## Compiler

Prérequis : JDK 21.

```bash
./gradlew build
```

Le fichier `.jar` est produit dans `build/libs/`.

## Licence

[MIT](LICENSE)
