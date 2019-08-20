package ca.ubc.msl.polyst.settings;

import ca.ubc.msl.polyst.model.Species;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Log4j2
@ConfigurationProperties(prefix = "polyst")
@Getter
@Setter
public class SpeciesSettings {
    private Map<Integer, Species> species = Collections.synchronizedMap( new LinkedHashMap<>() );

    private List<List<Species>> indexGroups = Lists.newArrayList();

    private List<Species> activeSpecies = Lists.newArrayList();

    @PostConstruct
    private void postConstruct() {
        if ( species.values().stream().noneMatch( Species::isActive ) ) {
            log.error( "No active species objects set in application.yml!" );
        }

        // Precompute some stuff
        indexGroups = new ArrayList<>( species.values().stream()
                .filter( Species::isActive )
                .sorted( Comparator.comparing( Species::getIndexGroup ) )
                .collect(Collectors.groupingBy( Species::getIndexGroup, LinkedHashMap::new, Collectors.toList())).values() );
        activeSpecies = species.values().stream().filter( Species::isActive ).collect( Collectors.toList());
    }
    public Species getSpecies( Integer id ) {
        Species res = species.get( id );
        if ( !res.isActive() ) {
            return null;
        }
        return res;
    }
}
