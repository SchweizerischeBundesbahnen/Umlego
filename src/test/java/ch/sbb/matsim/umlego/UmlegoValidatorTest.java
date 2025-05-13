package ch.sbb.matsim.umlego;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UmlegoValidatorTest {

    @Test
    void testValidUnroutableDemandProportionWithin90Days() {
        LocalDate targetDate = LocalDate.now().plusDays(30);
        double percentUnroutableDemand = 0.9;
        double unroutableZoneDemand = 1000.0;

        assertTrue(UmlegoValidator.isValid(targetDate, percentUnroutableDemand, unroutableZoneDemand));
    }

    @Test
    void testInvalidUnroutableDemandProportionWithin90Days() {
        LocalDate targetDate = LocalDate.now().plusDays(30);
        double percentUnroutableDemand = 1.1; // Exceeds the limit for <=90 days
        double unroutableZoneDemand = 1000.0;

        assertFalse(UmlegoValidator.isValid(targetDate, percentUnroutableDemand, unroutableZoneDemand));
    }

    @Test
    void testValidUnroutableDemandProportionAfter90Days() {
        LocalDate targetDate = LocalDate.now().plusDays(120);
        double percentUnroutableDemand = 1.4;
        double unroutableZoneDemand = 1000.0;

        assertTrue(UmlegoValidator.isValid(targetDate, percentUnroutableDemand, unroutableZoneDemand));
    }

    @Test
    void testInvalidUnroutableDemandProportionAfter90Days() {
        LocalDate targetDate = LocalDate.now().plusDays(120);
        double percentUnroutableDemand = 1.6;
        double unroutableZoneDemand = 1000.0;

        assertFalse(UmlegoValidator.isValid(targetDate, percentUnroutableDemand, unroutableZoneDemand));
    }

    @Test
    void testValidUnroutableZoneDemand() {
        LocalDate targetDate = LocalDate.now().plusDays(30);
        double percentUnroutableDemand = 0.9;
        double unroutableZoneDemand = 1500.0;

        assertTrue(UmlegoValidator.isValid(targetDate, percentUnroutableDemand, unroutableZoneDemand));
    }

    @Test
    void testInvalidUnroutableZoneDemand() {
        LocalDate targetDate = LocalDate.now().plusDays(30);
        double percentUnroutableDemand = 0.9;
        double unroutableZoneDemand = 2000.0;

        assertFalse(UmlegoValidator.isValid(targetDate, percentUnroutableDemand, unroutableZoneDemand));
    }

    @Test
    void testInvalidPastDate() {
        LocalDate targetDate = LocalDate.now().minusDays(10);
        double percentUnroutableDemand = 0.9;
        double unroutableZoneDemand = 1000.0;

        assertFalse(UmlegoValidator.isValid(targetDate, percentUnroutableDemand, unroutableZoneDemand));
    }
}
