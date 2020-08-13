/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.guing.exchange.adapter;

import static java.util.Objects.requireNonNull;
import org.opentcs.access.Kernel;
import org.opentcs.access.KernelServicePortal;
import org.opentcs.access.SharedKernelServicePortal;
import org.opentcs.access.SharedKernelServicePortalProvider;
import org.opentcs.components.kernel.services.ServiceUnavailableException;
import org.opentcs.guing.application.ApplicationState;
import org.opentcs.guing.application.OperationMode;
import org.opentcs.guing.components.properties.event.AttributesChangeEvent;
import org.opentcs.guing.components.properties.event.AttributesChangeListener;
import org.opentcs.guing.model.elements.LayoutModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henry
 */
public class TimeFactorAdapter 
    implements AttributesChangeListener {
  
  /**
   * This class's logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(TimeFactorAdapter.class);
  /**
   * The layout model.
   */
  private final LayoutModel model;
  /**
   * Provides access to a portal.
   */
  private final SharedKernelServicePortalProvider portalProvider;
  /**
   * The state of the plant overview.
   */
  private final ApplicationState applicationState;
  /**
   * The previous simulation time factor.
   */
  private double previousTimeFactor;
  
  /**
   * Creates a new instance.
   *
   * @param portalProvider A portal provider.
   * @param applicationState Keeps the plant overview's state.
   * @param model The layout model.
   */
  public TimeFactorAdapter(SharedKernelServicePortalProvider portalProvider,
                         ApplicationState applicationState,
                         LayoutModel model) {
    this.portalProvider = requireNonNull(portalProvider, "portalProvider");
    this.applicationState = requireNonNull(applicationState, "applicationState");
    this.model = requireNonNull(model, "model");
    this.previousTimeFactor = getTimeFactorInKernel();
    this.model.getPropertyTimeFactor().setText(String.valueOf(previousTimeFactor));
  }
  
  @Override
  public void propertiesChanged(AttributesChangeEvent e) {
    System.out.println("propertiesChanged");
    if (e.getModel() != model) {
      return;
    }
    if (applicationState.getOperationMode() != OperationMode.OPERATING) {
      LOG.debug("Ignoring TimeFactorEvent because the application is not in operating mode.");
      return;
    }

    double newTimeFactor = Double.parseDouble(model.getPropertyTimeFactor().getText());
    if (newTimeFactor == previousTimeFactor
        || newTimeFactor <= 0) {
      LOG.warn("WARNING The set of simulation time factor was failed.");
      return;
    }

    previousTimeFactor = newTimeFactor;
    new Thread(() -> updateTimeFactorInKernel(newTimeFactor)).start();
  }
  
  private double getTimeFactorInKernel(){
    double timeFactor = 1.0;
    try (SharedKernelServicePortal sharedPortal = portalProvider.register()) {
      KernelServicePortal portal = sharedPortal.getPortal();
      // Check if the kernel is in operating mode, too.
      if (portal.getState() == Kernel.State.OPERATING) {
        timeFactor =  portal.getTimeFactorService().getSimulationTimeFactor();
      }
    }
    catch (ServiceUnavailableException exc) {
      LOG.warn("Could not connect to kernel", exc);
    }
    return timeFactor;
  }
  
  private void updateTimeFactorInKernel(double timeFactor){
    try (SharedKernelServicePortal sharedPortal = portalProvider.register()) {
      KernelServicePortal portal = sharedPortal.getPortal();
      // Check if the kernel is in operating mode, too.
      if (portal.getState() == Kernel.State.OPERATING) {
        portal.getTimeFactorService().setSimulationTimeFactor(timeFactor);
      }
    }
    catch (ServiceUnavailableException exc) {
      LOG.warn("Could not connect to kernel", exc);
    }
  }
}
