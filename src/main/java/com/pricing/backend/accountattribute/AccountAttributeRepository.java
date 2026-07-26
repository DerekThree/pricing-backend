package com.pricing.backend.accountattribute;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAttributeRepository extends JpaRepository<AccountAttributeEntity, Long> {

	List<AccountAttributeEntity> findAllByOrderByAttributeCodeAsc();
}
