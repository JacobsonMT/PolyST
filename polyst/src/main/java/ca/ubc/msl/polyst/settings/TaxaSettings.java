package ca.ubc.msl.polyst.settings;

import ca.ubc.msl.polyst.model.Taxa;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.PropertySource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Log4j2
@ConfigurationProperties(prefix = "polyst")
@Getter
@Setter
public class TaxaSettings {
    private Map<Integer, Taxa> taxa = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        if ( taxa.values().stream().noneMatch( Taxa::isActive ) ) {
            log.error( "No active taxa objects set in application.yml!" );
        }
    }
    public Taxa getTaxa( Integer id) {
        Taxa res = taxa.get( id );
        if ( !res.isActive() ) {
            return null;
        }
        return res;
    }

    public List<Taxa> getActiveTaxa() {
        return taxa.values().stream().filter( Taxa::isActive ).sorted( Comparator.comparing( Taxa::getShortName )).collect( Collectors.toList());
    }
}
