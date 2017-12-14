package ca.ubc.msl.polyst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PolystApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolystApplication.class, args);
	}
}
