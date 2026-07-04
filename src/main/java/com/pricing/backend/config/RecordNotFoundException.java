package com.pricing.backend.config;

public class RecordNotFoundException extends RuntimeException {

	public RecordNotFoundException(String resourceName, Long id) {
		super(resourceName + " with id " + id + " was not found");
	}
}
