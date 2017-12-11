package ca.ubc.msl.polyst.repositories;

import ca.ubc.msl.polyst.model.Protein;

import java.util.List;

/**
 * Created by mjacobson on 11/12/17.
 */
public interface ProteinRepository {

    Protein getByAccession( String accession);

    List<String> getAllAccessions();
}
