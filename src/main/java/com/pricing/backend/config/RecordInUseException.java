package com.pricing.backend.config;

public class RecordInUseException extends RuntimeException {

	public RecordInUseException(String recordType, String referencingType, String referencingCode) {
		super("This " + recordType + " is used by " + referencingType + " with code "
				+ referencingCode + ". Please update the " + referencingType + " before deleting.");
	}
}
