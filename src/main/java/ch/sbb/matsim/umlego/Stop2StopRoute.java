package ch.sbb.matsim.umlego;

import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Stop2StopRoute {
    public final TransitStopFacility originStop;
    public final TransitStopFacility destinationStop;
    public final double depTime;
    public final double arrTime;
    public final double travelTimeWithoutAccess;
    public final int transfers;
    public final double distance;
    public final List<RaptorRoute.RoutePart> routeParts = new ArrayList<>();

    public Stop2StopRoute(RaptorRoute route) {
        double firstDepTime = Double.NaN;
        double lastArrTime = Double.NaN;

        TransitStopFacility originStopFacility = null;
        TransitStopFacility destinationStopFacility = null;

        double distanceSum = 0;
        RaptorRoute.RoutePart prevTransfer = null;
        int stageCount = 0;
        for (RaptorRoute.RoutePart part : route.getParts()) {
            if (part.line == null) {
                // it is a transfer
                prevTransfer = part;
                // still update the destination stop in case we arrive the destination by a transfer / walk-link
                destinationStopFacility = part.toStop;
            } else {
                stageCount++;
                if (routeParts.isEmpty()) {
                    // it is the first real stage
                    firstDepTime = part.vehicleDepTime;
                    originStopFacility = part.fromStop;
                } else if (prevTransfer != null) {
                    this.routeParts.add(prevTransfer);
                }
                this.routeParts.add(part);
                lastArrTime = part.arrivalTime;
                destinationStopFacility = part.toStop;
                distanceSum += part.distance;
            }
        }
        this.originStop = originStopFacility;
        this.destinationStop = destinationStopFacility;
        this.depTime = firstDepTime;
        this.arrTime = lastArrTime;
        this.travelTimeWithoutAccess = this.arrTime - this.depTime;
        this.transfers = stageCount - 1;
        this.distance = distanceSum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Stop2StopRoute that = (Stop2StopRoute) o;
        boolean isEqual = Double.compare(depTime, that.depTime) == 0
                && Double.compare(arrTime, that.arrTime) == 0
                && transfers == that.transfers
                && Objects.equals(originStop.getId(), that.originStop.getId())
                && Objects.equals(destinationStop.getId(), that.destinationStop.getId());
        if (isEqual) {
            // also check route parts
            for (int i = 0; i < routeParts.size(); i++) {
                RaptorRoute.RoutePart routePartThis = this.routeParts.get(i);
                RaptorRoute.RoutePart routePartThat = that.routeParts.get(i);

                boolean partIsEqual =
                        ((routePartThis.line == null && routePartThat.line == null) || (routePartThis.line != null
                                && routePartThat.line != null && Objects.equals(routePartThis.line.getId(),
                                routePartThat.line.getId())))
                                && ((routePartThis.route == null && routePartThat.route == null) || (
                                routePartThis.route != null && routePartThat.route != null && Objects.equals(
                                        routePartThis.route.getId(), routePartThat.route.getId())));
                if (!partIsEqual) {
                    return false;
                }
            }
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(depTime, arrTime, transfers);
    }

    public String getRouteAsString() {
        StringBuilder details = new StringBuilder();
        for (RaptorRoute.RoutePart part : this.routeParts) {
            if (part.line == null) {
                continue;
            }
            if (!details.isEmpty()) {
                details.append(", ");
            }
            details.append(getPartString(part));
            while (part.chainedPart != null) {
                part = part.chainedPart;
                details.append(" => ");
                details.append(getPartString(part));
            }
        }
        return details.toString();
    }

    private String getPartString(RaptorRoute.RoutePart part) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(part.line.getId());
        stringBuilder.append(" (");
        stringBuilder.append(part.route.getId());
        stringBuilder.append(") ");
        stringBuilder.append(": ");
        stringBuilder.append(part.fromStop.getName());
        stringBuilder.append(' ');
        stringBuilder.append(Time.writeTime(part.vehicleDepTime));
        stringBuilder.append(" - ");
        stringBuilder.append(part.toStop.getName());
        stringBuilder.append(' ');
        stringBuilder.append(Time.writeTime(part.arrivalTime));
        return stringBuilder.toString();
    }
}
