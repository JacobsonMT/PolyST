package ca.ubc.msl.polyst.settings;

import ca.ubc.msl.polyst.model.Taxa;
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
public class TaxaSettings {
    private Map<Integer, Taxa> taxa = Collections.synchronizedMap( new LinkedHashMap<>() );

    private List<List<Taxa>> indexGroups = Lists.newArrayList();

    private List<Taxa> activeTaxa = Lists.newArrayList();

    @PostConstruct
    private void postConstruct() {
        if ( taxa.values().stream().noneMatch( Taxa::isActive ) ) {
            log.error( "No active taxa objects set in application.yml!" );
        }

        // Precompute some stuff
        indexGroups = new ArrayList<>( taxa.values().stream()
                .filter( Taxa::isActive )
                .sorted( Comparator.comparing( Taxa::getIndexGroup ) )
                .collect(Collectors.groupingBy(Taxa::getIndexGroup, LinkedHashMap::new, Collectors.toList())).values() );
        activeTaxa = taxa.values().stream().filter( Taxa::isActive ).collect( Collectors.toList());
    }
    public Taxa getTaxa( Integer id ) {
        Taxa res = taxa.get( id );
        if ( !res.isActive() ) {
            return null;
        }
        return res;
    }
}
