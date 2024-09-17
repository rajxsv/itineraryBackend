package com.example.travelatlan.service;

import com.example.travelatlan.models.City;
import com.example.travelatlan.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityService {

  @Autowired
  private CityRepository cityRepository;

  public City addCity(String name, List<String> interests) {
    City city = new City();
    city.setName(name);
    city.setInterests(interests);
    return cityRepository.save(city);
  }
}
