/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.services;

import static java.util.Objects.requireNonNull;
import java.util.Set;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.BinOrderCreationTO;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.BinOrder;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.opentcs.kernel.workingset.BinOrderPool;
import org.opentcs.components.kernel.services.BinOrderService;

/**
 *
 * @author Henry
 */
public class StandardBinOrderService 
    extends AbstractTCSObjectService
    implements BinOrderService{
  /**
   * A global object to be used for synchronization within the kernel.
   */
  private final Object globalSyncObject;
  /**
   * The container of all course model and transport order objects.
   */
  private final TCSObjectPool globalObjectPool;
  /**
   * The order facade to the object pool.
   */
  private final BinOrderPool binOrderPool;
  
  /**
   * Creates a new instance.
   *
   * @param objectService
   * @param globalSyncObject The kernel threads' global synchronization object.
   * @param globalObjectPool The object pool to be used.
   * @param orderBinPool The oder bin pool to be used.
   */
  @Inject
  public StandardBinOrderService(TCSObjectService objectService,
                                       @GlobalSyncObject Object globalSyncObject,
                                       TCSObjectPool globalObjectPool,
                                       BinOrderPool orderBinPool) {
    super(objectService);
    this.globalSyncObject = requireNonNull(globalSyncObject, "globalSyncObject");
    this.globalObjectPool = requireNonNull(globalObjectPool, "globalObjectPool");
    this.binOrderPool = requireNonNull(orderBinPool, "orderPool");
  }
  
  @Override
  public BinOrder createBinOrder(BinOrderCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, KernelRuntimeException{
    synchronized (globalSyncObject) {
      return binOrderPool.createBinOrder(to).clone();
    }
  }
  
  @Override
  public BinOrder removeBinOrder(TCSObjectReference<BinOrder> ref)
      throws ObjectUnknownException{
    synchronized (globalSyncObject) {
      return binOrderPool.removeBinOrder(ref).clone();
    }
  }
  
  @Override
  public void updateBinOrderAttachedTOrder(TCSObjectReference<BinOrder> tOrderBinRef,
                                            TCSObjectReference<TransportOrder> tOrderRef)
      throws ObjectUnknownException{
    synchronized (globalSyncObject) {
      binOrderPool.setBinOrderAttachedTOrder(tOrderBinRef, tOrderRef);
    }
  }
  
  @Override
  public void updateBinOrderState(TCSObjectReference<BinOrder> tOrderBinRef,
                                            BinOrder.State state)
      throws ObjectUnknownException{
    synchronized (globalSyncObject) {
      binOrderPool.setBinOrderState(tOrderBinRef, state);
    }
  }
  
  @Override
  public void enableBinOrderForIdleVehicle(){
    synchronized (globalSyncObject) {
      Set<Vehicle> idleVehicles = fetchObjects(Vehicle.class, this::couldProcessTransportOrder);
      for(Vehicle vehicle : idleVehicles)
        binOrderPool.enableBinOrderForIdleVehicle(vehicle);
    }
  }
  
  private boolean couldProcessTransportOrder(Vehicle vehicle) {
    return vehicle.getIntegrationLevel() == Vehicle.IntegrationLevel.TO_BE_UTILIZED
        && vehicle.getType().equals(Vehicle.BIN_VEHICLE_TYPE)
        && vehicle.getCurrentPosition() != null
        && !vehicle.isEnergyLevelCritical()
        && (processesNoOrder(vehicle)
            || processesDispensableOrder(vehicle));
  }
  
  private boolean processesNoOrder(Vehicle vehicle) {
    return vehicle.hasProcState(Vehicle.ProcState.IDLE)
        && (vehicle.hasState(Vehicle.State.IDLE)
            || vehicle.hasState(Vehicle.State.CHARGING));
  }

  private boolean processesDispensableOrder(Vehicle vehicle) {
    return vehicle.hasProcState(Vehicle.ProcState.PROCESSING_ORDER)
        && fetchObject(TransportOrder.class, vehicle.getTransportOrder())
            .isDispensable();
  }
}
