package ca.ubc.msl.polyst.repositories;

import ca.ubc.msl.polyst.model.Base;
import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.ProteinInfo;
import ca.ubc.msl.polyst.model.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mjacobson on 11/12/17.
 */
@Component
public class FlatFileProteinRepository implements ProteinRepository {

    private static final Logger log = LoggerFactory.getLogger( FlatFileProteinRepository.class );

    @Value("${polyst.dataDir}")
    private String flatFileDirectory;

    @Cacheable(value = "protein")
    @Override
    public Protein getByAccession( Species species, String accession ) {

        InputStream is = null;

        try {

            Path file = Paths.get( flatFileDirectory, species.getSubdirectory(), accession + ".txt" );


            is = Files.newInputStream( file );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

            List<Base> sequence = species.isDisorderPrediction() ?
                    br.lines().skip( 1 ).map( mapBaseWithDisorder ).collect( Collectors.toList() ) :
                    br.lines().skip( 1 ).map( mapBaseWithoutDisorder ).collect( Collectors.toList() );

            Protein protein = new Protein( accession );
            protein.setSequence( sequence );

            return protein;
        } catch (NoSuchFileException ex) {
            log.debug( "No file found for: " + accession + " in species: " + species );
        } catch (FileNotFoundException | InvalidPathException ex) {
            log.warn( "File not accessible: " + accession + " in species: " + species );
        } catch (IOException ex) {
            log.error( "IO Error for: " + accession + " in species: " + species, ex );
        } finally {
            try {
                if ( is != null ) {
                    is.close();
                }
            } catch (IOException ex) {
                log.error( "IO Error when closing: " + accession + " in species: " + species, ex );
            }
        }

        return null;
    }

    @Override
    public File getRawData( Species species, String accession ) {
        return Paths.get( flatFileDirectory, species.getSubdirectory(), accession + ".txt" ).toFile();
    }

    @Cacheable(value="protein-info", sync=true)
    @Override
    public List<ProteinInfo> allProteinInfo( Species species ) {
        try ( Stream<Path> paths = Files.list( Paths.get( flatFileDirectory, species.getSubdirectory() ) ) ) {
            long startTime = System.currentTimeMillis();
            List<ProteinInfo> results = paths.filter( Files::isRegularFile ).map( p -> {
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
            return results;
//            return paths.filter( Files::isRegularFile ).map( p -> p.getFileName().toString().substring( 0, p.getFileName().toString().length() - 4 ) ).collect( Collectors.toList() );
        } catch ( IOException e ) {
            log.error( "Error walking data directory!" );
            return new ArrayList<>();
        } catch ( InvalidPathException e ) {
            log.error( "Requested invalid species: " + species );
            return new ArrayList<>();
        }
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
