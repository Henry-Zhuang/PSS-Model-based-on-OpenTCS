/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.access.rmi.services;

import java.rmi.RemoteException;
import org.opentcs.components.kernel.services.TimeFactorService;

/**
 * The default implementation of the simulation time factor service.
 * Delegates method invocations to the corresponding remote service.
 * 
 * @author Henry
 */
public class RemoteTimeFactorServiceProxy     
    extends AbstractRemoteServiceProxy<RemoteTimeFactorService>
    implements TimeFactorService {

  @Override
  public double getSimulationTimeFactor() {
    checkServiceAvailability();

    try {
      return getRemoteService().getSimulationTimeFactor(getClientId());
    }
    catch (RemoteException ex) {
      throw findSuitableExceptionFor(ex);
    }
  }

  @Override
  public void setSimulationTimeFactor(double simulationTimeFactor) {
    checkServiceAvailability();

    try {
      getRemoteService().setSimulationTimeFactor(getClientId() ,simulationTimeFactor);
    }
    catch (RemoteException ex) {
      throw findSuitableExceptionFor(ex);
    }
  }
}
