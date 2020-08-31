/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.services;

import java.time.Instant;
import static java.util.Objects.requireNonNull;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.InboundOrderCreationTO;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.InboundOrder;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.opentcs.components.kernel.services.InboundOrderService;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.model.Bin;
import org.opentcs.kernel.workingset.TransportOrderPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henry
 */
public class StandardInboundOrderService 
    extends AbstractTCSObjectService
    implements InboundOrderService{
  
  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(StandardInboundOrderService.class);
  /**
   * A global object to be used for synchronization within the kernel.
   */
  private final Object globalSyncObject;
  /**
   * The container of all course model and order objects.
   */
  private final TCSObjectPool objectPool;
  /**
   * The transport order pool.
   */
  private final TransportOrderPool tOrderPool;
  
  /**
   * Creates a new instance.
   *
   * @param objectService The openTCS object service.
   * @param globalSyncObject The kernel threads' global synchronization object.
   * @param globalObjectPool The object pool to be used.
   * @param tOrderPool The transport order pool.
   */
  @Inject
  public StandardInboundOrderService(TCSObjectService objectService,
                                       @GlobalSyncObject Object globalSyncObject,
                                       TCSObjectPool globalObjectPool,
                                       TransportOrderPool tOrderPool) {
    super(objectService);
    this.globalSyncObject = requireNonNull(globalSyncObject, "globalSyncObject");
    this.objectPool = requireNonNull(globalObjectPool, "globalObjectPool");
    this.tOrderPool = requireNonNull(tOrderPool,"tOrderPool");
  }
  
  @Override
  public InboundOrder createInboundOrder(InboundOrderCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, KernelRuntimeException{
    synchronized (globalSyncObject) {
      LOG.info("Create InboundOrder "+to.getName());
      InboundOrder newOrder = new InboundOrder(to.getName(),to.getBin())
          .withCreationTime(Instant.now())
          .withDeadline(to.getDeadline())
          .withAssignedBinStack(to.getAssignedBinStack())
          .withState(InboundOrder.State.AWAIT_DISPATCH)
          .withProperties(to.getProperties());

      objectPool.addObject(newOrder);
      objectPool.emitObjectEvent(newOrder.clone(), null, TCSObjectEvent.Type.OBJECT_CREATED);

      return newOrder;
    }
  }

  @Override
  public InboundOrder setInboundOrderBin(TCSObjectReference<InboundOrder> ref, Bin bin)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.debug("method entry");
      InboundOrder order = fetchObject(InboundOrder.class, ref);
      InboundOrder previousState = order.clone();
      order = objectPool.replaceObject(order.withBin(bin));
      objectPool.emitObjectEvent(order.clone(),
                                 previousState,
                                 TCSObjectEvent.Type.OBJECT_MODIFIED);
      return order;
    }
  }
  
  @Override
  public InboundOrder setInboundOrderState(TCSObjectReference<InboundOrder> ref,
                                           InboundOrder.State newState)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.info("Set the state of InboundOrder "+ref.getName()+" to "+newState);
      InboundOrder order = fetchObject(InboundOrder.class, ref);
      InboundOrder previousState = order.clone();
      order = objectPool.replaceObject(order.withState(newState));
      objectPool.emitObjectEvent(order.clone(),
                                 previousState,
                                 TCSObjectEvent.Type.OBJECT_MODIFIED);
      return order;
    }
  }

  @Override
  public InboundOrder setInboundOrderAttachedTOrder(TCSObjectReference<InboundOrder> inOrderRef,
                                                    TCSObjectReference<TransportOrder> tOrderRef)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.info("Create TransportOrder "+tOrderRef.getName()+" for InboundOrder "+inOrderRef.getName());
      InboundOrder inOrder = fetchObject(InboundOrder.class, inOrderRef);
      InboundOrder previousState = inOrder.clone();
      inOrder = objectPool.replaceObject(inOrder.withAttachedTransportOrder(tOrderRef));
      objectPool.emitObjectEvent(inOrder.clone(),
                                 previousState,
                                 TCSObjectEvent.Type.OBJECT_MODIFIED);
      return inOrder;
    }
  }
  
  @Override
  public InboundOrder removeInboundOrder(TCSObjectReference<InboundOrder> ref)
      throws ObjectUnknownException{
    synchronized (globalSyncObject) {
      LOG.debug("method entry");
      InboundOrder binOrder = objectPool.getObject(InboundOrder.class, ref);

      if(binOrder.getAttachedTransportOrder()!=null){
        tOrderPool.removeTransportOrder(binOrder.getAttachedTransportOrder());
      }

      objectPool.removeObject(ref);
      objectPool.emitObjectEvent(null,
                                 binOrder.clone(),
                                 TCSObjectEvent.Type.OBJECT_REMOVED);
      return binOrder;
    }
  }
}
