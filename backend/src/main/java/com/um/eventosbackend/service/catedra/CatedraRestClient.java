package com.um.eventosbackend.service.catedra;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CatedraRestClient {

    private final RestTemplate restTemplate;

    public CatedraRestClient(@Qualifier("catedraRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <T> ResponseEntity<T> get(String path, Class<T> responseType, Object... uriVariables) {
        return restTemplate.getForEntity(path, responseType, uriVariables);
    }

    public <T> ResponseEntity<T> post(String path, Object request, Class<T> responseType, Object... uriVariables) {
        return restTemplate.postForEntity(path, new HttpEntity<>(request), responseType, uriVariables);
    }

    public void put(String path, Object request, Object... uriVariables) {
        restTemplate.put(path, request, uriVariables);
    }

    public void delete(String path, Object... uriVariables) {
        restTemplate.delete(path, uriVariables);
    }
}

