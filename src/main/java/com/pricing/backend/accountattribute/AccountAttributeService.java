package com.pricing.backend.accountattribute;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.generated.model.AttributeDetail;
import com.pricing.backend.generated.model.AttributeListItem;
import com.pricing.backend.generated.model.AttributeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAttributeService {

	private final AccountAttributeRepository accountAttributeRepository;
	private final EligibilityReasonRepository eligibilityReasonRepository;
	private final AccountAttributeValidator accountAttributeValidator;
	private final AccountAttributeMapper accountAttributeMapper;

	public AccountAttributeService(AccountAttributeRepository accountAttributeRepository,
			EligibilityReasonRepository eligibilityReasonRepository,
			AccountAttributeValidator accountAttributeValidator, AccountAttributeMapper accountAttributeMapper) {
		this.accountAttributeRepository = accountAttributeRepository;
		this.eligibilityReasonRepository = eligibilityReasonRepository;
		this.accountAttributeValidator = accountAttributeValidator;
		this.accountAttributeMapper = accountAttributeMapper;
	}

	@Transactional(readOnly = true)
	public List<AttributeListItem> list() {
		return accountAttributeRepository.findAllByOrderByAttributeCodeAsc().stream()
				.map(accountAttributeMapper::toAttributeListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<AttributeDetail> get(Long id) {
		return accountAttributeRepository.findById(id).map(accountAttributeMapper::toAttributeDetail);
	}

	@Transactional
	public AttributeDetail create(AttributeRequest request) {
		AccountAttributeEntity entity = new AccountAttributeEntity();
		apply(entity, request);
		return accountAttributeMapper.toAttributeDetail(accountAttributeRepository.save(entity));
	}

	@Transactional
	public Optional<AttributeDetail> update(Long id, AttributeRequest request) {
		return accountAttributeRepository.findById(id)
				.map(entity -> {
					accountAttributeValidator.validateCanUpdate(entity, request);
					apply(entity, request);
					return accountAttributeMapper.toAttributeDetail(accountAttributeRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!accountAttributeRepository.existsById(id)) {
			return false;
		}

		eligibilityReasonRepository.findFirstByConditions_Attribute_IdOrderByReasonCodeAsc(id)
				.ifPresent(reason -> {
					throw new RecordInUseException(
							"account attribute", "eligibility reason", reason.getReasonCode());
				});

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

}
