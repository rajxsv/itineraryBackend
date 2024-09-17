package com.example.travelatlan.repository;


import com.example.travelatlan.models.City;
import org.springframework.data.mongodb.repository.Aggregation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CityRepository extends MongoRepository<City, String> {

  @Query("{name:'?0'}")
  City findCityByName(String name);

  @Query(value="{interests:'?0'}", fields="{'name' : 1}")
  List<City> findAllByInterest(String interest);

  @Query(value = "{ 'name': ?0 }", fields = "{ 'interests' : 1, '_id' : 0 }")
  List<String> findInterestsByCityName(String cityName);

  @Aggregation(pipeline = {
          "{ $unwind: '$interests' }",
          "{ $group: { _id: null, uniqueinterests: { $addToSet: '$interests' } } }",
          "{ $project: { _id: 0, uniqueinterests: 1 } }"
  })
  List<String> findAllUniqueInterests();

  long count();
}
