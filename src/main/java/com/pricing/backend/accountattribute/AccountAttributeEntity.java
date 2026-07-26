package com.pricing.backend.accountattribute;

import java.time.OffsetDateTime;

import com.pricing.backend.generated.model.AttributeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Setter
@NoArgsConstructor
@Table(name = "account_attributes")
public class AccountAttributeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "attribute_code", nullable = false, unique = true, length = 8)
	private String attributeCode;

	@Column(name = "attribute_name", nullable = false, length = 100)
	private String attributeName;

	@Enumerated(EnumType.STRING)
	@Column(name = "attribute_type", nullable = false, length = 20)
	private AttributeType attributeType;

	@Column(name = "updated_on", nullable = false)
	private OffsetDateTime updatedOn;

	@Column(name = "updated_by", nullable = false, length = 100)
	private String updatedBy;
}
