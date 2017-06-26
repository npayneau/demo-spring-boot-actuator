# Demo Spring Boot Actuator

Démo de mise en place d'Actuator sur une application Spring Boot.

## Pré-requis

Disposer d'un JDK8

## Compilation

Lancer la commande suivante:

```
mvn clean install
```

## Execution

Lancer la commande suivante:

```
java -jar core/target/core.jar
```

Vous disposez des endpoints suivants pour les tests:

* Endpoint de test: http://localhost:8180/public/hello

Accèder aux endpoints de gestion Actuator de votre application
 * ``` http://localhost:8181/manage/metrics ```
 * ``` http://localhost:8181/manage/env ```
 * ``` http://localhost:8181/manage/autoconfig ```
 * ``` http://localhost:8181/manage/health ```
 * ``` http://localhost:8181/manage/info ```
 * ``` http://localhost:8181/manage/trace ```