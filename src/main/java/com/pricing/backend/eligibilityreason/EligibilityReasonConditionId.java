package com.pricing.backend.eligibilityreason;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
public class EligibilityReasonConditionId implements Serializable {

	@Column(name = "reason_id", nullable = false)
	private Long reasonId;

	@Column(name = "attribute_id", nullable = false)
	private Long attributeId;

	@Column(name = "operator", nullable = false, length = 2)
	private String operator;
}
