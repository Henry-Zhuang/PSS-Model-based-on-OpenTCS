/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.BinOrderCreationTO;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.BinOrder;

/**
 *
 * @author Henry
 */
public interface BinOrderService 
    extends TCSObjectService {
  
  BinOrder createBinOrder(BinOrderCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, KernelRuntimeException;
  
  public BinOrder removeBinOrder(TCSObjectReference<BinOrder> ref)
      throws ObjectUnknownException;
  
  void updateBinOrderAttachedTOrder(TCSObjectReference<BinOrder> binOrderRef,
                                            TCSObjectReference<TransportOrder> tOrderRef)
      throws ObjectUnknownException;
  
  void updateBinOrderState(TCSObjectReference<BinOrder> binOrderRef,
                                            BinOrder.State state)
      throws ObjectUnknownException;
  
  void enableBinOrderForIdleVehicle();

}
