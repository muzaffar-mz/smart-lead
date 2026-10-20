package com.muzaffar.smartlead;

import net.datafaker.Faker;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that need a real PostgreSQL.
 * <p>
 * One container is started for the whole test run and shared by every test class (the singleton container
 * pattern). It is not managed by {@code @Container}, because that would stop and restart it between test
 * classes, giving it a new port while Spring still holds a cached context that points at the old one.
 * Testcontainers removes the container when the JVM exits.
 * <p>
 * {@link ServiceConnection} points the datasource at it, and Spring Boot runs the Liquibase changelogs
 * (db/changelog-master.xml) on startup, so the schema comes from the same files as in production.
 * With {@code ddl-auto: validate} Hibernate then checks the entities against that schema.
 */
public abstract class AbstractTestcontainers {

    @ServiceConnection
    protected static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("smart-lead-test")
            .withUsername("smartlead")
            .withPassword("smartlead");

    static {
        postgres.start();
    }

    protected static final Faker FAKER = new Faker();
}
