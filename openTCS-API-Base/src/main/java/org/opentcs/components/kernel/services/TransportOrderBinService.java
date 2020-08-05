/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.TransportOrderBinCreationTO;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.TransportOrderBin;

/**
 *
 * @author Henry
 */
public interface TransportOrderBinService 
    extends TCSObjectService {
  
  TransportOrderBin createTransportOrderBin(TransportOrderBinCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, KernelRuntimeException;
  
  public TransportOrderBin removeTransportOrderBin(TCSObjectReference<TransportOrderBin> ref)
      throws ObjectUnknownException;
  
  void updateTransportOrderBinAttachedTOrder(TCSObjectReference<TransportOrderBin> tOrderBinRef,
                                            TCSObjectReference<TransportOrder> tOrderRef)
      throws ObjectUnknownException;
  
  void updateTransportOrderBinState(TCSObjectReference<TransportOrderBin> tOrderBinRef,
                                            TransportOrderBin.State state)
      throws ObjectUnknownException;
  
  void enableTOrderBinForIdleVehicle();
    
  
}
