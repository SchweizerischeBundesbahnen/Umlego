package ch.sbb.matsim.umlego.readers.jdbc;

import static ch.sbb.matsim.umlego.readers.jdbc.TimesliceRepository.parseMatrix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.sbb.matsim.umlego.readers.jdbc.TimesliceRepository.TimesliceJdbcEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JdbcDemandMatrixParserTest {

    private static Connection connection;

    public JdbcDemandMatrixParserTest() {
    }

    @BeforeAll
    public static void setUpDatabase() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        connection = dataSource.getConnection();

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE simba_tgm_zeitscheibe (" +
                    "tag_typ VARCHAR(255), " +
                    "quell_bezirk_nummer VARCHAR(255) NOT NULL, " +
                    "ziel_bezirk_nummer VARCHAR(255) NOT NULL, " +
                    "scheibe INT, " +
                    "reisende DOUBLE, " +
                    "run_fp_version INT NOT NULL, " +
                    "run_id VARCHAR(255) NOT NULL)"
            );
        }
    }

    @BeforeEach
    public void insertTestData() throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
            "INSERT INTO simba_tgm_zeitscheibe" +
                "(tag_typ, quell_bezirk_nummer, ziel_bezirk_nummer, scheibe, reisende, run_fp_version, run_id)" +
                "VALUES ('saison', 'from1', 'to1', 10, 10.0, 1, 'run1')," +
                "('saison', 'from2', 'to2', 20, 20.0, 1, 'run2')"
        )) {
            pstmt.executeUpdate();
        }
    }

    @AfterAll
    public static void tearDownDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP TABLE simba_tgm_zeitscheibe");
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    public void testParseMatrix() throws SQLException {
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM simba_tgm_zeitscheibe")) {

            List<TimesliceJdbcEntry> result = parseMatrix(resultSet);

            assertNotNull(result);
            assertEquals(2, result.size());

            TimesliceJdbcEntry firstEntry = result.get(0);
            assertEquals(1, firstEntry.matrixIndex());
            assertEquals("from1", firstEntry.from());
            assertEquals("to1", firstEntry.to());
            assertEquals(10.0, firstEntry.value());

            TimesliceJdbcEntry secondEntry = result.get(1);
            assertEquals(2, secondEntry.matrixIndex());
            assertEquals("from2", secondEntry.from());
            assertEquals("to2", secondEntry.to());
            assertEquals(20.0, secondEntry.value());
        }
    }

    @Test
    public void testParseMatrix_NoData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM simba_tgm_zeitscheibe");
        }

        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM simba_tgm_zeitscheibe")) {

            List<TimesliceJdbcEntry> result = parseMatrix(resultSet);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
