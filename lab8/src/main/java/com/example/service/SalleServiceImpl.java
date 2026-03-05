package com.example.service;

import com.example.model.Salle;
import com.example.repository.SalleRepository;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SalleServiceImpl implements SalleService {

    private final EntityManager em;
    private final SalleRepository repo;

    public SalleServiceImpl(EntityManager em, SalleRepository repo) {
        this.em = em;
        this.repo = repo;
    }

    @Override
    public List<Salle> findAvailableRooms(LocalDateTime start, LocalDateTime end) {
        return repo.findAvailable(start, end);
    }

    @Override
    public List<Salle> searchRooms(Map<String, Object> criteres) {
        return repo.search(criteres);
    }

    @Override
    public List<Salle> getPaginatedRooms(int page, int pageSize) {
        return repo.findAll(page, pageSize);
    }

    @Override
    public long countRooms() {
        return repo.count();
    }

    @Override
    public int getTotalPages(int pageSize) {
        long total = repo.count();
        return (int) Math.ceil((double) total / pageSize);
    }
}