package com.pricing.backend.eligibilityreason;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
@Setter
@Table(name = "eligibility_reason_conditions")
public class EligibilityReasonConditionEntity {

	@EmbeddedId
	private EligibilityReasonConditionId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("reasonId")
	@JoinColumn(name = "reason_id", nullable = false)
	private EligibilityReasonEntity reason;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("attributeId")
	@JoinColumn(name = "attribute_id", nullable = false)
	private AccountAttributeEntity attribute;

	@Column(name = "attribute_value", nullable = false)
	private String attributeValue;
}
