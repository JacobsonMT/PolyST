package ca.ubc.msl.polyst.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode( of = "id" )
public class Species {
    private String shortName;
    private String longName;
    private int id;
    private boolean disorderPrediction;
    @JsonIgnore
    private String subdirectory;
    @JsonIgnore
    private boolean active;
    @JsonIgnore
    private int indexGroup;
}