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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.components.kernel.services.DataBaseService;
import org.opentcs.database.to.CsvBinTO;
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
      CsvBinTO tmpData;
      for(Location location:model.getObjectPool().getObjects(Location.class)){
//        write(writer, Arrays.asList(new String[]{location.getName(),String.valueOf(location.stackSize())}));//TEST
        for(i=location.stackSize()-1;i>=0;i--){
          tmpData = new CsvBinTO(location, i);
          if(!isPickStation(tmpData.getLocationName()))
            write(writer, tmpData.toList());
        }
      }
    }
    catch (IOException ex){
      LOG.error("Error updating Data base",ex);
    }
    finally{
      rwlock.writeLock().unlock();
    }
  }
  
  @Override
  public boolean lockSKU(String SKU){
    if (SKU.equals("")){
      LOG.error("Error SKU is empty when locking bin");
      return false;
    }
    rwlock.writeLock().lock();
    createNewFile();
    boolean modified=false;
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
                                                                 CHARSET));){
      write(writer, Arrays.asList(CsvBinTO.CsvTitle));
      int i;
      CsvBinTO tmpData;
      for(Location location:model.getObjectPool().getObjects(Location.class)){
        for(i=location.stackSize()-1;i>=0;i--){
          if(location.getBin(i).getSKUString().contains(SKU))
            location.getBin(i).lock();
          tmpData = new CsvBinTO(location, i);
          modified = true;
          write(writer, tmpData.toList());
        }
      }
    }
    catch (IOException ex){
      LOG.error("Error locking SKU in Data base",ex);
    }
    finally{
      rwlock.writeLock().unlock();
    }
    return modified;
  }
  
  @Override
  public boolean unlockSKU(String SKU){
    if (SKU.equals("")){
      LOG.error("Error SKU is empty when locking bin");
      return false;
    }
    rwlock.writeLock().lock();
    createNewFile();
    boolean modified=false;
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
                                                                 CHARSET));){
      write(writer, Arrays.asList(CsvBinTO.CsvTitle));
      int i;
      CsvBinTO tmpData;
      for(Location location:model.getObjectPool().getObjects(Location.class)){
        for(i=location.stackSize()-1;i>=0;i--){
          if(location.getBin(i).getSKUString().contains(SKU))
            location.getBin(i).unlock();
          tmpData = new CsvBinTO(location, i);
          modified = true;
          write(writer, tmpData.toList());
        }
      }
    }
    catch (IOException ex){
      LOG.error("Error unlocking SKU in Data base",ex);
    }
    finally{
      rwlock.writeLock().unlock();
    }
    return modified;
  }
  
  @Override
  public Map<CsvBinTO, Set<String>> getBinsViaSkus(Map<String, Integer> Skus) throws InterruptedException {
    for(Map.Entry<String, Integer> skuEntry : Skus.entrySet()){
      if (skuEntry.getKey().isEmpty())
        Skus.remove(skuEntry.getKey());
    }
    List<String> skuIDs = new ArrayList<>(Skus.keySet());
    Map<CsvBinTO, Set<String>> requiredBinTOs = new HashMap<>();
    rwlock.readLock().lock();
    try (RandomAccessFile randFile = new RandomAccessFile(file, "r");){
      String lineData ;
      while((lineData = randFile.readLine()) != null){
        // 仅查询还未满足数量要求的skuID
        Set<String> tmpSkuIDs = skuIDs.stream()
            .filter(isQuantityPositive(Skus))
            .collect(Collectors.toSet());
        // 如果各数量要求均已满足，则退出循环
        if(tmpSkuIDs.isEmpty())
          break;
        // 查询该料箱是否存有未满足数量要求的skuID
        tmpSkuIDs = tmpSkuIDs.stream().filter(lineData::contains).collect(Collectors.toSet());
        if(!tmpSkuIDs.isEmpty()){
          CsvBinTO bin = read(lineData);
          requiredBinTOs.put(bin, tmpSkuIDs);
          // 减去数量要求
          for(String skuID:tmpSkuIDs)
            Skus.put(skuID, Skus.get(skuID) - bin.getSKUQuantity(skuID));
        }
      }
    }
    catch (IOException ex){
      LOG.error("Error getBinsViaSKU in Data base",ex);
    }
    finally{
      rwlock.readLock().unlock();
    }
    Set<String> tmpSkuIDs = skuIDs.stream().filter(isQuantityPositive(Skus)).collect(Collectors.toSet());
    if(!tmpSkuIDs.isEmpty())
    {
      LOG.error("Error there're not enough available SKUs({}) in the data base",tmpSkuIDs);
//      throw new InterruptedException("There're not enough bins with the given SKU in the data base");
    }
    return requiredBinTOs;
  }
  
  @Override
  @Deprecated
  public List<List<DestinationCreationTO>> getDestinationsViaSKU (String skuID, int quantity) throws InterruptedException {
    if (skuID.equals("")){
      LOG.error("Error SKU is empty when getting bins via SKU");
      return null;
    }
    rwlock.readLock().lock();
    List<List<DestinationCreationTO>> destinationLists = new ArrayList<>();
    try (RandomAccessFile randFile = new RandomAccessFile(file, "r");){
      String lineData ;
      while((lineData = randFile.readLine()) != null && quantity > 0 ){
        if(lineData.contains(skuID)){
          CsvBinTO bin = read(lineData);
          List<DestinationCreationTO> destinations = outboundDestinations(bin);
          if(destinations != null){
            destinationLists.add(destinations);
            quantity -= bin.getSKUQuantity(skuID);
          }
        }
      }
    }
    catch (IOException ex){
      LOG.error("Error getDestinationsViaSKU in Data base",ex);
    }
    finally{
      rwlock.readLock().unlock();
    }
    if(quantity>0)
    {
      LOG.error("Error there're not enough available SKUs({}) in the data base",skuID);
      throw new InterruptedException("There're not enough bins with the given SKU in the data base");
    }
    return destinationLists;
  }
  
  @Override
  public int getStackSize(String locationName){
    rwlock.readLock().lock();
    Integer stackSize = 0;
    try (RandomAccessFile randFile = new RandomAccessFile(file, "r");){
      String lineData ;
      while((lineData = randFile.readLine())!=null){
        if(lineData.contains(locationName)){
          String[] strData = lineData.split(CSV_COLUMN_SEPARATOR);
          stackSize = Integer.valueOf(strData[5]) + 1;
          break;
        }
      }
    }
    catch (IOException ex){
      LOG.error("Error getStackSize in Data base",ex);
    }
    finally{
      rwlock.readLock().unlock();
    }
    return stackSize;
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
  
  @Deprecated
  private List<DestinationCreationTO> outboundDestinations(CsvBinTO bin){
    String originLoc = bin.getLocationName();
    List<DestinationCreationTO> result = new ArrayList<>();
    int stackSize = getStackSize(originLoc);
    List<String> tmpLocs = getVacantNeighbours(bin.getRow(), bin.getColumn(), stackSize-1-bin.getBinPosition());
    if(tmpLocs == null)
      return null;
    for(String tmpLoc:tmpLocs){
      result.add(new DestinationCreationTO(originLoc, getLoadOperation()));
      result.add(new DestinationCreationTO(tmpLoc, getUnloadOperation()));
    }    
   
    result.add(new DestinationCreationTO(originLoc, getLoadOperation()));
    result.add(new DestinationCreationTO(getPickStation(bin.getRow()), getUnloadOperation()));
   
    for(int i=tmpLocs.size()-1;i>=0;i--){
      result.add(new DestinationCreationTO(tmpLocs.get(i),getLoadOperation()));
      result.add(new DestinationCreationTO(originLoc, getUnloadOperation()));
    }
    return result;
  }
  
  @Override
  public List<DestinationCreationTO> outboundDestinations(String locationName, int row, int column, int binPosition){
    List<DestinationCreationTO> result = new ArrayList<>();
    int stackSize = getStackSize(locationName);
    List<String> tmpLocs = getVacantNeighbours(row, column, stackSize-1-binPosition);
    if(tmpLocs == null)
      return null;
    for(String tmpLoc:tmpLocs){
      result.add(new DestinationCreationTO(locationName, getLoadOperation()));
      result.add(new DestinationCreationTO(tmpLoc, getUnloadOperation()));
    }    
   
    result.add(new DestinationCreationTO(locationName, getLoadOperation()));
    result.add(new DestinationCreationTO(getPickStation(row), getUnloadOperation()));
   
    for(int i=tmpLocs.size()-1;i>=0;i--){
      result.add(new DestinationCreationTO(tmpLocs.get(i),getLoadOperation()));
      result.add(new DestinationCreationTO(locationName, getUnloadOperation()));
    }
    return result;
  }
  
  private String getPickStation(int row){
    for(String location:locationPosition[row-1]){
      if(location != null && isPickStation(location))
        return location;
    }
    return null;
  }
    
  private void write(BufferedWriter writer, List<String> data) throws IOException {
    writer.write(String.join(CSV_COLUMN_SEPARATOR, data));
    writer.newLine();
  }
  
  private CsvBinTO read(String lineData) {
    String[] strData = lineData.split(CSV_COLUMN_SEPARATOR);
    return new CsvBinTO(strData);
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
              .getType().getName().startsWith(Location.PICK_TYPE_PREFIX);
  }
  
  private static Predicate<String> isQuantityPositive(Map<String, Integer> Skus) {
    return p -> Skus.get(p) > 0;
  }
}
