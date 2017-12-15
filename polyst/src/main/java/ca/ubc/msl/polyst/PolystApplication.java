package ca.ubc.msl.polyst;

import ca.ubc.msl.polyst.repositories.ProteinRepository;
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

	@Autowired
	public PolystApplication( ProteinRepository repository ) {
		this.repository = repository;
	}

	public static void main(String[] args) {
		SpringApplication.run(PolystApplication.class, args);
	}

	@Scheduled(fixedDelay = 3600)
	public void warmCaches() {
		// Warm main cache every hour, will reload if cache has been cleared
		log.debug( "Warm caches" );
		repository.allProteinInfo();
	}

}
