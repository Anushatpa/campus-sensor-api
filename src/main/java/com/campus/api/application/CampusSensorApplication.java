package com.campus.api.application;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 * Sets the base URI path for all API resources to /api/v1
 */
@ApplicationPath("/api/v1")
public class CampusSensorApplication extends Application {
    // Jersey auto-discovers all resources via package scanning configured in Main.java
}
