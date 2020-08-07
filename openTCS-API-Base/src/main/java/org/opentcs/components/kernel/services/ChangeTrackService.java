/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Point;
import org.opentcs.data.order.TransportOrder;

/**
 *
 * @author Henry
 */
public interface ChangeTrackService 
    extends TCSObjectService {
  void initTrackList();
  void createChangeTrackOrder(String vehicleName);
  void notifyBinVehicle(String orderName);
  void notifyTrackVehicle(String orderName);
  void updateTrackOrder(TCSObjectReference<TransportOrder> orderRef, TransportOrder.State state);
  boolean isEnteringFirstTrackPoint(Point dstPoint, String orderName);
  boolean isLeavingFirstTrackPoint(Point srcPoint, String orderName);
}
