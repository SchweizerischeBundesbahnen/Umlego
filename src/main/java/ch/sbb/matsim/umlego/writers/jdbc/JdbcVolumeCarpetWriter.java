package ch.sbb.matsim.umlego.writers.jdbc;

import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;

import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import ch.sbb.matsim.umlego.writers.UmlegoWriterInterface;
import ch.sbb.matsim.umlego.writers.jdbc.VolumeCarpetRepository.BelastungsteppichEntry;
import ch.sbb.matsim.umlego.writers.types.volume.Journey;
import ch.sbb.matsim.umlego.writers.types.volume.JourneyItem;
import ch.sbb.matsim.umlego.writers.types.volume.TrainNo;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class JdbcVolumeCarpetWriter implements UmlegoWriterInterface {

    private static final Logger LOG = LogManager.getLogger(JdbcVolumeCarpetWriter.class);

    private final String runId;
    private final LocalDate targetDate;
    private final Map<Id<TransitLine>, Map<Id<TransitRoute>, Map<Id<TransitStopFacility>, Map<Double, TrainNo>>>> trainNos;
    private final Map<TrainNo, Journey> journeyByTrainNo = new HashMap<>();
    private final List<BelastungsteppichEntry> entries = new ArrayList<>();
    private final VolumeCarpetRepository repository = new VolumeCarpetRepository();
    private final Connection connection;

    public JdbcVolumeCarpetWriter(Connection connection, String runId, LocalDate targetDate, TransitSchedule schedule) {
        this.connection = connection;
        this.runId = runId;
        this.targetDate = targetDate;

        this.trainNos = new HashMap<>();
        for (TransitLine line : schedule.getTransitLines().values()) {
            var trainNosByRoutes = new HashMap<Id<TransitRoute>, Map<Id<TransitStopFacility>, Map<Double, TrainNo>>>();
            for (TransitRoute route : line.getRoutes().values()) {

                var trainNoByStop = new HashMap<Id<TransitStopFacility>, Map<Double, TrainNo>>();

                for (TransitRouteStop stop : route.getStops()) {
                    trainNoByStop.put(stop.getStopFacility().getId(), new HashMap<>());
                }

                for (Departure departure : route.getDepartures().values()) {

                    String operatorCode = line.getAttributes().getAttribute("operatorCode").toString();
                    var journey = new Journey(new TrainNo(operatorCode, departure.getId()), new ArrayList<>());

                    TransitRouteStop nextStop = null;
                    for (int stopIndex = 0; stopIndex < route.getStops().size() - 1; stopIndex++) {
                        TransitRouteStop curStop = route.getStops().get(stopIndex);
                        nextStop = route.getStops().get(stopIndex + 1);
                        JourneyItem ji = new JourneyItem(curStop, nextStop, departure, stopIndex + 1);
                        journey.items().add(ji);
                        trainNoByStop.get(curStop.getStopFacility().getId())
                            .put(ji.getDeparture(), journey.trainNo());
                    }
                    if (nextStop != null) {
                        // Add last stop
                        JourneyItem ji = new JourneyItem(nextStop, null, departure, journey.items().size() + 1);
                        journey.items().add(ji);
                        trainNoByStop.get(nextStop.getStopFacility().getId())
                            .put(ji.getDeparture(), journey.trainNo());
                    }

                    journeyByTrainNo.put(journey.trainNo(), journey);
                }

                trainNosByRoutes.put(route.getId(), trainNoByStop);
            }
            trainNos.put(line.getId(), trainNosByRoutes);
        }
    }

    @Override
    public void writeRoute(String origZone, String destZone, FoundRoute route) {
        double demand = route.demand.getDouble(destZone);

        for (int i = 0; i<route.routeParts.size(); i++) {
            RoutePart routePart = route.routeParts.get(i);
            if (routePart.line != null && routePart.mode.equals("pt")) {
                routePart.route.getAttributes();

                var trainNoByStopId = trainNos.get(routePart.line.getId()).get(routePart.route.getId());
                var trainNoByDeparture = trainNoByStopId.get(routePart.fromStop.getId());
                TrainNo trainNo = trainNoByDeparture.get(routePart.vehicleDepTime);

                var journey = this.journeyByTrainNo.get(trainNo);
                var record = false;
                for (JourneyItem item : journey.items()) {
                    if (item.getFromStopFacilityId().equals(routePart.fromStop.getId())
                        && item.getDeparture() == routePart.vehicleDepTime) {
                        record = true;
                        item.addBoarding(demand);
                        if (i == 0) {
                            item.addOriginBoarding(demand);
                        }
                    }

                    if (item.getFromStopFacilityId().equals(routePart.toStop.getId())) {
                        item.addAlighting(demand);
                        if (i == route.routeParts.size() - 1) {
                            item.addDestinationAlighting(demand);
                        }
                        break;
                    }

                    if (record) {
                        item.addDemand(demand);
                    }
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        LOG.info("Writing Umlego-Belastungsteppich entries to database...");

        for (TrainNo trainNo : this.journeyByTrainNo.keySet()) {
            var journey = this.journeyByTrainNo.get(trainNo);
            for (int i = 0; i < journey.items().size(); i++) {
                JourneyItem item = journey.items().get(i);
                JourneyItem nextItem = i + 1 < journey.items().size() ? journey.items().get(i + 1) : null;

                BelastungsteppichEntry entry = new BelastungsteppichEntry(
                    runId,
                    targetDate,
                    journey.trainNo().operatorCode(),
                    Integer.parseInt(journey.trainNo().departureId().toString()),
                    Integer.parseInt(journey.trainNo().departureId().toString()),
                    // Assuming train_no is same as departureId
                    item.getIndex(),
                    Time.writeTime(item.getArrival()),
                    Time.writeTime(item.getDeparture()),
                    nextItem == null ? null : Time.writeTime(nextItem.getArrival()),
                    Integer.parseInt(item.getFromStopFacilityId().toString()),
                    item.getToStopFacilityId() == null ? null : Integer.parseInt(item.getToStopFacilityId().toString()),
                    item.getVolume(),
                    item.getBoarding(),
                    item.getAlighting(),
                    item.getOriginBoarding(),
                    item.getDestinationAlighting()
                );
                entries.add(entry);
            }
        }
        repository.insertEntries(connection, entries);
        closeConnection(connection);
        LOG.info("Done writing Belastungsteppiche entries.");
    }
}
