# Vérification statique (sans JVM)

Ces deux scripts ont été écrits parce que l'environnement de développement du
module 2 ne disposait **ni de JDK ni d'accès à Maven Central** : `mvn compile` et
`mvn test` étaient impossibles à exécuter. Ils ne remplacent pas un compilateur —
ils couvrent les erreurs de câblage les plus coûteuses à trouver à la main.

## `parse.js` — syntaxe

Parse CST de chaque `*.java` avec [`java-parser`](https://www.npmjs.com/package/java-parser).

```bash
cd tools/static-analysis && npm install java-parser
node parse.js ../../src/main/java ../../src/test/java
```

## `xref.py` — cohérence croisée

```bash
python3 tools/static-analysis/xref.py
```

Contrôles effectués (aucune dépendance externe) :

1. `package` déclaré == répertoire du fichier ;
2. tout `import com.prdv.rdv.*` résout (fichier ou type imbriqué) ;
3. **toute interface du dépôt est intégralement implémentée** (nom + arité) —
   93 paires classe→interface, 310 méthodes comparées ;
4. tout `new X(...)` sur un record du dépôt a la bonne **arité** ;
5. toute constante `Enum.CONSTANT` référencée existe ;
6. les requêtes dérivées Spring Data (`findByPropAndProp`) portent sur des
   propriétés réelles de l'entité (héritage `@MappedSuperclass` compris) ;
7. tout `@Convert(converter = …)` pointe sur un convertisseur existant ;
8. les **getters/setters d'entités** appelés par les mappers correspondent à des
   champs réellement déclarés (Lombok génère le reste).

Chaque contrôle a été validé par injection d'une faute volontaire (méthode
d'interface supprimée, setter fantôme, constructeur de record à la mauvaise
arité) : le script doit alors signaler exactement la faute, puis plus rien une
fois le code restauré.

## Limites

Ce n'est **pas** un contrôle de types : signatures incompatibles entre deux
types existants, conversions implicites, annotations mal appliquées, requêtes
JPQL invalides ou mapping JPA incorrect ne sont pas détectés. Dès qu'un JDK est
disponible, `mvn -q test` reste la référence.
