package ch.sbb.matsim.umlego.writers.jdbc;

import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class JdbcVolumeCarpetWriterTest {

    private final JdbcDataSource dataSource = new JdbcDataSource();
    private JdbcVolumeCarpetWriter writer;
    private TransitLine transitLine;
    private TransitRoute transitRoute;
    private TransitStopFacility stop1;
    private TransitStopFacility stop2;

    @BeforeEach
    public void setUp() throws Exception {
        // Set up in-memory database
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        Connection connection = dataSource.getConnection();

        // Create the table
        String createTableSQL = """
            CREATE TABLE volume_carpet (
                RUN_ID          VARCHAR(255),
                TARGET_DATE     DATE,
                TU_CODE         VARCHAR(255),
                DEPARTURE_ID    INTEGER,
                TRAIN_NO        INTEGER,
                INDEX           INTEGER,
                ARRIVAL         VARCHAR(255),
                DEPARTURE       VARCHAR(255),
                TO_STOP_ARRIVAL VARCHAR(255),
                FROM_STOP_NO    INTEGER,
                TO_STOP_NO      INTEGER,
                VOLUME          DOUBLE,
                BOARDING        DOUBLE,
                ALIGHTING       DOUBLE,
                ORIGIN_BOARDING        DOUBLE,
                DESTINATION_ALIGHTING       DOUBLE
            )
            """;
        connection.createStatement().execute(createTableSQL);

        // Create real TransitSchedule and related classes
        TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
        TransitSchedule transitSchedule = factory.createTransitSchedule();

        // Create TransitStopFacilities
        stop1 = factory.createTransitStopFacility(Id.create("101", TransitStopFacility.class), null, false);
        stop1.setName("Stop 1");

        stop2 = factory.createTransitStopFacility(Id.create("102", TransitStopFacility.class), null, false);
        stop2.setName("Stop 2");

        // Add stops to schedule
        transitSchedule.addStopFacility(stop1);
        transitSchedule.addStopFacility(stop2);

        // Create TransitRouteStops
        TransitRouteStop routeStop1 = factory.createTransitRouteStop(stop1, 0.0, 0.0);
        TransitRouteStop routeStop2 = factory.createTransitRouteStop(stop2, 600.0, 600.0);

        // Create TransitRoute
        List<TransitRouteStop> stops = new ArrayList<>();
        stops.add(routeStop1);
        stops.add(routeStop2);

        transitRoute = factory.createTransitRoute(Id.create("route1", TransitRoute.class), null, stops, "pt");

        // Create Departure
        Departure departure = factory.createDeparture(Id.create("1001", Departure.class), 3600.0);

        // Add departure to route
        transitRoute.addDeparture(departure);

        // Create TransitLine
        transitLine = factory.createTransitLine(Id.create("line1", TransitLine.class));
        transitLine.addRoute(transitRoute);

        // Set operator code attribute
        transitLine.getAttributes().putAttribute("operatorCode", "TU1");

        // Add line to schedule
        transitSchedule.addTransitLine(transitLine);

        // Initialize writer
        writer = new JdbcVolumeCarpetWriter(connection, "run1", LocalDate.parse("2024-12-24"), transitSchedule);
    }

    @AfterEach
    public void tearDown() throws Exception {
        Connection connectionTearDown = dataSource.getConnection();
        connectionTearDown.createStatement().execute("DROP TABLE volume_carpet");
        connectionTearDown.close();
    }

    @Test
    public void testWriteRouteAndClose() throws Exception {
        // Mock RaptorRoute
        RaptorRoute raptorRoute = mock(RaptorRoute.class);

        // Create FoundRoute
        FoundRoute foundRoute = new FoundRoute(raptorRoute);
        foundRoute.demand = new Object2DoubleOpenHashMap<>();
        foundRoute.demand.put("destZone", 50.0);

        // Use reflection to create RoutePart
        Constructor<RoutePart> constructor = RoutePart.class.getDeclaredConstructor(
            TransitStopFacility.class,
            TransitStopFacility.class,
            String.class,
            double.class,
            double.class,
            double.class,
            double.class,
            double.class,
            TransitLine.class,
            TransitRoute.class,
            List.class
        );
        constructor.setAccessible(true);

        RoutePart routePart = constructor.newInstance(
            stop1,       // fromStop
            stop2,       // toStop
            "pt",        // mode
            3600.0,      // depTime
            3600.0,      // boardingTime
            3600.0,      // vehicleDepTime
            4200.0,      // arrivalTime
            600.0,       // distance
            transitLine, // line
            transitRoute,// route
            null         // planElements
        );

        foundRoute.routeParts = Collections.singletonList(routePart);
        writer.writeRoute("origZone", "destZone", foundRoute);
        writer.close();

        Connection connectionTest = dataSource.getConnection();

        // Verify data in the database
        String query = "SELECT * FROM volume_carpet";
        try (Statement stmt = connectionTest.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {

            // Verify first entry
            assertTrue(rs.next());

            assertEquals("run1", rs.getString("RUN_ID"));
            assertEquals("TU1", rs.getString("TU_CODE"));
            assertEquals(1001, rs.getInt("DEPARTURE_ID"));
            assertEquals(1001, rs.getInt("TRAIN_NO"));
            assertEquals(1, rs.getInt("INDEX"));
            System.out.println("Calculated time: " + Time.writeTime(3600.0));
            System.out.println("Result time: " + rs.getTime("ARRIVAL"));
            assertEquals(Time.writeTime(3600.0), rs.getString("ARRIVAL"));
            assertEquals(Time.writeTime(3600.0), rs.getString("DEPARTURE"));
            assertEquals(Time.writeTime(4200.0), rs.getString("TO_STOP_ARRIVAL"));
            assertEquals(101, rs.getInt("FROM_STOP_NO"));
            assertEquals(102, rs.getInt("TO_STOP_NO"));
            assertEquals(50.0, rs.getDouble("VOLUME"));
            assertEquals(50.0, rs.getDouble("BOARDING"));
            assertEquals(0.0, rs.getDouble("ALIGHTING"));
            assertEquals(50.0, rs.getDouble("ORIGIN_BOARDING"));
            assertEquals(0.0, rs.getDouble("DESTINATION_ALIGHTING"));

            // Verify second entry
            assertTrue(rs.next());

            assertEquals("run1", rs.getString("RUN_ID"));
            assertEquals("TU1", rs.getString("TU_CODE"));
            assertEquals(1001, rs.getInt("DEPARTURE_ID"));
            assertEquals(1001, rs.getInt("TRAIN_NO"));
            assertEquals(2, rs.getInt("INDEX"));
            assertEquals(Time.writeTime(4200.0), rs.getString("ARRIVAL"));
            assertEquals(Time.writeTime(4200.0), rs.getString("DEPARTURE"));
            // TO_STOP_ARRIVAL is null in the second entry
            assertNull(rs.getObject("TO_STOP_ARRIVAL"));
            assertEquals(102, rs.getInt("FROM_STOP_NO"));
            // TO_STOP_NO is null in the second entry
            assertNull(rs.getObject("TO_STOP_NO"));
            assertEquals(0.0, rs.getDouble("VOLUME"));
            assertEquals(0.0, rs.getDouble("BOARDING"));
            assertEquals(50.0, rs.getDouble("ALIGHTING"));
            assertEquals(0.0, rs.getDouble("ORIGIN_BOARDING"));
            assertEquals(50.0, rs.getDouble("DESTINATION_ALIGHTING"));

            // Now, no more entries should be present
            assertFalse(rs.next());
        }
        closeConnection(connectionTest);
    }
}
