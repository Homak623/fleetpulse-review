package com.pingine.fleetpulse.service.trip;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripDetectorTest {

    private final TripDetector detector = new TripDetector();

    @Test
    //@Disabled("Enable and make this pass.")
    void detectsSingleTripFromIgnitionOnToOff() {
        LocalDateTime t0 = LocalDateTime.parse("2026-04-27T08:00:00");
        List<TelemetryPoint> points = List.of(
                point("v1", t0,                 52.5200, 13.4050,  0.0, true),
                point("v1", t0.plusMinutes(5),  52.5300, 13.4200, 45.0, true),
                point("v1", t0.plusMinutes(10), 52.5450, 13.4400, 62.0, true),
                point("v1", t0.plusMinutes(20), 52.5600, 13.4700, 58.0, true),
                point("v1", t0.plusMinutes(30), 52.5700, 13.5000,  0.0, false)
        );

        List<Trip> trips = detector.detect(points);

        assertThat(trips).hasSize(1);
        Trip trip = trips.get(0);
        assertThat(trip.getVehicleId()).isEqualTo("v1");
        assertThat(trip.getPoints()).hasSize(5);
        assertThat(trip.getDistanceKm()).isGreaterThan(0.0);
        assertThat(trip.getAvgSpeedKph()).isGreaterThan(0.0);
    }

    @Test
    //@Disabled("Enable and make this pass.")
    void handlesDuplicateTimestamps() {
        LocalDateTime t0 = LocalDateTime.parse("2026-04-27T08:00:00");
        List<TelemetryPoint> points = List.of(
                point("v1", t0,                 52.5200, 13.4050,  0.0, true),
                point("v1", t0.plusMinutes(5),  52.5300, 13.4200, 45.0, true),
                point("v1", t0.plusMinutes(5),  52.5300, 13.4200, 45.0, true),
                point("v1", t0.plusMinutes(10), 52.5450, 13.4400, 62.0, true),
                point("v1", t0.plusMinutes(20), 52.5700, 13.5000,  0.0, false)
        );

        List<Trip> trips = detector.detect(points);

        assertThat(trips).hasSize(1);
        Trip trip = trips.get(0);
        assertThat(trip.getPoints()).hasSize(4);
    }

    private static TelemetryPoint point(String vehicleId, LocalDateTime ts,
                                        double lat, double lon, double speed, boolean ignition) {
        TelemetryPoint p = new TelemetryPoint();
        p.setVehicleId(vehicleId);
        p.setTs(ts);
        p.setLat(lat);
        p.setLon(lon);
        p.setSpeed(speed);
        p.setIgnition(ignition);
        return p;
    }
}
