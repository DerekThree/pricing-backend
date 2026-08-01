package com.pricing.backend.eligibilityreason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.generated.model.AccountAttributeType;
import com.pricing.backend.generated.model.AccountAttributeOption;
import com.pricing.backend.generated.model.ReasonCondition;
import com.pricing.backend.generated.model.ReasonConditionValue;
import com.pricing.backend.generated.model.ReasonDetail;
import com.pricing.backend.generated.model.ReasonListItem;
import com.pricing.backend.generated.model.ReasonOperator;
import com.pricing.backend.generated.model.ReasonOptions;
import com.pricing.backend.generated.model.ReasonRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibilityReasonService {

	private final EligibilityReasonRepository eligibilityReasonRepository;
	private final AccountAttributeRepository accountAttributeRepository;

	public EligibilityReasonService(EligibilityReasonRepository eligibilityReasonRepository,
			AccountAttributeRepository accountAttributeRepository) {
		this.eligibilityReasonRepository = eligibilityReasonRepository;
		this.accountAttributeRepository = accountAttributeRepository;
	}

	@Transactional(readOnly = true)
	public List<ReasonListItem> list() {
		return eligibilityReasonRepository.findAllByOrderByReasonCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<ReasonDetail> get(Long id) {
		return eligibilityReasonRepository.findById(id).map(this::toDetail);
	}

	@Transactional(readOnly = true)
	public ReasonOptions getOptions() {
		return buildOptions();
	}

	@Transactional
	public ReasonDetail create(ReasonRequest request) {
		EligibilityReasonEntity entity = new EligibilityReasonEntity();
		apply(entity, request);
		return toDetail(eligibilityReasonRepository.save(entity));
	}

	@Transactional
	public Optional<ReasonDetail> update(Long id, ReasonRequest request) {
		return eligibilityReasonRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toDetail(eligibilityReasonRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!eligibilityReasonRepository.existsById(id)) {
			return false;
		}

		eligibilityReasonRepository.deleteById(id);
		return true;
	}

	private void apply(EligibilityReasonEntity entity, ReasonRequest request) {
		Map<Long, AccountAttributeEntity> attributesById = accountAttributeRepository
				.findAllById(request.getConditions().stream().map(ReasonCondition::getAttributeId).toList())
				.stream()
				.collect(Collectors.toMap(AccountAttributeEntity::getId, Function.identity()));
		Set<String> uniqueConditions = new HashSet<>();
		entity.setReasonCode(request.getReasonCode());
		entity.setReasonName(request.getReasonName());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
		entity.getConditions().clear();
		for (ReasonCondition condition : request.getConditions()) {
			AccountAttributeEntity attribute = attributesById.get(condition.getAttributeId());
			if (attribute == null) {
				throw new IllegalArgumentException("Account attribute with id " + condition.getAttributeId() + " was not found");
			}

			String operator = condition.getOperator().getValue();
			String uniqueCondition = attribute.getId() + "|" + operator;
			if (!uniqueConditions.add(uniqueCondition)) {
				throw new IllegalArgumentException(
						"Duplicate condition for account attribute id " + attribute.getId() + " and operator " + operator);
			}

			entity.getConditions().add(new EligibilityReasonConditionEntity(
					new EligibilityReasonConditionId(entity.getId(), attribute.getId(), operator),
					entity,
					attribute,
					normalizeAttributeValue(attribute, condition.getValue())
			));
		}
	}

	private ReasonListItem toListItem(EligibilityReasonEntity entity) {
		return new ReasonListItem(
				entity.getId(),
				formatCodeAndName(entity.getReasonCode(), entity.getReasonName()),
				entity.getConditions().stream()
						.sorted(conditionComparator())
						.map(this::toConditionSummary)
						.toList(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private ReasonDetail toDetail(EligibilityReasonEntity entity) {
		return new ReasonDetail(
				entity.getReasonCode(),
				entity.getReasonName(),
				entity.getConditions().stream()
						.sorted(conditionComparator())
						.map(this::toCondition)
						.toList(),
				entity.getUpdatedBy(),
				entity.getId(),
				buildOptions(),
				entity.getUpdatedOn()
		);
	}

	private ReasonCondition toCondition(EligibilityReasonConditionEntity entity) {
		return new ReasonCondition(
				entity.getAttribute().getId(),
				ReasonOperator.fromValue(entity.getId().getOperator()),
				toApiValue(entity.getAttribute().getAttributeType(), entity.getAttributeValue())
		);
	}

	private ReasonOptions buildOptions() {
		return new ReasonOptions(
				accountAttributeRepository.findAllByOrderByAttributeCodeAsc().stream()
						.map(attribute -> new AccountAttributeOption(
								attribute.getId(),
								attribute.getAttributeCode(),
								attribute.getAttributeName(),
								attribute.getAttributeType()))
						.toList()
		);
	}

	private String normalizeAttributeValue(AccountAttributeEntity attribute, ReasonConditionValue value) {
		Object scalar = extractScalarValue(value);
		return switch (attribute.getAttributeType()) {
			case BOOLEAN -> normalizeBooleanValue(attribute, scalar);
			case DATE -> normalizeDateValue(attribute, scalar);
			case DECIMAL -> normalizeDecimalValue(attribute, scalar);
			case INTEGER -> normalizeIntegerValue(attribute, scalar);
			case TEXT -> normalizeTextValue(attribute, scalar);
		};
	}

	private String normalizeBooleanValue(AccountAttributeEntity attribute, Object value) {
		if (value instanceof Boolean booleanValue) {
			return booleanValue.toString();
		}

		throw new IllegalArgumentException("Condition value for account attribute "
				+ attribute.getAttributeCode() + " must be a boolean");
	}

	private String normalizeDateValue(AccountAttributeEntity attribute, Object value) {
		if (!(value instanceof String stringValue)) {
			throw new IllegalArgumentException("Condition value for account attribute "
					+ attribute.getAttributeCode() + " must be a date string");
		}

		try {
			return LocalDate.parse(stringValue).toString();
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("Condition value for account attribute "
					+ attribute.getAttributeCode() + " must be a valid ISO date");
		}
	}

	private String normalizeDecimalValue(AccountAttributeEntity attribute, Object value) {
		if (value instanceof BigDecimal decimalValue) {
			return decimalValue.stripTrailingZeros().toPlainString();
		}

		throw new IllegalArgumentException("Condition value for account attribute "
				+ attribute.getAttributeCode() + " must be a number");
	}

	private String normalizeIntegerValue(AccountAttributeEntity attribute, Object value) {
		if (!(value instanceof BigDecimal decimalValue)) {
			throw new IllegalArgumentException("Condition value for account attribute "
					+ attribute.getAttributeCode() + " must be an integer");
		}

		try {
			return decimalValue.toBigIntegerExact().toString();
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("Condition value for account attribute "
					+ attribute.getAttributeCode() + " must be an integer");
		}
	}

	private String normalizeTextValue(AccountAttributeEntity attribute, Object value) {
		if (value instanceof String stringValue) {
			return stringValue;
		}

		throw new IllegalArgumentException("Condition value for account attribute "
				+ attribute.getAttributeCode() + " must be a string");
	}

	private ReasonConditionValue toApiValue(AccountAttributeType type, String value) {
		return switch (type) {
			case BOOLEAN -> new EligibilityReasonConditionScalarValue(Boolean.valueOf(value));
			case DATE, TEXT -> new EligibilityReasonConditionScalarValue(value);
			case DECIMAL, INTEGER -> new EligibilityReasonConditionScalarValue(new BigDecimal(value));
		};
	}

	private Object extractScalarValue(ReasonConditionValue value) {
		if (value instanceof EligibilityReasonConditionScalarValue scalarValue) {
			return scalarValue.getValue();
		}

		throw new IllegalArgumentException("Condition value format is not supported");
	}

	private Comparator<EligibilityReasonConditionEntity> conditionComparator() {
		return Comparator.comparing(
				(EligibilityReasonConditionEntity condition) -> condition.getAttribute().getAttributeCode())
				.thenComparing(condition -> condition.getId().getOperator());
	}

	private String toConditionSummary(EligibilityReasonConditionEntity entity) {
		return entity.getAttribute().getAttributeCode() + " " + entity.getId().getOperator() + " "
				+ entity.getAttributeValue();
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
