package com.college.sportsbooking.console;

import com.college.sportsbooking.model.*;
import com.college.sportsbooking.service.BookingService;
import java.time.*;
import java.util.Scanner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.console", havingValue = "true")
public class ConsoleMenu implements CommandLineRunner {

    private final BookingService service;
    private final Scanner sc = new Scanner(System.in);

    public ConsoleMenu(BookingService service) { this.service = service; }

    private String ask(String q) {
        System.out.print(q + ": ");
        return sc.nextLine().trim();
    }

    @Override
    public void run(String... args) {
        while (true) {
            System.out.println("\n===== REAL-TIME SPORTS COURT BOOKING =====");
            System.out.println("1. Register Student");
            System.out.println("2. Add Sports Facility");
            System.out.println("3. View Facilities");
            System.out.println("4. Set Facility Status (AVAILABLE/MAINTENANCE)");
            System.out.println("5. View Available Time Slots");
            System.out.println("6. Book Facility");
            System.out.println("7. Cancel Booking");
            System.out.println("8. My Bookings / History");
            System.out.println("0. Exit Menu");
            String choice = ask("Enter choice");
            try {
                switch (choice) {
                    case "1" -> System.out.println(service.registerStudent(
                            new Student(null, ask("Name"), ask("Email"), ask("Contact"))));
                    case "2" -> System.out.println(service.addFacility(new SportsFacility(null,
                            ask("Facility name"), ask("Sport type"), ask("Location"),
                            LocalTime.parse(ask("Opening time (HH:mm)")),
                            LocalTime.parse(ask("Closing time (HH:mm)")), "AVAILABLE")));
                    case "3" -> service.getFacilities().forEach(System.out::println);
                    case "4" -> System.out.println(service.updateFacilityStatus(
                            Integer.parseInt(ask("Facility id")), ask("Status")));
                    case "5" -> service.getSlots(Integer.parseInt(ask("Facility id")),
                            LocalDate.parse(ask("Date (yyyy-MM-dd)"))).forEach(System.out::println);
                    case "6" -> System.out.println(service.bookFacility(new BookingRequest(
                            Integer.parseInt(ask("Student id")), Integer.parseInt(ask("Facility id")),
                            LocalDate.parse(ask("Date (yyyy-MM-dd)")),
                            LocalTime.parse(ask("Start time (HH:mm)")),
                            LocalTime.parse(ask("End time (HH:mm)")))));
                    case "7" -> System.out.println(service.cancelBooking(Integer.parseInt(ask("Booking id"))));
                    case "8" -> service.getHistory(Integer.parseInt(ask("Student id")), null)
                            .forEach(System.out::println);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}