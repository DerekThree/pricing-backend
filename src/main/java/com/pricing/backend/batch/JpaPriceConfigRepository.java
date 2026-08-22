package com.pricing.backend.batch;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.pricingplan.PricingPlanEntity;
import com.pricing.backend.product.ProductEntity;
import com.pricing.backend.region.RegionEntity;
import com.pricing.engine.PriceConfig;
import com.pricing.engine.PriceConfigRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaPriceConfigRepository implements PriceConfigRepository {

	private final EntityManager entityManager;
	private final PriceConfigMapper mapper;

	public JpaPriceConfigRepository(EntityManager entityManager, PriceConfigMapper mapper) {
		this.entityManager = entityManager;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public PriceConfig load(Set<LocalDate> pricingDates) {
		List<ProductEntity> products = entityManager
				.createQuery("select product from ProductEntity product", ProductEntity.class)
				.getResultList();
		List<BranchEntity> branches = entityManager
				.createQuery("select branch from BranchEntity branch", BranchEntity.class)
				.getResultList();
		List<RegionEntity> regions = entityManager
				.createQuery("select distinct region from RegionEntity region", RegionEntity.class)
				.getResultList();
		if (pricingDates.isEmpty()) {
			return mapper.toPriceConfig(products, branches, regions, List.of());
		}

		LocalDate earliestPricingDate = Collections.min(pricingDates);
		LocalDate latestPricingDate = Collections.max(pricingDates);
		TypedQuery<PricingPlanEntity> pricingPlanQuery = entityManager.createQuery("""
				select distinct pricingPlan
				from PricingPlanEntity pricingPlan
				join fetch pricingPlan.product
				join fetch pricingPlan.region
				left join fetch pricingPlan.fees pricingPlanFee
				left join fetch pricingPlanFee.fee
				where pricingPlan.activeFrom <= :latestPricingDate
					and pricingPlan.activeThrough >= :earliestPricingDate
				""", PricingPlanEntity.class);
		pricingPlanQuery.setParameter("latestPricingDate", latestPricingDate);
		pricingPlanQuery.setParameter("earliestPricingDate", earliestPricingDate);
		List<PricingPlanEntity> pricingPlans = pricingPlanQuery.getResultList();
		return mapper.toPriceConfig(products, branches, regions, pricingPlans);
	}
}
