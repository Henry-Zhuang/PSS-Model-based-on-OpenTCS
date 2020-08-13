/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.access.rmi.services;

import java.rmi.Remote;
import java.rmi.RemoteException;
import org.opentcs.access.rmi.ClientID;

/**
 * Declares the methods provided by the {@link TimeFactorService} via RMI.
 * 
 * @author Henry
 */
public interface RemoteTimeFactorService 
    extends Remote{
  
  double getSimulationTimeFactor(ClientID clientId)
      throws RemoteException;
  
  void setSimulationTimeFactor(ClientID clientId, double simulationTimeFactor)
      throws RemoteException;
}
