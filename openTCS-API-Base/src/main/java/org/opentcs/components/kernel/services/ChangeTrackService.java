/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.TransportOrder;

/**
 *
 * @author Henry
 */
public interface ChangeTrackService 
    extends TCSObjectService {
  /**
   * 更新无车轨道列表.
   * 换轨服务需要维护一个列表，以记录哪些轨道当前没有PSB或者没有PSB将要前往该轨道.
   */
  void updateTrackList();
  
  /**
   * 清空无车轨道列表
   */
  void clear();
  
  /**
   * 提醒换轨服务，车辆状态发生变化，换轨服务需要更新无车轨道列表.
   */
  void setVehicleStateChanged();
  
  /**
   * 将指定车辆（PSB）换轨到指定料箱所在的轨道.
   * @param bin 指定料箱
   * @param vehicleName 指定车辆ID
   * @return 生成的指定车辆（PSB）的换轨任务ID.
   */
  String createChangeTrackOrder(Bin bin, Vehicle vehicleName);
  
  /**
   * 提醒PSB，PST已就位.
   * @param orderName 换轨任务集ID
   */
  void notifyBinVehicle(String orderName);
  /**
   * 提醒PST，PSB已就位
   * @param orderName 换轨任务集ID
   */
  void notifyTrackVehicle(String orderName);
  
  /**
   * 根据换轨任务的完成状态，更新换轨任务池和无车轨道列表.
   * @param orderRef 换轨任务的索引
   * @param state 换轨任务的状态
   */
  void updateTrackOrder(TCSObjectReference<TransportOrder> orderRef, TransportOrder.State state);
  
  /**
   * 判断即将进入的目标点是否为指定换轨任务集的第一换轨点.
   * <p>PSB需要进行这种判断；当判定为true时，PSB需要等待PST就位.</p>
   * @param dstPoint 即将进入的目标点.
   * @param orderName 换轨任务集ID
   * @return 当且仅当即将进入的目标点是指定换轨任务集的第一换轨点时，返回{@code true}.
   */
  boolean isEnteringFirstTrackPoint(Point dstPoint, String orderName);
  
  /**
   * 判断即将离开的起始点是否为指定换轨任务集的第一换轨点.
   * <p>PST需要进行这种判断；当判定为true时，PST需要等待PSB就位.</p>
   * @param srcPoint 即将离开的起始点.
   * @param orderName 换轨任务集ID
   * @return 当且仅当即将离开的起始点是指定换轨任务集的第一换轨点时，返回{@code true}.
   */
  boolean isLeavingFirstTrackPoint(Point srcPoint, String orderName);
  
  /**
   * 判断指定的轨道是否是无车轨道.
   * <p>只需判断无车轨道列表是否包含该轨道</p>
   * @param psbTrack 待判断的轨道
   * @return 当前仅当指定的轨道是无车轨道时，返回{@code true}.
   */
  boolean isNoVehicleTrack(int psbTrack);
}
