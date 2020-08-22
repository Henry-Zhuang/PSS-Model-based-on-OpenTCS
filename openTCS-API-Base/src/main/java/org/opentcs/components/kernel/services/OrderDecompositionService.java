/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import org.opentcs.data.TCSObject;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Vehicle;

/**
 * 订单分解服务接口
 * @author Henry
 */
public interface OrderDecompositionService 
    extends TCSObjectService{
  /**
   * 根据指定的料箱，生成将该料箱出库或入库的运输订单.
   * @param bin 指定的料箱.
   * @param idleVehicle 指定的空闲车辆
   * @param order 入库或出库订单
   */
  void createTransportOrderForBin(Bin bin, Vehicle idleVehicle, TCSObject<?> order);
  /**
   * 为出库订单预订合适的料箱，并激活该料箱的搬运任务，将其分配给空闲PSB去运行.
   */
  void decomposeOutboundOrder();
  /**
   * 更新每个点和库位站的轨道信息，仅在内核创建模型时运行一次.
   * <p>轨道信息主要包括处于哪一条PSB轨道(psbTrack)，和处于哪一条PST轨道(pstTrack).
   * </p>注意：此处的PST轨道指的是与PSB轨道垂直的方向上的位置，可以理解为行与列的关系.
   */
  void updateTrackInfo();
  
  String[][] getLocPosition();
}
