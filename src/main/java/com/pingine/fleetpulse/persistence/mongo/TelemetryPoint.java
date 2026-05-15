package com.pingine.fleetpulse.persistence.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.time.LocalDateTime;

@Document(collection = "telemetry_points")
@Sharded(shardKey = {"vehicleId"})
@Getter
@Setter
@CompoundIndexes({
        @CompoundIndex(name = "vehicle_ts_idx", def = "{'vehicleId': 1, 'ts': -1}"),
        @CompoundIndex(name = "vehicle_ignition_ts_idx", def = "{'vehicleId': 1, 'ignition': 1, 'ts': -1}")
})
public class TelemetryPoint {

    @Id
    private String id;

    @Indexed
    private String vehicleId;

    @Indexed
    private LocalDateTime ts;

    private double lat;
    private double lon;

    @Field("loc")
    private GeoJsonPoint location;

    private double speed;

    @Indexed
    private boolean ignition;
}