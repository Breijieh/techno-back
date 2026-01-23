package com.techno.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * GPS Calculator Utility Class
 *
 * Provides GPS-related calculations for the Attendance System, including:
 * - Distance calculation using Haversine formula
 * - GPS coordinate validation
 * - Radius-based proximity checking
 *
 * Used for validating employee check-in/out locations against project GPS coordinates.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 4 - Attendance System
 */
@Slf4j
public class GPSCalculator {

    /**
     * Earth's mean radius in meters
     * Used in Haversine formula calculations
     */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * Valid latitude range: -90 to +90 degrees
     */
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;

    /**
     * Valid longitude range: -180 to +180 degrees
     */
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private GPSCalculator() {
        throw new UnsupportedOperationException("هذه فئة مساعدة ولا يمكن إنشاء مثيل منها");
    }

    /**
     * Calculates the distance between two GPS coordinates using the Haversine formula.
     *
     * The Haversine formula determines the great-circle distance between two points
     * on a sphere given their longitudes and latitudes. This is the shortest distance
     * over the earth's surface.
     *
     * Formula:
     * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
     * c = 2 * atan2(√a, √(1-a))
     * distance = R * c
     *
     * @param latitude1 Latitude of first point in decimal degrees
     * @param longitude1 Longitude of first point in decimal degrees
     * @param latitude2 Latitude of second point in decimal degrees
     * @param longitude2 Longitude of second point in decimal degrees
     * @return Distance in meters between the two points
     * @throws IllegalArgumentException if any coordinate is null or invalid
     */
    public static Double calculateDistance(BigDecimal latitude1, BigDecimal longitude1,
                                          BigDecimal latitude2, BigDecimal longitude2) {

        // Validate input parameters
        if (latitude1 == null || longitude1 == null || latitude2 == null || longitude2 == null) {
            log.error("Cannot calculate distance: one or more coordinates are null");
            throw new IllegalArgumentException("يجب ألا تكون إحداثيات GPS فارغة");
        }

        double lat1 = latitude1.doubleValue();
        double lon1 = longitude1.doubleValue();
        double lat2 = latitude2.doubleValue();
        double lon2 = longitude2.doubleValue();

        // Validate coordinate ranges
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            log.error("Invalid GPS coordinates: Point1({}, {}), Point2({}, {})", lat1, lon1, lat2, lon2);
            throw new IllegalArgumentException("إحداثيات GPS خارج النطاق المسموح");
        }

        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calculate differences
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Calculate distance
        double distanceMeters = EARTH_RADIUS_METERS * c;

        log.debug("Calculated distance: {} meters between ({}, {}) and ({}, {})",
                String.format("%.2f", distanceMeters), lat1, lon1, lat2, lon2);

        return distanceMeters;
    }

    /**
     * Checks if an employee's GPS location is within the allowed radius of a project location.
     *
     * This method is used during check-in/out to verify that the employee is physically
     * present at the project site.
     *
     * @param employeeLatitude Employee's current latitude
     * @param employeeLongitude Employee's current longitude
     * @param projectLatitude Project site's latitude
     * @param projectLongitude Project site's longitude
     * @param allowedRadiusMeters Maximum allowed distance in meters
     * @return true if employee is within the allowed radius, false otherwise
     * @throws IllegalArgumentException if any coordinate is null or invalid, or radius is negative
     */
    public static boolean isWithinRadius(BigDecimal employeeLatitude, BigDecimal employeeLongitude,
                                        BigDecimal projectLatitude, BigDecimal projectLongitude,
                                        Integer allowedRadiusMeters) {

        if (allowedRadiusMeters == null || allowedRadiusMeters < 0) {
            log.error("Invalid radius: {}", allowedRadiusMeters);
            throw new IllegalArgumentException("يجب أن يكون النطاق المسموح غير فارغ وغير سالب");
        }

        try {
            double actualDistance = calculateDistance(
                    employeeLatitude, employeeLongitude,
                    projectLatitude, projectLongitude
            );

            boolean withinRadius = actualDistance <= allowedRadiusMeters;

            log.info("GPS Check: Employee at ({}, {}), Project at ({}, {}), Distance: {:.2f}m, Allowed: {}m, Result: {}",
                    employeeLatitude, employeeLongitude,
                    projectLatitude, projectLongitude,
                    actualDistance, allowedRadiusMeters,
                    withinRadius ? "WITHIN RADIUS" : "OUTSIDE RADIUS");

            return withinRadius;

        } catch (IllegalArgumentException e) {
            log.error("Failed to validate GPS radius: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validates if GPS coordinates are within valid ranges.
     *
     * Valid ranges:
     * - Latitude: -90 to +90 degrees
     * - Longitude: -180 to +180 degrees
     *
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return true if both coordinates are valid, false otherwise
     */
    public static boolean isValidCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            log.warn("GPS validation failed: null coordinates");
            return false;
        }

        return isValidCoordinate(latitude.doubleValue(), longitude.doubleValue());
    }

    /**
     * Internal helper method to validate coordinate ranges.
     *
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return true if both coordinates are within valid ranges
     */
    private static boolean isValidCoordinate(double latitude, double longitude) {
        boolean validLat = latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE;
        boolean validLon = longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;

        if (!validLat) {
            log.warn("Invalid latitude: {} (must be between {} and {})", latitude, MIN_LATITUDE, MAX_LATITUDE);
        }
        if (!validLon) {
            log.warn("Invalid longitude: {} (must be between {} and {})", longitude, MIN_LONGITUDE, MAX_LONGITUDE);
        }

        return validLat && validLon;
    }

    /**
     * Formats distance in meters to a human-readable string.
     *
     * @param distanceMeters Distance in meters
     * @return Formatted string (e.g., "1.5 km" or "350 m")
     */
    public static String formatDistance(Double distanceMeters) {
        if (distanceMeters == null) {
            return "N/A";
        }

        if (distanceMeters >= 1000) {
            return String.format("%.2f km", distanceMeters / 1000);
        } else {
            return String.format("%.0f m", distanceMeters);
        }
    }
}