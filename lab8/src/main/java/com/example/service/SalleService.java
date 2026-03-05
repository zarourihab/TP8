package com.example.service;

import com.example.model.Salle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SalleService {
    List<Salle> findAvailableRooms(LocalDateTime start, LocalDateTime end);
    List<Salle> searchRooms(Map<String, Object> criteres);

    List<Salle> getPaginatedRooms(int page, int pageSize);
    long countRooms();
    int getTotalPages(int pageSize);
}