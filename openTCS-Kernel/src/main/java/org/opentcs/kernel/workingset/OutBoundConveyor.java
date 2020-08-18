/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.workingset;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.opentcs.components.kernel.services.TimeFactorService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Location;
import org.opentcs.data.order.BinOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henry
 */
public class OutBoundConveyor 
    implements Runnable{
  
  private static final int ENTRANCE_TRACK = 3;
  private static final int MOVING_TIME_UNIT = 1000;
  
  public static final long INTERVAL_TIME = 5000;
  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(OutBoundConveyor.class);
  /**
   * A global object to be used for synchronization within the kernel.
   */
  private final Object globalSyncObject;
  /**
   * The openTCS object pool.
   */
  private final TCSObjectPool objectPool;
  /**
   * The simulation time factor service.
   */
  private final TimeFactorService timeFactorService;
  /**
   * A list represents all the bins on the conveyor.
   * PickEntry 记录了该料箱实例及其放上传送带的时间
   */
  private final List<BinEntry> binsOnConveyor = new ArrayList<>();

  @Inject
  public OutBoundConveyor(@GlobalSyncObject Object globalSyncObject, 
                          TCSObjectPool objectPool,
                          TimeFactorService timeFactorService) {
    this.globalSyncObject = globalSyncObject;
    this.objectPool = objectPool;
    this.timeFactorService = timeFactorService;
  }

  public void clear(){
    binsOnConveyor.clear();
  }
  
  @Override
  public void run() {
    // 检查阶段
    checkOutBoundStation();
    // 拣选阶段
    pickingForCustomerOrder();
  }
  
  private void checkOutBoundStation() {
    // 检查阶段：
    // 检查各个出库站，并按照出库站距传送带入口的距离排序，较近的出库站优先被遍历
    // 出库站如果存放有料箱，则将料箱放上传送带，并记录其放上传送带的时间
    synchronized(globalSyncObject){
      objectPool
          .getObjects(Location.class,loc -> {
            return loc.getType().getName().equals(Location.OUT_BOUND_STATION_TYPE)
                && loc.stackSize() != 0;
            })
          .stream()
          .sorted((loc1,loc2) -> {
            Integer distance1 =  Math.abs(loc1.getPsbTrack() - ENTRANCE_TRACK);
            Integer distance2 = Math.abs(loc2.getPsbTrack() - ENTRANCE_TRACK);
            return distance1.compareTo(distance2);
          })
          .forEach(location -> {
            LOG.info("method called");
            // 将料箱放上传送带，并记录其放上传送带的时间
            for(int i = 0;i<location.stackSize();i++)
              binsOnConveyor.add(new BinEntry(location.getBin(i)));
            Location newLocation = objectPool.replaceObject(location.withBins(new ArrayList<>()));
            objectPool.emitObjectEvent(newLocation,
                               location,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
          });
    }
  }
  
  private void pickingForCustomerOrder() {
    // 拣选阶段：
    // 按前后顺序，遍历传送带上的每个料箱，仅对已经运行了足够距离的料箱进行分拣操作
    // 分拣时，更新料箱所含SKU以及客户订单的当前完成进度，分拣完成后根据料箱是否为空选择是否回库
    binsOnConveyor.stream().filter(this::hasMovedEnoughDistance).forEach(entry -> {
      synchronized(globalSyncObject){
        LOG.info("method called");
        Bin updatedBin = objectPool.getObject(Bin.class, entry.bin.getName());
        updateBinAndCustomerOrder(updatedBin);
        binsOnConveyor.remove(entry);
      }
    });
  }
  
  private void updateBinAndCustomerOrder(Bin bin) {
    // 根据客户订单要求的SKU进行拣选，更新料箱所含SKU，然后将其状态设为已拣选，
    // 并更新相应的客户订单完成进度
    BinOrder currBinOrder = objectPool
        .getObject(BinOrder.class,bin.getAssignedBinOrder());
    
    Map<String,Integer> requiredSkus = currBinOrder.getRequiredSku();
    Set<Bin.SKU> updatedSkus = new HashSet<>();
    
    bin.getSKUs().forEach(sku -> {
      Integer quantity = requiredSkus.get(sku.getSkuID());
      if(quantity != null)
        sku = new Bin.SKU(sku.getSkuID(), sku.getQuantity() - quantity);
      if(sku.getQuantity() > 0)
        updatedSkus.add(sku);
    });
    
    if(updatedSkus.isEmpty())
      objectPool.removeObject(bin.getReference());
    else{
      objectPool.replaceObject(bin.withSKUs(updatedSkus).withState(Bin.State.Picked).withAssignedBinOrder(null));
      // 将料箱回库
    }
    
    // 更新客户订单完成进度 （待完成）
    
  }

  private boolean hasMovedEnoughDistance(BinEntry entry) {
    // 根据当前时间与料箱被放上传送带的时间之差来表示料箱移动的距离
    // 足够的距离是由料箱原本的出库站所在轨与传送带入口的距离来定义的
    return Instant.now().toEpochMilli() - entry.enterTime.toEpochMilli()
        >= Math.abs(entry.bin.getPsbTrack() - ENTRANCE_TRACK) 
        * MOVING_TIME_UNIT / timeFactorService.getSimulationTimeFactor();
  }
  
  public long getIntervalTime(){
    return INTERVAL_TIME / (long)timeFactorService.getSimulationTimeFactor();
  }
  
  private static final class BinEntry{
    private final Bin bin;
    private final Instant enterTime;

    public BinEntry(Bin bin) {
      this.enterTime = Instant.now();
      this.bin = bin;
    }
  }
}
