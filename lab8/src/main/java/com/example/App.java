package com.example;

import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;

import com.example.repository.ReservationRepository;
import com.example.repository.ReservationRepositoryImpl;

import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;
import com.example.service.ReservationService;
import com.example.service.ReservationServiceImpl;

import com.example.test.TestScenarios;
import com.example.util.DataInitializer;
import com.example.util.PerformanceReport;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {

        System.out.println("=== APPLICATION DE RÉSERVATION DE SALLES ===");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-reservations");
        EntityManager em = emf.createEntityManager();

        try {

            SalleRepository salleRepo = new SalleRepositoryImpl(em);
            SalleService salleService = new SalleServiceImpl(em, salleRepo);


            ReservationRepository reservationRepo = new ReservationRepositoryImpl(em);
            ReservationService reservationService = new ReservationServiceImpl(em, reservationRepo);

            Scanner scanner = new Scanner(System.in);
            boolean exit = false;

            while (!exit) {

                System.out.println("\n=== MENU PRINCIPAL ===");
                System.out.println("1. Initialiser les données de test");
                System.out.println("2. Exécuter les scénarios de test");
                System.out.println("3. Générer un rapport de performance");
                System.out.println("4. Quitter");
                System.out.print("Votre choix: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {

                    case 1:
                        new DataInitializer(emf).initializeData();
                        break;

                    case 2:
                        new TestScenarios(emf, salleService, reservationService).runAllTests();
                        break;

                    case 3:
                        new PerformanceReport(emf).runPerformanceTests();
                        break;

                    case 4:
                        exit = true;
                        System.out.println("Au revoir !");
                        break;

                    default:
                        System.out.println("Choix invalide !");
                }
            }

        } finally {

            if (em.isOpen())
                em.close();

            emf.close();
        }
    }
}