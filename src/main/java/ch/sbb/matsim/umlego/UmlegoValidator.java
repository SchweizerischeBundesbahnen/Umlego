package ch.sbb.matsim.umlego;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class UmlegoValidator {

    private UmlegoValidator() {
    }

    private final static int d1 = 90;
    private final static double x1 = 1.0;
    private final static double x2 = 1.5;
    private final static double y = 2000.0;

    public static boolean isValid(LocalDate targetDate, Double percentUnroutableDemand, Double unroutableZoneDemand) {
        return isUnroutableDemandProportionValid(targetDate, percentUnroutableDemand) && isUnroutableZoneDemandValid(
                unroutableZoneDemand);
    }

    private static boolean isUnroutableDemandProportionValid(LocalDate targetDate, double percentUnroutableDemand) {
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);

        if (daysBetween >= 0 && daysBetween <= d1) {
            return percentUnroutableDemand <= x1;
        } else if (daysBetween > d1) {
            return percentUnroutableDemand <= x2;
        }

        return false;
    }

    private static boolean isUnroutableZoneDemandValid(Double unroutableZoneDemand) {
        if (unroutableZoneDemand == null) {
            return true;
        }
        return !(unroutableZoneDemand >= y);
    }

}
