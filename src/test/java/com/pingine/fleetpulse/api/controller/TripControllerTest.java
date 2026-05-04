package com.pingine.fleetpulse.api.controller;

import com.pingine.fleetpulse.api.TripController;
import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.service.TripNotFoundException;
import com.pingine.fleetpulse.service.TripService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    private static final String VEHICLE_ID = "test-vehicle-123";
    private static final String NON_EXISTENT_ID = "missing-404";

    @Test
    void getLastTripShouldReturnTripResponseWhenTripExists() throws Exception {
        TripResponse expectedResponse = createMockResponse();
        when(tripService.getLastTrip(VEHICLE_ID)).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/last-trip", VEHICLE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vehicle.id").value(VEHICLE_ID))
                .andExpect(jsonPath("$.vehicle.model").value("Mercedes Actros"))
                .andExpect(jsonPath("$.distanceKm").value(9.49))
                .andExpect(jsonPath("$.avgSpeedKph").value(23.33))
                .andExpect(jsonPath("$.pointCount").value(2))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points[0].speedKph").value(0.0))
                .andExpect(jsonPath("$.points[1].speedKph").value(70.0));
    }

    @Test
    void getLastTripShouldReturn404WhenNotFound() throws Exception {
        when(tripService.getLastTrip(NON_EXISTENT_ID))
                .thenThrow(new TripNotFoundException("No completed trips found for vehicle: " + NON_EXISTENT_ID));

        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/last-trip", NON_EXISTENT_ID))
                .andExpect(status().isNotFound());
    }

    private TripResponse createMockResponse() {
        VehicleResponse vehicle = VehicleResponse.builder()
                .id(VEHICLE_ID)
                .licensePlate("B-PG-1001")
                .model("Mercedes Actros")
                .vin("TESTVIN123")
                .driverName("Ivan Driver")
                .build();

        TripResponse.PointDto p1 = TripResponse.PointDto.builder()
                .ts(Instant.parse("2026-04-27T10:00:00Z"))
                .lat(52.57).lon(13.50).speedKph(0.0).build();
        TripResponse.PointDto p2 = TripResponse.PointDto.builder()
                .ts(Instant.parse("2026-04-27T10:15:00Z"))
                .lat(52.60).lon(13.55).speedKph(70.0).build();

        return TripResponse.builder()
                .vehicle(vehicle)
                .startedAt(Instant.parse("2026-04-27T10:00:00Z"))
                .endedAt(Instant.parse("2026-04-27T10:15:00Z"))
                .distanceKm(9.49)
                .avgSpeedKph(23.33)
                .pointCount(2)
                .points(List.of(p1, p2))
                .build();
    }
}