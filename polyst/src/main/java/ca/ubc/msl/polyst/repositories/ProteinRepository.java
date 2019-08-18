package ca.ubc.msl.polyst.repositories;

import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.ProteinInfo;
import ca.ubc.msl.polyst.model.Taxa;

import java.util.List;

/**
 * Created by mjacobson on 11/12/17.
 */
public interface ProteinRepository {

    Protein getByAccession( Taxa taxa, String accession );

    Object getRawData( Taxa taxa, String accession );

    List<ProteinInfo> allProteinInfo( Taxa taxa );
}
