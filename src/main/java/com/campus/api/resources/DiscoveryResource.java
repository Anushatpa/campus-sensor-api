package com.campus.api.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> discovery = new HashMap<>();

        discovery.put("apiName", "Campus Sensor Management API");
        discovery.put("version", "1.0.0");
        discovery.put("apiPath", "/api/v1");

        Map<String, String> contact = new HashMap<>();
        contact.put("name", "Campus IT Administration");
        contact.put("email", "admin@campus.ac.uk");
        contact.put("department", "Facilities & IoT Infrastructure");
        discovery.put("contact", contact);

        Map<String, String> resources = new HashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        discovery.put("resources", resources);

        Map<String, Object>[] links = new Map[]{
            buildLink("self", "/api/v1", "GET"),
            buildLink("rooms", "/api/v1/rooms", "GET"),
            buildLink("sensors", "/api/v1/sensors", "GET")
        };
        discovery.put("links", links);

        discovery.put("description",
            "RESTful API for managing campus IoT sensor rooms and readings.");

        return Response.ok(discovery).build();
    }

    private Map<String, Object> buildLink(String rel, String href, String method) {
        Map<String, Object> link = new HashMap<>();
        link.put("rel", rel);
        link.put("href", href);
        link.put("method", method);
        return link;
    }
}
