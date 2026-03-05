package com.example.repository;

import com.example.model.Reservation;
import java.util.List;

public interface ReservationRepository {

    Reservation findById(Long id);

    List<Reservation> findAll();

    void save(Reservation reservation);

    void delete(Long id);
}