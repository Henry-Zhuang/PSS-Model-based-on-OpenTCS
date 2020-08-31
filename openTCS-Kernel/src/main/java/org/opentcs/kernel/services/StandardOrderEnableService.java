/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.access.to.order.TransportOrderCreationTO;
import org.opentcs.components.kernel.services.ChangeTrackService;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.components.kernel.services.TransportOrderService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObject;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Bin.SKU;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.TrackDefinition;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.OrderBinConstants;
import org.opentcs.data.order.OrderConstants;
import org.opentcs.data.order.OutboundOrder;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.kernel.outbound.OutboundWorkingSet;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentcs.components.kernel.services.OutboundOrderService;
import org.opentcs.components.kernel.services.InboundOrderService;
import org.opentcs.components.kernel.services.OrderEnableService;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.order.InboundOrder;

/**
 * 订单激活服务的标准实现类
 * @author Henry
 */
public class StandardOrderEnableService 
    extends AbstractTCSObjectService 
    implements OrderEnableService{
  /**
   * 这个类的日志记录器.
   */
  private static final Logger LOG = LoggerFactory.getLogger(StandardOrderEnableService.class);
  /**
   * 一个在内核中用于全局数据池同步的锁.
   * 保证数据池的多线程安全.
   */
  private final Object globalSyncObject;
  /**
   * 内核的全局数据池.
   * 注意：其中的操作不是多线程安全的，需要使用同步锁
   */
  private final TCSObjectPool objectPool;
  /**
   * 工作台.
   */
  private final OutboundWorkingSet outboundWorkingSet;
  /**
   * 运输订单服务.
   */
  private final TransportOrderService tOrderService;
  /**
   * 入库订单服务.
   */
  private final InboundOrderService inOrderService;
  /**
   * 出库订单服务.
   */
  private final OutboundOrderService outOrderService;
  /**
   * 换轨服务.
   */
  private final ChangeTrackService changeTrackService;
  /**
   * 站点的位置矩阵，记录着所有站点（包括出库站、入库站、库位站）之间的相对位置信息.
   */
  public static String[][] locationPosition;

  @Inject
  public StandardOrderEnableService(@GlobalSyncObject Object globalSyncObject, 
                                           TCSObjectPool objectPool,
                                           OutboundWorkingSet outboundWorkingSet,
                                           TransportOrderService tOrderService,
                                           InboundOrderService inOrderService,
                                           OutboundOrderService outOrderService,
                                           ChangeTrackService changeTrackService,
                                           TCSObjectService objectService) {
    super(objectService);
    this.globalSyncObject = globalSyncObject;
    this.objectPool = objectPool;
    this.tOrderService = tOrderService;
    this.inOrderService = inOrderService;
    this.outOrderService = outOrderService;
    this.changeTrackService = changeTrackService;
    this.outboundWorkingSet = outboundWorkingSet;
  }
  
  @Override
  public void enableOrder() {

    // 激活入库订单
    Set<Vehicle> assignedVehicles = enableInboundOrder();
    // 分解出库订单
    decomposeOutboundOrder(assignedVehicles);
  }
  
  /**
   * 激活入库订单
   */
  private Set<Vehicle> enableInboundOrder(){
    Set<Vehicle> assignedVehicles = new HashSet<>();
    fetchObjects(InboundOrder.class,order -> order.hasState(InboundOrder.State.AWAIT_DISPATCH))
        .forEach(order -> {
          Location binStack = fetchObject(Location.class,order.getAssignedBinStack());
   
          if(!changeTrackService.isNoVehicleTrack(binStack.getPsbTrack())){
            Vehicle sameTrackVehicle = 
                fetchObjects(Vehicle.class, v -> v.getType().equals(Vehicle.BIN_VEHICLE_TYPE))
                    .stream()
                    .filter(psb -> inTheSameTrack(binStack.getPsbTrack(),psb))
                    .findFirst().orElse(null);
            if(sameTrackVehicle == null)
              return;
            
            Bin newBin = createBin(order, binStack.getPsbTrack());
            try{
              createTransportOrderForBin(newBin, sameTrackVehicle, order);
              inOrderService.setInboundOrderState(order.getReference(), InboundOrder.State.DISPATCHED);
              assignedVehicles.add(sameTrackVehicle);
            }
            catch(Exception exc){
              LOG.error("Error create change track order for {} failed ",order.getName(),exc);
            }
          }
          else{
            Vehicle idleVehicle = fetchObjects(Vehicle.class,this::couldProcessTransportOrder)
                .stream()
                .filter(psb -> !assignedVehicles.contains(psb))
                .sorted((psb1,psb2) -> compareDistance(psb1,psb2,binStack.getPsbTrack()))
                .findFirst().orElse(null);
            
            if(idleVehicle == null)
              return;
            Bin newBin = createBin(order, binStack.getPsbTrack());
            try{
              createTransportOrderForBin(newBin, idleVehicle, order);
              inOrderService.setInboundOrderState(order.getReference(), InboundOrder.State.DISPATCHED);
              assignedVehicles.add(idleVehicle);
            }
            catch(Exception exc){
              LOG.error("Error create change track order for {} failed ",order.getName(),exc);
            }
          }
        });
    return assignedVehicles;
  }
  /**
   * 分解出库订单
   */
  private void decomposeOutboundOrder(Set<Vehicle> assignedVehicles){
    List<OutboundOrder> outOrders = 
        outboundWorkingSet.getWorkingSets().stream()
            .map(ref -> fetchObject(OutboundOrder.class,ref))
            // 根据订单的完成度来定义订单的优先级，完成度越高，优先级越高
            // 如果完成度相同，则根据订单的DDL来排序
            .sorted(Comparator.comparing(OutboundOrder::getReservedCompletion,Comparator.reverseOrder())
                .thenComparing(OutboundOrder::getDeadline))
            // 不需要考虑已完成的订单.
            .filter(order -> order.getReservedCompletion() < 1.0)
            .collect(Collectors.toList());

    for(OutboundOrder outOrder : outOrders){
      // 寻找适合当前订单最优料箱
      Bin bin = getOptimalBin(outOrder);
      // 如果最优料箱存在，则对该料箱生成出库任务并分派空闲车辆运行
      // 否则跳过该订单，对下一订单寻找最优料箱
      if(bin != null){
        // 料箱所在轨道
        Integer binTrack = bin.getPsbTrack();
        // 找出离料箱最近的空闲车去执行此料箱的运输任务
        Vehicle idleVehicle = fetchObjects(Vehicle.class, this::couldProcessTransportOrder)
            .stream()
            .filter(psb -> !assignedVehicles.contains(psb))
            .sorted((psb1,psb2) -> compareDistance(psb1,psb2,binTrack))
            .findFirst()
            .orElse(null);

        if(idleVehicle == null)
          return;
        
        // 为当前订单预定料箱中的SKU
        bin = reserveSkuForOrder(bin,outOrder);

        // 查看其他订单是否需要预定该料箱剩下未被预定的SKU
        outOrders.remove(outOrder);
        for(OutboundOrder order : outOrders){
          bin = reserveSkuForOrder(bin,order);
        }
        // 将料箱的预订表同步到数据池中
        synchronized(globalSyncObject){
          objectPool.replaceObject(bin);
        }

        // 为该料箱创建出库运输任务，并分派空闲车辆去执行任务
        try{
          createTransportOrderForBin(bin,idleVehicle, outOrder);
        }
        catch(Exception exc){
          LOG.error("Error create change track order for {} failed ",outOrder.getName(),exc);
        }
        break;
      }
    }
  }
  
  /**
   * 为指定出库订单寻找最优出库料箱.
   * @param outOrder 指定出库订单.
   * @return 最优出库料箱.
   */
  private Bin getOptimalBin(OutboundOrder outOrder) {
    return fetchObjects(Bin.class, 
                        b ->  b.getAssignedTransportOrder() == null 
                            && b.hasState(Bin.State.Still))
        .stream()
        // 料箱所在轨道要么无车，要么只有空闲车
        .filter(bin -> 
            changeTrackService.isNoVehicleTrack(bin.getPsbTrack())
           || isIdleTrack(bin.getPsbTrack())
           )
        // 料箱必须能全部或部分满足该出库订单剩余的SKU需求
        .filter(bi -> !getMatchedSKUs(bi,outOrder.getLeftSKUs()).isEmpty())
        // 根据一定规则选择最优料箱
        .sorted((bin1,bin2) -> findBetterBin(bin1,bin2,outOrder))
        .findFirst()
        .orElse(null);
  }
  
  private boolean isIdleTrack(int psbTrack) {
    return fetchObjects(Vehicle.class, this::couldProcessTransportOrder)
        .stream()
        .map(psb -> fetchObject(Point.class,psb.getCurrentPosition()).getPsbTrack())
        .collect(Collectors.toSet())
        .contains(psbTrack);
  }
  
  /**
   * 根据给定的SKU需求，找到料箱中与这些需求相匹配的SKU.
   * @param bin 待查找的料箱
   * @param SKUs 给定的SKU需求.
   * @return 与需求相匹配的SKU集合
   */
  private Set<SKU> getMatchedSKUs(Bin bin, Set<SKU> SKUs){
    // 出库订单的需求表
    Map<String,Double> SkuMap = SKUs.stream().collect(Collectors.toMap(SKU::getSkuID, SKU::getQuantity));
    // 料箱中剩下还未被预订的SKU
    Set<SKU> leftSKUs = bin.getNotReservedSKUs();
    // 将未被预订的SKU与订单要求的SKU作交集
    leftSKUs.retainAll(SKUs);
    Set<SKU> matchedSKUs = new HashSet<>();
    // 由于交集后所得的SKU的数量有可能是超出了订单需求量
    // 因此需要将数量限制为 料箱持有量和订单需求量 中的较小者
    leftSKUs.forEach(sku -> {
      matchedSKUs.add(new SKU(sku.getSkuID(),getQuantityWithLimit(sku,SkuMap)));
    });
    
   return matchedSKUs;
  }
  
  /**
   * 根据指定的出库订单，找出两个料箱中最优的一个.
   */
  private int findBetterBin(Bin bin1, Bin bin2, OutboundOrder outOrder) {
    // 首先比较料箱距离栈顶的距离，优先选择离栈顶近的
    Integer convenience1 = getStackSize(bin1.getAttachedLocation().getName())-bin1.getBinPosition();
    Integer convenience2 = getStackSize(bin2.getAttachedLocation().getName())-bin2.getBinPosition();
    if(!Objects.equals(convenience1, convenience2))
      return convenience1.compareTo(convenience2);
    
    else{
      // 其次优先选择能够较大程度地满足出库订单需求的料箱
      Set<SKU> SKUs1 = getMatchedSKUs(bin1,outOrder.getLeftSKUs());
      Set<SKU> SKUs2 = getMatchedSKUs(bin1,outOrder.getLeftSKUs());
      Double amount1 = SKUs1.stream().map(SKU::getQuantity).reduce(Double::sum).orElse(0.0);
      Double amount2 = SKUs2.stream().map(SKU::getQuantity).reduce(Double::sum).orElse(0.0);
      return amount2.compareTo(amount1);
    }
  }
  
  /**
   * 查询料箱中是否有出库订单需要的SKU，若有则为出库订单预订.
   * @param bin 待查询的料箱
   * @param outOrder 出库订单
   * @return 更新了预订表之后的料箱.
   */
  private Bin reserveSkuForOrder(Bin bin, OutboundOrder outOrder) {
    // 查询该料箱中是否有出库订单想预订的SKU
    Set<SKU> matchedSKUs = getMatchedSKUs(bin,outOrder.getLeftSKUs());
    if(matchedSKUs.isEmpty())
      // 如果该料箱中并没有出库订单想要的SKU，则直接跳过预订
      return bin;
    
    // 如果有，更新料箱的预订表
    Map<String,Set<SKU>> reservations = bin.getReservations();
    reservations.put(outOrder.getName(), matchedSKUs);
    bin = bin.withReservations(reservations);
    
    // 更新出库订单的预订情况
    outOrderService.reserveSKUs(outOrder.getReference(), matchedSKUs);
    outOrderService.addAssignedBin(outOrder.getReference(), bin.getReference());
    
    return bin;
  }
  
  /**
   * 获取料箱持有量和订单需求量中的较小者.
   */
  private static Double getQuantityWithLimit(SKU sku,Map<String, Double> Skus){
      Double limit = Skus.get(sku.getSkuID());
      Double quantity = sku.getQuantity();
      return Math.min(limit, quantity);
  }
  
  @Override
  public void createTransportOrderForBin(Bin bin, Vehicle idleVehicle, TCSObject<?> order) throws Exception {
    LOG.debug("method entry");
    
    Set<String> changeTrackOrderName = new HashSet<>();
    if(!inTheSameTrack(bin.getPsbTrack(),idleVehicle)){
      String trackOrder = changeTrackService.createChangeTrackOrder(bin,idleVehicle);        
      changeTrackOrderName.add(trackOrder);
    }
    
    TransportOrderCreationTO to;
    
    if (order instanceof OutboundOrder){
      OutboundOrder outOrder = (OutboundOrder) order;
      to = new TransportOrderCreationTO(outOrder.getName()
                                        +"-"
                                        +bin.getName()
                                        +"["
                                        +bin.getAttachedLocation().getName()
                                        +":"
                                        +bin.getBinPosition()
                                        +"]")
          .withDestinations(outBoundDestinations(bin))
          .withType(OrderConstants.TYPE_OUT_BOUND)
          .withIntendedVehicleName(idleVehicle.getName())
          .withDependencyNames(changeTrackOrderName)
          .withDeadline(outOrder.getDeadline())
          .withProperties(outOrder.getProperties());
    }
    else {
      // 入库
      InboundOrder inOrder = (InboundOrder) order;
      to = new TransportOrderCreationTO(inOrder.getName()+"-")
          .withDestinations(inBoundDestinations(bin,inOrder))
          .withType(OrderConstants.TYPE_IN_BOUND)
          .withIntendedVehicleName(idleVehicle.getName())
          .withDependencyNames(changeTrackOrderName)
          .withDeadline(inOrder.getDeadline())
          .withAttachedInboundOrder(inOrder.getReference())
          .withProperties(inOrder.getProperties());
    }
    try{
      synchronized(globalSyncObject){
        objectPool.replaceObject(bin.withAssignedTransportOrder(
            tOrderService.createTransportOrder(to).getReference()));
      }
    }
    catch (ObjectUnknownException | ObjectExistsException exc) {
      throw new IllegalStateException("Unexpectedly interrupted",exc);
    }
    catch (KernelRuntimeException exc) {
      throw new KernelRuntimeException(exc.getCause());
    }
  }
  
  /**
   * 判断指定车辆（PSB）是否在指定料箱轨道上.
   * @param binTrack 指定料箱轨道
   * @param vehicle 指定车辆（PSB）
   * @return 当且仅当料箱和车辆在同一轨道上时返回{@code true}.
   */
  private boolean inTheSameTrack(int binTrack, Vehicle vehicle) {

      int vehicleTrack = fetchObject(Point.class,
                                     vehicle.getCurrentPosition()).getPsbTrack();
      return binTrack == vehicleTrack;
  }
  
  private List<DestinationCreationTO> inBoundDestinations(Bin bin, InboundOrder inOrder) {
    String inboundStation = getInBoundStation(bin.getPsbTrack());
    String binStack = inOrder.getAssignedBinStack().getName();
    List<DestinationCreationTO> result = new ArrayList<>();
    // 入库站抓箱
    result.add(new DestinationCreationTO(inboundStation, OrderBinConstants.OPERATION_LOAD));
    // 指定库位站放箱
    result.add(new DestinationCreationTO(binStack, OrderBinConstants.OPERATION_UNLOAD));
    return result;
  }
  
  /**
   * 为指定料箱生成出库运输指令集，
   * @param bin 待出库的料箱
   * @return 出库运输指令集
   */
  private List<DestinationCreationTO> outBoundDestinations(Bin bin){
    String locationName = bin.getAttachedLocation().getName();
    int psbTrack = bin.getPsbTrack();
    int pstTrack = bin.getPstTrack();
    int binPosition = bin.getBinPosition();
    
    List<DestinationCreationTO> result = new ArrayList<>();
    int stackSize = getStackSize(locationName);
    List<String> tmpLocs = getVacantNeighbours(psbTrack, pstTrack, stackSize - 1 - binPosition);
    if(tmpLocs == null)
      return null;
    // 该轨道的出库站
    String outboundStation = getOutBoundStation(psbTrack);

    // 倒箱
    for(String tmpLoc:tmpLocs){
      result.add(new DestinationCreationTO(locationName, OrderBinConstants.OPERATION_LOAD));
      result.add(new DestinationCreationTO(tmpLoc, OrderBinConstants.OPERATION_UNLOAD));
    }
    tmpLocs.size();
   
    // 指定料箱出库
    result.add(new DestinationCreationTO(locationName, OrderBinConstants.OPERATION_LOAD));
    result.add(new DestinationCreationTO(outboundStation, OrderBinConstants.OPERATION_UNLOAD));

    return result;
  }
  
  /**
   * 在料箱的出库搬运需要倒箱时，为其寻找附近能够用于倒箱的空位.
   * @param psbTrack 需要出库的料箱所在的PSB轨道.
   * @param pstTrack 需要出库的料箱所在的PST轨道.
   * @param vacancyNum 需要用于倒箱的空位总数
   * @return 用于倒箱的空位.
   */
  private List<String> getVacantNeighbours(int psbTrack, int pstTrack, int vacancyNum){
    int offset = 1;
    int row = psbTrack - 1;
    int column = pstTrack - 1;
    List<String> vacantNeighbours = new ArrayList<>();
      while(vacancyNum > 0 
          && (column-offset >= 0 || column+offset < locationPosition[row].length)){
        // A vacant neighbour firstly should be valid.
        // A picking station is not considered as a vacant neighbour.
        if(column-offset >= 0
            && locationPosition[row][column-offset] != null
            && !isOutBoundStation(locationPosition[row][column-offset])){
          int vacancy = Location.BINS_MAX_NUM - getStackSize(locationPosition[row][column-offset]);
          vacancy = vacancy > vacancyNum ? vacancyNum : vacancy;
          for(int i=0;i<vacancy;i++)
            vacantNeighbours.add(locationPosition[row][column-offset]);
          vacancyNum -= vacancy;
        }
        
        if(vacancyNum > 0 
            && column+offset < locationPosition[row].length
            && locationPosition[row][column+offset] != null
            && !isOutBoundStation(locationPosition[row][column+offset])){
          int vacancy = Location.BINS_MAX_NUM - getStackSize(locationPosition[row][column+offset]);
          vacancy = vacancy > vacancyNum ? vacancyNum : vacancy;
          for(int i=0;i<vacancy;i++)
            vacantNeighbours.add(locationPosition[row][column+offset]);
          vacancyNum -= vacancy;
        }
        offset++;
      }
    return vacancyNum <= 0 ? vacantNeighbours : null;
  }
  
  /**
   * 查询指定库位站的当前存放料箱数.
   * @param locationName 指定库位站的ID
   * @return 库位站的当前存放料箱数.
   */
  @Override
  public int getStackSize(String locationName){
    if(locationName == null)
      return 0;
    return fetchObject(Location.class, locationName).stackSize();
  }
  
  /**
   * 获取指定轨道上的入库站ID.
   * @param psbTrack 指定轨道.
   * @return 指定轨道上的入库站ID.
   */
  public String getInBoundStation(int psbTrack){
    for(String location:locationPosition[psbTrack-1]){
      if(location != null && isInBoundStation(location))
        return location;
    }
    return null;
  }
  
  private boolean isInBoundStation(String locationName) {
    return fetchObject(Location.class,locationName)
              .getType().getName().equals(Location.IN_BOUND_STATION_TYPE);
  }
  
  /**
   * 获取指定轨道上的出库站ID.
   * @param psbTrack 指定轨道.
   * @return 指定轨道上的出库站ID.
   */
  private String getOutBoundStation(int psbTrack){
    for(String location:locationPosition[psbTrack-1]){
      if(location != null && isOutBoundStation(location))
        return location;
    }
    return null;
  }
  
  private boolean isOutBoundStation(String locationName) {
    return fetchObject(Location.class,locationName)
              .getType().getName().equals(Location.OUT_BOUND_STATION_TYPE);
  }
  
  @Override
  public void updateTrackInfo(){
    synchronized(globalSyncObject){
      List<Long> sortedXPos = new ArrayList<>();
      List<Long> sortedYPos = new ArrayList<>();
      for (Point point:fetchObjects(Point.class)){
        sortedXPos.add(point.getPosition().getX());
        sortedYPos.add(point.getPosition().getY());
      }
      List<Long> psbTrackPool;
      List<Long> pstTrackPool;
      switch(TrackDefinition.PSB_TRACK_DEFINITION){
        case Y_POSITION :
          // 如果PSB轨道是以Y轴坐标进行划分
          psbTrackPool = sortedYPos.stream().distinct().sorted().collect(Collectors.toList());
          pstTrackPool = sortedXPos.stream().distinct().sorted().collect(Collectors.toList());

          locationPosition = new String[psbTrackPool.size()][pstTrackPool.size()];

          for (Point point:fetchObjects(Point.class)){
            point.setPsbTrack(psbTrackPool.indexOf(point.getPosition().getY()) + 1);
            point.setPstTrack(pstTrackPool.indexOf(point.getPosition().getX()) + 1);
            objectPool.replaceObject(point);
            Location tmpLoc;
            for(Location.Link link:point.getAttachedLinks()){
              tmpLoc = fetchObject(Location.class, link.getLocation());
              tmpLoc.setPsbTrack(point.getPsbTrack());
              tmpLoc.setPstTrack(point.getPstTrack());

              for(Bin bin : tmpLoc.getBins())
                objectPool.replaceObject(bin);

              objectPool.replaceObject(tmpLoc);
              locationPosition[tmpLoc.getPsbTrack()-1][tmpLoc.getPstTrack()-1] = tmpLoc.getName();
            }
          }
          break;
        default :
          // 如果PSB轨道是以X轴坐标进行划分
          psbTrackPool = sortedXPos.stream().distinct().sorted().collect(Collectors.toList());
          pstTrackPool = sortedYPos.stream().distinct().sorted().collect(Collectors.toList());

          locationPosition = new String[psbTrackPool.size()][pstTrackPool.size()];

          for (Point point:fetchObjects(Point.class)){
            point.setPsbTrack(psbTrackPool.indexOf(point.getPosition().getX()) + 1);
            point.setPstTrack(pstTrackPool.indexOf(point.getPosition().getY()) + 1);
            objectPool.replaceObject(point);
            Location tmpLoc;
            for(Location.Link link:point.getAttachedLinks()){
              tmpLoc = fetchObject(Location.class, link.getLocation());
              tmpLoc.setPsbTrack(point.getPsbTrack());
              tmpLoc.setPstTrack(point.getPstTrack());

              for(Bin bin : tmpLoc.getBins())
                objectPool.replaceObject(bin);

              objectPool.replaceObject(tmpLoc);
              locationPosition[tmpLoc.getPsbTrack()-1][tmpLoc.getPstTrack()-1] = tmpLoc.getName();
            }
          }
          break;
      }
    }
  }

  @Override
  public String[][] getLocPosition() {
    return locationPosition;
  }
  
  private boolean couldProcessTransportOrder(Vehicle vehicle) {
    return vehicle.getIntegrationLevel() == Vehicle.IntegrationLevel.TO_BE_UTILIZED
        && vehicle.getType().equals(Vehicle.BIN_VEHICLE_TYPE)
        && vehicle.getCurrentPosition() != null
        && !vehicle.isEnergyLevelCritical()
        && (processesNoOrder(vehicle)
            || processesDispensableOrder(vehicle));
  }
  
  private boolean processesNoOrder(Vehicle vehicle) {
    return vehicle.hasProcState(Vehicle.ProcState.IDLE)
        && (vehicle.hasState(Vehicle.State.IDLE)
            || vehicle.hasState(Vehicle.State.CHARGING));
  }

  private boolean processesDispensableOrder(Vehicle vehicle) {
    return vehicle.hasProcState(Vehicle.ProcState.PROCESSING_ORDER)
        && fetchObject(TransportOrder.class, vehicle.getTransportOrder())
            .isDispensable();
  }

  private int compareDistance(Vehicle psb1, Vehicle psb2, int binTrack) {
    Integer distance1 = Math.abs(fetchObject(Point.class,
                                             psb1.getCurrentPosition()).getPsbTrack() - binTrack);
    Integer distance2 = Math.abs(fetchObject(Point.class,
                                             psb2.getCurrentPosition()).getPsbTrack() - binTrack);
    return  distance1.compareTo(distance2);
  }

  private Bin createBin(InboundOrder order, int binTrack) {
    synchronized(globalSyncObject){
      Location inStation = fetchObject(Location.class, getInBoundStation(binTrack));
      Bin newBin = order.getBin().withAttachedLocation(inStation.getReference())
          .withPsbTrack(binTrack)
          .withPstTrack(inStation.getPstTrack())
          .withBinPosition(inStation.stackSize())
          .withState(Bin.State.Still);
      objectPool.addObject(newBin);
      objectPool.emitObjectEvent(newBin.clone(), null, TCSObjectEvent.Type.OBJECT_CREATED);

      Location previous = inStation;
      inStation.push(newBin);
      objectPool.replaceObject(inStation);
      objectPool.emitObjectEvent(inStation, previous, TCSObjectEvent.Type.OBJECT_MODIFIED);

      inOrderService.setInboundOrderBin(order.getReference(), newBin);
      return newBin;
    }
  }
}