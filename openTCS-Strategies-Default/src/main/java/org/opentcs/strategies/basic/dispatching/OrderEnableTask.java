/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.strategies.basic.dispatching;

import static java.util.Objects.requireNonNull;
import javax.inject.Inject;
import org.opentcs.components.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentcs.components.kernel.services.OrderEnableService;

/**
 *
 * @author Henry
 */
public class OrderEnableTask 
    implements Runnable,
               Lifecycle {
  
  /**
   * This class's logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(OrderEnableTask.class);

  private final OrderEnableService orderEnableService;
  /**
   * Indicates whether this component is enabled.
   */
  private boolean initialized;
  
  @Inject
  public OrderEnableTask(OrderEnableService orderEnableService){
    this.orderEnableService = requireNonNull(orderEnableService,"orderEnableService");
  }
  
  @Override
  public void initialize() {
    if (isInitialized()) {
      return;
    }
    initialized = true;
  }
  
  @Override
  public void terminate() {
    if (!isInitialized()) {
      return;
    }
    initialized = false;
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }
  
  @Override
  public final void run() {
    LOG.debug("Starting dispatchBin run...");
    orderEnableService.enableOrder();
  }
}
