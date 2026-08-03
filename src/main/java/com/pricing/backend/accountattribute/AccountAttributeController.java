package com.pricing.backend.accountattribute;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.AccountAttributesApi;
import com.pricing.backend.generated.model.AttributeDetail;
import com.pricing.backend.generated.model.AttributeListItem;
import com.pricing.backend.generated.model.AttributeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountAttributeController implements AccountAttributesApi {

	private final AccountAttributeService accountAttributeService;

	public AccountAttributeController(AccountAttributeService accountAttributeService) {
		this.accountAttributeService = accountAttributeService;
	}

	@Override
	public ResponseEntity<List<AttributeListItem>> listAttributes() {
		return ResponseEntity.ok(accountAttributeService.list());
	}

	@Override
	public ResponseEntity<AttributeDetail> getAttribute(Long id) {
		AttributeDetail accountAttribute = accountAttributeService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Account attribute", id));

		return ResponseEntity.ok(accountAttribute);
	}

	@Override
	public ResponseEntity<AttributeDetail> createAttribute(AttributeRequest attributeRequest) {
		return ResponseEntity.status(HttpStatus.CREATED).body(accountAttributeService.create(attributeRequest));
	}

	@Override
	public ResponseEntity<AttributeDetail> updateAttribute(Long id, AttributeRequest attributeRequest) {
		AttributeDetail accountAttribute = accountAttributeService.update(id, attributeRequest)
				.orElseThrow(() -> new RecordNotFoundException("Account attribute", id));

		return ResponseEntity.ok(accountAttribute);
	}

	@Override
	public ResponseEntity<Void> deleteAttribute(Long id) {
		if (accountAttributeService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Account attribute", id);
	}
}
