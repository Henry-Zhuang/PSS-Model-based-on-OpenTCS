/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.components.kernel.services;

import java.nio.charset.Charset;

/**
 * Provide methods concerning database.
 * 
 * @author Henry
 */
public interface CsvFileService {
  /**
   * The charset to use for the reader/writer.
   */
  Charset CHARSET = Charset.forName("UTF-8");
  /**
   * csv file column separator.
   */
  String CSV_COLUMN_SEPARATOR = ",";
  /**
   * The stock infomation file directory.
   */
  String STOCK_FILE_DIR = "C:\\Users\\admin\\Desktop\\";
  /**
   * The input order file directory.
   */
  String ORDER_FILE_DIR = "C:\\Users\\admin\\Desktop\\";
  /**
   * The default file suffix.
   */
  String DEFAULT_SUFFIX = ".csv";
  
  /**
   * Update the data base through the current model in kernel.
   */
  void outputStockInfo();

}
