package ca.ubc.msl.polyst.repositories;

import ca.ubc.msl.polyst.exception.CacheWarmingException;
import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.ProteinInfo;
import ca.ubc.msl.polyst.model.Species;
import java.util.List;

/**
 * Created by mjacobson on 11/12/17.
 */
public interface ProteinRepository {

    Protein getProtein( Species species, String accession );

    Object getRawProteinData( Species species, String accession );

    Long getProteinCount( Species species );

    List<ProteinInfo> getProteinInfo( Species species );

    void warm( Species species ) throws CacheWarmingException;
}
