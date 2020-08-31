/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.InboundOrderCreationTO;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.InboundOrder;

/**
 *
 * @author Henry
 */
public interface InboundOrderService 
    extends TCSObjectService {
  
  InboundOrder createInboundOrder(InboundOrderCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, KernelRuntimeException;
  
  InboundOrder setInboundOrderBin(TCSObjectReference<InboundOrder> ref,
                            Bin bin)
      throws ObjectUnknownException;
  
  InboundOrder setInboundOrderState(TCSObjectReference<InboundOrder> ref,
                            InboundOrder.State newState)
      throws ObjectUnknownException;
  
  InboundOrder setInboundOrderAttachedTOrder(TCSObjectReference<InboundOrder> binOrderRef,
                                     TCSObjectReference<TransportOrder> tOrderRef)
      throws ObjectUnknownException;
  
  public InboundOrder removeInboundOrder(TCSObjectReference<InboundOrder> ref)
      throws ObjectUnknownException;

}
