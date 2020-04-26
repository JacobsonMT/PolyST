package ca.ubc.msl.polyst.repositories;

import ca.ubc.msl.polyst.exception.CacheWarmingException;
import ca.ubc.msl.polyst.model.Base;
import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.ProteinInfo;
import ca.ubc.msl.polyst.model.Species;
import ca.ubc.msl.polyst.settings.SpeciesSettings;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by mjacobson on 11/12/17.
 */
@Component
public class FlatFileProteinRepository implements ProteinRepository {

    private static final Logger log = LoggerFactory.getLogger( FlatFileProteinRepository.class );

    private static final String SPECIES_CACHE_DIR = "cache";
    private static final String CACHE_FILENAME = "info.cache";

    @Value("${polyst.dataDir}")
    private String flatFileDirectory;

    private final SpeciesSettings speciesSettings;

    private LoadingCache<Species, List<ProteinInfo>> proteinInfoCache;
    private LoadingCache<Species, Long> proteinCountCache;
    private LoadingCache<ProteinCacheKey, Protein> proteinCache;

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class ProteinCacheKey {
        private Species species;
        private String accession;
    }

    public FlatFileProteinRepository( SpeciesSettings speciesSettings ) {
        this.speciesSettings = speciesSettings;
    }

    @PostConstruct
    private void postConstruct() {
        proteinInfoCache = Caffeine.newBuilder()
                .maximumSize(speciesSettings.getSpecies().size())
                .refreshAfterWrite(1, TimeUnit.HOURS)
                .build( this::loadProteinInfo );

        proteinCountCache = Caffeine.newBuilder()
                .maximumSize(speciesSettings.getSpecies().size())
                .refreshAfterWrite(1, TimeUnit.HOURS)
                .build( this::loadProteinCount );

        proteinCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build( this::loadProtein );
    }

    /**
     * Persist cached values for a species to disk. Used to warm the cache after restarts.
     *
     * @param species species
     * @param values values to persist
     * @throws IOException When issues creating or writing to cache file
     */
    private synchronized void persist(Species species, List<ProteinInfo> values) throws IOException {
        Path cacheFile = Paths.get(flatFileDirectory, species.getSubdirectory(), SPECIES_CACHE_DIR,
            CACHE_FILENAME);
        Files.createDirectories(cacheFile.getParent());

        if (!Files.exists(cacheFile)) {
            Files.createFile(cacheFile);
        }

        if (values == null) {
            return;
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
            oos.writeObject(values);
        }
    }

    /**
     * Warm cache for species from disk
     *
     * @param species species
     * @throws CacheWarmingException When cache cannot be warmed from disk
     */
    @Override
    public synchronized void warm(Species species) throws CacheWarmingException {
        Path cacheFile = Paths.get(flatFileDirectory, species.getSubdirectory(), SPECIES_CACHE_DIR, CACHE_FILENAME);

        if (!Files.exists(cacheFile)) {
            throw new CacheWarmingException("Cache not found");
        }

        try ( ObjectInputStream ois = new ObjectInputStream( Files.newInputStream( cacheFile ) ) ) {
            List<ProteinInfo> values = (List<ProteinInfo>) ois.readObject();

            if (values.size() != getProteinCount( species )) {
                throw new CacheWarmingException("Cache is stale");
            }
            proteinInfoCache.put(species, values);
        } catch (IOException e ) {
            throw new CacheWarmingException("Error reading from cache");
        } catch (ClassNotFoundException e) {
            throw new CacheWarmingException("Cache File Corrupt");
        }
    }

    @Override
    public Protein getProtein( Species species, String accession ) {
        return proteinCache.get( new ProteinCacheKey( species, accession ) );
    }

    private Protein loadProtein( ProteinCacheKey key ) {

        InputStream is = null;

        try {

            Path file = Paths.get( flatFileDirectory, key.species.getSubdirectory(), key.accession + ".txt" );


            is = Files.newInputStream( file );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

            List<Base> sequence = key.species.isDisorderPrediction() ?
                    br.lines().skip( 1 ).map( mapBaseWithDisorder ).collect( Collectors.toList() ) :
                    br.lines().skip( 1 ).map( mapBaseWithoutDisorder ).collect( Collectors.toList() );

            Protein protein = new Protein( key.accession );
            protein.setSequence( sequence );
            log.info( "Loaded protein from disk: {}/{}", key.species.getCommonName(), key.accession );
            return protein;
        } catch (NoSuchFileException ex) {
            log.debug( "No file found for: " + key.accession + " in species: " + key.species );
        } catch (FileNotFoundException | InvalidPathException ex) {
            log.warn( "File not accessible: " + key.accession + " in species: " + key.species );
        } catch (IOException ex) {
            log.error( "IO Error for: " + key.accession + " in species: " + key.species, ex );
        } finally {
            try {
                if ( is != null ) {
                    is.close();
                }
            } catch (IOException ex) {
                log.error( "IO Error when closing: " + key.accession + " in species: " + key.species, ex );
            }
        }

        return null;
    }
    @Override
    public Long getProteinCount( Species species ) {
        return proteinCountCache.get( species );
    }

    private Long loadProteinCount( Species species ) {
        try ( Stream<Path> paths = Files.list( Paths.get( flatFileDirectory, species.getSubdirectory() ) ) ) {
            long startTime = System.currentTimeMillis();
            long count = paths.filter( Files::isRegularFile ).count();
            log.info( "Load Protein Count Complete for {} in {}ms", species.getCommonName(), (System.currentTimeMillis() - startTime) );
            return count;
        } catch ( IOException e ) {
            log.error( "Error walking data directory!" );
            return 0L;
        } catch ( InvalidPathException e ) {
            log.error( "Requested invalid species: " + species );
            return 0L;
        }
    }

    @Override
    public List<ProteinInfo> getProteinInfo( Species species ) {
        return proteinInfoCache.get( species );
    }

    private List<ProteinInfo> loadProteinInfo( Species species ) {
        List<ProteinInfo> results;
        try ( Stream<Path> paths = Files.list( Paths.get( flatFileDirectory, species.getSubdirectory() ) ) ) {
            long startTime = System.currentTimeMillis();
            results = paths.filter( Files::isRegularFile ).map( p -> {
                try {
                    int lines = 0;
                    try {
                        lines = fastCountLines( p.toFile() ) - 1;
                    } catch ( IOException ignored ) {
                    }
                    if (lines == 0) {
                        lines = ( int ) Files.lines(p).count();
                    }
                    return new ProteinInfo( getNameWithoutExtension( p ), lines );
                } catch ( Exception e ) {
                    log.warn( "Issue Obtaining ProteinInfo from: " + p.getFileName() + " in species: " + species );
                    return new ProteinInfo( getNameWithoutExtension( p ), 0 );
                }
            } )
                    .collect( Collectors.toList() );
            log.info( "Load Protein Info Complete for {} in {}s", species.getCommonName(), (System.currentTimeMillis() - startTime)/1000 );
//            return paths.filter( Files::isRegularFile ).map( p -> p.getFileName().toString().substring( 0, p.getFileName().toString().length() - 4 ) ).collect( Collectors.toList() );
        } catch ( IOException e ) {
            log.error( "Error walking data directory!" );
            results = new ArrayList<>();
        } catch ( InvalidPathException e ) {
            log.error( "Requested invalid species: " + species );
            results = new ArrayList<>();
        }

        if (!results.isEmpty()) {
            try {
                persist(species, results);
                log.info("ProteinInfo persisted to disk for {}", species.getCommonName());
            } catch (IOException e) {
                log.error("Failed to persist to disk", e);
            }
        } else {
            log.info("Not persisting empty ProteinInfo to disk for {}", species.getCommonName());
        }

        return results;
    }

    @Override
    public File getRawProteinData( Species species, String accession ) {
        return Paths.get( flatFileDirectory, species.getSubdirectory(), accession + ".txt" ).toFile();
    }

    private static Function<String, Base> mapBaseWithDisorder = ( rawLine ) -> {
        List<String> line = Arrays.asList( rawLine.split( "\t" ) );


        Base base = new Base( line.get( 2 ), Integer.parseInt( line.get( 3 ) ) );

        base.setIupred( Double.parseDouble( line.get( 4 ) ) );
        base.setEspritz( Double.parseDouble( line.get( 5 ) ) );

        if ( line.size() > 6 ) {
            base.setConservation( Double.parseDouble( line.get( 6 ) ) );
        }

        if ( line.size() > 7 ) {
            base.setList( line.stream().skip( 7 ).map( Double::parseDouble ).collect( Collectors.toList() ) );
        }
        return base;
    };

    private static Function<String, Base> mapBaseWithoutDisorder = ( rawLine ) -> {
        List<String> line = Arrays.asList( rawLine.split( "\t" ) );


        Base base = new Base( line.get( 2 ), Integer.parseInt( line.get( 3 ) ) );

        if ( line.size() > 4 ) {
            base.setConservation( Double.parseDouble( line.get( 4 ) ) );
        }

        if ( line.size() > 5 ) {
            base.setList( line.stream().skip( 5 ).map( Double::parseDouble ).collect( Collectors.toList() ) );
        }
        return base;
    };

    private static String getNameWithoutExtension(Path path) {
        String fileName = path.toFile().getName();
        int extensionPos = fileName.lastIndexOf(".");

        if (extensionPos == -1) {
            return fileName;
        } else {
            return fileName.substring(0, extensionPos);
        }

    }

    private static int fastCountLines(File aFile) throws IOException {
        int count = 0;
        try (FileInputStream stream = new FileInputStream(aFile)) {
            byte[] buffer = new byte[8192];
            int n;
            while ( (n = stream.read( buffer )) > 0 ) {
                for ( int i = 0; i < n; i++ ) {
                    if ( buffer[i] == '\n' ) count++;
                }
            }
        }
        return count;
    }
}
