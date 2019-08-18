package ca.ubc.msl.polyst.controllers;

import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.Taxa;
import ca.ubc.msl.polyst.repositories.ProteinRepository;
import ca.ubc.msl.polyst.settings.TaxaSettings;
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
    private final TaxaSettings taxaSettings;

    @Autowired
    public HomeController( ProteinRepository repository, TaxaSettings taxaSettings ) {
        this.repository = repository;
        this.taxaSettings = taxaSettings;
    }

    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/taxa/{taxaId}")
    public String taxa( @PathVariable int taxaId, Model model ) {
        Taxa taxa = taxaSettings.getTaxa( taxaId );
        model.addAttribute("taxa", taxa );
        return "taxa";
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

    @RequestMapping(value = "/taxa/{taxaId}/proteins/{accession}", method = RequestMethod.GET)
    public String protein( @PathVariable int taxaId, @PathVariable String accession, Model model) {
        Taxa taxa = taxaSettings.getTaxa( taxaId );

        Protein protein = null;
        if ( taxa != null ) {
            protein = repository.getByAccession( taxa, accession );
        }

        model.addAttribute("taxa", taxa );
        model.addAttribute("protein", protein );
        return "protein";
    }
}