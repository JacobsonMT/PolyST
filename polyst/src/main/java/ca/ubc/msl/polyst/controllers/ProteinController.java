package ca.ubc.msl.polyst.controllers;

import ca.ubc.msl.polyst.model.Base;
import ca.ubc.msl.polyst.model.Mutation;
import ca.ubc.msl.polyst.model.Protein;
import ca.ubc.msl.polyst.model.ProteinInfo;
import ca.ubc.msl.polyst.repositories.ProteinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by mjacobson on 11/12/17.
 */
@RestController
public class ProteinController {

    private final ProteinRepository repository;

    @Autowired
    public ProteinController( ProteinRepository repository ) {
        this.repository = repository;
    }

    @RequestMapping(value = "/api/proteins/{accession}", method = RequestMethod.GET)
    public Protein getByAccession( @PathVariable String accession ) {
        return repository.getByAccession( accession );
    }

    @RequestMapping(value = "/api/proteins/{accession}/{location}", method = RequestMethod.GET)
    public ResponseEntity<?> getByAccession( @PathVariable String accession,
                                             @PathVariable int location ) {

        Base base = repository.getBase( accession, location );

        if ( base == null ) {
            return new ResponseEntity<>( "Cannot find location (1 based).", HttpStatus.BAD_REQUEST );
        }

        return new ResponseEntity<>( base , HttpStatus.OK );

    }

    @RequestMapping(value = "/api/proteins/{accession}/{location}/{ref}/{alt}", method = RequestMethod.GET)
    public ResponseEntity<?> getByAccession( @PathVariable String accession,
                                             @PathVariable int location,
                                             @PathVariable String ref,
                                             @PathVariable String alt ) {

        Mutation mutation;
        try {
            mutation = Mutation.valueOf( alt.toUpperCase() );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>( "Unknown alternate.", HttpStatus.BAD_REQUEST );
        }

        Base base = repository.getBase( accession, location );

        if ( base == null ) {
            return new ResponseEntity<>( "Cannot find location (1 based).", HttpStatus.BAD_REQUEST );
        }

        if ( !base.getReference().equalsIgnoreCase( ref ) ) {
            return new ResponseEntity<>( "Incorrect Reference. Reference at this location (1 based) is: " +  base.getReference(), HttpStatus.BAD_REQUEST );
        }

        if (base.getPst().size() > mutation.ordinal() ) {
            return new ResponseEntity<>( base.getPst().get( mutation.ordinal() ), HttpStatus.OK );
        } else {
            return new ResponseEntity<>( "No data for this query.", HttpStatus.BAD_REQUEST );
        }

    }

    @RequestMapping(value = "/api/proteins", method = RequestMethod.GET)
    public List<ProteinInfo> allProteins() {
        return repository.allProteinInfo();
    }

    @RequestMapping(value = "/api/proteins/{accession}/download", method = RequestMethod.GET)
    public void downloadByAccession( HttpServletResponse response, @PathVariable String accession ) throws IOException {
        File file = (File) repository.getRawData( accession );


        if ( !file.exists() ) {
            String errorMessage = "Sorry. The file you are looking for does not exist";
            OutputStream outputStream = response.getOutputStream();
            outputStream.write( errorMessage.getBytes( Charset.forName( "UTF-8" ) ) );
            outputStream.close();
            return;
        }

        String mimeType = URLConnection.guessContentTypeFromName( file.getName() );
        if ( mimeType == null ) {
            mimeType = "text/html";
        }

        response.setContentType( mimeType );

        /* "Content-Disposition : inline" will show viewable types [like images/text/pdf/anything viewable by browser] right on browser
            while others(zip e.g) will be directly downloaded [may provide save as popup, based on your browser setting.]*/
        response.setHeader( "Content-Disposition", String.format( "inline; filename=\"" + file.getName() + "\"" ) );


        /* "Content-Disposition : attachment" will be directly download, may provide save as popup, based on your browser setting*/
        response.setHeader( "Content-Disposition", String.format( "attachment; filename=\"%s\"", file.getName() ) );

        response.setContentLength( (int) file.length() );

        InputStream inputStream = new BufferedInputStream( new FileInputStream( file ) );

        //Copy bytes from source to destination(outputstream in this example), closes both streams.
        FileCopyUtils.copy( inputStream, response.getOutputStream() );
    }
}
