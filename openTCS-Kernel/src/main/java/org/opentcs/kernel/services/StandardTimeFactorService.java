/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.services;

import static java.util.Objects.requireNonNull;
import javax.inject.Inject;
import org.opentcs.components.kernel.services.TimeFactorService;
import org.opentcs.virtualvehicle.VirtualVehicleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the standard implementation of the {@link TimeFactorService} interface.
 * 
 * @author Henry
 */
public class StandardTimeFactorService 
    implements TimeFactorService{
  
  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(StandardTimeFactorService.class);
  
  private double simulationTimeFactor;

  @Inject
  public StandardTimeFactorService(VirtualVehicleConfiguration configuration) {
    simulationTimeFactor = configuration.simulationTimeFactor();
  }
  
  @Override
  public double getSimulationTimeFactor() {
    return simulationTimeFactor;
  }

  @Override
  public void setSimulationTimeFactor(double simulationTimeFactor) {
    requireNonNull(simulationTimeFactor,"simulationTimeFactor");
    if(simulationTimeFactor > 0)
      this.simulationTimeFactor = simulationTimeFactor;
    else{
      LOG.error("ERROR The simulation time factor must be positive.");
      LOG.info("The current simulation time factor is {}",this.simulationTimeFactor);
    }
    System.out.println(this.simulationTimeFactor);
  }
}
