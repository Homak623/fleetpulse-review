package com.pingine.fleetpulse.service.trip;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Splits a stream of telemetry points into completed trips.
 * A trip starts on ignition=true and ends on the next ignition=false.
 */
@Component
public class TripDetector {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public List<Trip> detect(List<TelemetryPoint> points) {
        if (points == null || points.isEmpty()) {
            return Collections.emptyList();
        }

        List<TelemetryPoint> sorted = points.stream()
                .sorted(Comparator.comparing(TelemetryPoint::getTs))
                .collect(Collectors.toList());

        return extractTrips(sorted);
    }

    private List<Trip> extractTrips(List<TelemetryPoint> points) {
        List<Trip> trips = new ArrayList<>();
        TripAccumulator accumulator = null;

        for (TelemetryPoint point : points) {
            if (accumulator == null) {
                if (point.isIgnition()) {
                    accumulator = new TripAccumulator(point);
                }
            } else {
                if (accumulator.tryAdd(point) && !point.isIgnition()) {
                    trips.add(accumulator.build());
                    accumulator = null;
                }
            }
        }
        return trips;
    }

    private static final class TripAccumulator {
        private final List<TelemetryPoint> points = new ArrayList<>();
        private TelemetryPoint last;

        TripAccumulator(TelemetryPoint first) {
            points.add(first);
            last = first;
        }

        boolean tryAdd(TelemetryPoint point) {
            if (isDuplicate(point)) return false;
            points.add(point);
            last = point;
            return true;
        }

        private boolean isDuplicate(TelemetryPoint candidate) {
            return last != null
                    && candidate.getTs().equals(last.getTs())
                    && candidate.getVehicleId().equals(last.getVehicleId());
        }

        Trip build() {
            TelemetryPoint first = points.get(0);
            double distance = calculateTotalDistance(points);
            double avgSpeed = calculateAvgSpeed(distance, first.getTs(), last.getTs());

            return Trip.builder()
                    .vehicleId(first.getVehicleId())
                    .startedAt(toInstant(first.getTs()))
                    .endedAt(toInstant(last.getTs()))
                    .distanceKm(distance)
                    .avgSpeedKph(avgSpeed)
                    .points(toTripPoints(points))
                    .build();
        }
    }

    private static double calculateTotalDistance(List<TelemetryPoint> points) {
        double total = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            total += haversineKm(points.get(i), points.get(i + 1));
        }
        return total;
    }

    private static double haversineKm(TelemetryPoint p1, TelemetryPoint p2) {
        double dLat = Math.toRadians(p2.getLat() - p1.getLat());
        double dLon = Math.toRadians(p2.getLon() - p1.getLon());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(p1.getLat()))
                * Math.cos(Math.toRadians(p2.getLat()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double calculateAvgSpeed(double distanceKm,
                                            LocalDateTime start,
                                            LocalDateTime end) {
        long seconds = Duration.between(start, end).getSeconds();
        return seconds <= 0 ? 0.0 : distanceKm / (seconds / 3600.0);
    }

    private static List<Trip.TripPoint> toTripPoints(List<TelemetryPoint> points) {
        return points.stream()
                .map(p -> Trip.TripPoint.builder()
                        .ts(toInstant(p.getTs()))
                        .lat(p.getLat())
                        .lon(p.getLon())
                        .speedKph(p.getSpeed())
                        .build())
                .collect(Collectors.toList());
    }

    private static Instant toInstant(LocalDateTime dt) {
        return dt.toInstant(ZoneOffset.UTC);
    }
}