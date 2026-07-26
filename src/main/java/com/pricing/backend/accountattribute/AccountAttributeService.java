package com.pricing.backend.accountattribute;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.generated.model.AccountAttributeDetail;
import com.pricing.backend.generated.model.AccountAttributeListItem;
import com.pricing.backend.generated.model.AccountAttributeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAttributeService {

	private final AccountAttributeRepository accountAttributeRepository;

	public AccountAttributeService(AccountAttributeRepository accountAttributeRepository) {
		this.accountAttributeRepository = accountAttributeRepository;
	}

	@Transactional(readOnly = true)
	public List<AccountAttributeListItem> list() {
		return accountAttributeRepository.findAllByOrderByAttributeCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<AccountAttributeDetail> get(Long id) {
		return accountAttributeRepository.findById(id).map(this::toDetail);
	}

	@Transactional
	public AccountAttributeDetail create(AccountAttributeRequest request) {
		AccountAttributeEntity entity = new AccountAttributeEntity();
		apply(entity, request);
		return toDetail(accountAttributeRepository.save(entity));
	}

	@Transactional
	public Optional<AccountAttributeDetail> update(Long id, AccountAttributeRequest request) {
		return accountAttributeRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toDetail(accountAttributeRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!accountAttributeRepository.existsById(id)) {
			return false;
		}

		accountAttributeRepository.deleteById(id);
		return true;
	}

	private void apply(AccountAttributeEntity entity, AccountAttributeRequest request) {
		entity.setAttributeCode(request.getAttributeCode());
		entity.setAttributeName(request.getAttributeName());
		entity.setAttributeType(request.getAttributeType());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private AccountAttributeListItem toListItem(AccountAttributeEntity entity) {
		return new AccountAttributeListItem(
				entity.getId(),
				formatCodeAndName(entity.getAttributeCode(), entity.getAttributeName()),
				entity.getAttributeType(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private AccountAttributeDetail toDetail(AccountAttributeEntity entity) {
		return new AccountAttributeDetail(
				entity.getId(),
				entity.getAttributeCode(),
				entity.getAttributeName(),
				entity.getAttributeType(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
