package ca.ubc.msl.polyst.model;

import lombok.*;

import java.util.List;

/**
 * Created by mjacobson on 11/12/17.
 */
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"reference"})
@ToString(exclude = "conservation")
public class Base {

    private final String reference;
    private final int cover;
    private final double iupPrediction;
    private final List<Double> conservation;

}
