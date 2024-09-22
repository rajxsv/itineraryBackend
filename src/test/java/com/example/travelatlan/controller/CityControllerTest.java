package com.example.travelatlan.controller;

import com.example.travelatlan.controller.CityController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CityControllerTest {

  @Mock
  private Driver driver;

  @Mock
  private Session session;

  @Mock
  private Result result;

  @Mock
  private Record record;

  @InjectMocks
  private CityController cityController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(driver.session()).thenReturn(session);
  }

  @Test
  void testFindOptimalTripsWithStartCity() {
    double userMaxBudget = 1000.0;
    double maxTripDuration = 5.0;
    List<String> userInterests = Arrays.asList("Sightseeing", "Food");
    String userBudget = "MEDIUM";
    String startCityName = "New York";

    Record mockRecord = mock(Record.class);
    when(mockRecord.get("citiesInPath")).thenReturn(Values.value(Arrays.asList(
            Map.of("cityName", "New York", "attractions", Arrays.asList("Statue of Liberty"), "accommodations", Arrays.asList("Hotel A"), "restaurants", Arrays.asList("Restaurant X"), "activities", Arrays.asList("City Tour"))
    )));
    when(mockRecord.get("tripCost")).thenReturn(Values.value(800.0));
    when(mockRecord.get("tripDuration")).thenReturn(Values.value(4.0));

    when(session.run(anyString(), anyMap())).thenReturn(result);
    when(result.list(any())).thenReturn(Arrays.asList(
            Map.of(
                    "citiesInPath", Arrays.asList(
                            Map.of("cityName", "New York", "attractions", Arrays.asList("Statue of Liberty"), "accommodations", Arrays.asList("Hotel A"), "restaurants", Arrays.asList("Restaurant X"), "activities", Arrays.asList("City Tour"))
                    ),
                    "tripCost", 800.0,
                    "tripDuration", 4.0
            )
    ));

    List<Map<String, Object>> trips = cityController.findOptimalTrips(userMaxBudget, maxTripDuration, userInterests, userBudget, startCityName);

    assertNotNull(trips);
    assertEquals(1, trips.size());
    Map<String, Object> trip = trips.get(0);
    assertEquals(800.0, trip.get("tripCost"));
    assertEquals(4.0, trip.get("tripDuration"));

    List<Map<String, Object>> citiesInPath = (List<Map<String, Object>>) trip.get("citiesInPath");
    assertEquals(1, citiesInPath.size());
    Map<String, Object> city = citiesInPath.get(0);
    assertEquals("New York", city.get("cityName"));
    assertEquals(Arrays.asList("Statue of Liberty"), city.get("attractions"));
    assertEquals(Arrays.asList("Hotel A"), city.get("accommodations"));
    assertEquals(Arrays.asList("Restaurant X"), city.get("restaurants"));
    assertEquals(Arrays.asList("City Tour"), city.get("activities"));

    verify(driver).session();
    verify(session).run(anyString(), anyMap());
    verify(result).list(any());
  }

  @Test
  void testFindOptimalTripsWithStartCity_ExceptionHandling() {
    double userMaxBudget = 1000.0;
    double maxTripDuration = 5.0;
    List<String> userInterests = Arrays.asList("Sightseeing", "Food");
    String userBudget = "MEDIUM";
    String startCityName = "New York";

    when(session.run(anyString(), anyMap())).thenThrow(new RuntimeException("Database error"));

    Exception exception = assertThrows(RuntimeException.class, () -> {
      cityController.findOptimalTrips(userMaxBudget, maxTripDuration, userInterests, userBudget, startCityName);
    });

    assertEquals("Failed to retrieve optimal trips", exception.getMessage());
    assertTrue(exception.getCause().getMessage().contains("Database error"));

    verify(driver).session();
    verify(session).run(anyString(), anyMap());
  }

  @Test
  void testFindOptimalTrips() {
    double userMaxBudget = 1000.0;
    double maxTripDuration = 5.0;
    List<String> userInterests = Arrays.asList("Sightseeing", "Food");
    String userBudget = "MEDIUM";

    when(session.run(anyString(), anyMap())).thenReturn(result);
    when(result.list(any())).thenReturn(Arrays.asList(
            Map.of(
                    "citiesInPath", Arrays.asList(
                            Map.of("cityName", "New York", "attractions", Arrays.asList("Statue of Liberty"), "accommodations", Arrays.asList("Hotel A"), "restaurants", Arrays.asList("Restaurant X"), "activities", Arrays.asList("City Tour")),
                            Map.of("cityName", "Boston", "attractions", Arrays.asList("Freedom Trail"), "accommodations", Arrays.asList("Hotel B"), "restaurants", Arrays.asList("Restaurant Y"), "activities", Arrays.asList("Harbor Cruise"))
                    ),
                    "tripCost", 800.0,
                    "tripDuration", 4.0
            )
    ));

    List<Map<String, Object>> trips = cityController.findOptimalTrips(userMaxBudget, maxTripDuration, userInterests, userBudget);

    assertNotNull(trips);
    assertEquals(1, trips.size());
    Map<String, Object> trip = trips.get(0);
    assertEquals(800.0, trip.get("tripCost"));
    assertEquals(4.0, trip.get("tripDuration"));

    List<Map<String, Object>> citiesInPath = (List<Map<String, Object>>) trip.get("citiesInPath");
    assertEquals(2, citiesInPath.size());

    Map<String, Object> city1 = citiesInPath.get(0);
    assertEquals("New York", city1.get("cityName"));
    assertEquals(Arrays.asList("Statue of Liberty"), city1.get("attractions"));
    assertEquals(Arrays.asList("Hotel A"), city1.get("accommodations"));
    assertEquals(Arrays.asList("Restaurant X"), city1.get("restaurants"));
    assertEquals(Arrays.asList("City Tour"), city1.get("activities"));

    Map<String, Object> city2 = citiesInPath.get(1);
    assertEquals("Boston", city2.get("cityName"));
    assertEquals(Arrays.asList("Freedom Trail"), city2.get("attractions"));
    assertEquals(Arrays.asList("Hotel B"), city2.get("accommodations"));
    assertEquals(Arrays.asList("Restaurant Y"), city2.get("restaurants"));
    assertEquals(Arrays.asList("Harbor Cruise"), city2.get("activities"));

    verify(driver).session();
    verify(session).run(anyString(), anyMap());
    verify(result).list(any());
  }

  @Test
  void testFindOptimalTrips_ExceptionHandling() {
    double userMaxBudget = 1000.0;
    double maxTripDuration = 5.0;
    List<String> userInterests = Arrays.asList("Sightseeing", "Food");
    String userBudget = "MEDIUM";

    when(session.run(anyString(), anyMap())).thenThrow(new RuntimeException("Database error"));

    Exception exception = assertThrows(RuntimeException.class, () -> {
      cityController.findOptimalTrips(userMaxBudget, maxTripDuration, userInterests, userBudget);
    });

    assertEquals("Failed to retrieve optimal trips", exception.getMessage());
    assertTrue(exception.getCause().getMessage().contains("Database error"));

    verify(driver).session();
    verify(session).run(anyString(), anyMap());
  }

  @Test
  void testFindAllInterests() {
    List<String> expectedInterests = Arrays.asList("Hiking", "Italian", "Hotel", "Eiffel Tower");
    when(session.run(anyString())).thenReturn(result);
    when(result.single()).thenReturn(record);
    when(record.get("userInterests")).thenReturn(Values.value(expectedInterests));

    List<String> actualInterests = cityController.findAllInterests();

    assertNotNull(actualInterests);
    assertEquals(expectedInterests, actualInterests);
    verify(driver).session();
    verify(session).run(anyString());
    verify(result).single();
  }

  @Test
  void testFindAllInterests_ExceptionHandling() {
    when(session.run(anyString())).thenThrow(new RuntimeException("Database error"));

    Exception exception = assertThrows(RuntimeException.class, () -> {
      cityController.findAllInterests();
    });

    assertEquals("Failed to retrieve interests", exception.getMessage());
    assertTrue(exception.getCause().getMessage().contains("Database error"));
    verify(driver).session();
    verify(session).run(anyString());
  }

  @Test
  void testHandleUndefinedRoutes() {
    String result = cityController.handleUndefinedRoutes();
    assertEquals("Invalid Route", result);
  }

}
