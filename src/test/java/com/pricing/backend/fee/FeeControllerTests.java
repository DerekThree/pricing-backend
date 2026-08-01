package com.pricing.backend.fee;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.model.FeeDetail;
import com.pricing.backend.generated.model.FeeListItem;
import com.pricing.backend.generated.model.FeeRequest;
import com.pricing.backend.generated.model.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeeControllerTests {

	private static final long MISSING_FEE_ID = 999L;

	private FeeService feeService;
	private FeeController feeController;

	@BeforeEach
	void setUp() {
		feeService = mock(FeeService.class);
		feeController = new FeeController(feeService);
	}

	@Test
	void createsFee() {
		FeeDetail createdFee = getFeeDetail();
		FeeRequest request = getRequest(createdFee);
		when(feeService.create(request)).thenReturn(createdFee);

		ResponseEntity<FeeDetail> response = feeController.createFee(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isEqualTo(createdFee);
		verify(feeService).create(request);
	}

	@Test
	void listsFees() {
		FeeListItem firstFee = getFeeListItem();
		FeeListItem secondFee = new FeeListItem(
				2L,
				"FEE00002 - Overdraft Fee",
				ProductType.DEPOSIT,
				OffsetDateTime.parse("2026-07-31T09:00:00+08:00"),
				"John Smith"
		);
		List<FeeListItem> fees = List.of(firstFee, secondFee);
		when(feeService.list()).thenReturn(fees);

		ResponseEntity<List<FeeListItem>> response = feeController.listFees();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(fees);
		verify(feeService).list();
	}

	@Test
	void returnsStoredFee() {
		FeeDetail storedFee = getFeeDetail();
		when(feeService.get(storedFee.getId())).thenReturn(Optional.of(storedFee));

		ResponseEntity<FeeDetail> response = feeController.getFee(storedFee.getId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(storedFee);
		verify(feeService).get(storedFee.getId());
	}

	@Test
	void updatesStoredFee() {
		FeeDetail updatedFee = getFeeDetail();
		FeeRequest request = getRequest(updatedFee);
		when(feeService.update(updatedFee.getId(), request)).thenReturn(Optional.of(updatedFee));

		ResponseEntity<FeeDetail> response = feeController.updateFee(updatedFee.getId(), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(updatedFee);
		verify(feeService).update(updatedFee.getId(), request);
	}

	@Test
	void deletesStoredFee() {
		FeeDetail fee = getFeeDetail();
		when(feeService.delete(fee.getId())).thenReturn(true);

		ResponseEntity<Void> response = feeController.deleteFee(fee.getId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(feeService).delete(fee.getId());
	}

	@Test
	void throwsWhenGettingMissingFee() {
		when(feeService.get(MISSING_FEE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> feeController.getFee(MISSING_FEE_ID))
				.isInstanceOf(RecordNotFoundException.class);

		verify(feeService).get(MISSING_FEE_ID);
	}

	@Test
	void throwsWhenUpdatingMissingFee() {
		FeeRequest request = getRequest(getFeeDetail());
		when(feeService.update(MISSING_FEE_ID, request)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> feeController.updateFee(MISSING_FEE_ID, request))
				.isInstanceOf(RecordNotFoundException.class);

		verify(feeService).update(MISSING_FEE_ID, request);
	}

	@Test
	void throwsWhenDeletingMissingFee() {
		when(feeService.delete(MISSING_FEE_ID)).thenReturn(false);

		assertThatThrownBy(() -> feeController.deleteFee(MISSING_FEE_ID))
				.isInstanceOf(RecordNotFoundException.class);

		verify(feeService).delete(MISSING_FEE_ID);
	}

	private FeeDetail getFeeDetail() {
		return new FeeDetail(
				"FEE00001",
				"Monthly Maintenance Fee",
				ProductType.DEPOSIT,
				"Jane Smith",
				1L,
				OffsetDateTime.parse("2026-07-31T09:00:00+08:00")
		);
	}

	private FeeListItem getFeeListItem() {
		return new FeeListItem(
				1L,
				"FEE00001 - Monthly Maintenance Fee",
				ProductType.DEPOSIT,
				OffsetDateTime.parse("2026-07-31T09:00:00+08:00"),
				"Jane Smith"
		);
	}

	private FeeRequest getRequest(FeeDetail fee) {
		return new FeeRequest(
				fee.getFeeCode(),
				fee.getFeeName(),
				fee.getProductType(),
				fee.getUpdatedBy()
		);
	}
}
