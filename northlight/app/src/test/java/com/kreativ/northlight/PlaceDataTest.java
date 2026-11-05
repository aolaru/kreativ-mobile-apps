package com.kreativ.northlight;

import org.json.JSONException;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlaceDataTest {
    @Test public void placesRoundTripRenameAndDeleteWithoutChangingCoordinates() throws Exception {
        PlaceData places = new PlaceData("[]");
        assertTrue(places.add("Car", 44.4268, 26.1025, 8.5));
        places = new PlaceData(places.toString());
        places.rename(0, "Parking");
        assertEquals("Parking", places.name(0));
        assertEquals(44.4268, places.latitude(0), 1e-9);
        assertEquals(26.1025, places.longitude(0), 1e-9);
        assertEquals(8.5, places.accuracy(0), 1e-9);
        places.delete(0); assertEquals(0, new PlaceData(places.toString()).size());
    }
    @Test public void fullStoreDoesNotOverwriteSavedPlaces() throws Exception {
        PlaceData places = new PlaceData("[]");
        for (int i = 0; i < PlaceData.LIMIT; i++) assertTrue(places.add("Place " + i, i, i, 5));
        String original = places.toString();
        assertFalse(places.add("Extra", 0, 0, 5));
        assertEquals(original, places.toString());
    }
    @Test public void invalidOrCorruptCoordinatesAreRejected() throws Exception {
        PlaceData places = new PlaceData("[]");
        assertThrows(JSONException.class, () -> places.add("bad", 91, 0, 3));
        assertThrows(JSONException.class, () -> places.add("bad", 1, Double.NaN, 3));
        assertThrows(JSONException.class, () -> places.add("bad", 1, 1, 51));
        assertThrows(JSONException.class, () -> new PlaceData("[{\"name\":\"x\",\"lat\":0,\"lon\":400,\"accuracy\":5}]"));
        assertEquals(0, places.size());
    }
}
