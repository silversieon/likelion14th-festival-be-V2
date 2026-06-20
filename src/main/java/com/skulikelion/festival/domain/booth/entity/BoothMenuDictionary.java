/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "booth_menu_dictionary")
public class BoothMenuDictionary {

  @Id
  @Column(name = "name_ko")
  String nameKo;

  @Column(name = "name_en")
  String nameEn;

  @Column(name = "name_zh")
  String nameZh;
}
