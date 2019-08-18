package ca.ubc.msl.polyst;

import ca.ubc.msl.polyst.model.Taxa;
import ca.ubc.msl.polyst.repositories.ProteinRepository;
import ca.ubc.msl.polyst.settings.TaxaSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class PolystApplication {

	private static final Logger log = LoggerFactory.getLogger( PolystApplication.class );


	private final ProteinRepository repository;
	private final TaxaSettings taxaSettings;

	@Autowired
	public PolystApplication( ProteinRepository repository, TaxaSettings taxaSettings ) {
		this.repository = repository;
		this.taxaSettings = taxaSettings;
	}

	public static void main(String[] args) {
		SpringApplication.run(PolystApplication.class, args);
	}

	@Scheduled(fixedDelay = 3600000)
	public void warmCaches() {
		// Warm main cache every hour, will reload if cache has been cleared
		log.debug( "Warm caches" );
		for ( Taxa taxa: taxaSettings.getTaxa().values() ) {
			if ( taxa.isActive() ) {
				log.info( "Loading Protein Info cache for " + taxa.getShortName() );
				repository.allProteinInfo( taxa );
			}
		}
		log.info( "Protein Info caches populated." );

	}

}
