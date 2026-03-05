package com.example.repository;

import com.example.model.Salle;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.*;

public class SalleRepositoryImpl implements SalleRepository {

    private final EntityManager em;

    public SalleRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Salle findById(Long id) {
        return em.find(Salle.class, id);
    }

    @Override
    public List<Salle> findAll(int page, int pageSize) {
        return em.createQuery("SELECT s FROM Salle s ORDER BY s.id", Salle.class)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    @Override
    public long count() {
        return em.createQuery("SELECT COUNT(s) FROM Salle s", Long.class).getSingleResult();
    }

    @Override
    public List<Salle> findAvailable(LocalDateTime start, LocalDateTime end) {
        return em.createQuery(
                        "SELECT DISTINCT s FROM Salle s " +
                                "WHERE s.id NOT IN (" +
                                "   SELECT r.salle.id FROM Reservation r " +
                                "   WHERE r.statut = com.example.model.StatutReservation.CONFIRMEE " +
                                "   AND (r.dateDebut <= :end AND r.dateFin >= :start)" +
                                ")",
                        Salle.class
                )
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    @Override
    public List<Salle> search(Map<String, Object> criteres) {
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT s FROM Salle s ");
        Map<String, Object> params = new HashMap<>();

        boolean joinEquip = criteres.containsKey("equipement");
        if (joinEquip) {
            jpql.append(" JOIN s.equipements e ");
        }

        jpql.append(" WHERE 1=1 ");

        if (criteres.containsKey("capaciteMin")) {
            jpql.append(" AND s.capacite >= :capMin ");
            params.put("capMin", criteres.get("capaciteMin"));
        }
        if (criteres.containsKey("capaciteMax")) {
            jpql.append(" AND s.capacite <= :capMax ");
            params.put("capMax", criteres.get("capaciteMax"));
        }
        if (criteres.containsKey("batiment")) {
            jpql.append(" AND s.batiment = :bat ");
            params.put("bat", criteres.get("batiment"));
        }
        if (criteres.containsKey("etage")) {
            jpql.append(" AND s.etage = :etg ");
            params.put("etg", criteres.get("etage"));
        }
        if (joinEquip) {
            jpql.append(" AND e.id = :eqId ");
            params.put("eqId", criteres.get("equipement"));
        }

        TypedQuery<Salle> q = em.createQuery(jpql.toString(), Salle.class);
        params.forEach(q::setParameter);
        return q.getResultList();
    }
}