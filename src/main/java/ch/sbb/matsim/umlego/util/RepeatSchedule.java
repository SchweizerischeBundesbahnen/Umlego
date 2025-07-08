package ch.sbb.matsim.umlego.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.ChainedDepartureImpl;
import org.matsim.pt.transitSchedule.api.ChainedDeparture;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepeatSchedule {

	/**
	 * Duplicates all departures between 0 and <code>limit</code> with the specified <code>offset</code>.
	 *
	 * This allows to repeat the departures early in the morning (e.g. limit = 6*3600) after one day (offset = 24*3600).
	 */
	public static void repeat(Scenario scenario, double limit, double offset, String suffix) {
		TransitScheduleFactory sf = scenario.getTransitSchedule().getFactory();
		VehiclesFactory vf = scenario.getTransitVehicles().getFactory();

		Map<Id<TransitLine>, Map<Id<TransitRoute>, Map<Id<Departure>, Departure>>> duplicatedDepartures = new HashMap<>();


		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				Map<Id<Departure>, Departure> existingDepartures = new HashMap<>(route.getDepartures());
				for (Departure departure : existingDepartures.values()) {
					if (departure.getDepartureTime() <= limit) {
						Vehicle existingVehicle = scenario.getTransitVehicles().getVehicles().get(departure.getVehicleId());
						double newDepartureTime = departure.getDepartureTime() + offset;

						Vehicle vehicle = vf.createVehicle(Id.create(existingVehicle.getId().toString() + suffix, Vehicle.class), existingVehicle.getType());
						scenario.getTransitVehicles().addVehicle(vehicle);

						Departure newDeparture = sf.createDeparture(Id.create(departure.getId().toString() + suffix, Departure.class), newDepartureTime);
						newDeparture.setVehicleId(vehicle.getId());
						route.addDeparture(newDeparture);

						duplicatedDepartures.computeIfAbsent(line.getId(), k -> new HashMap<>())
								.computeIfAbsent(route.getId(), k -> new HashMap<>())
								.put(departure.getId(), newDeparture);
					}
				}
			}
		}

		// check if we have to add some chained departures

		duplicatedDepartures.forEach((lineId, routes) -> {
			TransitLine line = scenario.getTransitSchedule().getTransitLines().get(lineId);
			routes.forEach((routeId, departures) -> {
				TransitRoute route = line.getRoutes().get(routeId);
				for (Map.Entry<Id<Departure>, Departure> entry : departures.entrySet()) {
					Id<Departure> oldDepartureId = entry.getKey();
					Departure newDeparture = entry.getValue();
					Departure oldDeparture = route.getDepartures().get(oldDepartureId);
					List<ChainedDeparture> newChainedDepartures = new ArrayList<>();

					for (ChainedDeparture chainedDeparture : oldDeparture.getChainedDepartures()) {
						var linesDuplicates = duplicatedDepartures.get(chainedDeparture.getChainedTransitLineId());
						if (linesDuplicates == null) {
							continue;
						}
						var routesDuplicates = linesDuplicates.get(chainedDeparture.getChainedRouteId());
						if (routesDuplicates == null) {
							continue;
						}
						var newChainedDeparture = routesDuplicates.get(chainedDeparture.getChainedDepartureId());
						if (newChainedDeparture != null) {
							newChainedDepartures.add(new ChainedDepartureImpl(chainedDeparture.getChainedTransitLineId(), chainedDeparture.getChainedRouteId(), newChainedDeparture.getId()));
						}
					}
					if (!newChainedDepartures.isEmpty()) {
						newDeparture.setChainedDepartures(newChainedDepartures);
					}
				}
			});
		}	);
	}

}
