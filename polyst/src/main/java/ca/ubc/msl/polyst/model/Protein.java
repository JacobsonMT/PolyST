package ca.ubc.msl.polyst.model;

import lombok.*;

import java.util.List;

/**
 * Created by mjacobson on 11/12/17.
 */
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"accession"})
@ToString(exclude = "sequence")
public class Protein {

    private final String accession;
    private List<Base> sequence;

    public Base getBase(int location) {
        if ( location <= 0 ) {
            return null;
        }

        if ( location < this.sequence.size() ) { // location is 1 based
            return this.sequence.get( location - 1 );
        } else {
            return null;
        }
    }

}
