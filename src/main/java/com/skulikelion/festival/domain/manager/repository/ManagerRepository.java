/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.manager.entity.Manager;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
  Optional<Manager> findByUsername(String username);
}
