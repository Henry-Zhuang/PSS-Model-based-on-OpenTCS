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
import org.opentcs.access.to.order.TransportOrderBinCreationTO;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.components.kernel.services.TransportOrderBinService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.TransportOrderBin;
import org.opentcs.kernel.workingset.Model;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.opentcs.kernel.workingset.TransportOrderBinPool;

/**
 *
 * @author Henry
 */
public class StandardTransportOrderBinService 
    extends AbstractTCSObjectService
    implements TransportOrderBinService{
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
  private final TransportOrderBinPool orderBinPool;
  /**
   * The model facade to the object pool.
   */
  private final Model model;
  
  /**
   * Creates a new instance.
   *
   * @param objectService
   * @param globalSyncObject The kernel threads' global synchronization object.
   * @param globalObjectPool The object pool to be used.
   * @param orderBinPool The oder bin pool to be used.
   * @param model The model to be used.
   */
  @Inject
  public StandardTransportOrderBinService(TCSObjectService objectService,
                                       @GlobalSyncObject Object globalSyncObject,
                                       TCSObjectPool globalObjectPool,
                                       TransportOrderBinPool orderBinPool,
                                       Model model) {
    super(objectService);
    this.globalSyncObject = requireNonNull(globalSyncObject, "globalSyncObject");
    this.globalObjectPool = requireNonNull(globalObjectPool, "globalObjectPool");
    this.orderBinPool = requireNonNull(orderBinPool, "orderPool");
    this.model = requireNonNull(model, "model");
  }
  
  @Override
  public TransportOrderBin createTransportOrderBin(TransportOrderBinCreationTO to)
      throws ObjectUnknownException, ObjectExistsException, KernelRuntimeException{
    synchronized (globalSyncObject) {
      return orderBinPool.createTransportOrderBin(to).clone();
    }
  }
  
  @Override
  public TransportOrderBin removeTransportOrderBin(TCSObjectReference<TransportOrderBin> ref)
      throws ObjectUnknownException{
    synchronized (globalSyncObject) {
      return orderBinPool.removeTransportOrderBin(ref).clone();
    }
  }
  
  @Override
  public void updateTransportOrderBinAttachedTOrder(TCSObjectReference<TransportOrderBin> tOrderBinRef,
                                            TCSObjectReference<TransportOrder> tOrderRef)
      throws ObjectUnknownException{
    synchronized (globalSyncObject) {
      orderBinPool.setTransportOrderBinAttachedTOrder(tOrderBinRef, tOrderRef);
    }
  }
  
  @Override
  public void updateTransportOrderBinState(TCSObjectReference<TransportOrderBin> tOrderBinRef,
                                            TransportOrderBin.State state)
      throws ObjectUnknownException{
    synchronized (globalSyncObject) {
      orderBinPool.setTransportOrderBinState(tOrderBinRef, state);
    }
  }
  
  @Override
  public void enableTOrderBinForIdleVehicle(){
    synchronized (globalSyncObject) {
      Set<Vehicle> idleVehicles = fetchObjects(Vehicle.class, this::couldProcessTransportOrder);
      for(Vehicle vehicle : idleVehicles)
        orderBinPool.enableTOrderBinForIdleVehicle(vehicle);
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
