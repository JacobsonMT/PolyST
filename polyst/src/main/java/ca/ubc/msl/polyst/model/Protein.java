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

}
