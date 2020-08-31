/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.inbound;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.InboundOrderCreationTO;
import org.opentcs.components.kernel.ObjectNameProvider;
import org.opentcs.components.kernel.services.InboundOrderService;
import org.opentcs.components.kernel.services.TimeFactorService;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Location;
import org.opentcs.data.order.InboundOrder;
import org.opentcs.data.order.OrderConstants;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentcs.components.kernel.services.OrderEnableService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import static org.opentcs.kernel.services.StandardOrderEnableService.locationPosition;
/**
 *
 * @author Henry
 */
public class InboundConveyor 
    implements Runnable{
  
  /**
   * 库区队列数量
   */
  private static final int AREA_NUM = 2;
  /**
   * 与主传送带的出口相连接的库区入口
   */
  private static final int ENTRANCE_AREA = 2;
  /**
   * 料箱从被放上入库传送带到进入主传输带需要花费的时间
   */
  private static final int MAIN_CONVEYOR_TIME = 20000;
  /**
   * 料箱从一个库区队列入口到邻接的库区队列入口所花费时间
   */
  private static final int AREA_INTERVAL = 15000;
  /**
   * 内核执行器执行传送带任务流程的间隔
   */
  public static final long RUN_INTERVAL = 10000;
  
  private static final double WEIGHT_VACANCY = 1.0;
  private static final double WEIGHT_WAIT_NUM = 1.0;
  private static final double WEIGHT_AREA_STOCK = 3.0;
  
  private static final double WEIGHT_TRACK_STOCK = 3.0;
  private static final double WEIGHT_TRACK_NUM = 0.01;
  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(InboundConveyor.class);
  /**
   * A global object to be used for synchronization within the kernel.
   */
  private final Object globalSyncObject;
  /**
   * The openTCS object pool.
   */
  private final TCSObjectPool objectPool;
  /**
   * The outbound order service
   */
    private final InboundOrderService inOrderService;
  /**
   * The simulation time factor service.
   */
  private final TimeFactorService timeFactorService;
  /**
   * The order enable service.
   */
  private final OrderEnableService orderEnableService;
  /**
   * Provides names for change-track orders.
   */
  private final ObjectNameProvider objectNameProvider;
  /**
   * A list represents all the bins on the conveyor.
   * BinEntry 记录了该料箱实例及其放上入库传送带的时间
   */
  private final List<BinEntry> inboundList = new ArrayList<>();
  /**
   * 仓库库区.
   */
  private final List<AreaQueue> binAreas = new ArrayList<>();
  /**
   * This instance's <em>initialized</em> flag.
   */
  private boolean initialized = false;
  private int test = 0;

  @Inject
  public InboundConveyor(@GlobalSyncObject Object globalSyncObject,
                          TCSObjectPool objectPool,
                          InboundOrderService outOrderService,
                          TimeFactorService timeFactorService,
                          OrderEnableService orderEnableService,
                          ObjectNameProvider orderNameProvider) {
    this.globalSyncObject = globalSyncObject;
    this.objectPool = objectPool;
    this.inOrderService = outOrderService;
    this.timeFactorService = timeFactorService;
    this.orderEnableService = orderEnableService;
    this.objectNameProvider = orderNameProvider;
  }

  public void initialize(){
    if(initialized)
      return;
    
    // 初始化库区队列
    int trackNumTotal = 0;
    if(locationPosition != null)
      trackNumTotal = locationPosition.length;
    if(trackNumTotal == 0)
      return;
    
    int trackNumPerArea = trackNumTotal / AREA_NUM;
    if(trackNumTotal % AREA_NUM != 0)
      trackNumPerArea += 1;
    
    for(int i = 0;i<AREA_NUM;i++){
      int minTrack = i * trackNumPerArea + 1;
      int maxTrack = (i+1)*trackNumPerArea <= trackNumTotal ? (i+1)*trackNumPerArea : trackNumTotal;
      binAreas.add(new AreaQueue(i,minTrack,maxTrack));
    }
    
    initialized = true;
  }
  
  public void clear(){
    inboundList.clear();
    binAreas.clear();
    initialized = false;
  }
  
  public synchronized void onConveyor(Bin bin){
    inboundList.add(new BinEntry(bin));
  }
  
  private void Test(){
    test += 1;
    if(test % 4 == 0){
      Random r = new Random();
      String binID = String.format("%s%d", "BIN",test);
      String skuID = String.format("%s%03d", "SKU",r.nextInt(30));
      Double quantity = r.nextDouble()*100+100;
      Set<Bin.SKU> SKUs = new HashSet<>();
      SKUs.add(new Bin.SKU(skuID, quantity));
      Bin bin = new Bin(binID).withSKUs(SKUs).withState(Bin.State.Inbounding);
      onConveyor(bin);
      System.out.println("Inbound bin "+binID+" "+skuID+" : "+quantity);
    }
  }
  
  @Override
  public void run() {
    if(initialized){
      Test();
      // 选择库区阶段
      selectArea();
      // 选择轨道阶段
      selectTrack();
      // 生成入库订单阶段
      enableInboundOrder();
    }
  }
  
  /**
   * 选择库区阶段:.
   * 检查传送带上各箱子，如果箱子已在入库传送带上移动了足够的距离
   * 则说明它已进入了主传送带，考虑将其放入某个库区队列中，并让其继续移动；
   * 考虑因素：库区队列剩余空位、库区队列排队料箱数、库区库存量.
   */
  private synchronized void selectArea() {
    
    // 找出已进入主传送带且未分配库区的料箱
    List<BinEntry> requiredBins = inboundList.stream().filter(bin -> bin.assignedArea == null)
        .filter(this::hasArrivedMainConveyor).collect(Collectors.toList());
    
    if(requiredBins.isEmpty())
      return;
    
    //统计各库区的料箱数量
    binAreas.stream().forEach(area -> {
    Integer binTotalNum = 0;
      for(int i=area.minTrack;i<=area.maxTrack;i++){
        binTotalNum += getTrackStock(i, area);
      }
      area.setAreaStock(binTotalNum);
    });

    // 为已进入主传送带且未分配库区的料箱选择库区
    requiredBins.forEach(entry -> {
          AreaQueue optimalArea = binAreas.stream().filter(a -> a.vacancy > 0)
              .sorted((area1,area2) -> getBestArea(area1,area2)).findFirst().orElse(null);

      // 如果最优库区不存在，说明入库传送带发生堵塞，每个队列都满了.
      if(optimalArea == null){
//        LOG.error("The inbound conveyor is blocked when selecting area. All the areas are full.");
        return;
      }
      LOG.info("method entry");
      entry.setAssignedArea(optimalArea.ID);
      binAreas.get(optimalArea.ID).decreaseVacancy();
      binAreas.get(optimalArea.ID).addWaitingBin(entry);
    });
  }
  
  /**
   * 选择轨道阶段:.
   * 根据该库区各轨道上的库存和轨道与库区队列入口的距离，
   * 为各库区队列中未被分配轨道的料箱分配入库轨道.
   */
  private void selectTrack() {
    binAreas.stream().forEach(area -> {
      area.waitingBins.stream().filter(bin -> bin.assignedTrack == null)
          .forEach(entry -> {
            Integer optimalTrack  = 
                IntStream.range(area.minTrack, area.maxTrack + 1).boxed()
                    .sorted((track1,track2) -> getBestTrack(track1,track2,area))
                    .findFirst().orElse(null);

            // 如果最优轨道不存在，说明该库区队列发生堵塞
            if(optimalTrack == null){
              LOG.error("The area{} queue is blocked.", area.ID);
              return;
            }

            LOG.info("method entry");
            entry.setAssignedTrack(optimalTrack);
          });
    });
  }
  
  /**
   * 生成入库订单阶段.
   * 检查分配给各库区的料箱是否已到达相应的库区队列入口，
   * 若已到达且未生成相应入库订单，则考虑为其生成入库订单，
   * 同时需考虑当前该库区内已生成入库订单的离入口最近的料箱所在轨道.
   */
  private void enableInboundOrder() {
    binAreas.stream().forEach(area -> {
      area.waitingBins.stream()
          .filter(bin -> bin.assignedTrack != null && bin.assignedInOrder == null)
          .filter(this::hasArrivedAreaQueue)
          .forEach(entry -> {
            BinEntry nearestBin = area.getNearestBin();
            
            if(area.ID < ENTRANCE_AREA){
              // 库区队列入口设在高序号轨道处
              if(nearestBin == null || nearestBin.assignedTrack > entry.assignedTrack){
                entry.setAssignedInOrder(createInboundOrder(entry).getReference());
                area.updateVacancy();
              }
            }
            else{
              // 库区队列入口设在低序号轨道处
              if(nearestBin == null || nearestBin.assignedTrack < entry.assignedTrack){
                entry.setAssignedInOrder(createInboundOrder(entry).getReference());
                area.updateVacancy();
              }
            }
            
          });
    });
  }
  
  /**
   * 根据当前时间与料箱被放上入库传送带的时间之差来判断料箱是否已到达主传送带
   * @param entry 待判断的料箱.
   * @return 当且仅当料箱已达主传送带时，返回{@code true};
   */
  private boolean hasArrivedMainConveyor(BinEntry entry) {
    return Instant.now().toEpochMilli() - entry.enterTime.toEpochMilli()
        >=  MAIN_CONVEYOR_TIME / timeFactorService.getSimulationTimeFactor();
  }
  
  /**
   * 根据当前时间与料箱到达主传送带的时间之差来判断料箱是否已到达所分配的库区队列入口.
   * @param entry 待判断的料箱.
   * @return 当且仅当料箱已达所分配的库区队列入口时，返回{@code true};
   */
  private boolean hasArrivedAreaQueue(BinEntry entry) {
    
    return Instant.now().toEpochMilli() - entry.enterTime.toEpochMilli()
        - MAIN_CONVEYOR_TIME / timeFactorService.getSimulationTimeFactor()
        >= Math.abs(entry.assignedArea - ENTRANCE_AREA) 
        * AREA_INTERVAL / timeFactorService.getSimulationTimeFactor();
  }
  
  public long getRunInterval(){
    return RUN_INTERVAL / (long)timeFactorService.getSimulationTimeFactor();
  }

  private int getBestArea(AreaQueue area1, AreaQueue area2) {
    Double inferiority1 = 
        - WEIGHT_VACANCY * area1.getVacantRate()
        + WEIGHT_WAIT_NUM * area1.getWaitRate()
        + WEIGHT_AREA_STOCK * area1.getStockRate();
    Double inferiority2 = 
        - WEIGHT_VACANCY * area2.getVacantRate()
        + WEIGHT_WAIT_NUM * area2.getWaitRate()
        + WEIGHT_AREA_STOCK * area2.getStockRate();
    return inferiority1.compareTo(inferiority2);
  }
  
  private int getBestTrack(Integer track1, Integer track2, AreaQueue area) {
    Double occupancy1 = getTrackStock(track1,area)/(double)getFullTrackStock(track1);
    Double occupancy2 = getTrackStock(track2,area)/(double)getFullTrackStock(track2);
    System.out.println(occupancy1);
    System.out.println(occupancy2);
    if(area.ID < ENTRANCE_AREA){
      // 此时库区队列入口设在高序号轨道处，越往里，轨道序号越小，则越优先；库存占用率越小越优先
      Double inferiority1 = WEIGHT_TRACK_STOCK * occupancy1 + WEIGHT_TRACK_NUM * (track1-area.minTrack)/(area.maxTrack-area.minTrack);
      Double inferiority2 = WEIGHT_TRACK_STOCK * occupancy2 + WEIGHT_TRACK_NUM * (track2-area.minTrack)/(area.maxTrack-area.minTrack);
      return inferiority1.compareTo(inferiority2);
    }
    else{
      // 此时库区队列入口设在低序号轨道处，越往里，轨道序号越大，则越优先；库存占用率越小越优先
      Double inferiority1 = WEIGHT_TRACK_STOCK * occupancy1 + WEIGHT_TRACK_NUM * (area.maxTrack-track1)/(area.maxTrack-area.minTrack);
      Double inferiority2 = WEIGHT_TRACK_STOCK * occupancy2 + WEIGHT_TRACK_NUM * (area.maxTrack-track2)/(area.maxTrack-area.minTrack);
      return inferiority1.compareTo(inferiority2);
    }
  }

  private int getTrackStock(int psbTrack, AreaQueue area) {
    int sum = 0;
    for(String location : locationPosition[psbTrack-1])
      sum += orderEnableService.getStackSize(location);
    long waitNum = area.waitingBins.stream()
        .filter(bin -> bin.assignedTrack!=null && bin.assignedTrack == psbTrack).count();
    sum += waitNum;
    return sum;
  }
  
  private static long getFullTrackStock(int psbTrack){
    long fullSum = 0;
    for(String location : locationPosition[psbTrack-1]){
      if(location != null)
        fullSum += Location.BINS_MAX_NUM;
    }
    return fullSum;
  }

  private InboundOrder createInboundOrder(BinEntry entry) {
    String orderPrefix = orderPrefixFor(OrderConstants.TYPE_IN_BOUND);
    InboundOrderCreationTO to = new InboundOrderCreationTO(orderPrefix+"-"+entry.bin.getName(),entry.bin)
        .withAssignedBinStack(getBestLocation(entry).getReference());
    
    try{
      InboundOrder inOrder = inOrderService.createInboundOrder(to);
      return inOrder;
    }
    catch (ObjectUnknownException | ObjectExistsException exc) {
      throw new IllegalStateException("Unexpectedly interrupted",exc);
    }
    catch (KernelRuntimeException exc) {
      throw new KernelRuntimeException(exc.getCause());
    }
  }

  private String orderPrefixFor(String prefix) {
    return objectNameProvider.getUniqueName(prefix);
  }

  private Location getBestLocation(BinEntry entry) {
    Set<String> locations = new HashSet<>(Arrays.asList(locationPosition[entry.assignedTrack-1]));
    Location optimalLocation = 
        locations.stream().filter(n -> n!=null)
            .map(name -> orderEnableService.fetchObject(Location.class,name))
            .filter(location -> location.getType().getName().equals(Location.BIN_STACK_TYPE))
            .sorted((location1,location2) -> {
              Integer size1 = location1.stackSize();
              Integer size2 = location2.stackSize();
              return size1.compareTo(size2);
            }).findFirst().orElse(null);
    return optimalLocation;
  }
  
  public synchronized void updateAreaQueue(int binTrack){
    binAreas.stream().forEach(area -> {
      for(BinEntry entry : area.waitingBins.stream()
          .filter(bin -> bin.assignedInOrder != null && bin.assignedTrack == binTrack)
          .collect(Collectors.toList())){
        area.waitingBins.remove(entry);
        area.increaseVacancy();
        inboundList.remove(entry);
      }
    });
  }
  
  private static final class BinEntry{
    private final Bin bin;
    private final Instant enterTime;
    private Integer assignedArea;
    private Integer assignedTrack;
    private TCSObjectReference<InboundOrder> assignedInOrder;

    public BinEntry(Bin bin) {
      this.enterTime = Instant.now();
      this.bin = bin;
    }

    public Bin getBin(){
      return bin;
    }
    
    public Integer getAssignedTrack() {
      return assignedTrack;
    }
    
    public void setAssignedArea(Integer assignedArea) {
      this.assignedArea = assignedArea;
    }

    public void setAssignedTrack(Integer assignedTrack) {
      this.assignedTrack = assignedTrack;
    }

    public void setAssignedInOrder(TCSObjectReference<InboundOrder> assignedInOrder) {
      this.assignedInOrder = assignedInOrder;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof BinEntry) {
        BinEntry other = (BinEntry) obj;
        return this.bin.equals(other.getBin()) && this.getClass().equals(other.getClass());
      }
      else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return getBin().hashCode()
        ^ this.getClass().getName().hashCode();
    }
  }
  
  private static final class AreaQueue{
    // 队列序号
    private final Integer ID;
    // 队列空位
    private Integer vacancy;
    // 队列中等待的料箱
    private List<BinEntry> waitingBins = new ArrayList<>();
    // 该库区的总库存量
    private Integer areaStock;
    // 该库区内序号最小的轨道
    private final Integer minTrack;
    // 该库区内序号最大的轨道
    private final Integer maxTrack;

    public AreaQueue(Integer ID, Integer minTrack,Integer maxTrack) {
      this.ID = ID;
      this.minTrack = minTrack;
      this.maxTrack = maxTrack;
      this.vacancy = maxTrack - minTrack + 1;
    }

    public Double getVacantRate(){
      double v = vacancy;
      return v/(maxTrack-minTrack+1);
    }
    
    public Double getWaitRate(){
      double w = waitingBins.size();
      return w/(maxTrack-minTrack+1);
    }
    
    public Double getStockRate(){
      double s = areaStock + waitingBins.size();
      return s/(maxTrack-minTrack+1)*getFullTrackStock(minTrack);
    }
    
    public Set<Integer> getAssignedTracks(){
      return waitingBins.stream().map(entry -> entry.assignedTrack).filter(track -> track!=null)
          .collect(Collectors.toSet());
    }
    
    /**
     * 从所有已生成入库订单的料箱中，选出最接近队列入口的料箱,
     * 该料箱所在的轨道将直接影响该队列的空位.
     * 
     * @return 已生成入库订单的料箱中，最接近队列入口的料箱.
     */
    public BinEntry getNearestBin(){
      if(ID < ENTRANCE_AREA)
        // 此时库区队列入口设在高序号轨道处
        return waitingBins.stream().filter(entry -> entry.assignedInOrder != null)
            .sorted(Comparator.comparing(BinEntry::getAssignedTrack,Comparator.reverseOrder()))
            .findFirst().orElse(null);
      else
        // 此时库区队列入口设在低序号轨道处
        return waitingBins.stream().filter(entry -> entry.assignedInOrder != null)
            .sorted(Comparator.comparing(BinEntry::getAssignedTrack))
            .findFirst().orElse(null);
    }
    
    public void updateVacancy() {
      if(ID < ENTRANCE_AREA){
        // 此时库区队列入口设在高序号轨道处
        BinEntry nearestBin = getNearestBin(); 
        long newVacancy = maxTrack - nearestBin.assignedTrack
            - waitingBins.stream().filter(entry -> entry.enterTime.isAfter(nearestBin.enterTime)).count();
        vacancy = newVacancy > 0 ? (int) newVacancy : 0;
      }
      else{
        // 此时库区队列入口设在低序号轨道处
       BinEntry nearestBin = getNearestBin(); 
        long newVacancy = nearestBin.assignedTrack - minTrack
            - waitingBins.stream().filter(entry -> entry.enterTime.isAfter(nearestBin.enterTime)).count();
        vacancy = newVacancy > 0 ? (int) newVacancy : 0;
      }
    }

    public void increaseVacancy(){
      this.vacancy += 1;
    }
    
    public void decreaseVacancy(){
      this.vacancy -= 1;
    }

    public void addWaitingBin(BinEntry waitingBin) {
      this.waitingBins.add(waitingBin);
    }

    public void setAreaStock(Integer areaStock) {
      this.areaStock = areaStock;
    }
  }
}