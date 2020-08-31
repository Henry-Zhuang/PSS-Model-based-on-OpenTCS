/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.outbound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.order.OutboundOrder;
import org.opentcs.data.order.OutboundOrder.State;
import org.opentcs.components.kernel.services.OrderEnableService;

/**
 * 经过出库传送带后料箱到达的分拣工作台.
 * @author Henry
 */
public class OutboundWorkingSet {
  /**
   * 工作台的数量，每个工作台可用于分拣一个出库订单.
   */
  public static final int WORKING_ORDER_NUM = 4;
  /**
   * 工作台列表，用于表示当前各工作台上分拣的出库订单名.
   */
  private final List<TCSObjectReference<OutboundOrder>> workingSets = new ArrayList<>();
  /**
   * 订单激活服务，用于分解出库订单并将其放入工作台列表.
   */
  private final OrderEnableService orderEnableService;
  /**
   * 用零长度的byte数组作为同步锁.
   */
  private final byte[] lock = new byte[0];
  
  @Inject
  public OutboundWorkingSet(OrderEnableService orderEnableService) {
    this.orderEnableService = orderEnableService;
  }
  
  public List<TCSObjectReference<OutboundOrder>> getWorkingSets(){
    synchronized(lock){
      return workingSets;
    }
  }
  
  public void removePickingOrder(TCSObjectReference<OutboundOrder> pickingOrder){
    synchronized(lock){
      workingSets.remove(pickingOrder);
    }
  }
  
  public void enableOutboundOrder(){
    List<OutboundOrder> sortedOrders = 
        orderEnableService.fetchObjects(OutboundOrder.class,order -> order.hasState(State.WAITING))
            .stream()
            .sorted(Comparator.comparing(OutboundOrder::getDeadline))
            .collect(Collectors.toList());
    for(int i=0;i < WORKING_ORDER_NUM - workingSets.size();i++){
      synchronized(lock){
        workingSets.add(sortedOrders.get(i).getReference());
      }
    }
  }
}
