# Smoke test sans dépendances Spring

Avec un JDK 17 ou ultérieur :

```sh
mkdir -p target/planning-smoke
javac --release 17 -d target/planning-smoke \
  src/main/java/com/prdv/rdv/common/domain/DomainException.java \
  src/main/java/com/prdv/rdv/planning/domain/*.java \
  src/main/java/com/prdv/rdv/planning/application/port/input/*.java \
  src/main/java/com/prdv/rdv/planning/application/port/output/*.java \
  src/main/java/com/prdv/rdv/planning/application/service/*.java \
  tools/planning/PlanningSmoke.java
java -cp target/planning-smoke PlanningSmoke
```

Ce contrôle exécute 15 assertions sur le domaine : immutabilité, capacité,
report sans annulation, fermeture, questionnaire, propositions temporaires,
propriété du patient, expiration et acceptation.

Il ne remplace **pas** `mvn verify` : aucun mapping JPA, contexte Spring ou
contrat HTTP n'est validé par ce smoke test.

Dans le sandbox, le domaine et les services applicatifs ont été compilés via
le `javac` portable de `@tracecode/tracejvm` (compilateur Java 23), puis les
classes ont été exécutées avec le runtime Java 25 fourni par `jdk4py`.
Résultat : `Planning smoke: 15 checks passed`. Ce n'est pas une validation du
build cible Java 17 ; celle-ci reste confiée à Maven/CI.
Les dépendances et classes de cette vérification sont restées hors Git.
