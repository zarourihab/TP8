TP (Capstone) : Finaliser l'application "Réservation de salle
1. Évaluation globale

Votre base est bien structurée et couvre les entités principales attendues pour une application de réservation de salles :

Utilisateur

Salle

Reservation

Equipement

Les relations métiers essentielles sont présentes :

un Utilisateur peut avoir plusieurs Réservations

une Salle peut avoir plusieurs Réservations

une Salle peut avoir plusieurs Équipements

un Équipement peut appartenir à plusieurs Salles

Sur le plan conceptuel, le modèle est donc cohérent et suffisant pour un premier périmètre fonctionnel.

Cela dit, il y a plusieurs points à corriger ou renforcer pour que le projet soit réellement robuste, performant et exécutable en production.

2. Vérification du modèle de données
Entités présentes
Utilisateur

Correctement défini pour :

identité

email unique

rattachement à des réservations

versioning optimiste

Salle

Correctement défini pour :

capacité

localisation

description

relation avec les réservations

relation many-to-many avec les équipements

Reservation

Correctement défini pour :

plage horaire

statut

motif

lien vers utilisateur

lien vers salle

Equipement

Correctement défini pour :

nom

description

référence

relation inverse avec salle

Conclusion sur le modèle métier
Conclusion

Les relations sont correctes.

Mais il manque un point important : la synchronisation bidirectionnelle via des méthodes utilitaires complètes.

Exemple recommandé :

public void addReservation(Reservation reservation) {
    reservations.add(reservation);
    reservation.setUtilisateur(this);
}

public void removeReservation(Reservation reservation) {
    reservations.remove(reservation);
    reservation.setUtilisateur(null);
}

Et côté Salle :

public void addReservation(Reservation reservation) {
    reservations.add(reservation);
    reservation.setSalle(this);
}

public void removeReservation(Reservation reservation) {
    reservations.remove(reservation);
    reservation.setSalle(null);
}

public void addEquipement(Equipement equipement) {
    equipements.add(equipement);
    equipement.getSalles().add(this);
}

public void removeEquipement(Equipement equipement) {
    equipements.remove(equipement);
    equipement.getSalles().remove(this);
}

Sans ces méthodes, les deux côtés de la relation peuvent devenir incohérents en mémoire.

4. Points manquants ou recommandés dans le modèle
4.1 Contraintes métier manquantes
a) Vérification que dateFin > dateDebut

Vous l’avez bien ajoutée dans le script SQL, mais il faut aussi la mettre au niveau Java.

Exemple :

@AssertTrue(message = "La date de fin doit être après la date de début")
public boolean isDateCoherente() {
    return dateDebut != null && dateFin != null && dateFin.isAfter(dateDebut);
}
b) Prévention du chevauchement des réservations

C’est une règle métier essentielle : une salle ne doit pas être réservée deux fois sur la même plage horaire.

Cette contrainte n’est pas gérable simplement avec une contrainte SQL standard dans MySQL. Elle doit être contrôlée :

dans le service

via une requête de vérification avant insertion

Exemple logique :

SELECT r FROM Reservation r
WHERE r.salle.id = :salleId
AND r.statut <> 'ANNULEE'
AND r.dateDebut < :dateFin
AND r.dateFin > :dateDebut
c) Unicité utile sur Salle

Le champ nom n’est pas unique. Selon votre métier, il serait préférable d’imposer par exemple :

nom unique
ou

(batiment, numero) unique

Exemple :

@Table(
    name = "salles",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"batiment", "numero"})
    }
)
d) Longueurs SQL explicites

Certaines colonnes gagneraient à être bornées :

nom

prenom

telephone

email

departement

reference

5. Cache de second niveau : état et corrections
Ce qui est bien

Vous avez activé dans persistence.xml :

<property name="hibernate.cache.use_second_level_cache" value="true"/>
<property name="hibernate.cache.region.factory_class" value="org.hibernate.cache.ehcache.EhCacheRegionFactory"/>
<property name="hibernate.cache.use_query_cache" value="true"/>
<property name="hibernate.generate_statistics" value="true"/>

L’intention est bonne.

Ce qui manque

@Cacheable seul n’est pas suffisant pour un usage Hibernate optimal du cache de second niveau.

Il faut ajouter les annotations Hibernate sur les entités et collections :

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

Exemple :

@Entity
@Table(name = "utilisateurs")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Utilisateur {
    ...
}

Même chose pour Salle, Reservation, Equipement.


Votre configuration EhCacheRegionFactory correspond à une approche ancienne. Selon la version de Hibernate utilisée, cela peut être :

correct en Hibernate 5 avec le bon module

obsolète en Hibernate 6

Donc :

si vous êtes sur Hibernate 5, votre approche peut convenir

si vous êtes sur Hibernate 6, il vaut mieux basculer vers JCache

6. Analyse du fichier ehcache.xml

Le fichier est globalement cohérent.

Points positifs

cache par entité

cache de collection Salle.equipements

cache des requêtes

UpdateTimestampsCache configuré

Point à vérifier impérativement

Les noms de cache doivent correspondre exactement aux noms des classes utilisées par Hibernate.

Vous avez :

<cache name="com.example.model.Utilisateur" ... />

Cela suppose que vos entités sont bien dans com.example.model.

Même chose pour :

<cache name="com.example.model.Salle.equipements" ... />

Cela ne fonctionnera que si Hibernate génère cette région avec ce nom exact.

7. Analyse de DataInitializer
Ce qui est bien

La classe remplit correctement le rôle de jeu de données réaliste :

10 équipements

20 utilisateurs

15 salles

100 réservations

La répartition par bâtiments et types de salles est pertinente.

Points à améliorer
a) Risque de doublons de réservations sur une même salle

Votre génération est aléatoire et ne vérifie pas les conflits horaires.
Résultat : vous pouvez produire plusieurs réservations qui se chevauchent pour la même salle.

Pour des données réalistes, il faut filtrer avant insertion.

b) Réservations uniquement futures

Vous générez uniquement sur les 90 prochains jours. Pour des tests réalistes, il serait utile d’avoir :

des réservations passées

du jour même

futures

annulées

en attente

c) Emails potentiellement accentués ou sensibles à la casse

Dans certains cas, fabriquer les emails à partir de noms/prénoms peut produire des caractères spéciaux. Pour un jeu de test, mieux vaut normaliser.

8. Analyse du script SQL de migration
Problème majeur : script SQL hybride

Votre script mélange plusieurs syntaxes de SGBD :

Syntaxe plutôt PostgreSQL
ADD COLUMN IF NOT EXISTS
CREATE INDEX IF NOT EXISTS
SUBSTRING(nom FROM '[A-Za-z]+([0-9]+)' FOR 1)
Syntaxe MySQL
DELIMITER //
CREATE PROCEDURE ...
ON DUPLICATE KEY UPDATE
DATE_SUB(CURRENT_DATE(), INTERVAL nb_jours DAY)
Conclusion

Le script n’est pas portable et ne s’exécutera pas correctement tel quel sur un seul moteur sans adaptation.

Vous devez choisir explicitement :

soit MySQL/MariaDB

soit PostgreSQL

et écrire un script dédié.

9. Analyse de DatabaseMigrationTool

Cette classe a une bonne idée générale, mais elle a un défaut bloquant :

Problème majeur

Vous découpez le script avec :

String[] instructions = migrationScript.split(";");

Cela casse :

les procédures stockées

les blocs BEGIN ... END

les scripts utilisant DELIMITER

Donc votre outil ne peut pas exécuter proprement le script actuel.

Recommandation

Pour une vraie migration, utilisez :

Flyway
ou

Liquibase

Ce sont les solutions standard.

Si vous gardez cette classe, limitez-vous à des scripts simples sans procédure stockée ni changement de délimiteur.

10. Analyse de PerformanceReport
Idée générale

Bonne intention :

mesurer temps d’exécution

nombre de requêtes

hits/miss cache

taille des résultats

Problèmes techniques
a) em.find(Object.class, 1L) est incorrect

Ici :

result = em.find(Object.class, 1L);

Object.class n’est pas une entité JPA.
Il faut mettre une vraie entité, par exemple :

result = em.find(Salle.class, 1L);
b) createQuery(..., Object.class) est mal typé

Exemple :

em.createQuery("SELECT s FROM Salle s ORDER BY s.id", Object.class)

Il faut écrire :

em.createQuery("SELECT s FROM Salle s ORDER BY s.id", Salle.class)
c) Fuite potentielle de EntityManager

Vous faites :

Session session = emf.createEntityManager().unwrap(Session.class);

sans fermer l’EntityManager créé.

d) Statistiques récupérées de manière fragile

Mieux vaut récupérer directement les statistiques depuis le SessionFactory, pas via un EntityManager temporaire.

11. Analyse de la classe App
Point positif

La structure menu + services + repositories est claire.

Erreur bloquante de compilation

Cette ligne est corrompue :

import com.examp donner le.model.*; donne rapport

Elle doit être remplacée par :

import com.example.model.*;

Sinon le projet ne compile pas.

Point d’architecture

Vous créez un seul EntityManager partagé pour plusieurs repositories/services et pour toute la durée du programme.
C’est acceptable pour une petite démo console, mais ce n’est pas idéal.

En pratique, on préfère :

un EntityManager par unité de travail

ou gestion par conteneur / transaction manager

12. Rapport de conformité par rapport à votre demande
Conforme

entités principales présentes

relations principales présentes

versioning optimiste présent

configuration initiale du cache présente

jeu de données réaliste présent

script de migration fourni

outil de migration fourni

outil de rapport de performance fourni

Partiellement conforme

cache second niveau : activé mais annotations Hibernate manquantes

migration : fournie mais SQL incohérent entre moteurs

rapport de performance : idée correcte mais code à corriger

jeu de données : réaliste mais sans contrôle des conflits de réservation

Non conforme / à corriger

import cassé dans App

Object.class utilisé à la place d’entités réelles

découpage SQL par ; incompatible avec procédures stockées

absence de validation Java sur dateFin > dateDebut

absence de contrôle anti-chevauchement des réservations

13. Recommandations prioritaires
Priorité 1

Corriger les erreurs bloquantes :

import de App

Object.class dans PerformanceReport

gestion des EntityManager

Priorité 2

Compléter le cache Hibernate :

@Cache

CacheConcurrencyStrategy

cache des collections

Priorité 3

Fiabiliser les règles métier :

validation des dates

contrôle de chevauchement

contrainte d’unicité logique pour les salles

Priorité 4

Refondre la migration :

script SQL dédié à un seul SGBD

idéalement Flyway/Liquibase

14. Verdict final

Oui, votre modèle de données comprend bien les entités essentielles et leurs relations principales.
Sur le plan métier, la structure est bonne.

En revanche, le projet tel qu’il est présenté reste à consolider techniquement avant d’être considéré comme proprement finalisé :

le cache est partiellement configuré

la migration est non fiable telle quelle

le rapport de performance contient des erreurs de typage

certaines règles métier importantes ne sont pas encore imposées


<img width="1920" height="1017" alt="Capture d&#39;écran 2026-03-05 220234" src="https://github.com/user-attachments/assets/93530757-e3c0-4dd2-a24e-0999b4c82f43" />

<img width="1920" height="992" alt="Capture d&#39;écran 2026-03-05 220230" src="https://github.com/user-attachments/assets/1c466146-5738-49ca-af57-9cdb8afe6ede" />

<img width="1920" height="1026" alt="Capture d&#39;écran 2026-03-05 220217" src="https://github.com/user-attachments/assets/813da284-3526-47e9-9846-d7014627ef17" />

<img width="1920" height="1013" alt="Capture d&#39;écran 2026-03-05 220209" src="https://github.com/user-attachments/assets/bc75b9c9-238e-4fd0-94e0-45cca4bd225c" />

<img width="1920" height="1005" alt="Capture d&#39;écran 2026-03-05 220159" src="https://github.com/user-attachments/assets/88cd6a73-c7f5-4053-9ba5-05254e2ea612" />

<img width="1920" height="1000" alt="Capture d&#39;écran 2026-03-05 220156" src="https://github.com/user-attachments/assets/39e6854d-0f6c-4e9f-ab7e-8483afc7e899" />

<img width="1920" height="1017" alt="Capture d&#39;écran 2026-03-05 220147" src="https://github.com/user-attachments/assets/64d82a07-4836-40e9-a2b2-fe99eacd9b73" />

<img width="1920" height="1006" alt="Capture d&#39;écran 2026-03-05 220136" src="https://github.com/user-attachments/assets/b8743331-cc23-4601-b17b-ebc7ef46888e" />
<img width="1920" height="1030" alt="Capture d&#39;écran 2026-03-05 220931" src="https://github.com/user-attachments/assets/d560f80c-28a6-47de-92dd-4eb85c6f5971" />
<img width="1920" height="1026" alt="Capture d&#39;écran 2026-03-05 220903" src="https://github.com/user-attachments/assets/1d452634-6ddd-4b2c-82f8-0426d6540890" />
<img width="1920" height="1013" alt="Capture d&#39;écran 2026-03-05 220852" src="https://github.com/user-attachments/assets/c92ce065-39e8-4024-a8bf-7d3dcd644f63" />
<img width="1920" height="1017" alt="Capture d&#39;écran 2026-03-05 220837" src="https://github.com/user-attachments/assets/0a1d4ea0-649b-4d9c-ae48-361064b619ea" />
