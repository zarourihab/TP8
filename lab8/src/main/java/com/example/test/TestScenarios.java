package com.example.test;

import com.example.model.*;
import com.example.service.ReservationService;
import com.example.service.SalleService;
import com.example.util.PaginationResult;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class TestScenarios {

    private final EntityManagerFactory emf;
    private final SalleService salleService;
    private final ReservationService reservationService;

    public TestScenarios(EntityManagerFactory emf, SalleService salleService, ReservationService reservationService) {
        this.emf = emf;
        this.salleService = salleService;
        this.reservationService = reservationService;
    }

    public void runAllTests() {
        System.out.println("\n=== EXÉCUTION DES SCÉNARIOS DE TEST ===\n");
        testRechercheDisponibilite();
        testRechercheMultiCriteres();
        testPagination();
        testOptimisticLocking();
        testCachePerformance();
        System.out.println("\n=== TOUS LES TESTS TERMINÉS ===\n");
    }

    private void testRechercheDisponibilite() {
        System.out.println("\n=== TEST 1: RECHERCHE DE DISPONIBILITÉ ===");

        LocalDateTime demainMatin = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime demainMidi = demainMatin.plusHours(3);

        System.out.println("Recherche de salles disponibles entre " + demainMatin + " et " + demainMidi);
        List<Salle> sallesDisponibles = salleService.findAvailableRooms(demainMatin, demainMidi);

        System.out.println("Nombre de salles disponibles: " + sallesDisponibles.size());
        for (int i = 0; i < Math.min(5, sallesDisponibles.size()); i++) {
            Salle salle = sallesDisponibles.get(i);
            System.out.println("- " + salle.getNom() + " (Capacité: " + salle.getCapacite() + ", Bâtiment: " + salle.getBatiment() + ")");
        }
    }

    private void testRechercheMultiCriteres() {
        System.out.println("\n=== TEST 2: RECHERCHE MULTI-CRITÈRES ===");

        Map<String, Object> criteres1 = new HashMap<>();
        criteres1.put("capaciteMin", 30);
        criteres1.put("equipement", 1L); // dépend de ta DB (ex: écran interactif)
        System.out.println("Recherche: capacité >= 30 + équipement id=1");
        List<Salle> resultat1 = salleService.searchRooms(criteres1);
        System.out.println("Salles trouvées: " + resultat1.size());

        Map<String, Object> criteres2 = new HashMap<>();
        criteres2.put("batiment", "Bâtiment C");
        criteres2.put("etage", 2);
        System.out.println("\nRecherche: Bâtiment C étage 2");
        List<Salle> resultat2 = salleService.searchRooms(criteres2);
        System.out.println("Salles trouvées: " + resultat2.size());

        Map<String, Object> criteres3 = new HashMap<>();
        criteres3.put("capaciteMin", 20);
        criteres3.put("capaciteMax", 50);
        criteres3.put("batiment", "Bâtiment B");
        criteres3.put("equipement", 7L); // dépend de ta DB (ex: WiFi)
        System.out.println("\nRecherche complexe: 20-50, Bâtiment B, équipement id=7");
        List<Salle> resultat3 = salleService.searchRooms(criteres3);
        System.out.println("Salles trouvées: " + resultat3.size());
    }

    private void testPagination() {
        System.out.println("\n=== TEST 3: PAGINATION ===");

        int pageSize = 5;
        int totalPages = salleService.getTotalPages(pageSize);
        System.out.println("Nombre total de pages: " + totalPages);

        for (int page = 1; page <= totalPages; page++) {
            System.out.println("\nPage " + page + ":");
            List<Salle> sallesPage = salleService.getPaginatedRooms(page, pageSize);
            for (Salle salle : sallesPage) {
                System.out.println("- " + salle.getNom() + " (Capacité: " + salle.getCapacite() + ", Bâtiment: " + salle.getBatiment() + ")");
            }
        }

        long totalItems = salleService.countRooms();
        List<Salle> firstPageItems = salleService.getPaginatedRooms(1, pageSize);
        PaginationResult<Salle> paginationResult = new PaginationResult<>(firstPageItems, 1, pageSize, totalItems);

        System.out.println("\nTest PaginationResult:");
        System.out.println("Total items: " + paginationResult.getTotalItems());
        System.out.println("Total pages: " + paginationResult.getTotalPages());
        System.out.println("Has next: " + paginationResult.hasNext());
    }

    private void testOptimisticLocking() {
        System.out.println("\n=== TEST 4: OPTIMISTIC LOCKING ===");

        EntityManager em = emf.createEntityManager();
        Reservation reservation;
        try {
            reservation = em.createQuery(
                            "SELECT r FROM Reservation r WHERE r.statut = :statut",
                            Reservation.class
                    )
                    .setParameter("statut", StatutReservation.CONFIRMEE)
                    .setMaxResults(1)
                    .getSingleResult();

            System.out.println("Réservation sélectionnée: ID=" + reservation.getId());
        } finally {
            em.close();
        }

        final Long reservationId = reservation.getId();
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                latch.await();
                EntityManager em1 = emf.createEntityManager();
                try {
                    em1.getTransaction().begin();
                    Reservation r1 = em1.find(Reservation.class, reservationId);
                    System.out.println("Thread 1: version=" + r1.getVersion());
                    Thread.sleep(800);
                    r1.setMotif("Motif modifié par Thread 1");
                    em1.getTransaction().commit();
                    System.out.println("Thread 1: OK");
                } catch (OptimisticLockException e) {
                    System.out.println("Thread 1: OptimisticLockException");
                    if (em1.getTransaction().isActive()) em1.getTransaction().rollback();
                } finally {
                    em1.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            try {
                latch.await();
                Thread.sleep(100);
                EntityManager em2 = emf.createEntityManager();
                try {
                    em2.getTransaction().begin();
                    Reservation r2 = em2.find(Reservation.class, reservationId);
                    System.out.println("Thread 2: version=" + r2.getVersion());
                    r2.setDateFin(r2.getDateFin().plusHours(1));
                    em2.getTransaction().commit();
                    System.out.println("Thread 2: OK");
                } catch (OptimisticLockException e) {
                    System.out.println("Thread 2: OptimisticLockException");
                    if (em2.getTransaction().isActive()) em2.getTransaction().rollback();
                } finally {
                    em2.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        latch.countDown();
        executor.shutdown();
        try { executor.awaitTermination(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        em = emf.createEntityManager();
        try {
            Reservation finalR = em.find(Reservation.class, reservationId);
            System.out.println("État final: motif=" + finalR.getMotif() + ", dateFin=" + finalR.getDateFin() + ", version=" + finalR.getVersion());
        } finally {
            em.close();
        }
    }

    private void testCachePerformance() {
        System.out.println("\n=== TEST 5: PERFORMANCE DU CACHE ===");

        emf.getCache().evictAll();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            EntityManager em = emf.createEntityManager();
            try {
                Salle s = em.find(Salle.class, (i % 15) + 1L);
                s.getEquipements().size();
            } finally {
                em.close();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("Temps sans cache (après evict): " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            EntityManager em = emf.createEntityManager();
            try {
                Salle s = em.find(Salle.class, (i % 15) + 1L);
                s.getEquipements().size();
            } finally {
                em.close();
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Temps avec cache: " + (end - start) + "ms");
    }
}