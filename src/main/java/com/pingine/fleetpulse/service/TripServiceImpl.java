package com.pingine.fleetpulse.service;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import com.pingine.fleetpulse.persistence.mongo.TelemetryRepository;
import com.pingine.fleetpulse.service.trip.TripDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TelemetryRepository telemetryRepository;
    private final TripDetector tripDetector;
    private final VehicleService vehicleService;

    @Value("${fleet-pulse.trip.points-limit}")
    private int pointsLimit;

    @Override
    public TripResponse getLastTrip(String vehicleId) {
        log.info("Fetching last completed trip for vehicle: {}", vehicleId);

        VehicleResponse vehicle = vehicleService.getById(vehicleId);
        List<TelemetryPoint> points = fetchTelemetryPoints(vehicleId);
        Trip lastTrip = findLastCompletedTrip(points, vehicleId);

        return buildTripResponse(lastTrip, vehicle);
    }

    private List<TelemetryPoint> fetchTelemetryPoints(String vehicleId) {
        List<TelemetryPoint> points = telemetryRepository.findRecentPoints(vehicleId, pointsLimit);

        if (points.isEmpty()) {
            log.warn("No telemetry data found for vehicle: {}", vehicleId);
            throw new TripNotFoundException("No telemetry data for vehicle: " + vehicleId);
        }

        return points;
    }

    private Trip findLastCompletedTrip(List<TelemetryPoint> points, String vehicleId) {
        List<Trip> trips = tripDetector.detect(points);

        if (trips.isEmpty()) {
            log.warn("No completed trips detected for vehicle: {}", vehicleId);
            throw new TripNotFoundException("No completed trips found for vehicle: " + vehicleId);
        }

        return trips.get(trips.size() - 1);
    }

    private TripResponse buildTripResponse(Trip trip, VehicleResponse vehicle) {
        List<TripResponse.PointDto> pointDtos = trip.getPoints().stream()
                .map(this::mapToPointDto)
                .collect(Collectors.toList());;

        return TripResponse.builder()
                .vehicle(vehicle)
                .startedAt(trip.getStartedAt())
                .endedAt(trip.getEndedAt())
                .distanceKm(trip.getDistanceKm())
                .avgSpeedKph(trip.getAvgSpeedKph())
                .pointCount(pointDtos.size())
                .points(pointDtos)
                .build();
    }

    private TripResponse.PointDto mapToPointDto(Trip.TripPoint point) {
        return TripResponse.PointDto.builder()
                .ts(point.getTs())
                .lat(point.getLat())
                .lon(point.getLon())
                .speedKph(point.getSpeedKph())
                .build();
    }
}