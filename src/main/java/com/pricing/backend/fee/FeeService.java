package com.pricing.backend.fee;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.generated.model.FeeDetail;
import com.pricing.backend.generated.model.FeeListItem;
import com.pricing.backend.generated.model.FeeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeService {

	private final FeeRepository feeRepository;

	public FeeService(FeeRepository feeRepository) {
		this.feeRepository = feeRepository;
	}

	@Transactional(readOnly = true)
	public List<FeeListItem> list() {
		return feeRepository.findAllByOrderByFeeCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<FeeDetail> get(Long id) {
		return feeRepository.findById(id).map(this::toDetail);
	}

	@Transactional
	public FeeDetail create(FeeRequest request) {
		FeeEntity entity = new FeeEntity();
		apply(entity, request);
		return toDetail(feeRepository.save(entity));
	}

	@Transactional
	public Optional<FeeDetail> update(Long id, FeeRequest request) {
		return feeRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toDetail(feeRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!feeRepository.existsById(id)) {
			return false;
		}

		feeRepository.deleteById(id);
		return true;
	}

	private void apply(FeeEntity entity, FeeRequest request) {
		entity.setFeeCode(request.getFeeCode());
		entity.setFeeName(request.getFeeName());
		entity.setProductTypes(new ArrayList<>(request.getProductTypes()));
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private FeeListItem toListItem(FeeEntity entity) {
		return new FeeListItem(
				entity.getId(),
				formatCodeAndName(entity.getFeeCode(), entity.getFeeName()),
				new ArrayList<>(entity.getProductTypes()),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private FeeDetail toDetail(FeeEntity entity) {
		return new FeeDetail(
				entity.getFeeCode(),
				entity.getFeeName(),
				new ArrayList<>(entity.getProductTypes()),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
