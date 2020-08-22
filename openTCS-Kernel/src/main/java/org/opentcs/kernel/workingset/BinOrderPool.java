/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.workingset;

import java.time.Instant;
import java.util.HashSet;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.opentcs.access.to.order.BinOrderCreationTO;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObject;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.BinOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henry
 */
public class BinOrderPool {
  
  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(TransportOrderPool.class);
  /**
   * The system's global object pool.
   */
  private final TCSObjectPool objectPool;

  private final TransportOrderPool tOrderPool;
  
  /**
   * Creates a new instance.
   *
   * @param objectPool The object pool serving as the container for this binOrder pool's data.
   * @param tOrderPool The kernel's transport binOrder pool.
   */
  @Inject
  public BinOrderPool(TCSObjectPool objectPool,
                            TransportOrderPool tOrderPool) {
    this.objectPool = requireNonNull(objectPool, "objectPool");
    this.tOrderPool = requireNonNull(tOrderPool, "tOrderPool");
  }

  /**
   * Returns the <code>TCSObjectPool</code> serving as the container for this
 binOrder pool's data.
   *
   * @return The <code>TCSObjectPool</code> serving as the container for this
 binOrder pool's data.
   */
  public TCSObjectPool getObjectPool() {
    LOG.debug("method entry");
    return objectPool;
  }

  /**
   * Removes all transport orders from this pool.
   */
  public void clear() {
    Set<TCSObject<?>> objects = objectPool.getObjects((Pattern) null);
    Set<String> removableNames = new HashSet<>();
    for (TCSObject<?> curObject : objects) {
      if (curObject instanceof BinOrder) {
        removableNames.add(curObject.getName());
      }
    }
    objectPool.removeObjects(removableNames);
  }

  /**
   * Adds a new bin order to the pool.
   * This method implicitly adds the transport order to its wrapping sequence, if any.
   *
   * @param to The transfer object from which to create the new transport binOrder bin.
   * @return The newly created transport binOrder.
   * @throws ObjectExistsException If an object with the new object's name already exists.
   * @throws ObjectUnknownException If any object referenced in the TO does not exist.
   * @throws IllegalArgumentException If the binOrder is supposed to be part of an binOrder sequence, but
 the sequence is already complete, the categories of the two differ or the intended vehicles of
 the two differ.
   */
  @SuppressWarnings("deprecation")
  public BinOrder createBinOrder(BinOrderCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, IllegalArgumentException {
    LOG.debug("method entry");
    BinOrder newOrder = new BinOrder(to.getName(),to.getType())
        .withCreationTime(Instant.now())
        .withDeadline(to.getDeadline())
        .withBinID(to.getBinID())
        .withCustomerOrderName(to.getCustomerOrderName())
        .withRequiredSku(to.getRequiredSku())
        .withState(BinOrder.State.AWAIT_DISPATCH)
        .withProperties(to.getProperties());
//    Bin bin = objectPool.getObject(Bin.class, newOrder.getBinID());
//    objectPool.replaceObject(bin.withAssignedBinOrder(newOrder.getReference()));
    objectPool.addObject(newOrder);
    objectPool.emitObjectEvent(newOrder.clone(), null, TCSObjectEvent.Type.OBJECT_CREATED);
    // Return the newly created transport binOrder.
    return newOrder;
  }
  
  /**
   * Sets a bin Order's state.
   *
   * @param ref A reference to the bin binOrder to be modified.
   * @param newState The bin binOrder's new state.
   * @return The modified bin binOrder.
   * @throws ObjectUnknownException If the referenced bin binOrder is not
 in this pool.
   */
  @SuppressWarnings("deprecation")
  public BinOrder setBinOrderState(TCSObjectReference<BinOrder> ref,
                                               BinOrder.State newState)
      throws ObjectUnknownException {
    LOG.debug("method entry");
    BinOrder order = objectPool.getObject(BinOrder.class, ref);
    BinOrder previousState = order.clone();
    order = objectPool.replaceObject(order.withState(newState));
    objectPool.emitObjectEvent(order.clone(),
                               previousState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
    return order;
  }

  /**
   * Set a bin binOrder's attached transport binOrder.
   *
   * @param binOrderRef A reference to the bin binOrder to be modified.
   * @param tOrderRef A reference to the transport binOrder to be attached.
   * @return The modified bin binOrder.
   * @throws ObjectUnknownException If the referenced transport binOrder and bin binOrder are not in this pool.
   */
  @SuppressWarnings("deprecation")
  public BinOrder setBinOrderAttachedTOrder(TCSObjectReference<BinOrder> binOrderRef,
                                                     TCSObjectReference<TransportOrder> tOrderRef)
      throws ObjectUnknownException{
    LOG.debug("method entry");
    BinOrder binOrder = objectPool.getObject(BinOrder.class, binOrderRef);
    BinOrder previousState = binOrder.clone();
    binOrder = objectPool.replaceObject(binOrder.withAttachedTransportOrder(tOrderRef));
    objectPool.emitObjectEvent(binOrder.clone(),
                               previousState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
    return binOrder;
  }

  /**
   * Removes the referenced bin binOrder from this pool.
   *
   * @param ref A reference to the bin binOrder to be removed.
   * @return The removed bin binOrder.
   * @throws ObjectUnknownException If the referenced bin binOrder is not in this pool.
   */
  @SuppressWarnings("deprecation")
  public BinOrder removeBinOrder(TCSObjectReference<BinOrder> ref)
      throws ObjectUnknownException {
    LOG.debug("method entry");
    BinOrder binOrder = objectPool.getObject(BinOrder.class, ref);

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