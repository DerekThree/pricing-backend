package com.pricing.backend.eligibilityreason;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibilityReasonRepository extends JpaRepository<EligibilityReasonEntity, Long> {

	@EntityGraph(attributePaths = {"conditions", "conditions.attribute"})
	List<EligibilityReasonEntity> findAllByOrderByReasonCodeAsc();

	@Override
	@EntityGraph(attributePaths = {"conditions", "conditions.attribute"})
	Optional<EligibilityReasonEntity> findById(Long id);

	Optional<EligibilityReasonEntity> findFirstByConditions_Attribute_IdOrderByReasonCodeAsc(Long attributeId);
}
