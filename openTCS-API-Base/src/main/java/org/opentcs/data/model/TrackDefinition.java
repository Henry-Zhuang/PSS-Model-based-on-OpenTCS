/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.data.model;

/**
 * Define the track definition is based on row or column.
 * 
 * @author Henry
 */
public class TrackDefinition {
  public static Axis PSB_TRACK_DEFINITION = Axis.X_POSITION;
  
  public static enum Axis{
    X_POSITION,
    Y_POSITION
  }
}
