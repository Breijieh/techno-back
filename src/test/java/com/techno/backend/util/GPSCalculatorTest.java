package com.techno.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive unit tests for GPSCalculator utility class.
 * Tests all GPS-related calculations including distance, validation, and radius checks.
 *
 * @author Techno HR System
 * @version 1.0
 */
@DisplayName("GPS Calculator Tests")
class GPSCalculatorTest {

    // Test coordinates: Kempinski Hotel, Riyadh
    private static final BigDecimal KEMPINSKI_LAT = new BigDecimal("24.664417");
    private static final BigDecimal KEMPINSKI_LON = new BigDecimal("46.674198");

    // Test coordinates: Nearby location (within 500m)
    private static final BigDecimal NEARBY_LAT = new BigDecimal("24.665000");
    private static final BigDecimal NEARBY_LON = new BigDecimal("46.674500");

    // Test coordinates: Far location (outside 500m)
    private static final BigDecimal FAR_LAT = new BigDecimal("24.670000");
    private static final BigDecimal FAR_LON = new BigDecimal("46.680000");

    // ==================== Distance Calculation Tests ====================

    @Test
    @DisplayName("Calculate distance between two identical points should return 0")
    void calculateDistance_SamePoint_ReturnsZero() {
        Double distance = GPSCalculator.calculateDistance(
                KEMPINSKI_LAT, KEMPINSKI_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON
        );

        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Calculate distance between Kempinski and nearby location")
    void calculateDistance_KempinskiToNearby_ReturnsCorrectDistance() {
        Double distance = GPSCalculator.calculateDistance(
                KEMPINSKI_LAT, KEMPINSKI_LON,
                NEARBY_LAT, NEARBY_LON
        );

        // Should be approximately 100-200 meters
        assertThat(distance).isGreaterThan(50.0);
        assertThat(distance).isLessThan(500.0);
    }

    @Test
    @DisplayName("Calculate distance between Kempinski and far location")
    void calculateDistance_KempinskiToFar_ReturnsCorrectDistance() {
        Double distance = GPSCalculator.calculateDistance(
                KEMPINSKI_LAT, KEMPINSKI_LON,
                FAR_LAT, FAR_LON
        );

        // Should be more than 800m (actual distance is ~853.88m)
        assertThat(distance).isGreaterThan(800.0);
        assertThat(distance).isLessThan(1000.0);
    }

    @Test
    @DisplayName("Calculate distance with null coordinates should throw exception")
    void calculateDistance_NullCoordinates_ThrowsException() {
        assertThatThrownBy(() -> GPSCalculator.calculateDistance(
                null, KEMPINSKI_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("فارغة");

        assertThatThrownBy(() -> GPSCalculator.calculateDistance(
                KEMPINSKI_LAT, null,
                KEMPINSKI_LAT, KEMPINSKI_LON
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> GPSCalculator.calculateDistance(
                KEMPINSKI_LAT, KEMPINSKI_LON,
                null, KEMPINSKI_LON
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> GPSCalculator.calculateDistance(
                KEMPINSKI_LAT, KEMPINSKI_LON,
                KEMPINSKI_LAT, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "91.0, 46.0",  // Latitude > 90
            "-91.0, 46.0", // Latitude < -90
            "24.0, 181.0", // Longitude > 180
            "24.0, -181.0" // Longitude < -180
    })
    @DisplayName("Calculate distance with invalid coordinates should throw exception")
    void calculateDistance_InvalidCoordinates_ThrowsException(double lat, double lon) {
        BigDecimal invalidLat = BigDecimal.valueOf(lat);
        BigDecimal invalidLon = BigDecimal.valueOf(lon);

        assertThatThrownBy(() -> GPSCalculator.calculateDistance(
                invalidLat, invalidLon,
                KEMPINSKI_LAT, KEMPINSKI_LON
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("خارج النطاق");
    }

    // ==================== Coordinate Validation Tests ====================

    @Test
    @DisplayName("Valid coordinates should return true")
    void isValidCoordinates_ValidCoordinates_ReturnsTrue() {
        assertThat(GPSCalculator.isValidCoordinates(KEMPINSKI_LAT, KEMPINSKI_LON)).isTrue();
        assertThat(GPSCalculator.isValidCoordinates(new BigDecimal("0.0"), new BigDecimal("0.0"))).isTrue();
        assertThat(GPSCalculator.isValidCoordinates(new BigDecimal("-90.0"), new BigDecimal("-180.0"))).isTrue();
        assertThat(GPSCalculator.isValidCoordinates(new BigDecimal("90.0"), new BigDecimal("180.0"))).isTrue();
    }

    @Test
    @DisplayName("Null coordinates should return false")
    void isValidCoordinates_NullCoordinates_ReturnsFalse() {
        assertThat(GPSCalculator.isValidCoordinates(null, KEMPINSKI_LON)).isFalse();
        assertThat(GPSCalculator.isValidCoordinates(KEMPINSKI_LAT, null)).isFalse();
        assertThat(GPSCalculator.isValidCoordinates(null, null)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "91.0, 46.674198",    // Latitude > 90
            "-91.0, 46.674198",   // Latitude < -90
            "24.664417, 181.0",   // Longitude > 180
            "24.664417, -181.0"   // Longitude < -180
    })
    @DisplayName("Invalid coordinate ranges should return false")
    void isValidCoordinates_InvalidRanges_ReturnsFalse(double lat, double lon) {
        assertThat(GPSCalculator.isValidCoordinates(
                BigDecimal.valueOf(lat),
                BigDecimal.valueOf(lon)
        )).isFalse();
    }

    // ==================== Radius Check Tests ====================

    @Test
    @DisplayName("Employee within radius should return true")
    void isWithinRadius_WithinRadius_ReturnsTrue() {
        Integer radiusMeters = 500;
        boolean result = GPSCalculator.isWithinRadius(
                NEARBY_LAT, NEARBY_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                radiusMeters
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Employee outside radius should return false")
    void isWithinRadius_OutsideRadius_ReturnsFalse() {
        Integer radiusMeters = 500;
        boolean result = GPSCalculator.isWithinRadius(
                FAR_LAT, FAR_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                radiusMeters
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Employee exactly at radius boundary should return true")
    void isWithinRadius_ExactlyAtBoundary_ReturnsTrue() {
        // Use same coordinates (distance = 0)
        Integer radiusMeters = 500;
        boolean result = GPSCalculator.isWithinRadius(
                KEMPINSKI_LAT, KEMPINSKI_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                radiusMeters
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Null radius should throw exception")
    void isWithinRadius_NullRadius_ThrowsException() {
        assertThatThrownBy(() -> GPSCalculator.isWithinRadius(
                NEARBY_LAT, NEARBY_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("غير فارغ");
    }

    @Test
    @DisplayName("Negative radius should throw exception")
    void isWithinRadius_NegativeRadius_ThrowsException() {
        assertThatThrownBy(() -> GPSCalculator.isWithinRadius(
                NEARBY_LAT, NEARBY_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                -100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("غير سالب");
    }

    @Test
    @DisplayName("Very small radius (50m) should work correctly")
    void isWithinRadius_VerySmallRadius_WorksCorrectly() {
        // Same location should be within 50m
        boolean result = GPSCalculator.isWithinRadius(
                KEMPINSKI_LAT, KEMPINSKI_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                50
        );
        assertThat(result).isTrue();

        // Far location should be outside 50m
        boolean result2 = GPSCalculator.isWithinRadius(
                FAR_LAT, FAR_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                50
        );
        assertThat(result2).isFalse();
    }

    @Test
    @DisplayName("Very large radius (5000m) should work correctly")
    void isWithinRadius_VeryLargeRadius_WorksCorrectly() {
        // Far location should be within 5000m
        boolean result = GPSCalculator.isWithinRadius(
                FAR_LAT, FAR_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                5000
        );
        assertThat(result).isTrue();
    }

    // ==================== Distance Formatting Tests ====================

    @Test
    @DisplayName("Format distance less than 1000m should show meters")
    void formatDistance_LessThan1000m_ShowsMeters() {
        String formatted = GPSCalculator.formatDistance(350.0);
        assertThat(formatted).isEqualTo("350 m");
    }

    @Test
    @DisplayName("Format distance exactly 1000m should show km")
    void formatDistance_Exactly1000m_ShowsKm() {
        String formatted = GPSCalculator.formatDistance(1000.0);
        assertThat(formatted).isEqualTo("1.00 km");
    }

    @Test
    @DisplayName("Format distance greater than 1000m should show km with 2 decimals")
    void formatDistance_GreaterThan1000m_ShowsKm() {
        String formatted = GPSCalculator.formatDistance(1500.0);
        assertThat(formatted).isEqualTo("1.50 km");

        String formatted2 = GPSCalculator.formatDistance(2500.75);
        assertThat(formatted2).isEqualTo("2.50 km");
    }

    @Test
    @DisplayName("Format null distance should return N/A")
    void formatDistance_NullDistance_ReturnsNA() {
        String formatted = GPSCalculator.formatDistance(null);
        assertThat(formatted).isEqualTo("N/A");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Distance calculation should handle opposite sides of Earth")
    void calculateDistance_OppositeSidesOfEarth_ReturnsCorrectDistance() {
        // Riyadh coordinates
        BigDecimal riyadhLat = new BigDecimal("24.7136");
        BigDecimal riyadhLon = new BigDecimal("46.6753");

        // Opposite side (approximately)
        BigDecimal oppositeLat = new BigDecimal("-24.7136");
        BigDecimal oppositeLon = new BigDecimal("-133.3247");

        Double distance = GPSCalculator.calculateDistance(
                riyadhLat, riyadhLon,
                oppositeLat, oppositeLon
        );

        // Should be approximately half the Earth's circumference (~20,000 km)
        assertThat(distance).isGreaterThan(15000000.0); // More than 15,000 km
        assertThat(distance.isInfinite()).isFalse();
        assertThat(distance.isNaN()).isFalse();
    }

    @Test
    @DisplayName("Distance calculation should handle very close points")
    void calculateDistance_VeryClosePoints_ReturnsSmallDistance() {
        BigDecimal lat1 = new BigDecimal("24.664417");
        BigDecimal lon1 = new BigDecimal("46.674198");
        BigDecimal lat2 = new BigDecimal("24.664418"); // 1 meter difference
        BigDecimal lon2 = new BigDecimal("46.674199");

        Double distance = GPSCalculator.calculateDistance(lat1, lon1, lat2, lon2);

        // Should be very small (less than 200 meters)
        assertThat(distance).isLessThan(200.0);
        assertThat(distance).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Radius check with 1 meter outside should return false")
    void isWithinRadius_OneMeterOutside_ReturnsFalse() {
        // Calculate actual distance first
        Double actualDistance = GPSCalculator.calculateDistance(
                FAR_LAT, FAR_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON
        );

        // Set radius to 1 meter less than actual distance
        Integer radiusMeters = actualDistance.intValue() - 1;

        boolean result = GPSCalculator.isWithinRadius(
                FAR_LAT, FAR_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                radiusMeters
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Radius check with 1 meter inside should return true")
    void isWithinRadius_OneMeterInside_ReturnsTrue() {
        // Calculate actual distance first
        Double actualDistance = GPSCalculator.calculateDistance(
                NEARBY_LAT, NEARBY_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON
        );

        // Set radius to 1 meter more than actual distance
        Integer radiusMeters = actualDistance.intValue() + 1;

        boolean result = GPSCalculator.isWithinRadius(
                NEARBY_LAT, NEARBY_LON,
                KEMPINSKI_LAT, KEMPINSKI_LON,
                radiusMeters
        );

        assertThat(result).isTrue();
    }
}
