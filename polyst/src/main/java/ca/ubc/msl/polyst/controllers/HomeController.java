package ca.ubc.msl.polyst.controllers;

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

    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/api")
    public String apiDocumentation() {
        return "api";
    }

    @RequestMapping(value = "/help")
    public String about() {
        return "help";
    }

    @RequestMapping(value = "/citing")
    public String citing() {
        return "citing";
    }

    @RequestMapping(value = "/benchmarking")
    public String benchmark() {
        return "benchmarking";
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

    @RequestMapping(value = "/proteins/{accession}", method = RequestMethod.GET)
    public String protein( @PathVariable String accession, Model model) {
        model.addAttribute("accession", accession);
        return "/protein";
    }
}