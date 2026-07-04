package com.pricing.backend.pricingplan;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "pricing_plans")
public class PricingPlanEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "plan_code", nullable = false, unique = true, length = 8)
	private String planCode;

	@Column(name = "plan_name", nullable = false, length = 100)
	private String planName;

	@Column(name = "product_code", nullable = false, length = 8)
	private String productCode;

	@Column(name = "product_name", nullable = false, length = 100)
	private String productName;

	@Column(name = "region_code", nullable = false, length = 8)
	private String regionCode;

	@Column(name = "region_name", nullable = false, length = 100)
	private String regionName;

	@Column(name = "active_from", nullable = false)
	private OffsetDateTime activeFrom;

	@Column(name = "active_to", nullable = false)
	private OffsetDateTime activeTo;

	@Column(name = "updated_on", nullable = false)
	private OffsetDateTime updatedOn;

	@Column(name = "updated_by", nullable = false, length = 100)
	private String updatedBy;
}
