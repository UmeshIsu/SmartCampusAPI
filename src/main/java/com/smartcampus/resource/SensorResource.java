package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 – Sensor Operations & Linking Base path: /api/v1/sensors
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.INSTANCE;

    // ── GET /api/v1/sensors[?type=XX] ─────────────────────────────────────
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> list = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.isBlank()) {
            list = list.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(list).build();
    }

    // ── POST /api/v1/sensors ───────────────────────────────────────────────
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor id is required."))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Validate that the referenced room exists
        String roomId = sensor.getRoomId();
        if (roomId == null || !store.getRooms().containsKey(roomId)) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: the referenced roomId '" + roomId
                    + "' does not exist in the system. Create the room first."
            );
        }

        store.getSensors().put(sensor.getId(), sensor);
        // Register sensor id inside its room
        store.getRooms().get(roomId).addSensorId(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // ── GET /api/v1/sensors/{sensorId} ────────────────────────────────────
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ── DELETE /api/v1/sensors/{sensorId} ─────────────────────────────────
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }
        // Remove sensor reference from its parent room
        if (sensor.getRoomId() != null && store.getRooms().containsKey(sensor.getRoomId())) {
            store.getRooms().get(sensor.getRoomId()).removeSensorId(sensorId);
        }
        store.getSensors().remove(sensorId);
        return Response.ok(Map.of("message", "Sensor '" + sensorId + "' successfully deleted.")).build();
    }

    @PATCH
    @Path("/{sensorId}/status")
    public Response updateSensorStatus(
            @PathParam("sensorId") String sensorId,
            Map<String, String> body) {

        // Check sensor exists
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }

        // Validate the new status value from request body
        String newStatus = body == null ? null : body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Request body must contain a 'status' field."))
                    .build();
        }

        String upperStatus = newStatus.toUpperCase();
        if (!upperStatus.equals("ACTIVE")
                && !upperStatus.equals("MAINTENANCE")
                && !upperStatus.equals("OFFLINE")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "error", "Invalid status value '" + newStatus + "'.",
                            "allowed", "ACTIVE, MAINTENANCE, OFFLINE"
                    ))
                    .build();
        }

        // Apply the update
        sensor.setStatus(upperStatus);
        return Response.ok(sensor).build();
    }

    // ── Part 4: Sub-Resource Locator ───────────────────────────────────────
    /**
     * Delegates all /api/v1/sensors/{sensorId}/readings/* requests to
     * SensorReadingResource.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
