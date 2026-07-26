package com.pricing.backend.accountattribute;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.AccountAttributesApi;
import com.pricing.backend.generated.model.AccountAttributeDetail;
import com.pricing.backend.generated.model.AccountAttributeListItem;
import com.pricing.backend.generated.model.AccountAttributeRequest;
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
	public ResponseEntity<List<AccountAttributeListItem>> listAccountAttributes() {
		return ResponseEntity.ok(accountAttributeService.list());
	}

	@Override
	public ResponseEntity<AccountAttributeDetail> getAccountAttribute(Long id) {
		AccountAttributeDetail accountAttribute = accountAttributeService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Account attribute", id));

		return ResponseEntity.ok(accountAttribute);
	}

	@Override
	public ResponseEntity<AccountAttributeDetail> createAccountAttribute(AccountAttributeRequest accountAttributeRequest) {
		return ResponseEntity.status(HttpStatus.CREATED).body(accountAttributeService.create(accountAttributeRequest));
	}

	@Override
	public ResponseEntity<AccountAttributeDetail> updateAccountAttribute(Long id, AccountAttributeRequest accountAttributeRequest) {
		AccountAttributeDetail accountAttribute = accountAttributeService.update(id, accountAttributeRequest)
				.orElseThrow(() -> new RecordNotFoundException("Account attribute", id));

		return ResponseEntity.ok(accountAttribute);
	}

	@Override
	public ResponseEntity<Void> deleteAccountAttribute(Long id) {
		if (accountAttributeService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Account attribute", id);
	}
}
