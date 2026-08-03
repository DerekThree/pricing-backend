package com.pricing.backend.accountattribute;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.generated.model.AttributeDetail;
import com.pricing.backend.generated.model.AttributeListItem;
import com.pricing.backend.generated.model.AttributeRequest;
import com.pricing.backend.generated.model.AttributeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAttributeService {

	private final AccountAttributeRepository accountAttributeRepository;

	public AccountAttributeService(AccountAttributeRepository accountAttributeRepository) {
		this.accountAttributeRepository = accountAttributeRepository;
	}

	@Transactional(readOnly = true)
	public List<AttributeListItem> list() {
		return accountAttributeRepository.findAllByOrderByAttributeCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<AttributeDetail> get(Long id) {
		return accountAttributeRepository.findById(id).map(this::toDetail);
	}

	@Transactional
	public AttributeDetail create(AttributeRequest request) {
		AccountAttributeEntity entity = new AccountAttributeEntity();
		apply(entity, request);
		return toDetail(accountAttributeRepository.save(entity));
	}

	@Transactional
	public Optional<AttributeDetail> update(Long id, AttributeRequest request) {
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

	private void apply(AccountAttributeEntity entity, AttributeRequest request) {
		entity.setAttributeCode(request.getAttributeCode());
		entity.setAttributeName(request.getAttributeName());
		entity.setAttributeType(request.getAttributeType());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private AttributeListItem toListItem(AccountAttributeEntity entity) {
		return new AttributeListItem(
				entity.getId(),
				formatCodeAndName(entity.getAttributeCode(), entity.getAttributeName()),
				entity.getAttributeType(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private AttributeDetail toDetail(AccountAttributeEntity entity) {
		return new AttributeDetail(
				entity.getAttributeCode(),
				entity.getAttributeName(),
				entity.getAttributeType(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
