package com.bloomtech.library.services;

import com.bloomtech.library.exceptions.LibraryNotFoundException;
import com.bloomtech.library.exceptions.ResourceExistsException;
import com.bloomtech.library.models.*;
import com.bloomtech.library.models.checkableTypes.Checkable;
import com.bloomtech.library.models.checkableTypes.Media;
import com.bloomtech.library.repositories.CheckableRepository;
import com.bloomtech.library.repositories.LibraryRepository;
import com.bloomtech.library.models.CheckableAmount;
import com.bloomtech.library.views.LibraryAvailableCheckouts;
import com.bloomtech.library.views.OverdueCheckout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LibraryService {

    //TODO: Implement behavior described by the unit tests in tst.com.bloomtech.library.services.LibraryService

    @Autowired
    private LibraryRepository libraryRepository;
    @Autowired
    private CheckableService checkableService;

    public List<Library> getLibraries() {
        return libraryRepository.findAll();
    }

//    public Library getLibraryByName(String name) {
//        Library library = new Library(name);
//        if (libraryRepository.findByName(name).isPresent()) {
//            return library;
//        } else {
//            throw new LibraryNotFoundException("Library not found.");
//        }
//    }

    public Library getLibraryByName(String name) {
        Optional<Library> library = libraryRepository.findByName(name);
        if (library.isEmpty()) {
            library = getLibraries()
                    .stream()
                    .filter(l -> l.getName().equals(name))
                    .findFirst();
            if(library.isEmpty()){
                throw new LibraryNotFoundException(String.format("Library with the name: %s was not found", name));
            }
        }
        return library.get();
    }


    public void save(Library library) {
        List<Library> libraries = libraryRepository.findAll();
        if (libraries.stream().filter(p->p.getName().equals(library.getName())).findFirst().isPresent()) {
            throw new ResourceExistsException("Library with name: " + library.getName() + " already exists!");
        }
        libraryRepository.save(library);
    }
    public CheckableAmount getCheckableAmount(String libraryName, String checkableIsbn) {
//         Fetch library object using library name.
//         Cycle through every checkable amount in the library until we find a match
//         return the amount
//         If Checkable does not exist, return checkable amount as 0.

        Library library = getLibraryByName(libraryName);

        Checkable checkable = checkableService.getByIsbn(checkableIsbn);

        for (CheckableAmount checkableAmount : library.getCheckables()) {
            if (checkableAmount.getCheckable().getIsbn().equals(checkableIsbn)) {
                return checkableAmount;
            }
        }

        return new CheckableAmount(checkable, 0);
    }

    public List<LibraryAvailableCheckouts> getLibrariesWithAvailableCheckout(String isbn) {
//        Given a certain book, fetch all libraries that have that certain book as a pending payment
//        Fetch all the libraries getLibraries()
//        Fetch the checkable object from the checkable service using the isbn
//        Loop through every library
//        For every library, call getCheckableAmount()
//        If the amount is greater than 0, create a new Library available checkout object and add it to available List

        List<Library> libraries = getLibraries();
        List<LibraryAvailableCheckouts> available = new ArrayList<>();
        Checkable checkable = checkableService.getByIsbn(isbn);

        for (Library library : libraries) {
//            int amount = getCheckableAmount(library.getName(), checkable.getIsbn()).getAmount();
            for (CheckableAmount checkableAmount : library.getCheckables()) {
                if (checkableAmount.getCheckable().getIsbn().equals(checkable.getIsbn()) && checkableAmount.getAmount() > 0) {
                    LibraryAvailableCheckouts libraryAvailableCheckouts = new LibraryAvailableCheckouts(checkableAmount.getAmount(), library.getName());
                    available.add(libraryAvailableCheckouts);
                }
            }
        }



        return available;
    }

    public List<OverdueCheckout> getOverdueCheckouts(String libraryName) {
//        For a given library, fetch all checkouts that have a due date before todays date
//        Fetch the specific library with the library name
//        Fetch the library cards from the library object
//        Loop through every library card
//        for every library card, fetch all checkouts
//        Loop through every checkout (Nested Loop)
//        If the due date is before today, create new OverdueCheckout object and add it to the overdue checkouts list
//        Overdue Checkout constructor requires Patron object

        List<OverdueCheckout> overdueCheckouts = new ArrayList<>();
        Library library = getLibraryByName(libraryName);

        Set<LibraryCard> libraryCards = library.getLibraryCards();

        for (LibraryCard libraryCard : libraryCards) {
            for (Checkout checkout : libraryCard.getCheckouts()) {
                if (checkout.getDueDate().isBefore(LocalDateTime.now())) {
                    OverdueCheckout overdueCheckout = new OverdueCheckout(libraryCard.getPatron(), checkout);
                    overdueCheckouts.add(overdueCheckout);
                }
            }
        }

        return overdueCheckouts;
    }
}
