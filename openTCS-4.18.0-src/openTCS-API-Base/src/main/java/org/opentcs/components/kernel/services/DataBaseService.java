/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.database.to.CsvBinTO;

/**
 * Provide methods concerning database.
 * 
 * @author Henry
 */
public interface DataBaseService {
  /**
   * The load operation for PSB vehicle.
   */
  String LOAD_OPERATION = "Catch";
  /**
   * The unload operation for PSB vehicle.
   */
  String UNLOAD_OPERATION = "Drop";
  /**
   * The charset to use for the reader/writer.
   */
  Charset CHARSET = Charset.forName("UTF-8");
  /**
   * csv file column separator.
   */
  String CSV_COLUMN_SEPARATOR = ",";
  /**
   * The default file directory.
   */
  String DEFAULT_FILE_DIR = "C:\\Users\\admin\\Desktop\\";
  /**
   * The default file suffix.
   */
  String DEFAULT_SUFFIX = ".csv";
  
  /**
   * Update the data base through the current model in kernel.
   */
  void updateDataBase();
  /**
   * Lock a specific SKU so that it can not be caught.
   * @param SKU The SKU that is to be locked.
   * @return <code>true</code> If, and only if the SKU in the data base is locked successfully
   */
  boolean lockSKU(String SKU);
  /**
   * Unlock a specific SKU so that it can be caught.
   * @param SKU The SKU that is to be unlocked.
   * @return <code>true</code> If, and only if the SKU in the data base is unlocked successfully
   */
  boolean unlockSKU(String SKU);
  
  Map<CsvBinTO, Set<String>> getBinsViaSkus(Map<String, Integer> Skus) throws InterruptedException;
  @Deprecated
  List<List<DestinationCreationTO>> getDestinationsViaSKU (String skuID, int quantity) throws InterruptedException ;
  
  List<DestinationCreationTO> outboundDestinations(String locationName, int row, int column, int binPosition);
  
  int getStackSize(String locationName);
  /**
   * Update the row and the column of all points.
   */
  void updateRowAndColumn();

  String[][] getLocPosition();
  
  /**
   * Get the neighbours of the given location in the position matrix.
   * @param row The row of the given location.
   * @param column The column of the given location.
   * @param vacancyNum
   * @return The neighbours of the given location in the position matrix.
   */
  List<String> getVacantNeighbours(int row, int column, int vacancyNum);
  
  default String getLoadOperation(){
   return LOAD_OPERATION;
 }
  
  default String getUnloadOperation(){
   return UNLOAD_OPERATION;
 }
  
}
