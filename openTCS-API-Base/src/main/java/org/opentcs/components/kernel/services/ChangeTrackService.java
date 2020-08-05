/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.order.TransportOrder;

/**
 *
 * @author admin
 */
public interface ChangeTrackService 
    extends TCSObjectService {
  void initialize();
  void createChangeTrackOrder(String vehicleName);
  void notifyBinVehicle(String orderName);
  void notifyTrackVehicle(String orderName);
  void updateTrackOrder(TCSObjectReference<TransportOrder> orderRef, TransportOrder.State state);
}