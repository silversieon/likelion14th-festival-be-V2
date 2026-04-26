/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.mapper;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.manager.dto.response.ManagerResponse;
import com.skulikelion.festival.domain.manager.entity.Manager;

@Component
public class ManagerMapper {

  public ManagerResponse toManagerResponse(Manager manager) {
    return ManagerResponse.builder()
        .managerId(manager.getId())
        .department(manager.getDepartment())
        .role(manager.getRole())
        .build();
  }
}
