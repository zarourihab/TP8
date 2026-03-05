package com.example.service;

import com.example.repository.ReservationRepository;

import javax.persistence.EntityManager;

public class ReservationServiceImpl implements ReservationService {

    private final EntityManager em;
    private final ReservationRepository reservationRepository;

    public ReservationServiceImpl(EntityManager em, ReservationRepository reservationRepository) {

        this.em = em;
        this.reservationRepository = reservationRepository;

    }

}