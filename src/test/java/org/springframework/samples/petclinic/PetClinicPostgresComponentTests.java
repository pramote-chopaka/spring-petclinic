package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
@Testcontainers
public class PetClinicPostgresComponentTests {

	@Container
	public static DockerComposeContainer dockerComposeContainer = new DockerComposeContainer<>(
		new File("src/test/resources/docker-compose.yml"))
		.withExposedService("postgres", 5432, Wait.forLogMessage(".* database system is ready to accept connections*\\n", 1));

	@LocalServerPort
	int port;

	@Autowired
	private VetRepository vets;

	@Autowired
	private RestTemplateBuilder builder;

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("POSTGRES_URL", () -> String.format("jdbc:postgresql://%s:%s/petclinic",
			dockerComposeContainer.getServiceHost("postgres", 5432),
			dockerComposeContainer.getServicePort("postgres", 5432)));
		registry.add("POSTGRES_USER", () -> "petclinic");
		registry.add("POSTGRES_PASSWORD", () -> "petclinic");
	}


	@Test
	void testFindAll() throws Exception {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void testOwnerDetails() {
		RestTemplate template = builder.rootUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template.exchange(RequestEntity.get("/owners/1").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
