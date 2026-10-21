package com.pricing.backend.accountattribute;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.generated.model.AttributeDetail;
import com.pricing.backend.generated.model.AttributeListItem;
import com.pricing.backend.generated.model.AttributeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAttributeService {

	private static final Logger log = LoggerFactory.getLogger(AccountAttributeService.class);

	private final AccountAttributeRepository accountAttributeRepository;
	private final EligibilityReasonRepository eligibilityReasonRepository;
	private final AccountAttributeValidator validator;
	private final AccountAttributeMapper mapper;

	public AccountAttributeService(AccountAttributeRepository accountAttributeRepository,
			EligibilityReasonRepository eligibilityReasonRepository,
			AccountAttributeValidator validator, AccountAttributeMapper mapper) {
		this.accountAttributeRepository = accountAttributeRepository;
		this.eligibilityReasonRepository = eligibilityReasonRepository;
		this.validator = validator;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<AttributeListItem> list() {
		return accountAttributeRepository.findAllByOrderByAttributeCodeAsc().stream()
				.map(mapper::toAttributeListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<AttributeDetail> get(Long id) {
		return accountAttributeRepository.findById(id).map(mapper::toAttributeDetail);
	}

	@Transactional
	public AttributeDetail create(AttributeRequest request) {
		log.info("Create Account Attribute requested: code={}", request.getAttributeCode());
		AccountAttributeEntity entity = new AccountAttributeEntity();
		apply(entity, request);
		return mapper.toAttributeDetail(accountAttributeRepository.save(entity));
	}

	@Transactional
	public Optional<AttributeDetail> update(Long id, AttributeRequest request) {
		log.info("Update Account Attribute requested: id={}", id);
		return accountAttributeRepository.findById(id)
				.map(entity -> {
					validator.validateCanUpdate(entity, request);
					apply(entity, request);
					return mapper.toAttributeDetail(accountAttributeRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		log.info("Delete Account Attribute requested: id={}", id);
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
		entity.setProductTypes(request.getProductTypes());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

}
