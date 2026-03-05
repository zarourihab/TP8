package com.example.repository;

import com.example.model.Salle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SalleRepository {

    Salle findById(Long id);

    List<Salle> findAll(int page, int pageSize);

    long count();

    List<Salle> findAvailable(LocalDateTime start, LocalDateTime end);

    List<Salle> search(Map<String, Object> criteres);
}