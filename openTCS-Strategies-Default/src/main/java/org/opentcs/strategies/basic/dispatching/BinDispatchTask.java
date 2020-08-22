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
import org.opentcs.components.kernel.services.OrderDecompositionService;

/**
 *
 * @author admin
 */
public class BinDispatchTask 
    implements Runnable,
               Lifecycle {
  
  /**
   * This class's logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(BinDispatchTask.class);

  private final OrderDecompositionService orderDecomService;
  /**
   * Indicates whether this component is enabled.
   */
  private boolean initialized;
  
  @Inject
  public BinDispatchTask(OrderDecompositionService orderDecomService){
    this.orderDecomService = requireNonNull(orderDecomService,"orderDecomService");
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
    orderDecomService.decomposeOutboundOrder();
  }
}
