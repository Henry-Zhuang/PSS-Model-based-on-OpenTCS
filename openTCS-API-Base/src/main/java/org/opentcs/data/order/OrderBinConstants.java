/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.data.order;

/**
 *
 * @author Henry
 */
public interface OrderBinConstants {
  
  /**
   * The load operation for PSB vehicle.
   */
  String OPERATION_LOAD = "Catch";
  /**
   * The unload operation for PSB vehicle.
   */
  String OPERATION_UNLOAD = "Drop";
  /**
   * The operation means waiting for picking.
   */
  String OPERATION_WAIT_PICKING = "Wait Picking";
}
