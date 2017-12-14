package ca.ubc.msl.polyst.model;

import lombok.*;

/**
 * Created by mjacobson on 12/12/17.
 */
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"accession"})
@ToString
public class ProteinInfo {

    private final String accession;
    private final int size;

}