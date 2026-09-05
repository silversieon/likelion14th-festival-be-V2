/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.event.listener;

import com.skulikelion.festival.domain.order.event.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.event.payload.WaitingOrderPayload;

public interface OrderEventListener {

  void handleWaitingOrderEvent(WaitingOrderPayload waitingOrderPayload);

  void handleCookingOrderEvent(CookingOrderPayload cookingOrderPayload);

  void handleCompletedOrderEvent(CompletedOrderPayload completedOrderPayload);

  void handleCanceledOrderEvent(CanceledOrderPayload canceledOrderPayload);

  void handleOrderItemUnitStatusEvent(OrderItemUnitStatusPayload orderItemUnitStatusPayload);

  void handleDismissOrderEvent(DismissOrderPayload dismissOrderPayload);
}
