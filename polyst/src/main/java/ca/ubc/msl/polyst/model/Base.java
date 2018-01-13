package ca.ubc.msl.polyst.model;

import com.google.common.collect.Lists;
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
    private final int depth;
    private final double iupred;
    private double conservation;
    private List<Double> list = Lists.newArrayList();

}
