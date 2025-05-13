package ch.sbb.matsim.umlego.demand.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandPart;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnroutableDemandRepositoryTest {

    private Connection connection;
    private UnroutableDemandRepository repository;

    @BeforeEach
    public void setUp() throws SQLException {
        // Set up H2 in-memory database
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        connection = dataSource.getConnection();

        // Create simba_umlego_skims table
        String createTableSQL = """
                CREATE TABLE unroutable_demand (
                    RUN_ID VARCHAR(255),
                    TARGET_DATE DATE,
                    ORIGIN INTEGER,
                    DESTINATION INTEGER,
                    DEMAND DOUBLE
                )
                """;
        connection.createStatement().execute(createTableSQL);

        repository = new UnroutableDemandRepository();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.createStatement().execute("DROP TABLE unroutable_demand");
        connection.close();
    }

    @Test
    public void testSave() throws SQLException {
        // Prepare test data
        UnroutableDemand ud = new UnroutableDemand();
        ud.addPart(new UnroutableDemandPart("1", "2", 20.0));
        ud.addPart(new UnroutableDemandPart("1", "3", 30.0));
        ud.addPart(new UnroutableDemandPart("1", "4", 40.0));
        ud.addPart(new UnroutableDemandPart("1", "5", 50.0));

        // Call the method under test
        repository.save(connection, "rundId", LocalDate.parse("2024-12-24"), ud);

        // Verify that data was inserted
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM unroutable_demand")) {

            UnroutableDemand result = new UnroutableDemand();
            while (rs.next()) {
                result.addPart(new UnroutableDemandPart(rs.getString("RUN_ID"), rs.getString("ORIGIN"), rs.getDouble("DESTINATION")));
            }
            assertEquals(4, result.getParts().size());
        }
    }
}