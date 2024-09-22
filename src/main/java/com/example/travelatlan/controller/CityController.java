package com.example.travelatlan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


import org.neo4j.driver.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class CityController {

  @Autowired
  Driver driver;

  @Autowired
  ObjectMapper objectMapper;

  @GetMapping("/v1/findAllInterests")
  public List<String> findAllInterests() {
    String cypherQuery = """
            MATCH (activity:Activity)
            WITH collect(DISTINCT activity.type) AS activityTypes
            
            MATCH (restaurant:Restaurant)
            WITH activityTypes, collect(DISTINCT restaurant.cuisine) AS restaurantCuisines

            WITH activityTypes + restaurantCuisines AS interestTypes

            MATCH (accommodation:Accommodation)
            WITH interestTypes, collect(DISTINCT accommodation.type) AS accommodationTypes

            WITH interestTypes + accommodationTypes AS interestTypes

            MATCH (attraction:Attraction)
            WITH interestTypes, collect(DISTINCT attraction.name) AS attractionNames

            WITH interestTypes + attractionNames AS allInterests

            RETURN allInterests AS userInterests
        """;

    try (Session session = driver.session()) {
      Result result = session.run(cypherQuery);
      return result.single()
              .get("userInterests")
              .asList()
              .stream()
              .map(Object::toString)
              .collect(Collectors.toList());
    } catch (Exception e) {
      System.err.println("Error executing query: " + e.getMessage());
      throw new RuntimeException("Failed to retrieve interests", e);
    }
  }

  @GetMapping("/v1/findOptimalTripsWithStartCity")
  public List<Map<String, Object>> findOptimalTrips(
          @RequestParam("userMaxBudget") double userMaxBudget,
          @RequestParam("maxTripDuration") double maxTripDuration,
          @RequestParam("userInterests") List<String> userInterests,
          @RequestParam("userBudget") String userBudget,
          @RequestParam("startCityName") String startCityName) {

    String cypherQuery = """
    WITH $userMaxBudget AS userMaxBudget, 
         $maxTripDuration AS maxTripDuration, 
         $userInterests AS userInterests, 
         $userBudget AS userBudget,
         $startCityName AS startCityName

    MATCH path = (startCity:City {name: startCityName})-[r:CONNECTED_TO*1..6]->(endCity:City)
    WITH DISTINCT nodes(path) AS cities, relationships(path) AS connections, userMaxBudget, maxTripDuration, userInterests, userBudget

    UNWIND cities AS city
    OPTIONAL MATCH (city)-[:HAS_ATTRACTION]->(attraction:Attraction)
    OPTIONAL MATCH (city)-[:HAS_ACCOMMODATION]->(accommodation:Accommodation)
    OPTIONAL MATCH (city)-[:HAS_RESTAURANT]->(restaurant:Restaurant)
    OPTIONAL MATCH (attraction)-[:OFFERS]->(activity:Activity)

    WHERE city.budget_level = userBudget 
    AND (activity.type IN userInterests OR restaurant.cuisine IN userInterests)
    AND (accommodation.cost_per_night + attraction.cost + restaurant.average_cost <= userMaxBudget)
    AND attraction.duration <= maxTripDuration

    WITH cities, connections, city, 
         collect(DISTINCT attraction.name) AS attractions, 
         collect(DISTINCT accommodation.name) AS accommodations, 
         collect(DISTINCT restaurant.name) AS restaurants, 
         collect(DISTINCT activity.name) AS activities,
         sum(accommodation.cost_per_night + attraction.cost + restaurant.average_cost) AS cityCost,
         sum(attraction.duration) AS cityDuration,
         userMaxBudget, maxTripDuration

    WITH cities, connections, 
         sum(cityCost) AS totalCityCost, 
         sum(cityDuration) AS totalCityDuration,
         collect({
           cityName: city.name, 
           attractions: attractions, 
           accommodations: accommodations, 
           restaurants: restaurants, 
           activities: activities
         }) AS cityDetails,
         userMaxBudget, maxTripDuration

    WITH cities, connections, cityDetails, 
         totalCityCost, totalCityDuration,
         sum(REDUCE(travelCost = 0, r IN connections | travelCost + r.travelCost)) AS totalTravelCost,
         sum(REDUCE(travelTime = 0, r IN connections | travelTime + r.time)) AS totalTravelTime,
         userMaxBudget, maxTripDuration

    WITH cities, 
         totalCityCost + totalTravelCost AS totalTripCost, 
         totalCityDuration + totalTravelTime AS totalTripDuration,
         cityDetails, userMaxBudget, maxTripDuration

    WHERE totalTripCost <= userMaxBudget
    AND totalTripDuration <= maxTripDuration

    RETURN cityDetails AS citiesInPath,
           totalTripCost AS tripCost,
           totalTripDuration AS tripDuration
""";

    try (Session session = driver.session()) {
      Result result = session.run(cypherQuery, Map.of(
              "userMaxBudget", userMaxBudget,
              "maxTripDuration", maxTripDuration,
              "userInterests", userInterests,
              "userBudget", userBudget,
              "startCityName", startCityName
      ));

      return result.list(record -> {
        return Map.of(
                "citiesInPath", record.get("citiesInPath").asList(),
                "tripCost", record.get("tripCost").asDouble(),
                "tripDuration", record.get("tripDuration").asDouble()
        );
      });
    } catch (Exception e) {
      System.out.println("Error executing query: " + e.getMessage());
      throw new RuntimeException("Failed to retrieve optimal trips", e);
    }
  }


  @GetMapping("/v1/findOptimalTrips")
  public List<Map<String, Object>> findOptimalTrips(
          @RequestParam("userMaxBudget") double userMaxBudget,
          @RequestParam("maxTripDuration") double maxTripDuration,
          @RequestParam("userInterests") List<String> userInterests,
          @RequestParam("userBudget") String userBudget) {

    String cypherQuery = "WITH $userMaxBudget AS userMaxBudget, " +
            "$maxTripDuration AS maxTripDuration, " +
            "$userInterests AS userInterests, " +
            "$userBudget AS userBudget " +

            "MATCH path = (startCity:City)-[r:CONNECTED_TO*1..6]->(endCity:City) " +
            "WITH DISTINCT nodes(path) AS cities, relationships(path) AS connections, userMaxBudget, maxTripDuration, userInterests, userBudget " +

            "UNWIND cities AS city " +
            "OPTIONAL MATCH (city)-[:HAS_ATTRACTION]->(attraction:Attraction) " +
            "OPTIONAL MATCH (city)-[:HAS_ACCOMMODATION]->(accommodation:Accommodation) " +
            "OPTIONAL MATCH (city)-[:HAS_RESTAURANT]->(restaurant:Restaurant) " +
            "OPTIONAL MATCH (attraction)-[:OFFERS]->(activity:Activity) " +

            "WHERE city.budget_level = userBudget " +
            "AND (activity.type IN userInterests OR restaurant.cuisine IN userInterests) " +
            "AND (accommodation.cost_per_night + attraction.cost + restaurant.average_cost <= userMaxBudget) " +
            "AND attraction.duration <= maxTripDuration " +

            "WITH cities, connections, city, " +
            "collect(DISTINCT attraction.name) AS attractions, " +
            "collect(DISTINCT accommodation.name) AS accommodations, " +
            "collect(DISTINCT restaurant.name) AS restaurants, " +
            "collect(DISTINCT activity.name) AS activities, " +
            "sum(accommodation.cost_per_night + attraction.cost + restaurant.average_cost) AS cityCost, " +
            "sum(attraction.duration) AS cityDuration, " +
            "userMaxBudget, maxTripDuration " +

            "WITH cities, connections, " +
            "sum(cityCost) AS totalCityCost, " +
            "sum(cityDuration) AS totalCityDuration, " +
            "collect({ " +
            "cityName: city.name, " +
            "attractions: attractions, " +
            "accommodations: accommodations, " +
            "restaurants: restaurants, " +
            "activities: activities " +
            "}) AS cityDetails, " +
            "userMaxBudget, maxTripDuration " +

            "WITH cities, connections, cityDetails, " +
            "totalCityCost, totalCityDuration, " +
            "sum(REDUCE(travelCost = 0, r IN connections | travelCost + r.travelCost)) AS totalTravelCost, " +
            "sum(REDUCE(travelTime = 0, r IN connections | travelTime + r.time)) AS totalTravelTime, " +
            "userMaxBudget, maxTripDuration " +

            "WITH cities, " +
            "totalCityCost + totalTravelCost AS totalTripCost, " +
            "totalCityDuration + totalTravelTime AS totalTripDuration, " +
            "cityDetails, userMaxBudget, maxTripDuration " +

            "WHERE totalTripCost <= userMaxBudget " +
            "AND totalTripDuration <= maxTripDuration " +

            "RETURN cityDetails AS citiesInPath, " +
            "totalTripCost AS tripCost, " +
            "totalTripDuration AS tripDuration";


    try (Session session = driver.session()) {
      Result result = session.run(cypherQuery, Map.of(
              "userMaxBudget", userMaxBudget,
              "maxTripDuration", maxTripDuration,
              "userInterests", userInterests,
              "userBudget", userBudget
      ));

      return result.list(record -> {
        return Map.of(
                "citiesInPath", record.get("citiesInPath").asList(),
                "tripCost", record.get("tripCost").asDouble(),
                "tripDuration", record.get("tripDuration").asDouble()
        );
      });
    } catch (Exception e) {
      System.err.println("Error executing query: " + e.getMessage());
      throw new RuntimeException("Failed to retrieve optimal trips", e);
    }
  }

  @GetMapping("*")
  public String handleUndefinedRoutes() {
    return "Invalid Route";
  }
}
