package ca.ubc.msl.polyst.controllers;

import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.Species;
import ca.ubc.msl.polyst.repositories.ProteinRepository;
import ca.ubc.msl.polyst.settings.SpeciesSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by mjacobson on 11/12/17.
 */
@Controller
public class HomeController {

    private final ProteinRepository repository;
    private final SpeciesSettings speciesSettings;

    @Autowired
    public HomeController( ProteinRepository repository, SpeciesSettings speciesSettings ) {
        this.repository = repository;
        this.speciesSettings = speciesSettings;
    }

    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/species/{speciesId}")
    public String species( @PathVariable int speciesId, Model model ) {
        Species species = speciesSettings.getSpecies( speciesId );
        model.addAttribute("species", species );
        return "species";
    }

    @RequestMapping(value = "/help")
    public String about() {
        return "help";
    }

    @RequestMapping(value = "/citing")
    public String citing() {
        return "citing";
    }

    @RequestMapping(value = "/contact")
    public String contact() {
        return "contact";
    }

    @RequestMapping(value = "/humans")
    public String humans() {
        return "humans";
    }

    @RequestMapping(value = "/humans.txt")
    public String humanstxt() {
        return "humans";
    }

    @RequestMapping(value = "/species/{speciesId}/proteins/{accession}", method = RequestMethod.GET)
    public String protein( @PathVariable int speciesId, @PathVariable String accession, Model model) {
        Species species = speciesSettings.getSpecies( speciesId );

        Protein protein = null;
        if ( species != null ) {
            protein = repository.getProtein( species, accession );
        }

        model.addAttribute("species", species );
        model.addAttribute("protein", protein );
        return "protein";
    }
}