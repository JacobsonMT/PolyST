package ca.ubc.msl.polyst.controllers;

import ca.ubc.msl.polyst.model.*;
import ca.ubc.msl.polyst.repositories.ProteinRepository;
import ca.ubc.msl.polyst.settings.TaxaSettings;
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
import java.nio.charset.StandardCharsets;
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
    @EqualsAndHashCode( of = {"accession"} )
    @ToString
    static final class ProteinRequest {
        private final String accession;
        private final int location;
        private final String ref;
        private final String alt;
    }

    private final ProteinRepository repository;
    private final TaxaSettings taxaSettings;

    @Autowired
    public ProteinController( ProteinRepository repository, TaxaSettings taxaSettings ) {
        this.repository = repository;
        this.taxaSettings = taxaSettings;
    }

    @RequestMapping( value = "/api/taxa/{taxaId}/proteins/{accession}", method = RequestMethod.GET )
    public Protein getByAccession( @PathVariable int taxaId, @PathVariable String accession ) {
        Taxa taxa = taxaSettings.getTaxa( taxaId );

        if ( taxa == null ) {
            return null;
        }
        return repository.getByAccession( taxa, accession );
    }

    @RequestMapping( value = "/api/taxa/{taxaId}/proteins/{accession}/{location}", method = RequestMethod.GET )
    public ResponseEntity<?> getByAccession( @PathVariable int taxaId,
                                             @PathVariable String accession,
                                             @PathVariable int location ) {

        Taxa taxa = taxaSettings.getTaxa( taxaId );

        if ( taxa == null ) {
            return new ResponseEntity<>( "Taxa unavailable.", HttpStatus.BAD_REQUEST );
        }

        Protein protein = repository.getByAccession( taxa, accession );

        if ( protein == null ) {
            return new ResponseEntity<>( "Protein accession unavailable.", HttpStatus.BAD_REQUEST );
        }

        Base base = protein.getBase( location );

        if ( base == null ) {
            return new ResponseEntity<>( "Cannot find location (1 based).", HttpStatus.BAD_REQUEST );
        }

        return new ResponseEntity<>( base, HttpStatus.OK );

    }

    @RequestMapping( value = "/api/taxa/{taxaId}/proteins/{accession}/{location}/{ref}/{alt}", method = RequestMethod.GET )
    public ResponseEntity<?> getByAccession( @PathVariable int taxaId,
                                             @PathVariable String accession,
                                             @PathVariable int location,
                                             @PathVariable String ref,
                                             @PathVariable String alt ) {

        Taxa taxa = taxaSettings.getTaxa( taxaId );

        if ( taxa == null ) {
            return new ResponseEntity<>( "Taxa unavailable.", HttpStatus.BAD_REQUEST );
        }

        Protein protein = repository.getByAccession( taxa, accession );

        if ( protein == null ) {
            return new ResponseEntity<>( "Protein accession unavailable.", HttpStatus.BAD_REQUEST );
        }

        Base base = protein.getBase( location );

        if ( base == null ) {
            return new ResponseEntity<>( "Cannot find location (1 based).", HttpStatus.BAD_REQUEST );
        }

        if ( !base.getReference().equalsIgnoreCase( ref ) ) {
            return new ResponseEntity<>( "Incorrect Reference. Reference at this location (1 based) is: " + base.getReference(), HttpStatus.BAD_REQUEST );
        }

        Mutation mutation;
        try {
            mutation = Mutation.valueOf( alt.toUpperCase() );
        } catch ( Exception e ) {
            return new ResponseEntity<>( "Unknown alternate.", HttpStatus.BAD_REQUEST );
        }

        if ( base.getList().size() > mutation.ordinal() ) {
            return new ResponseEntity<>( base.getList().get( mutation.ordinal() ), HttpStatus.OK );
        } else {
            return new ResponseEntity<>( "No data for this query.", HttpStatus.BAD_REQUEST );
        }

    }

    @RequestMapping( value = "/api/taxa/{taxaId}/proteins", method = RequestMethod.GET )
    public List<ProteinInfo> allProteins( @PathVariable int taxaId ) {
        Taxa taxa = taxaSettings.getTaxa( taxaId );

        if ( taxa == null ) {
            return null;
        }

        return repository.allProteinInfo( taxa );
    }

    @RequestMapping( value = "/api/taxa/{taxaId}/proteins/datatable", method = RequestMethod.POST )
    public DataTablesResponse<ProteinInfo> proteinDatatable( @PathVariable int taxaId,
                                                             @RequestBody final DataTablesRequest dataTablesRequest ) {
        Taxa taxa = taxaSettings.getTaxa( taxaId );

        if ( taxa == null ) {
            DataTablesResponse<ProteinInfo> response = new DataTablesResponse<>();
            response.setError( "Unsupported Taxa" );
            return response;
        }

        List<ProteinInfo> rawResults = repository.allProteinInfo( taxa );
        Stream<ProteinInfo> resultStream = rawResults.stream();

        List<Predicate<ProteinInfo>> filters = Lists.newArrayList();
        for ( DataTablesRequest.Column col : dataTablesRequest.getColumns() ) {
            if ( col.isSearchable() ) {
                switch ( col.getData() ) {
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
            resultStream = resultStream.filter( pi -> filters.stream().anyMatch( t -> t.test( pi ) ) );
        }


        List<Comparator<ProteinInfo>> sorts = Lists.newArrayList();
        for ( DataTablesRequest.Order order : dataTablesRequest.getOrders() ) {
            try {
                DataTablesRequest.Column col = dataTablesRequest.getColumns().get( order.getColumn() );
                Comparator<ProteinInfo> c;
                switch ( col.getData() ) {
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

        List<ProteinInfo> filteredOrderedResults = resultStream.collect( Collectors.toList() );

        resultStream = filteredOrderedResults.stream().skip( dataTablesRequest.getStart() );

        if ( dataTablesRequest.getLength() >= 0 ) {
            resultStream = resultStream.limit( dataTablesRequest.getLength() );
        }

        DataTablesResponse<ProteinInfo> response = new DataTablesResponse<>();
        response.setData( resultStream.collect( Collectors.toList() ) );
        response.setDraw( dataTablesRequest.getDraw() );
        response.setRecordsTotal( rawResults.size() );
        response.setRecordsFiltered( filteredOrderedResults.size() );

        return response;
    }

    @RequestMapping( value = "/api/taxa/{taxaId}/proteins", method = RequestMethod.POST )
    public List<Object> manyProteins( @PathVariable int taxaId,
                                      @RequestBody List<ProteinRequest> proteinRequests ) {

        Taxa taxa = taxaSettings.getTaxa( taxaId );

        if ( taxa == null ) {
            return null;
        }

        // This can be expanded for more versatility and efficiency with many calls to same protein
        List<Object> res = Lists.newArrayList();
        for ( ProteinRequest pr : proteinRequests ) {
            ResponseEntity<?> re = getByAccession( taxa.getId(), pr.getAccession(), pr.getLocation(), pr.getRef(), pr.getAlt() );
            res.add( re.getBody().toString() );
        }
        return res;
    }

    @RequestMapping( value = "/api/taxa/{taxaId}/proteins/{accession}/download", method = RequestMethod.GET )
    public void downloadByAccession( HttpServletResponse response,
                                     @PathVariable int taxaId,
                                     @PathVariable String accession ) throws IOException {

        Taxa taxa = taxaSettings.getTaxa( taxaId );

        if ( taxa == null ) {
            String errorMessage = "Sorry. Taxa " + taxaId + " is not supported";
            OutputStream outputStream = response.getOutputStream();
            outputStream.write( errorMessage.getBytes( StandardCharsets.UTF_8 ) );
            outputStream.close();
            return;
        }

        File file = ( File ) repository.getRawData( taxa, accession );


        if ( !file.exists() ) {
            String errorMessage = "Sorry. The file you are looking for does not exist";
            OutputStream outputStream = response.getOutputStream();
            outputStream.write( errorMessage.getBytes( StandardCharsets.UTF_8 ) );
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

        response.setContentLength( ( int ) file.length() );

        InputStream inputStream = new BufferedInputStream( new FileInputStream( file ) );

        //Copy bytes from source to destination(outputstream in this example), closes both streams.
        FileCopyUtils.copy( inputStream, response.getOutputStream() );
    }
}
