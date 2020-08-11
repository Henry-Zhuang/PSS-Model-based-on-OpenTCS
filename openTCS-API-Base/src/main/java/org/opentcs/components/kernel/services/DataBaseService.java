/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.opentcs.data.model.Bin;

/**
 * Provide methods concerning database.
 * 
 * @author Henry
 */
public interface DataBaseService {
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
  
  Map<String, Map<String, Integer>> getBinsViaSkus(Map<String, Integer> Skus) throws InterruptedException;
    
  int getStackSize(String locationName);
  /**
   * Update the row and the column of all points.
   */
  void updateRowAndColumn();

  String[][] getLocPosition();
  
  /**
   * Get vacant neighbours of the given location in the position matrix.
   * @param row The row of the given location.
   * @param column The column of the given location.
   * @param vacancyNum
   * @return The neighbours of the given location in the position matrix.
   */
  List<String> getVacantNeighbours(int row, int column, int vacancyNum);
}
