package com.campus.api.resources;

import com.campus.api.application.DataStore;
import com.campus.api.exceptions.RoomNotEmptyException;
import com.campus.api.models.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());

        Map<String, Object> response = new HashMap<>();
        response.put("count", roomList.size());
        response.put("rooms", roomList);
        response.put("links", new Map[]{
            buildLink("self", "/api/v1/rooms", "GET"),
            buildLink("create", "/api/v1/rooms", "POST")
        });

        return Response.ok(response).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getName() == null || room.getName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room name is required"))
                    .build();
        }

        store.getRooms().put(room.getId(), room);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room created successfully");
        response.put("room", room);
        response.put("links", new Map[]{
            buildLink("self", "/api/v1/rooms/" + room.getId(), "GET"),
            buildLink("delete", "/api/v1/rooms/" + room.getId(), "DELETE"),
            buildLink("all-rooms", "/api/v1/rooms", "GET")
        });

        return Response.created(URI.create("/api/v1/rooms/" + room.getId()))
                .entity(response)
                .build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", "Room with ID '" + roomId + "' does not exist."
                    ))
                    .build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("room", room);
        response.put("links", new Map[]{
            buildLink("self", "/api/v1/rooms/" + roomId, "GET"),
            buildLink("delete", "/api/v1/rooms/" + roomId, "DELETE"),
            buildLink("all-rooms", "/api/v1/rooms", "GET")
        });

        return Response.ok(response).build();
    }
    
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        // 404 if room doesn't exist 
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", "Room with ID '" + roomId + "' does not exist or has already been deleted."
                    ))
                    .build();
        }

        // Room must have no sensors before deletion
        if (store.roomHasSensors(roomId)) {
            throw new RoomNotEmptyException(
                "Cannot delete room '" + room.getName() + "' (ID: " + roomId + "). " +
                "It still has active sensors assigned. Please reassign or delete sensors first."
            );
        }

        store.getRooms().remove(roomId);

        return Response.ok(Map.of(
            "message", "Room '" + room.getName() + "' deleted successfully.",
            "deletedRoomId", roomId
        )).build();
    }

    private Map<String, Object> buildLink(String rel, String href, String method) {
        Map<String, Object> link = new HashMap<>();
        link.put("rel", rel);
        link.put("href", href);
        link.put("method", method);
        return link;
    }
}
