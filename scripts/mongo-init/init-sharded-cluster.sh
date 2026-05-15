#!/bin/bash
set -e

echo "Waiting for config server..."
until mongosh --host mongo-config-1 --port 27019 --eval "db.adminCommand('ping')" > /dev/null 2>&1; do
  echo "Config server not ready, waiting..."
  sleep 2
done

echo "Initializing config server replica set..."
mongosh --host mongo-config-1 --port 27019 <<EOF
rs.initiate({
  _id: "configsvr",
  configsvr: true,
  members: [
    { _id: 0, host: "mongo-config-1:27019" }
  ]
})
EOF

echo "Waiting for config server to become primary..."
sleep 5

echo "Waiting for shard 1 primary..."
until mongosh --host mongo-shard-1a --port 27018 --eval "db.adminCommand('ping')" > /dev/null 2>&1; do
  echo "Shard 1 not ready, waiting..."
  sleep 2
done

echo "Initializing shard 1 replica set..."
mongosh --host mongo-shard-1a --port 27018 <<EOF
rs.initiate({
  _id: "shard1",
  members: [
    { _id: 0, host: "mongo-shard-1a:27018" },
    { _id: 1, host: "mongo-shard-1b:27018" }
  ]
})
EOF

echo "Waiting for shard 1 to become primary..."
sleep 5

echo "Waiting for shard 2 primary..."
until mongosh --host mongo-shard-2a --port 27018 --eval "db.adminCommand('ping')" > /dev/null 2>&1; do
  echo "Shard 2 not ready, waiting..."
  sleep 2
done

echo "Initializing shard 2 replica set..."
mongosh --host mongo-shard-2a --port 27018 <<EOF
rs.initiate({
  _id: "shard2",
  members: [
    { _id: 0, host: "mongo-shard-2a:27018" },
    { _id: 1, host: "mongo-shard-2b:27018" }
  ]
})
EOF

echo "Waiting for shard 2 to become primary..."
sleep 5

echo "Waiting for mongos..."
until mongosh --host mongos --port 27017 --eval "db.adminCommand('ping')" > /dev/null 2>&1; do
  echo "Mongos not ready, waiting..."
  sleep 2
done

echo "Adding shards to cluster..."
mongosh --host mongos --port 27017 <<EOF
sh.addShard("shard1/mongo-shard-1a:27018,mongo-shard-1b:27018")
sh.addShard("shard2/mongo-shard-2a:27018,mongo-shard-2b:27018")
EOF

echo "Waiting for shards to be added..."
sleep 10

echo "Enabling sharding for database..."
mongosh --host mongos --port 27017 <<EOF
sh.enableSharding("pingine")
EOF

echo "Creating collection and indexes..."
mongosh --host mongos --port 27017 <<EOF
use pingine

// Create collection with sharding
db.createCollection("telemetry_points")

// Create shard key on vehicleId for optimal distribution
sh.shardCollection("pingine.telemetry_points", { "vehicleId": "hashed" })

// Create compound index for queries sorted by vehicleId and ts
db.telemetry_points.createIndex(
  { "vehicleId": 1, "ts": -1 },
  { name: "vehicle_ts_idx" }
)

// Create index for time-based queries across all vehicles
db.telemetry_points.createIndex(
  { "ts": -1 },
  { name: "ts_idx" }
)

// Create index for ignition status queries
db.telemetry_points.createIndex(
  { "vehicleId": 1, "ignition": 1, "ts": -1 },
  { name: "vehicle_ignition_ts_idx" }
)

print("MongoDB sharded cluster initialization completed!")
EOF

echo "Verification..."
mongosh --host mongos --port 27017 --eval "sh.status()"