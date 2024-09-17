package com.example.travelatlan.controller;

import com.example.travelatlan.models.City;
import com.example.travelatlan.repository.CityRepository;
import com.example.travelatlan.service.CityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.internal.util.Iterables;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CityController {

  @Autowired
  private CityService cityService;

  @Autowired
  Driver driver;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  CityRepository cityRepository;

  @GetMapping("/addCity")
  public City addCity(@RequestParam String name, @RequestParam List<String> interests) {
    return cityService.addCity(name, interests);
  }

  @GetMapping("/findAllInterests")
  public List<String> findAllInterests() {
    return cityRepository.findAllUniqueInterests();
  }

  @GetMapping("/find")
  public Map<String,Object> findShortest(@RequestParam String startCity, String endCity, int maxBudget, int maxDuration) {
    try (var session = driver.session()) {
      System.out.println(startCity + endCity + maxBudget + maxDuration);
      String cypherQuery = "MATCH p = (start:City {name: '" + startCity + "'})-[:CONNECTS*]->(end:City {name: '" + endCity + "'}) "
              + "WHERE ALL(rel in relationships(p) WHERE rel.cost <= 5000 AND rel.travelTime <= 5) "
              + "WITH p, reduce(totalCost = 0, rel in relationships(p) | totalCost + rel.cost) AS totalCost, "
              + "reduce(totalTime = 0, rel in relationships(p) | totalTime + rel.travelTime) AS totalTime "
              + "WHERE totalCost <= " + maxBudget + " AND totalTime <= " + maxDuration + " "
              + "RETURN p, totalCost, totalTime "
              + "ORDER BY totalCost ASC, totalTime ASC "
              + "LIMIT 10;";

      Result result = session.run(cypherQuery);
      ArrayNode pathsArray = objectMapper.createArrayNode();
      List<String> cities = new ArrayList<>();

      while (result.hasNext()) {
        var record = result.next();
        var path = record.get("p").asPath();
        var totalCost = record.get("totalCost").asInt();
        var totalTime = record.get("totalTime").asDouble();

        ObjectNode pathObject = objectMapper.createObjectNode();
        ArrayNode nodesArray = objectMapper.createArrayNode();
        ArrayNode edgesArray = objectMapper.createArrayNode();

        List<Node> nodeList = Iterables.asList(path.nodes());
        List<Relationship> relationshipList = Iterables.asList(path.relationships());

        for (int i = 0; i < nodeList.size(); i++) {
          var node = nodeList.get(i);
          var nodeName = node.get("name").asString();

          cities.add(nodeName);

          ObjectNode nodeObject = objectMapper.createObjectNode();
          nodeObject.put("name", nodeName);
          nodesArray.add(nodeObject);

          if (i < nodeList.size() - 1) {
            var rel = relationshipList.get(i);
            ObjectNode edgeObject = objectMapper.createObjectNode();
            edgeObject.put("cost", rel.get("cost").asInt());
            edgeObject.put("travelTime", rel.get("travelTime").asDouble());
            edgesArray.add(edgeObject);
          }
        }

        pathObject.put("nodes", nodesArray);
        pathObject.put("edges", edgesArray);
        pathObject.put("totalCost", totalCost);
        pathObject.put("totalTime", totalTime);

        pathsArray.add(pathObject);
      }

      ObjectNode responseObject = objectMapper.createObjectNode();
      responseObject.set("paths", pathsArray);
      String res = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseObject);

      Map<String,Object> response = new HashMap<>();
      response.put("interests",populate(cities));
      response.put("paths",pathsArray);

      return response;

    } catch (Exception e) {
      e.printStackTrace();
      Map<String,Object> response = new HashMap<>();
      response.put("message","{\"error\":\"Error occurred: " + e.getMessage() + "\"}");
      return response;
    }
  }

  @GetMapping("/init")
  public void seedData() throws Exception {
    try (var session = driver.session()) {

      String cypherQuery = "CREATE (c1:City {name: 'Delhi'}) "
              + "CREATE (c2:City {name: 'Mumbai'}) "
              + "CREATE (c3:City {name: 'Kolkata'}) "
              + "CREATE (c4:City {name: 'Chennai'}) "
              + "CREATE (c5:City {name: 'Bangalore'}) "
              + "CREATE (c6:City {name: 'Hyderabad'}) "
              + "CREATE (c7:City {name: 'Pune'}) "
              + "CREATE (c8:City {name: 'Jaipur'}) "
              + "CREATE (c9:City {name: 'Ahmedabad'}) "
              + "CREATE (c10:City {name: 'Lucknow'}) "
              + "CREATE (c1)-[:CONNECTS {cost: 3500, travelTime: 2}]->(c2) "
              + "CREATE (c2)-[:CONNECTS {cost: 4000, travelTime: 2.5}]->(c3) "
              + "CREATE (c3)-[:CONNECTS {cost: 3000, travelTime: 2}]->(c4) "
              + "CREATE (c4)-[:CONNECTS {cost: 2500, travelTime: 1.5}]->(c5) "
              + "CREATE (c5)-[:CONNECTS {cost: 2000, travelTime: 1}]->(c6) "
              + "CREATE (c6)-[:CONNECTS {cost: 1500, travelTime: 0.75}]->(c7) "
              + "CREATE (c7)-[:CONNECTS {cost: 1800, travelTime: 1}]->(c8) "
              + "CREATE (c8)-[:CONNECTS {cost: 2200, travelTime: 1.25}]->(c9) "
              + "CREATE (c9)-[:CONNECTS {cost: 3200, travelTime: 1.75}]->(c10) "
              + "CREATE (c10)-[:CONNECTS {cost: 3800, travelTime: 2.25}]->(c1) "
              + "CREATE (c2)-[:CONNECTS {cost: 4500, travelTime: 3}]->(c4) "
              + "CREATE (c3)-[:CONNECTS {cost: 3500, travelTime: 2.5}]->(c5) "
              + "CREATE (c4)-[:CONNECTS {cost: 2800, travelTime: 2}]->(c6) "
              + "CREATE (c5)-[:CONNECTS {cost: 2400, travelTime: 1.75}]->(c7) "
              + "CREATE (c6)-[:CONNECTS {cost: 2700, travelTime: 2}]->(c8) "
              + "CREATE (c7)-[:CONNECTS {cost: 1900, travelTime: 1.25}]->(c9) "
              + "CREATE (c8)-[:CONNECTS {cost: 2100, travelTime: 1.5}]->(c10) "
              + "CREATE (c9)-[:CONNECTS {cost: 3400, travelTime: 2}]->(c1) "
              + "CREATE (c10)-[:CONNECTS {cost: 3600, travelTime: 2.5}]->(c2);";
      session.run(cypherQuery);
      System.out.println("Done");
    } catch (Exception e) {
      System.out.println(e);
      throw  new Exception();
    }
  }

  private HashMap<String,List<String>> populate(List<String> cities) {
    HashMap<String,List<String>> mp = new HashMap<>();

    for (int i=0; i<cities.size(); i++) {
      List<String> interests;

      interests = cityRepository.findInterestsByCityName(cities.get(i));
      mp.put(cities.get(i),interests);
    }

    return mp;
  }
}
