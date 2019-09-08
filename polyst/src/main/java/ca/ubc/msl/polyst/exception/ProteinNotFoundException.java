package ca.ubc.msl.polyst.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Protein Not Found")
public class ProteinNotFoundException extends RuntimeException {
}