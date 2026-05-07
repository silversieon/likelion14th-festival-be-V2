/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.payload;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;

import lombok.Builder;

@Builder
public record WaitingOrderPayload(Booth booth, WaitingOrderResponse waitingOrderResponse) {}
