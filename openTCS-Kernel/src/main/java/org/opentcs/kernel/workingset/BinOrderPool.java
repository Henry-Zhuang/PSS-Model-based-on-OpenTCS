/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.workingset;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.access.to.order.BinOrderCreationTO;
import org.opentcs.access.to.order.TransportOrderCreationTO;
import org.opentcs.components.kernel.services.ChangeTrackService;
import org.opentcs.components.kernel.services.TransportOrderService;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObject;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.OrderBinConstants;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.BinOrder;
import static org.opentcs.kernel.services.StandardDataBaseService.locationPosition;
import static org.opentcs.util.Assertions.checkArgument;
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

  private final TransportOrderService orderService;
  
  private final ChangeTrackService changeTrackService;
  /**
   * Creates a new instance.
   *
   * @param objectPool The object pool serving as the container for this order pool's data.
   * @param orderService The kernel's transport order service.
   * @param changeTrackService The kernel's change-track service
   */
  @Inject
  public BinOrderPool(TCSObjectPool objectPool,
                            TransportOrderService orderService,
                            ChangeTrackService changeTrackService) {
    this.objectPool = requireNonNull(objectPool, "objectPool");
    this.orderService = requireNonNull(orderService, "orderService");
    this.changeTrackService = requireNonNull(changeTrackService, "changeTrackService");
  }

  /**
   * Returns the <code>TCSObjectPool</code> serving as the container for this
   * order pool's data.
   *
   * @return The <code>TCSObjectPool</code> serving as the container for this
   * order pool's data.
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
   * @param to The transfer object from which to create the new transport order bin.
   * @return The newly created transport order.
   * @throws ObjectExistsException If an object with the new object's name already exists.
   * @throws ObjectUnknownException If any object referenced in the TO does not exist.
   * @throws IllegalArgumentException If the order is supposed to be part of an order sequence, but
   * the sequence is already complete, the categories of the two differ or the intended vehicles of
   * the two differ.
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
    Bin bin = objectPool.getObject(Bin.class, newOrder.getBinID());
    objectPool.replaceObject(bin.withAssignedBinOrder(newOrder.getReference()));
    objectPool.addObject(newOrder);
    objectPool.emitObjectEvent(newOrder.clone(), null, TCSObjectEvent.Type.OBJECT_CREATED);
    // Return the newly created transport order.
    return newOrder;
  }
  
  /**
   * Sets a bin Order's state.
   *
   * @param ref A reference to the bin order to be modified.
   * @param newState The bin order's new state.
   * @return The modified bin order.
   * @throws ObjectUnknownException If the referenced bin order is not
   * in this pool.
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
   * Set a bin order's attached transport order.
   *
   * @param binOrderRef A reference to the bin order to be modified.
   * @param tOrderRef A reference to the transport order to be attached.
   * @return The modified bin order.
   * @throws ObjectUnknownException If the referenced transport order and bin order are not in this pool.
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
   * Removes the referenced bin order from this pool.
   *
   * @param ref A reference to the bin order to be removed.
   * @return The removed bin order.
   * @throws ObjectUnknownException If the referenced bin order is not in this pool.
   */
  @SuppressWarnings("deprecation")
  public BinOrder removeBinOrder(TCSObjectReference<BinOrder> ref)
      throws ObjectUnknownException {
    LOG.debug("method entry");
    BinOrder order = objectPool.getObject(BinOrder.class, ref);
    // Make sure orders currently being processed are not removed.
    if(order.getAttachedTransportOrder()!=null){
      TransportOrder tOrder 
          = objectPool.getObject(TransportOrder.class, order.getAttachedTransportOrder());
      checkArgument(!tOrder.hasState(TransportOrder.State.BEING_PROCESSED),
                    "Transport order %s is being processed.",
                    tOrder.getName());
      objectPool.removeObject(tOrder.getReference());
      objectPool.emitObjectEvent(null,
                                 tOrder.clone(),
                                 TCSObjectEvent.Type.OBJECT_REMOVED);
    }
      
    objectPool.removeObject(ref);
    objectPool.emitObjectEvent(null,
                               order.clone(),
                               TCSObjectEvent.Type.OBJECT_REMOVED);
    return order;
  }

  public void enableBinOrderForIdleVehicle(Vehicle vehicle){
    LOG.debug("method entry");
    BinOrder binOrder 
        =  objectPool.getObjects(BinOrder.class).stream()
                .filter(bOrder -> bOrder.hasState(BinOrder.State.AWAIT_DISPATCH))
                .filter(inTheSameTrackWith(vehicle))
                .sorted(Comparator.comparing(BinOrder::getDeadline))
                .findFirst()
                .orElse(null);
    
    if(binOrder == null){
      if(vehicle != null)
        changeTrackService.createChangeTrackOrder(vehicle.getName());
      return;
    }
    
    Bin bin = objectPool.getObject(Bin.class, binOrder.getBinID());
    
    TransportOrderCreationTO to = new TransportOrderCreationTO(binOrder.getName()
                                                              +"["
                                                              +bin.getAttachedLocation().getName()
                                                              +":"
                                                              +bin.getBinPosition()
                                                              +"]");
    if (binOrder.hasType(OrderBinConstants.TYPE_OUTBOUND)){
      
      to = to.withDestinations(outboundDestinations(bin))
          .withIntendedVehicleName(vehicle.getName())
          //.withDependencyNames(new HashSet<>(order.getDependencies()))
          .withDeadline(binOrder.getDeadline())
          .withProperties(binOrder.getProperties())
          .withAttachedBinOrder(binOrder.getReference());
    }
    else if(binOrder.hasType(OrderBinConstants.TYPE_INBOUND)){
      
    }
    try{
      setBinOrderAttachedTOrder(binOrder.getReference()
                                    ,orderService.createTransportOrder(to).getReference());
      setBinOrderState(binOrder.getReference(), BinOrder.State.DISPATCHED);
    }
    catch (ObjectUnknownException | ObjectExistsException exc) {
      throw new IllegalStateException("Unexpectedly interrupted",exc);
    }
    catch (KernelRuntimeException exc) {
      throw new KernelRuntimeException(exc.getCause());
    }
  }

  private Predicate<BinOrder> inTheSameTrackWith(Vehicle vehicle) {
    return binOrder -> {
      Bin bin = objectPool.getObject(Bin.class,binOrder.getBinID());
      if(bin.getAttachedLocation() == null){
        LOG.error("A bin {} which has been attached to a TransportOrder, has unknown location.",bin.getName());
        return false;
      }
      
      int binTrack = bin.getPsbTrack();
      int vehicleTrack = objectPool.getObject(Point.class,
                                               vehicle.getCurrentPosition()).getPsbTrack();
      return binTrack == vehicleTrack;
    };
  }
  
  private List<DestinationCreationTO> outboundDestinations(Bin bin){
    String locationName = bin.getAttachedLocation().getName();
    int psbTrack = bin.getPsbTrack();
    int pstTrack = bin.getPstTrack();
    int binPosition = bin.getBinPosition();
    
    List<DestinationCreationTO> result = new ArrayList<>();
    int stackSize = getStackSize(locationName);
    List<String> tmpLocs = getVacantNeighbours(psbTrack, pstTrack, stackSize - 1 - binPosition);
    if(tmpLocs == null)
      return null;
    // 该轨道的分拣台
    String pickStation = getOutBoundStation(psbTrack);

    // 倒箱
    for(String tmpLoc:tmpLocs){
      result.add(new DestinationCreationTO(locationName, OrderBinConstants.OPERATION_LOAD));
      result.add(new DestinationCreationTO(tmpLoc, OrderBinConstants.OPERATION_UNLOAD));
    }
   
    // 指定料箱出库
    result.add(new DestinationCreationTO(locationName, OrderBinConstants.OPERATION_LOAD));
    result.add(new DestinationCreationTO(pickStation, OrderBinConstants.OPERATION_UNLOAD));

    return result;
  }
  
  private List<String> getVacantNeighbours(int psbTrack, int pstTrack, int vacancyNum){
    int offset = 1;
    int row = psbTrack - 1;
    int column = pstTrack - 1;
    List<String> vacantNeighbours = new ArrayList<>();
      while(vacancyNum > 0 
          && (column-offset >= 0 || column+offset < locationPosition[row].length)){
        // A vacant neighbour firstly should be valid.
        // A picking station is not considered as a vacant neighbour.
        if(column-offset >= 0
            && locationPosition[row][column-offset] != null
            && !isOutBoundStation(locationPosition[row][column-offset])){
          int vacancy = Location.BINS_MAX_NUM - getStackSize(locationPosition[row][column-offset]);
          vacancy = vacancy > vacancyNum ? vacancyNum : vacancy;
          for(int i=0;i<vacancy;i++)
            vacantNeighbours.add(locationPosition[row][column-offset]);
          vacancyNum -= vacancy;
        }
        
        if(vacancyNum > 0 
            && column+offset < locationPosition[row].length
            && locationPosition[row][column+offset] != null
            && !isOutBoundStation(locationPosition[row][column+offset])){
          int vacancy = Location.BINS_MAX_NUM - getStackSize(locationPosition[row][column+offset]);
          vacancy = vacancy > vacancyNum ? vacancyNum : vacancy;
          for(int i=0;i<vacancy;i++)
            vacantNeighbours.add(locationPosition[row][column+offset]);
          vacancyNum -= vacancy;
        }
        offset++;
      }
    return vacancyNum <= 0 ? vacantNeighbours : null;
  }
  
  private int getStackSize(String locationName){
    return objectPool.getObject(Location.class, locationName).stackSize();
  }
  
  private String getOutBoundStation(int psbTrack){
    for(String location:locationPosition[psbTrack-1]){
      if(location != null && isOutBoundStation(location))
        return location;
    }
    return null;
  }
  
  private boolean isOutBoundStation(String locationName) {
    return objectPool.getObject(Location.class,locationName)
              .getType().getName().equals(Location.OUT_BOUND_STATION_TYPE);
  }
}

