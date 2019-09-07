package ca.ubc.msl.polyst;

import ca.ubc.msl.polyst.model.Species;
import ca.ubc.msl.polyst.repositories.ProteinRepository;
import ca.ubc.msl.polyst.settings.SpeciesSettings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Log4j2
@SpringBootApplication
public class PolystApplication implements CommandLineRunner {

	private final ProteinRepository repository;
	private final SpeciesSettings speciesSettings;

	@Autowired
	public PolystApplication( ProteinRepository repository, SpeciesSettings speciesSettings ) {
		this.repository = repository;
		this.speciesSettings = speciesSettings;
	}

	public static void main(String[] args) {
		SpringApplication.run(PolystApplication.class, args);
	}

	@Override
	public void run( String... args ) throws Exception {
		// Warm main cache, will refresh asynchronously based on FlatFileProteinRepository policies
		for ( Species species : speciesSettings.getSpecies().values() ) {
			// We do this first because it's fast and blocks main page load
			if ( species.isActive() ) {
				log.info( "Loading protein count cache for " + species.getCommonName() );
				repository.getProteinCount( species );
			}
		}
		for ( Species species : speciesSettings.getSpecies().values() ) {
			if ( species.isActive() ) {
				log.info( "Loading ProteinInfo cache for " + species.getCommonName() );
				repository.getProteinInfo( species );
			}
		}
		log.info( "Caches populated." );
	}
}
