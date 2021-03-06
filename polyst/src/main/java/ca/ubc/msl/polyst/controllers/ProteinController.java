package ca.ubc.msl.polyst.controllers;

import ca.ubc.msl.polyst.model.*;
import ca.ubc.msl.polyst.repositories.ProteinRepository;
import com.google.common.collect.Lists;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mjacobson on 11/12/17.
 */
@Log4j2
@RestController
public class ProteinController {

    @Getter
    @Setter
    @RequiredArgsConstructor
    @EqualsAndHashCode(of = {"accession"})
    @ToString
    static final class ProteinRequest {
        private final String accession;
        private final int location;
        private final String ref;
        private final String alt;
    }

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

        Protein protein = repository.getByAccession( accession );

        if ( protein == null ) {
            return new ResponseEntity<>( "Protein accession unavailable.", HttpStatus.BAD_REQUEST );
        }

        Base base = protein.getBase( location );

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

        Protein protein = repository.getByAccession( accession );

        if ( protein == null ) {
            return new ResponseEntity<>( "Protein accession unavailable.", HttpStatus.BAD_REQUEST );
        }

        Base base = protein.getBase( location );

        if ( base == null ) {
            return new ResponseEntity<>( "Cannot find location (1 based).", HttpStatus.BAD_REQUEST );
        }

        if ( !base.getReference().equalsIgnoreCase( ref ) ) {
            return new ResponseEntity<>( "Incorrect Reference. Reference at this location (1 based) is: " +  base.getReference(), HttpStatus.BAD_REQUEST );
        }

        Mutation mutation;
        try {
            mutation = Mutation.valueOf( alt.toUpperCase() );
        } catch (Exception e) {
            return new ResponseEntity<>( "Unknown alternate.", HttpStatus.BAD_REQUEST );
        }

        if (base.getList().size() > mutation.ordinal() ) {
            return new ResponseEntity<>( base.getList().get( mutation.ordinal() ), HttpStatus.OK );
        } else {
            return new ResponseEntity<>( "No data for this query.", HttpStatus.BAD_REQUEST );
        }

    }

    @RequestMapping(value = "/api/proteins", method = RequestMethod.GET)
    public List<ProteinInfo> allProteins() {
        return repository.allProteinInfo();
    }

    @RequestMapping(value = "/api/proteins/datatable", method = RequestMethod.POST)
    public DataTablesResponse<ProteinInfo> proteinDatatable(@RequestBody final DataTablesRequest dataTablesRequest ) {
        List<ProteinInfo> rawResults = repository.allProteinInfo();
        Stream<ProteinInfo> resultStream = rawResults.stream();

        List<Predicate<ProteinInfo>> filters = Lists.newArrayList();
        for ( DataTablesRequest.Column col : dataTablesRequest.getColumns() ) {
            if ( col.isSearchable() ) {
                switch( col.getData() ) {
                    case "accession":
                        filters.add( pi -> pi.getAccession().toLowerCase().contains( dataTablesRequest.getSearch().getValue().toLowerCase() ) );
                        break;
                    case "size":
                        filters.add( pi -> String.valueOf( pi.getSize() ).toLowerCase().contains( dataTablesRequest.getSearch().getValue().toLowerCase() ) );
                        break;
                    default:
                        // Do Nothing
                }
            }
        }
        if ( !filters.isEmpty() ) {
            resultStream = resultStream.filter( pi -> filters.stream().anyMatch( t -> t.test( pi )) );
        }


        List<Comparator<ProteinInfo>> sorts = Lists.newArrayList();
        for ( DataTablesRequest.Order order : dataTablesRequest.getOrders() ) {
            try {
                DataTablesRequest.Column col = dataTablesRequest.getColumns().get( order.getColumn() );
                Comparator<ProteinInfo> c;
                switch( col.getData() ) {
                    case "accession":
                        c = Comparator.comparing( ProteinInfo::getAccession );
                        sorts.add( order.getDir().equals( "asc" ) ? c : c.reversed() );
                        break;
                    case "size":
                        c = Comparator.comparing( ProteinInfo::getSize );
                        sorts.add( order.getDir().equals( "asc" ) ? c : c.reversed() );
                        break;
                    default:
                        // Do Nothing
                }

            } catch ( IndexOutOfBoundsException e ) {
                // Ignore
                log.warn( "Attempted to order by column that doesn't exist: " + order.getColumn() );
            }
        }
        if ( !sorts.isEmpty() ) {
            Comparator<ProteinInfo> comp = Comparator.nullsLast( null );
            for ( Comparator<ProteinInfo> comparator : sorts ) {
                comp = comp.thenComparing( comparator );
            }
            resultStream = resultStream.sorted( comp );
        }

        List<ProteinInfo> filteredOrderedResults = resultStream.collect( Collectors.toList());

        resultStream = filteredOrderedResults.stream().skip( dataTablesRequest.getStart() );

        if ( dataTablesRequest.getLength() >= 0 ) {
            resultStream = resultStream.limit( dataTablesRequest.getLength() );
        }

        DataTablesResponse<ProteinInfo> response = new DataTablesResponse<>();
        response.setData( resultStream.collect( Collectors.toList()) );
        response.setDraw( dataTablesRequest.getDraw() );
        response.setRecordsTotal( rawResults.size() );
        response.setRecordsFiltered( filteredOrderedResults.size() );

        return response;
    }

    @RequestMapping(value = "/api/proteins", method = RequestMethod.POST)
    public List<Object> manyProteins(@RequestBody List<ProteinRequest> proteinRequests) {
        // This can be expanded for more versatility and efficiency with many calls to same protein
        List<Object> res = Lists.newArrayList();
        for ( ProteinRequest pr : proteinRequests ) {
            ResponseEntity<?> re = getByAccession( pr.getAccession(), pr.getLocation(), pr.getRef(), pr.getAlt() );
            res.add( re.getBody().toString() );
        }
        return res;
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
