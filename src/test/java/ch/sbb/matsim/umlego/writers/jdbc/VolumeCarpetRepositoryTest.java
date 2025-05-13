package ch.sbb.matsim.umlego.writers.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.sbb.matsim.umlego.writers.jdbc.VolumeCarpetRepository.BelastungsteppichEntry;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.core.utils.misc.Time;

public class VolumeCarpetRepositoryTest {

    private Connection connection;
    private VolumeCarpetRepository repository;

    @BeforeEach
    public void setUp() throws SQLException {
        // Set up H2 in-memory database
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        connection = dataSource.getConnection();

        // Create simba_umlego_belastungsteppich table
        String createTableSQL = """
            CREATE TABLE volume_carpet (
                RUN_ID VARCHAR(255),
                TARGET_DATE DATE,
                TU_CODE VARCHAR(255),
                DEPARTURE_ID INTEGER,
                TRAIN_NO INTEGER,
                INDEX INTEGER,
                ARRIVAL VARCHAR(255),
                DEPARTURE VARCHAR(255),
                TO_STOP_ARRIVAL VARCHAR(255),
                FROM_STOP_NO INTEGER,
                TO_STOP_NO INTEGER,
                VOLUME DOUBLE,
                BOARDING DOUBLE,
                ALIGHTING DOUBLE,
                ORIGIN_BOARDING DOUBLE,
                DESTINATION_ALIGHTING DOUBLE
            )
            """;
        connection.createStatement().execute(createTableSQL);

        repository = new VolumeCarpetRepository();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.createStatement().execute("DROP TABLE volume_carpet");
        connection.close();
    }

    @Test
    public void testInsertEntries() throws SQLException {
        // Prepare test data
        List<BelastungsteppichEntry> entries = new ArrayList<>();
        entries.add(new BelastungsteppichEntry(
            "run1",
            LocalDate.parse("2024-12-24"),
            "TU1",
            1001,
            2001,
            1,
            Time.writeTime(3600.0),
            Time.writeTime(3660.0),
            Time.writeTime(3720.0),
            101,
            102,
            50.0,
            10.0,
            5.0,
            10.0,
            5.0
        ));
        entries.add(new BelastungsteppichEntry(
            "run1",
            LocalDate.parse("2024-12-24"),
            "TU2",
            1002,
            2002,
            2,
            Time.writeTime(7200.0),
            Time.writeTime(7260.0),
            Time.writeTime(7320.0),
            103,
            104,
            60.0,
            15.0,
            8.0,
            15.0,
            8.0
        ));

        // Call the method under test
        repository.insertEntries(connection, entries);

        // Verify that data was inserted
        String query = "SELECT * FROM volume_carpet";
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {

            List<BelastungsteppichEntry> results = new ArrayList<>();
            while (rs.next()) {
                BelastungsteppichEntry entry = new BelastungsteppichEntry(
                    rs.getString("RUN_ID"),
                    rs.getDate("TARGET_DATE").toLocalDate(),
                    rs.getString("TU_CODE"),
                    rs.getInt("DEPARTURE_ID"),
                    rs.getInt("TRAIN_NO"),
                    rs.getInt("INDEX"),
                    rs.getString("ARRIVAL"),
                    rs.getString("DEPARTURE"),
                    rs.getString("TO_STOP_ARRIVAL"),
                    rs.getInt("FROM_STOP_NO"),
                    rs.getInt("TO_STOP_NO"),
                    rs.getDouble("VOLUME"),
                    rs.getDouble("BOARDING"),
                    rs.getDouble("ALIGHTING"),
                    rs.getDouble("ORIGIN_BOARDING"),
                    rs.getDouble("DESTINATION_ALIGHTING")
                );
                results.add(entry);
            }

            assertEquals(2, results.size());

            // Verify first entry
            BelastungsteppichEntry firstEntry = results.get(0);
            assertEquals("run1", firstEntry.runId());
            assertEquals("TU1", firstEntry.tuCode());
            assertEquals(1001, firstEntry.departureId());
            assertEquals(2001, firstEntry.trainNo());
            assertEquals(1, firstEntry.index());
            assertEquals(Time.writeTime(3600.0), firstEntry.arrival());
            assertEquals(Time.writeTime(3660.0), firstEntry.departure());
            assertEquals(Time.writeTime(3720.0), firstEntry.toStopArrival());
            assertEquals(101, firstEntry.fromStopNo());
            assertEquals(102, firstEntry.toStopNo());
            assertEquals(50.0, firstEntry.volume());
            assertEquals(10.0, firstEntry.boarding());
            assertEquals(5.0, firstEntry.alighting());
            assertEquals(10.0, firstEntry.originBoarding());
            assertEquals(5.0, firstEntry.destinationAlighting());

            // Verify second entry
            BelastungsteppichEntry secondEntry = results.get(1);
            assertEquals("run1", secondEntry.runId());
            assertEquals("TU2", secondEntry.tuCode());
            assertEquals(1002, secondEntry.departureId());
            assertEquals(2002, secondEntry.trainNo());
            assertEquals(2, secondEntry.index());
            assertEquals(Time.writeTime(7200.0), secondEntry.arrival());
            assertEquals(Time.writeTime(7260.0), secondEntry.departure());
            assertEquals(Time.writeTime(7320.0), secondEntry.toStopArrival());
            assertEquals(103, secondEntry.fromStopNo());
            assertEquals(104, secondEntry.toStopNo());
            assertEquals(60.0, secondEntry.volume());
            assertEquals(15.0, secondEntry.boarding());
            assertEquals(8.0, secondEntry.alighting());
            assertEquals(15.0, secondEntry.originBoarding());
            assertEquals(8.0, secondEntry.destinationAlighting());
        }
    }

    @Test
    public void testInsertEntries_EmptyList() throws SQLException {
        // Prepare empty test data
        List<BelastungsteppichEntry> entries = new ArrayList<>();

        // Call the method under test
        repository.insertEntries(connection, entries);

        // Verify that no data was inserted
        String query = "SELECT COUNT(*) AS rowcount FROM volume_carpet";
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {

            rs.next();
            int count = rs.getInt("rowcount");
            assertEquals(0, count);
        }
    }
}
