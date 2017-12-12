package ca.ubc.msl.polyst.controllers;

import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.repositories.ProteinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by mjacobson on 11/12/17.
 */
@RestController
public class ProteinController {

    private final ProteinRepository repository;

    @Autowired
    public ProteinController(ProteinRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "/api/proteins/{accession}", method = RequestMethod.GET)
    public Protein allAccessions(@PathVariable String accession) {
        return repository.getByAccession( accession );
    }

    @RequestMapping(value = "/api/accessions", method = RequestMethod.GET)
    public List<String> allAccessions() {
        return repository.getAllAccessions();
    }
}
