/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.kernel.extensions.servicewebapi.v1.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.access.to.order.OutboundOrderCreationTO;
import org.opentcs.access.to.order.TransportOrderCreationTO;
import org.opentcs.components.kernel.services.DispatcherService;
import org.opentcs.components.kernel.services.TransportOrderService;
import org.opentcs.components.kernel.services.VehicleService;
import org.opentcs.customizations.kernel.KernelExecutor;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.kernel.extensions.servicewebapi.v1.order.binding.Destination;
import org.opentcs.kernel.extensions.servicewebapi.v1.order.binding.Property;
import org.opentcs.kernel.extensions.servicewebapi.v1.order.binding.Transport;
import org.opentcs.kernel.extensions.servicewebapi.v1.order.binding.CustomerOrder;
import org.opentcs.data.model.Bin;
import org.opentcs.components.kernel.services.OutboundOrderService;

/**
 * Handles requests for creating or withdrawing transport orders.
 *
 * @author Stefan Walter (Fraunhofer IML)
 */
public class OrderHandler {

  /**
   * The service we use to create transport orders.
   */
  private final TransportOrderService orderService;
  /**
   * The service we use to update vehicle states.
   */
  private final VehicleService vehicleService;
  /**
   * The service we use to withdraw transport orders.
   */
  private final DispatcherService dispatcherService;
  /**
   * Executes tasks modifying kernel data.
   */
  private final ExecutorService kernelExecutor;

  private final OutboundOrderService outboundOrderService;
  /**
   * 用零长度的byte数组作为同步锁.
   */
  private final byte[] lock = new byte[0];

  /**
   * Creates a new instance.
   *
   * @param orderService Used to create transport orders.
   * @param vehicleService Used to update vehicle state.
   * @param dispatcherService Used to withdraw transport orders.
   * @param kernelExecutor Executes tasks modifying kernel data.
   * @param outboundService
   */
  @Inject
  public OrderHandler(TransportOrderService orderService,
                      VehicleService vehicleService,
                      DispatcherService dispatcherService,
                      @KernelExecutor ExecutorService kernelExecutor,
                      OutboundOrderService outboundService) {
    this.orderService = requireNonNull(orderService, "orderService");
    this.vehicleService = requireNonNull(vehicleService, "vehicleService");
    this.dispatcherService = requireNonNull(dispatcherService, "dispatcherService");
    this.kernelExecutor = requireNonNull(kernelExecutor, "kernelExecutor");
    this.outboundOrderService = requireNonNull(outboundService,"outboundService");
  }

  public void createOrder(String name, Transport order)
      throws ObjectUnknownException,
             ObjectExistsException,
             KernelRuntimeException,
             IllegalStateException {
    requireNonNull(name, "name");
    requireNonNull(order, "order");

    TransportOrderCreationTO to
        = new TransportOrderCreationTO(name, destinations(order))
            .withIntendedVehicleName(order.getIntendedVehicle())
            .withDependencyNames(new HashSet<>(order.getDependencies()))
            .withDeadline(deadline(order))
            .withProperties(properties(order.getProperties()));

    try {
      kernelExecutor.submit(() -> {
        orderService.createTransportOrder(to);
        dispatcherService.dispatch();
      }).get();
    }
    catch (InterruptedException exc) {
      throw new IllegalStateException("Unexpectedly interrupted");
    }
    catch (ExecutionException exc) {
      if (exc.getCause() instanceof RuntimeException) {
        throw (RuntimeException) exc.getCause();
      }
      throw new KernelRuntimeException(exc.getCause());
    }
  }
  
  //////////////////////////////////////////////////////////////// created by Henry
  public void createOutboundOrder(String name, CustomerOrder order)
      throws ObjectUnknownException,
             ObjectExistsException,
             KernelRuntimeException,
             IllegalStateException {
    requireNonNull(name, "name");
    requireNonNull(order, "order");
    
//    try{
      Set<Bin.SKU> requiredSKUs = new HashSet<>();
      order.getSkus().stream().forEach(sku -> {
        requiredSKUs.add(new Bin.SKU(sku.getSkuID(), sku.getQuantity()));
      });
      OutboundOrderCreationTO to
          = new OutboundOrderCreationTO(name)
              .withRequiredSKUs(requiredSKUs)
              .withDeadline(order.getDeadline())
              .withProperties(properties(order.getProperties()));
      outboundOrderService.createOutboundOrder(to);
//      for(Map.Entry<String, Map<String,Integer>> binEntry : binRequirements.entrySet()){
//        BinOrderCreationTO to
//            = new BinOrderCreationTO(nameFor(name, binEntry.getKey()) ,binEntry.getKey(), OrderBinConstants.TYPE_OUTBOUND)
//                .withCustomerOrderName(name)
//                .withDeadline(deadline(order))
//                .withProperties(properties(order.getProperties()))
//                .withRequiredSku(binEntry.getValue());
//        binOrderService.createBinOrder(to);
//      }
//      kernelExecutor.submit(() -> {
//        dispatcherService.dispatchBin();
//        dispatcherService.dispatch();            
//      }).get();
//    }
//    catch (InterruptedException exc) {
//      throw new IllegalStateException("Unexpectedly interrupted",exc);
//    }
//    catch (ExecutionException exc) {
//      if (exc.getCause() instanceof RuntimeException) {
//        throw (RuntimeException) exc.getCause();
//      }
//      throw new KernelRuntimeException(exc.getCause());
//    }
  }
  //////////////////////////////////////////////////////////////// created end

  public void withdrawByTransportOrder(String name, boolean immediate, boolean disableVehicle)
      throws ObjectUnknownException {
    requireNonNull(name, "name");

    if (orderService.fetchObject(TransportOrder.class, name) == null) {
      throw new ObjectUnknownException("Unknown transport order: " + name);
    }

    kernelExecutor.submit(() -> {
      TransportOrder order = orderService.fetchObject(TransportOrder.class, name);
      if (disableVehicle && order.getProcessingVehicle() != null) {
        vehicleService.updateVehicleIntegrationLevel(order.getProcessingVehicle(),
                                                     Vehicle.IntegrationLevel.TO_BE_RESPECTED);
      }

      dispatcherService.withdrawByTransportOrder(order.getReference(), immediate);
    });
  }

  public void withdrawByVehicle(String name, boolean immediate, boolean disableVehicle)
      throws ObjectUnknownException {
    requireNonNull(name, "name");

    Vehicle vehicle = orderService.fetchObject(Vehicle.class, name);
    if (vehicle == null) {
      throw new ObjectUnknownException("Unknown vehicle: " + name);
    }

    kernelExecutor.submit(() -> {
      if (disableVehicle) {
        vehicleService.updateVehicleIntegrationLevel(vehicle.getReference(),
                                                     Vehicle.IntegrationLevel.TO_BE_RESPECTED);
      }

      dispatcherService.withdrawByVehicle(vehicle.getReference(), immediate);
    });
  }

  private List<DestinationCreationTO> destinations(Transport order) {
    List<DestinationCreationTO> result = new ArrayList<>(order.getDestinations().size());

    for (Destination dest : order.getDestinations()) {
      DestinationCreationTO to = new DestinationCreationTO(dest.getLocationName(),
                                                           dest.getOperation());

      for (Property prop : dest.getProperties()) {
        to = to.withProperty(prop.getKey(), prop.getValue());
      }

      result.add(to);
    }

    return result;
  }
  
////////////////////////////////////////////// created by Henry
  private Instant deadline(CustomerOrder order) {
    return order.getDeadline() == null ? Instant.MAX : order.getDeadline();
  }
  
  private String nameFor(String customerOrderName, String binID){
    return customerOrderName + "-" + binID;
  }
  
///////////////////////////////////////////// created end
  
  private Instant deadline(Transport order) {
    return order.getDeadline() == null ? Instant.MAX : order.getDeadline();
  }
  
  private Map<String, String> properties(List<Property> properties) {
    Map<String, String> result = new HashMap<>();
    for (Property prop : properties) {
      result.put(prop.getKey(), prop.getValue());
    }
    return result;
  }
}
