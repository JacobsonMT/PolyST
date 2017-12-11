package ca.ubc.msl.polyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Created by mjacobson on 11/12/17.
 */

@Component
public class DatabaseLoader implements CommandLineRunner {


    private static final Logger log = LoggerFactory.getLogger(DatabaseLoader.class);


    @Override
    public void run(String... strings) throws Exception {
        log.info( "DatabaseLoader Run!" );
    }
}
