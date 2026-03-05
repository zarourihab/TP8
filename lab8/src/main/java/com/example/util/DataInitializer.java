package com.example.util;

import com.example.model.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Random;

public class DataInitializer {

    private final EntityManagerFactory emf;
    private final Random random = new Random();

    public DataInitializer(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void initializeData() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Equipement[] equipements = createEquipements(em);
            Utilisateur[] utilisateurs = createUtilisateurs(em);
            Salle[] salles = createSalles(em, equipements);
            createReservations(em, utilisateurs, salles);

            em.getTransaction().commit();
            System.out.println("Jeu de données initialisé avec succès !");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private Equipement[] createEquipements(EntityManager em) {
        System.out.println("Création des équipements...");
        Equipement[] eq = new Equipement[10];

        eq[0] = new Equipement("Projecteur HD", "Projecteur haute définition 4K"); eq[0].setReference("PROJ-4K-001");
        eq[1] = new Equipement("Écran interactif", "Écran tactile 65 pouces"); eq[1].setReference("ECRAN-T-65");
        eq[2] = new Equipement("Système de visioconférence", "Système complet avec caméra HD"); eq[2].setReference("VISIO-HD-100");
        eq[3] = new Equipement("Tableau blanc", "Tableau blanc magnétique 2m x 1m"); eq[3].setReference("TB-MAG-2X1");
        eq[4] = new Equipement("Système audio", "Système audio avec 4 haut-parleurs"); eq[4].setReference("AUDIO-4HP");
        eq[5] = new Equipement("Microphones sans fil", "Set de 4 microphones sans fil"); eq[5].setReference("MIC-SF-4");
        eq[6] = new Equipement("Ordinateur fixe", "PC Windows 11 + Office"); eq[6].setReference("PC-W11-OFF");
        eq[7] = new Equipement("Connexion WiFi haut débit", "WiFi 6 jusqu'à 1 Gbps"); eq[7].setReference("WIFI-6-1G");
        eq[8] = new Equipement("Système de climatisation", "Climatisation réglable"); eq[8].setReference("CLIM-REG");
        eq[9] = new Equipement("Prises électriques multiples", "10 prises réparties"); eq[9].setReference("PRISES-10");

        for (Equipement e : eq) em.persist(e);
        return eq;
    }

    private Utilisateur[] createUtilisateurs(EntityManager em) {
        System.out.println("Création des utilisateurs...");
        Utilisateur[] users = new Utilisateur[20];

        String[] noms = {"Martin","Bernard","Dubois","Thomas","Robert","Richard","Petit","Durand","Leroy","Moreau",
                "Simon","Laurent","Lefebvre","Michel","Garcia","David","Bertrand","Roux","Vincent","Fournier"};

        String[] prenoms = {"Jean","Marie","Pierre","Sophie","Thomas","Catherine","Nicolas","Isabelle","Philippe","Nathalie",
                "Michel","Françoise","Patrick","Monique","René","Sylvie","Louis","Anne","Daniel","Christine"};

        String[] deps = {"RH","Informatique","Finance","Marketing","Commercial",
                "Production","R&D","Juridique","Communication","Direction"};

        for (int i = 0; i < 20; i++) {
            users[i] = new Utilisateur(noms[i], prenoms[i], prenoms[i].toLowerCase() + "." + noms[i].toLowerCase() + "@example.com");
            users[i].setTelephone("06" + (10000000 + random.nextInt(90000000)));
            users[i].setDepartement(deps[i % deps.length]);
            em.persist(users[i]);
        }
        return users;
    }

    private Salle[] createSalles(EntityManager em, Equipement[] equipements) {
        System.out.println("Création des salles...");
        Salle[] salles = new Salle[15];

        // Bâtiment A
        for (int i = 0; i < 5; i++) {
            salles[i] = new Salle("Salle A" + (i + 1), 10 + i * 2);
            salles[i].setDescription("Salle de réunion standard");
            salles[i].setBatiment("Bâtiment A");
            salles[i].setEtage(i % 3 + 1);
            salles[i].setNumero("A" + (i + 1));

            salles[i].addEquipement(equipements[3]);
            salles[i].addEquipement(equipements[7]);
            salles[i].addEquipement(equipements[9]);

            if (i % 2 == 0) salles[i].addEquipement(equipements[0]);
            if (i % 3 == 0) salles[i].addEquipement(equipements[4]);

            em.persist(salles[i]);
        }

        // Bâtiment B
        for (int i = 5; i < 10; i++) {
            salles[i] = new Salle("Salle B" + (i - 4), 20 + (i - 5) * 5);
            salles[i].setDescription("Salle de formation équipée");
            salles[i].setBatiment("Bâtiment B");
            salles[i].setEtage(i % 4 + 1);
            salles[i].setNumero("B" + (i - 4));

            salles[i].addEquipement(equipements[0]);
            salles[i].addEquipement(equipements[3]);
            salles[i].addEquipement(equipements[6]);
            salles[i].addEquipement(equipements[7]);
            salles[i].addEquipement(equipements[9]);
            if (i % 2 == 0) salles[i].addEquipement(equipements[1]);

            em.persist(salles[i]);
        }

        // Bâtiment C
        for (int i = 10; i < 15; i++) {
            salles[i] = new Salle("Salle C" + (i - 9), 50 + (i - 10) * 20);
            salles[i].setDescription("Salle de conférence haut de gamme");
            salles[i].setBatiment("Bâtiment C");
            salles[i].setEtage(i % 3 + 1);
            salles[i].setNumero("C" + (i - 9));

            salles[i].addEquipement(equipements[0]);
            salles[i].addEquipement(equipements[2]);
            salles[i].addEquipement(equipements[4]);
            salles[i].addEquipement(equipements[5]);
            salles[i].addEquipement(equipements[7]);
            salles[i].addEquipement(equipements[8]);
            salles[i].addEquipement(equipements[9]);

            em.persist(salles[i]);
        }

        return salles;
    }

    private void createReservations(EntityManager em, Utilisateur[] utilisateurs, Salle[] salles) {
        System.out.println("Création des réservations...");

        LocalDateTime now = LocalDateTime.now();
        String[] motifs = {"Réunion d'équipe","Entretien","Formation","Présentation client",
                "Brainstorming","Réunion projet","Conférence","Atelier","Séminaire","Réunion direction","Démo produit"};

        for (int i = 0; i < 100; i++) {
            int jourOffset = random.nextInt(90);
            int heureDebut = 8 + random.nextInt(9);
            int duree = 1 + random.nextInt(3);

            LocalDateTime debut = now.plusDays(jourOffset).withHour(heureDebut).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime fin = debut.plusHours(duree);

            Utilisateur u = utilisateurs[random.nextInt(utilisateurs.length)];
            Salle s = salles[random.nextInt(salles.length)];

            Reservation r = new Reservation(debut, fin, motifs[random.nextInt(motifs.length)]);

            int sr = random.nextInt(10);
            if (sr < 8) r.setStatut(StatutReservation.CONFIRMEE);
            else if (sr < 9) r.setStatut(StatutReservation.EN_ATTENTE);
            else r.setStatut(StatutReservation.ANNULEE);

            u.addReservation(r);
            s.addReservation(r);

            em.persist(r);
        }
    }
}