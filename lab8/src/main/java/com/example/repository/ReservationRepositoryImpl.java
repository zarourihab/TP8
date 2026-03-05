package com.example.repository;

import com.example.model.Reservation;

import javax.persistence.EntityManager;
import java.util.List;

public class ReservationRepositoryImpl implements ReservationRepository {

    private final EntityManager em;

    public ReservationRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Reservation findById(Long id) {
        return em.find(Reservation.class, id);
    }

    @Override
    public List<Reservation> findAll() {
        return em.createQuery("SELECT r FROM Reservation r", Reservation.class)
                .getResultList();
    }

    @Override
    public void save(Reservation reservation) {
        em.getTransaction().begin();
        em.persist(reservation);
        em.getTransaction().commit();
    }

    @Override
    public void delete(Long id) {
        Reservation reservation = em.find(Reservation.class, id);
        if (reservation != null) {
            em.getTransaction().begin();
            em.remove(reservation);
            em.getTransaction().commit();
        }
    }
}