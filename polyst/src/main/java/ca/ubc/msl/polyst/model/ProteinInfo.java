package ca.ubc.msl.polyst.model;

import java.io.Serializable;
import lombok.*;

/**
 * Created by mjacobson on 12/12/17.
 */
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"accession"})
@ToString
public class ProteinInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String accession;
    private final int size;

}