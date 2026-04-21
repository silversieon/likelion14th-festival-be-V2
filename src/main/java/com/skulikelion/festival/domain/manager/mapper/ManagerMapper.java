/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.manager.dto.response.ManagerResponse;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;

@Component
public class ManagerMapper {

  public ManagerResponse toManagerResponse(Manager manager) {
    String boothName;
    if (Objects.requireNonNull(manager.getRole()) == Role.BOOTH_MANAGER) {
      boothName = manager.getBooth().getName();
    } else {
      boothName = null;
    }
    return ManagerResponse.builder()
        .managerId(manager.getId())
        .username(manager.getUsername())
        .role(manager.getRole())
        .boothName(boothName)
        .build();
  }
}
