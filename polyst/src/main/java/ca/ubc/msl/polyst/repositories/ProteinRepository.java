package ca.ubc.msl.polyst.repositories;

import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.ProteinInfo;
import ca.ubc.msl.polyst.model.Species;

import java.util.List;

/**
 * Created by mjacobson on 11/12/17.
 */
public interface ProteinRepository {

    Protein getByAccession( Species species, String accession );

    Object getRawData( Species species, String accession );

    List<ProteinInfo> allProteinInfo( Species species );
}
