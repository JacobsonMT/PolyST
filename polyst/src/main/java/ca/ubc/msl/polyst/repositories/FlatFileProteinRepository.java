package ca.ubc.msl.polyst.repositories;

import ca.ubc.msl.polyst.model.Base;
import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.ProteinInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final String flatFileDirectory = "/home/mjacobson/git/PolyST/data/";

    @Override
    public Protein getByAccession( String accession ) {

        InputStream is = null;

        try {

            Path file = Paths.get( flatFileDirectory, accession + ".txt" );


            is = Files.newInputStream( file );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

            List<Base> sequence = br.lines().skip( 1 ).map( mapBase ).collect( Collectors.toList() );

            Protein protein = new Protein( accession );
            protein.setSequence( sequence );

            return protein;

        } catch (FileNotFoundException ex) {
            log.warn( "No file found for: " + accession );
        } catch (IOException ex) {
            log.error( "IO Error for: " + accession, ex );
        } finally {
            try {
                if ( is != null ) {
                    is.close();
                }
            } catch (IOException ex) {
                log.error( "IO Error when closing: " + accession, ex );
            }
        }

        return null;
    }


    @Cacheable("protein-info")
    public List<ProteinInfo> allProteinInfo() {
        try (Stream<Path> paths = Files.list( Paths.get( flatFileDirectory ) )) {
            return paths.filter( Files::isRegularFile ).map( p -> {
                try {
                    return new ProteinInfo(
                            p.getFileName().toString().substring( 0, p.getFileName().toString().length() - 4 ),
                            Integer.valueOf( lastLine( p.toFile() ).split( "\t" )[1] ) );
                } catch (IndexOutOfBoundsException e) {
                    log.warn( "Issue Obtaining ProteinInfo from: " + p.getFileName() );
                    return new ProteinInfo(
                            p.getFileName().toString().substring( 0, p.getFileName().toString().length() - 4 ),0 );
                }
            })
                    .collect( Collectors.toList() );
//            return paths.filter( Files::isRegularFile ).map( p -> p.getFileName().toString().substring( 0, p.getFileName().toString().length() - 4 ) ).collect( Collectors.toList() );
        } catch (IOException e) {
            log.error( "Error walking data directory!" );
            return null;
        }
    }

//    private static Base mapBase( String[] rawLine ) {
//        List<String> line = Arrays.asList( rawLine );
//        List<Double> pst = line.stream().skip( Math.max( 0, line.size() - 20 ) ).map( Double::parseDouble ).collect( Collectors.toList() );
//        return new Base( line.get( 2 ), Integer.valueOf( line.get( 3 ) ), Double.valueOf( line.get( 4 ) ), Double.valueOf( line.get( 5 ) ), pst );
//    }

    private static Function<String, Base> mapBase = ( rawLine ) -> {
        List<String> line = Arrays.asList( rawLine.split( "\t" ) );

        Base base = new Base( line.get( 2 ), Integer.valueOf( line.get( 3 ) ), Double.valueOf( line.get( 4 ) ) );

        if ( line.size() > 5 ) {
            base.setConservation( Double.valueOf( line.get( 5 ) ) );
        }

        if ( line.size() > 6 ) {
            base.setPst( line.stream().skip( 6 ).map( Double::parseDouble ).collect( Collectors.toList() ) );
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
