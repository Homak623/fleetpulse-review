package com.pingine.fleetpulse.domain;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class Trip {
    String vehicleId;
    Instant startedAt;
    Instant endedAt;
    double distanceKm;
    double avgSpeedKph;
    List<TripPoint> points;

    @Value
    @Builder
    public static class TripPoint {
        Instant ts;
        double lat;
        double lon;
        double speedKph;
    }
}
