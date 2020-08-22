/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import java.util.Set;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.OutboundOrderCreationTO;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.order.OutboundOrder;

/**
 *
 * @author Henry
 */
public interface OutboundOrderService  
    extends TCSObjectService {
  
  OutboundOrder createOutboundOrder(OutboundOrderCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, KernelRuntimeException;
  
  public OutboundOrder removeOutboundOrder(TCSObjectReference<OutboundOrder> ref)
      throws ObjectUnknownException;
  
  void addAssignedBin(TCSObjectReference<OutboundOrder> orderRef, TCSObjectReference<Bin> binRef) 
      throws ObjectUnknownException;
  
  void reserveSKUs(TCSObjectReference<OutboundOrder> orderRef, Set<Bin.SKU> reservedSKUs)
      throws ObjectUnknownException;
  
  OutboundOrder pickSKUs(TCSObjectReference<OutboundOrder> orderRef, Set<Bin.SKU> pickedSKUs)
      throws ObjectUnknownException;
  
  void updateOutboundOrderState(TCSObjectReference<OutboundOrder> orderRef,
                                            OutboundOrder.State state)
      throws ObjectUnknownException;
}
