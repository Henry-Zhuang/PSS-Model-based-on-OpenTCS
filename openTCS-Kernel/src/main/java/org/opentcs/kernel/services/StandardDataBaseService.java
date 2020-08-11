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
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.opentcs.components.kernel.services.DataBaseService;
import org.opentcs.database.to.CsvBinTO;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.Point;
import org.opentcs.kernel.workingset.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author Henry
 */
public class StandardDataBaseService implements DataBaseService {
   /**
   * This class' logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(StandardVehicleService.class);
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
   * The position matrix of all locations.
   */
  public static String[][] locationPosition;
  /**
   * Creates a new instance.
   *
   * @param rwlock The kernel database's read write lock.
   * @param model The model to be used.
   */
  @Inject
  public StandardDataBaseService(ReentrantReadWriteLock rwlock,
                                    Model model) {
    this.rwlock = requireNonNull(rwlock, "globalSyncObject");
    this.model = requireNonNull(model, "model");
  }
  
  @Override
  public void updateDataBase(){
    rwlock.writeLock().lock();
    createNewFile();
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
                                                                 CHARSET));){
      write(writer, Arrays.asList(CsvBinTO.CsvTitle));
      int i;
      model.getObjectPool().getObjects(Bin.class)
          .stream()
          .filter(bin -> bin.getAttachedLocation() != null)
          .sorted((Bin bin1, Bin bin2) -> {
            if(bin1.getAttachedLocation().equals(bin2.getAttachedLocation())){
              Integer binPosition1 = bin1.getBinPosition();
              Integer binPosition2 = bin2.getBinPosition();
              return binPosition1.compareTo(binPosition2);
            }
            else
              return bin2.getAttachedLocation().getName()
                  .compareTo(bin1.getAttachedLocation().getName());
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
  
//  @Override
//  public Map<CsvBinTO, Set<String>> getBinsViaSkus(Map<String, Integer> Skus) throws InterruptedException {
//    for(Map.Entry<String, Integer> skuEntry : Skus.entrySet()){
//      if (skuEntry.getKey().isEmpty())
//        Skus.remove(skuEntry.getKey());
//    }
//    List<String> skuIDs = new ArrayList<>(Skus.keySet());
//    Map<CsvBinTO, Set<String>> requiredBinTOs = new HashMap<>();
//    rwlock.readLock().lock();
//    try (RandomAccessFile randFile = new RandomAccessFile(file, "r");){
//      String lineData ;
//      while((lineData = randFile.readLine()) != null){
//        // 仅查询还未满足数量要求的skuID
//        Set<String> tmpSkuIDs = skuIDs.stream()
//            .filter(isQuantityPositive(Skus))
//            .collect(Collectors.toSet());
//        // 如果各数量要求均已满足，则退出循环
//        if(tmpSkuIDs.isEmpty())
//          break;
//        // 查询该料箱是否存有未满足数量要求的skuID
//        tmpSkuIDs = tmpSkuIDs.stream().filter(lineData::contains).collect(Collectors.toSet());
//        if(!tmpSkuIDs.isEmpty()){
//          CsvBinTO bin = read(lineData);
//          requiredBinTOs.put(bin, tmpSkuIDs);
//          // 减去数量要求
//          for(String skuID:tmpSkuIDs)
//            Skus.put(skuID, Skus.get(skuID) - bin.getSKUQuantity(skuID));
//        }
//      }
//    }
//    catch (IOException ex){
//      LOG.error("Error getBinsViaSKU in Data base",ex);
//    }
//    finally{
//      rwlock.readLock().unlock();
//    }
//    Set<String> tmpSkuIDs = skuIDs.stream().filter(isQuantityPositive(Skus)).collect(Collectors.toSet());
//    if(!tmpSkuIDs.isEmpty())
//    {
//      LOG.error("Error there're not enough available SKUs({}) in the data base",tmpSkuIDs);
////      throw new InterruptedException("There're not enough bins with the given SKU in the data base");
//    }
//    return requiredBinTOs;
//  }
  @Override
  public Map<String, Map<String, Integer>> getBinsViaSkus(Map<String, Integer> Skus) throws InterruptedException {
    for(Map.Entry<String, Integer> skuEntry : Skus.entrySet()){
      if (skuEntry.getKey().isEmpty())
        Skus.remove(skuEntry.getKey());
    }
    List<String> skuIDs = new ArrayList<>(Skus.keySet());
    Map<String, Map<String, Integer>> requiredBins = new HashMap<>();
    
    Set<Bin> bins = model.getObjectPool().getObjects(Bin.class, bin -> {
      return !bin.isLocked() && bin.getAttachedLocation() != null && !bin.getSKUs().isEmpty();
    });
    // 未满足数量要求的skuID
    Set<String> requiredSkuIDs = skuIDs.stream()
        .filter(isQuantityPositive(Skus)).collect(Collectors.toSet());
    
    // 遍历数据库中的每个料箱
    for(Bin bin : bins){
      // 如果各数量要求均已满足，则退出循环
      if(requiredSkuIDs.isEmpty())
        break;
      
      // 查询该料箱是否存有未满足数量要求的skuID
      Map<String, Integer> requiredSku = requiredSkuIDs.stream()
          .map(id -> bin.getSKU(id))
          .filter(sku -> sku != null)
          .collect(Collectors.toMap(Bin.SKU::getSkuID, getQuantityWithLimit(Skus)));
      
      // 如果该料箱含有要求的SKU
      if(!requiredSku.isEmpty()){
        requiredBins.put(bin.getName(), requiredSku);
        model.getObjectPool().replaceObject(bin.lock());
        // 减去数量要求
        for(Map.Entry<String,Integer> sku : requiredSku.entrySet())
          Skus.put(sku.getKey(), Skus.get(sku.getKey()) - sku.getValue());
        
        // 更新剩下未满足数量要求的skuID
        requiredSkuIDs = skuIDs.stream()
            .filter(isQuantityPositive(Skus)).collect(Collectors.toSet());
      }
    }
    
    if(!requiredSkuIDs.isEmpty())
    {
      LOG.error("Error there're not enough available SKUs({}) in the data base",requiredSkuIDs);
//      throw new InterruptedException("There're not enough bins with the given SKU in the data base");
    }
    
    return requiredBins;
  }
  
  @Override
  public int getStackSize(String locationName){
    return model.getObjectPool().getObject(Location.class, locationName).stackSize();
  }

  @Override
  public void updateRowAndColumn(){
    List<Long> sortedXPos = new ArrayList<>();
    List<Long> sortedYPos = new ArrayList<>();
    for (Point point:model.getObjectPool().getObjects(Point.class)){
      sortedXPos.add(point.getPosition().getX());
      sortedYPos.add(point.getPosition().getY());
    }
    
    sortedXPos = sortedXPos.stream().distinct().sorted().collect(Collectors.toList());
    sortedYPos = sortedYPos.stream().distinct().sorted().collect(Collectors.toList());
    
    locationPosition = new String[sortedYPos.size()][sortedXPos.size()];
    
    for (Point point:model.getObjectPool().getObjects(Point.class)){
      point.setColumn(sortedXPos.indexOf(point.getPosition().getX()) + 1);
      point.setRow(sortedYPos.indexOf(point.getPosition().getY()) + 1);
      model.getObjectPool().replaceObject(point);
      Location tmpLoc;
      for(Location.Link link:point.getAttachedLinks()){
        tmpLoc = model.getObjectPool().getObject(Location.class, link.getLocation());
        tmpLoc.setColumn(point.getColumn());
        tmpLoc.setRow(point.getRow());
        
        for(Bin bin : tmpLoc.getBins())
          model.getObjectPool().replaceObject(bin);
        
        model.getObjectPool().replaceObject(tmpLoc);
        locationPosition[tmpLoc.getRow()-1][tmpLoc.getColumn()-1] = tmpLoc.getName();
      }
    }
  }
  
  @Override
  public String[][] getLocPosition(){
    return locationPosition;
  }
  
  @Override
  public List<String> getVacantNeighbours(int locationRow, int locationColumn, int vacancyNum){
    rwlock.readLock().lock();
    int offset = 1;
    int row = locationRow - 1;
    int column = locationColumn - 1;
    List<String> vacantNeighbours = new ArrayList<>();
    try {
      while(vacancyNum > 0 
          && (column-offset >= 0 || column+offset < locationPosition[row].length)){
        // A vacant neighbour firstly should be valid.
        // A picking station is not considered as a vacant neighbour.
        if(column-offset >= 0
            && locationPosition[row][column-offset] != null
            && !isPickStation(locationPosition[row][column-offset])){
          int vacancy = Location.BINS_MAX_NUM - getStackSize(locationPosition[row][column-offset]);
          vacancy = vacancy > vacancyNum ? vacancyNum : vacancy;
          for(int i=0;i<vacancy;i++)
            vacantNeighbours.add(locationPosition[row][column-offset]);
          vacancyNum -= vacancy;
        }
        
        if(vacancyNum > 0 
            && column+offset < locationPosition[row].length
            && locationPosition[row][column+offset] != null
            && !isPickStation(locationPosition[row][column+offset])){
          int vacancy = Location.BINS_MAX_NUM - getStackSize(locationPosition[row][column+offset]);
          vacancy = vacancy > vacancyNum ? vacancyNum : vacancy;
          for(int i=0;i<vacancy;i++)
            vacantNeighbours.add(locationPosition[row][column+offset]);
          vacancyNum -= vacancy;
        }
        offset++;
      }
    }
    finally {
      rwlock.readLock().unlock();
    }
    return vacancyNum <= 0 ? vacantNeighbours : null;
  }
  
  private static Function<Bin.SKU, Integer> getQuantityWithLimit(Map<String, Integer> Skus){
    return p -> {
      Integer limit = Skus.get(p.getSkuID());
      Integer quantity = p.getQuantity();
      return quantity <= limit ? quantity : limit;
    };      
  }
  
  private void write(BufferedWriter writer, List<String> data) throws IOException {
    writer.write(String.join(CSV_COLUMN_SEPARATOR, data));
    writer.newLine();
  }

  private void createNewFile() {
    try{
      if(file!=null && file.exists())
        file.delete();
      file = new File(DEFAULT_FILE_DIR+model.getName()+DEFAULT_SUFFIX);
      if(file!=null && file.exists())
        file.delete();
      file.createNewFile();
      LOG.debug("Updating Data base of model {}",model.getName());
    }
    catch (IOException ex){
      LOG.error("Error creating new file ",ex);
    }
  }

  private boolean isPickStation(String locationName) {
    return model.getObjectPool().getObject(Location.class,locationName)
              .getType().getName().startsWith(Location.PICK_STATION_PREFIX);
  }
  
  private static Predicate<String> isQuantityPositive(Map<String, Integer> Skus) {
    return p -> Skus.get(p) > 0;
  }
}
