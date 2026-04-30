package tn.limtic.limtic_backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LimticBackendApplication {

	public static void main(String[] args) {
		// Load .env file and expose all entries as system properties
		// so Spring's ${VAR} placeholders in application.properties resolve correctly.
		Dotenv dotenv = Dotenv.configure()
				.directory(".")          // look for .env in the working directory
				.ignoreIfMissing()       // don't crash if .env is absent (e.g. CI/CD with real env vars)
				.systemProperties()      // push each entry into System.setProperty(...)
				.load();

		SpringApplication.run(LimticBackendApplication.class, args);
	}

}
