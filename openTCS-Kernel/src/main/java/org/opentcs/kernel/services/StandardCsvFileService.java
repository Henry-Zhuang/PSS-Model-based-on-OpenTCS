/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Inject;
import org.opentcs.data.model.Bin;
import org.opentcs.kernel.workingset.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentcs.components.kernel.services.CsvFileService;
/**
 *
 * @author Henry
 */
public class StandardCsvFileService implements CsvFileService {
   /**
   * This class' logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(StandardVehicleService.class);
  /**
   * The csv file's first line.
   */
  public static final String[] CsvTitle={"BinID", "SKUs", "AttachedObject", "Row", "Column", "Position", "Locked"};
  /**
   * A read write lock be used for accessing the database of the kernel.
   */
  private final ReentrantReadWriteLock rwlock;
  /**
   * The model facade to the object pool.
   */
  private final Model model;
  /**
   * The currently used csv file in kernel.
   */
  private File file = null;
  
  /**
   * Creates a new instance.
   *
   * @param rwlock The kernel database's read write lock.
   * @param model The model to be used.
   */
  @Inject
  public StandardCsvFileService(ReentrantReadWriteLock rwlock,
                                    Model model) {
    this.rwlock = requireNonNull(rwlock, "globalSyncObject");
    this.model = requireNonNull(model, "model");
  }
  
  @Override
  public void outputStockInfo(){
    rwlock.writeLock().lock();
    createNewFile();
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
                                                                 CHARSET));){
      write(writer, Arrays.asList(CsvTitle));
      int i;
      model.getObjectPool().getObjects(Bin.class)
          .stream()
          .filter(bin -> bin.getAttachedLocation() != null)
          .sorted((Bin bin1, Bin bin2) -> {
            if(bin1.getAttachedLocation().equals(bin2.getAttachedLocation())){
              Integer binPosition1 = bin1.getBinPosition();
              Integer binPosition2 = bin2.getBinPosition();
              return binPosition2.compareTo(binPosition1);
            }
            else
              return bin1.getAttachedLocation().getName()
                  .compareTo(bin2.getAttachedLocation().getName());
          })
          .forEach(b -> {
            try{
              write(writer, b.toList());
            }
            catch (IOException ex){
              LOG.error("Error updating Data base",ex);
            }
          });
    }
    catch (IOException ex){
      LOG.error("Error updating Data base",ex);
    }
    finally{
      rwlock.writeLock().unlock();
    }
  }
  
  private void write(BufferedWriter writer, List<String> data) throws IOException {
    writer.write(String.join(CSV_COLUMN_SEPARATOR, data));
    writer.newLine();
  }

  private void createNewFile() {
    try{
      if(file!=null && file.exists())
        file.delete();
      file = new File(STOCK_FILE_DIR+model.getName()+DEFAULT_SUFFIX);
      if(file!=null && file.exists())
        file.delete();
      file.createNewFile();
      LOG.debug("Updating Data base of model {}",model.getName());
    }
    catch (IOException ex){
      LOG.error("Error creating new file ",ex);
    }
  }
}
