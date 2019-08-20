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


    @Cacheable(value="protein-info", unless="#result == null or #result.size()==0")
    @Override
    public List<ProteinInfo> allProteinInfo( Species species ) {
        try (Stream<Path> paths = Files.list( Paths.get( flatFileDirectory, species.getSubdirectory() ) )) {
            List<ProteinInfo> results = paths.filter( Files::isRegularFile ).map( p -> {
                try {
                    return new ProteinInfo(
                            p.getFileName().toString().substring( 0, p.getFileName().toString().length() - 4 ),
                            Integer.parseInt( lastLine( p.toFile() ).split( "\t" )[1] ) );
                } catch (Exception e) {
                    log.warn( "Issue Obtaining ProteinInfo from: " + p.getFileName() + " in species: " + species );
                    return new ProteinInfo(
                            p.getFileName().toString().substring( 0, p.getFileName().toString().length() - 4 ), 0 );
                }
            } )
                    .collect( Collectors.toList() );
            log.info( "Load Protein Info Complete for " + species.getShortName() );
            return results;
//            return paths.filter( Files::isRegularFile ).map( p -> p.getFileName().toString().substring( 0, p.getFileName().toString().length() - 4 ) ).collect( Collectors.toList() );
        } catch (IOException e) {
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

    private static String lastLine( File file ) {

        RandomAccessFile fileHandler = null;
        try {
            fileHandler = new RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();

            for ( long filePointer = fileLength; filePointer != -1; filePointer-- ) {
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                if ( readByte == 0xA ) {
                    if ( filePointer == fileLength ) {
                        continue;
                    }
                    break;

                } else if ( readByte == 0xD ) {
                    if ( filePointer == fileLength - 1 ) {
                        continue;
                    }
                    break;
                }

                sb.append( (char) readByte );
            }

            return sb.reverse().toString();
        } catch (java.io.FileNotFoundException e) {
            log.warn( "No file found for: " + file.getName() );
            return null;
        } catch (java.io.IOException e) {
            log.error( "IO Error for: " + file.getName(), e );
            return null;
        } finally {
            if ( fileHandler != null )
                try {
                    fileHandler.close();
                } catch (IOException e) {
                /* ignore */
                }
        }
    }


}
