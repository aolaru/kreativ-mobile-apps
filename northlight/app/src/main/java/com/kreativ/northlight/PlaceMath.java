package com.kreativ.northlight;

/** Great-circle estimates; direction is true-north until corrected for magnetic declination. */
final class PlaceMath {
    private static final double EARTH_METRES = 6_371_000.0;
    private PlaceMath() { }
    static boolean valid(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
    static double distance(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = p2 - p1, dl = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dp / 2), 2) + Math.cos(p1) * Math.cos(p2) * Math.pow(Math.sin(dl / 2), 2);
        return 2 * EARTH_METRES * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a))));
    }
    static double trueBearing(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
    static int magneticBearing(double trueBearing, double declination) {
        return HeadingMath.rounded((float) (trueBearing - declination));
    }
    static boolean reliableDirection(double distance, double currentAccuracy, double savedAccuracy) {
        return Double.isFinite(distance) && Double.isFinite(currentAccuracy) && Double.isFinite(savedAccuracy)
                && currentAccuracy >= 0 && currentAccuracy <= 50 && savedAccuracy >= 0 && savedAccuracy <= 50
                && distance > Math.max(10, currentAccuracy + savedAccuracy);
    }
}
