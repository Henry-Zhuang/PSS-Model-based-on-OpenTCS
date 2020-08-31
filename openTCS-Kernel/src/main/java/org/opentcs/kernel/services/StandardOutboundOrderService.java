/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.services;

import java.util.Set;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.OutboundOrderCreationTO;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.order.OutboundOrder;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentcs.components.kernel.services.OutboundOrderService;

/**
 * 出库订单服务的标准实现类.
 * @author Henry
 */
public class StandardOutboundOrderService 
    extends AbstractTCSObjectService
    implements OutboundOrderService{
  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(StandardOutboundOrderService.class);
  /**
   * A global object to be used for synchronization within the kernel.
   */
  private final Object globalSyncObject;
  /**
   * The system's global object pool.
   */
  private final TCSObjectPool objectPool;
  
  @Inject
  public StandardOutboundOrderService(TCSObjectService objectService,
                                      @GlobalSyncObject Object globalSyncObject,
                                      TCSObjectPool objectPool) {
    super(objectService);
    this.globalSyncObject = globalSyncObject;
    this.objectPool = objectPool;
  }

  @Override
  public OutboundOrder createOutboundOrder(OutboundOrderCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, KernelRuntimeException {
    synchronized (globalSyncObject) {
      LOG.info("method entry");
      OutboundOrder newOrder = new OutboundOrder(to.getName())
          .withRequiredSKUs(to.getRequiredSKUs())
          .updateTotalAmount()
          .withDeadline(to.getDeadline());
      objectPool.addObject(newOrder);
      objectPool.emitObjectEvent(newOrder, null, TCSObjectEvent.Type.OBJECT_CREATED);
      return newOrder;
    }
  }

  @Override
  public OutboundOrder removeOutboundOrder(TCSObjectReference<OutboundOrder> ref)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.info("method entry");
      OutboundOrder outBoundOrder = objectPool.getObject(OutboundOrder.class, ref);
      if(outBoundOrder.getState() == OutboundOrder.State.WORKING){
      // 遍历出库订单所预订的所有料箱，在料箱的预订表中删去该出库订单的预订
      // 若删完后，料箱预订表为空，则检查该料箱对应的出库运输任务是否已执行
      // 若未执行，则将对应的运输任务删除；否则，不删除；
      // 然后从出库工作台中删去该出库订单
      //OutboundWorkingSet outBoundWorkingSet.removePickingOrder(ref)
      }
      objectPool.removeObject(ref);
      objectPool.emitObjectEvent(null,
                                 outBoundOrder,
                                 TCSObjectEvent.Type.OBJECT_REMOVED);
      return outBoundOrder;
    }
  }

  @Override
  public void addAssignedBin(TCSObjectReference<OutboundOrder> orderRef, TCSObjectReference<Bin> binRef)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.info("method entry");
      OutboundOrder outBoundOrder = objectPool.getObject(OutboundOrder.class,orderRef);
      OutboundOrder previousState = outBoundOrder;
      Set<TCSObjectReference<Bin>> binRefs = outBoundOrder.getAssignedBins();
      binRefs.add(binRef);
      outBoundOrder = objectPool.replaceObject(outBoundOrder.withAssignedBins(binRefs));
      objectPool.emitObjectEvent(outBoundOrder,
                               previousState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
    }
  }
  
  @Override
  public void reserveSKUs(TCSObjectReference<OutboundOrder> orderRef, Set<Bin.SKU> reservedSKUs)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.info("method entry");
      OutboundOrder outBoundOrder = objectPool.getObject(OutboundOrder.class,orderRef);
      OutboundOrder previousState = outBoundOrder;
      outBoundOrder = objectPool.replaceObject(outBoundOrder.afterReservation(reservedSKUs));
      objectPool.emitObjectEvent(outBoundOrder,
                               previousState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
    }
  }

  @Override
  public OutboundOrder pickSKUs(TCSObjectReference<OutboundOrder> orderRef, Set<Bin.SKU> pickedSKUs)
      throws ObjectUnknownException {
     synchronized (globalSyncObject) {
      LOG.info("method entry");
      OutboundOrder outBoundOrder = objectPool.getObject(OutboundOrder.class,orderRef);
      OutboundOrder previousState = outBoundOrder;
      outBoundOrder = objectPool.replaceObject(outBoundOrder.afterPicking(pickedSKUs));
      objectPool.emitObjectEvent(outBoundOrder,
                               previousState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
      return outBoundOrder;
    }
  }

  @Override
  public void updateOutboundOrderState(TCSObjectReference<OutboundOrder> orderRef,
                                       OutboundOrder.State state)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.info("method entry");
      OutboundOrder outBoundOrder = objectPool.getObject(OutboundOrder.class,orderRef);
      OutboundOrder previousState = outBoundOrder;
      outBoundOrder = objectPool.replaceObject(outBoundOrder.withState(state));
      objectPool.emitObjectEvent(outBoundOrder,
                               previousState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
    }
  }
}
