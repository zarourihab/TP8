package com.example.util;

import com.example.model.Salle;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PerformanceReport {

    private final EntityManagerFactory emf;
    private final Map<String, TestResult> results = new HashMap<>();

    public PerformanceReport(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void runPerformanceTests() {
        System.out.println("Exécution des tests de performance...");

        testPerformance("Recherche salles dispo", () -> {
            EntityManager em = emf.createEntityManager();
            try {
                LocalDateTime start = LocalDateTime.now().plusDays(1);
                LocalDateTime end = start.plusHours(2);

                return em.createQuery(
                                "SELECT DISTINCT s FROM Salle s WHERE s.id NOT IN " +
                                        "(SELECT r.salle.id FROM Reservation r " +
                                        "WHERE (r.dateDebut <= :end AND r.dateFin >= :start))",
                                Salle.class
                        )
                        .setParameter("start", start)
                        .setParameter("end", end)
                        .getResultList();
            } finally {
                em.close();
            }
        });

        testPerformance("Pagination salles", () -> {
            EntityManager em = emf.createEntityManager();
            try {
                return em.createQuery("SELECT s FROM Salle s ORDER BY s.id", Salle.class)
                        .setFirstResult(0)
                        .setMaxResults(10)
                        .getResultList();
            } finally {
                em.close();
            }
        });

        testPerformance("Accès répété cache", () -> {
            Object result = null;
            for (int i = 0; i < 100; i++) {
                EntityManager em = emf.createEntityManager();
                try {
                    result = em.find(Salle.class, 1L);
                } finally {
                    em.close();
                }
            }
            return result;
        });

        generateReport();
    }

    private void testPerformance(String name, Supplier<?> test) {
        resetStats();
        long start = System.currentTimeMillis();
        Object result = test.get();
        long exec = System.currentTimeMillis() - start;

        Statistics stats = getStats();

        TestResult tr = new TestResult();
        tr.executionTime = exec;
        tr.queryCount = stats.getQueryExecutionCount();
        tr.entityLoadCount = stats.getEntityLoadCount();
        tr.cacheHitCount = stats.getSecondLevelCacheHitCount();
        tr.cacheMissCount = stats.getSecondLevelCacheMissCount();
        tr.resultSize = (result instanceof java.util.Collection) ? ((java.util.Collection<?>) result).size() : (result != null ? 1 : 0);

        results.put(name, tr);
        System.out.println(name + " => " + exec + "ms");
    }

    private Statistics getStats() {
        EntityManager em = emf.createEntityManager();
        try {
            Session s = em.unwrap(Session.class);
            return s.getSessionFactory().getStatistics();
        } finally {
            em.close();
        }
    }

    private void resetStats() {
        Statistics stats = getStats();
        stats.clear();
    }

    private void generateReport() {
        String file = "performance_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";

        try (PrintWriter w = new PrintWriter(new FileWriter(file))) {
            w.println("=== RAPPORT DE PERFORMANCE ===");
            w.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            w.println();

            for (Map.Entry<String, TestResult> e : results.entrySet()) {
                TestResult r = e.getValue();
                w.println("Test: " + e.getKey());
                w.println("Temps: " + r.executionTime + "ms");
                w.println("Queries: " + r.queryCount);
                w.println("Entity loads: " + r.entityLoadCount);
                w.println("Cache hits: " + r.cacheHitCount);
                w.println("Cache miss: " + r.cacheMissCount);
                w.println("Result size: " + r.resultSize);
                long total = r.cacheHitCount + r.cacheMissCount;
                double ratio = total > 0 ? (double) r.cacheHitCount / total : 0;
                w.println("Cache hit ratio: " + String.format("%.2f", ratio * 100) + "%");
                w.println("--------------------------------");
            }

            w.println("\nRecommandations:");
            w.println("- Utiliser JOIN FETCH sur les relations souvent lues.");
            w.println("- Indexer date_debut/date_fin et statut.");
            w.println("- Vérifier les régions Ehcache et TTL selon usage.");

            System.out.println("Rapport généré: " + file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class TestResult {
        long executionTime;
        long queryCount;
        long entityLoadCount;
        long cacheHitCount;
        long cacheMissCount;
        int resultSize;
    }
}