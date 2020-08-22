/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.access.to.order.TransportOrderCreationTO;
import org.opentcs.components.kernel.ObjectNameProvider;
import org.opentcs.components.kernel.services.ChangeTrackService;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.components.kernel.services.TransportOrderService;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.DriveOrder;
import org.opentcs.data.order.OrderConstants;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.BinOrder;
import org.opentcs.drivers.vehicle.VehicleControllerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentcs.components.kernel.services.OrderDecompositionService;

/**
 *
 * @author Henry
 */
public class StandardChangeTrackService 
    extends AbstractTCSObjectService
    implements ChangeTrackService {
  
  public static final String BIN_ORDER_SUFFIX = "-1";
  public static final String TRACK_ORDER_SUFFIX = "-2";
  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(StandardChangeTrackService.class);
  /**
   * The service we use to create change-track orders.
   */
  private final TransportOrderService orderService;
  /**
   * Provides names for change-track orders.
   */
  private final ObjectNameProvider objectNameProvider;
  /**
   * The data base service.
   */
  private final OrderDecompositionService orderDecomService;
  /**
   * The vehicle controller pool.
   */
  private final VehicleControllerPool controllerPool;
  /**
   * The change-track order pool.
   * <p>The key is a unique change-track order name prefix.</p>
   * <p>The value is a track order entry of this change-track order.</p>
   */
  private final Map<String, TrackOrderEntry> trackOrderPool = new HashMap<>();
  /**
   * A list of the tracks where there is no vehicles.
   * <p>If there is any transport order assigned to a no-vehicle track, we need to create
   * a change-track order for this transport order.</p>
   */
  private Set<Integer> noVehicleTracks = new HashSet<>();
  /**
   * Whether the vehicles' states are changed or not.
   */
  private volatile boolean vehicleStateChanged;
  
  @Inject
  public StandardChangeTrackService(TCSObjectService objectService,
                                    TransportOrderService orderService,
                                    ObjectNameProvider orderNameProvider,
                                    OrderDecompositionService orderDecomService,
                                    VehicleControllerPool controllerPool) {
    super(objectService);
    this.orderService = requireNonNull(orderService, "orderService");
    this.objectNameProvider = requireNonNull(orderNameProvider, "orderNameProvider");
    this.orderDecomService = requireNonNull(orderDecomService, "orderDecomService");
    this.controllerPool = requireNonNull(controllerPool, "controllerPool");
  }

  @Override
  public void updateTrackList() {
    if (!isVehicleStateChanged())
      return;
    
    if(orderDecomService.getLocPosition() == null)
      return;
    
    noVehicleTracks = IntStream.range(1, orderDecomService.getLocPosition().length + 1)
        .boxed().collect(Collectors.toSet());
    
    fetchObjects(Vehicle.class,
                 vehicle -> vehicle.getIntegrationLevel() == Vehicle.IntegrationLevel.TO_BE_UTILIZED)
        .stream()
        .filter(veh -> veh.getType().equals(Vehicle.BIN_VEHICLE_TYPE))
        .forEach(PSB -> noVehicleTracks.remove(fetchObject(Point.class,
                                                           PSB.getCurrentPosition()).getPsbTrack()));
    vehicleStateChanged = false;
  }
  
  private boolean isVehicleStateChanged() {
    return vehicleStateChanged;
  }

  @Override
  public void clear() {

    trackOrderPool.clear();
    noVehicleTracks.clear();

    vehicleStateChanged = true;
  }
  
  @Override
  public void setVehicleStateChanged(){
    vehicleStateChanged = true;
  }
  
  @Override
  public String createChangeTrackOrder(Bin bin, Vehicle binVehicle) {
    synchronized(this){
      
      updateTrackList();
      
      if(bin.getAttachedLocation() == null)
        return null;
      // 该PSB已被分配了换轨任务，不可再分配新的换轨任务
      for(Map.Entry<String, TrackOrderEntry> entry : trackOrderPool.entrySet()){
        if(binVehicle.getName().equals(entry.getValue().getBinVehicle()))
          return null;
      }
      
      LOG.debug("method entry");

    }
  }
  
  private String createChangeTrackOrder(Vehicle binVehicle, Bin bin) {
    // 换轨起始轨
    int srcTrack = fetchObject(Point.class,binVehicle.getCurrentPosition()).getPsbTrack();

    // 换轨终点轨
    int dstTrack = bin.getPsbTrack();
    
    // 目标箱所在库位
    String dstLocation = bin.getAttachedLocation().getName();
    // 需要执行该换轨任务的PST
    Vehicle trackVehicle = 
        fetchObjects(Vehicle.class,this::canBeUtilized).stream()
            .filter(pst -> pst.getAllowedTracks().contains(srcTrack)
                        && pst.getAllowedTracks().contains(dstTrack))
            .sorted((Vehicle PST1, Vehicle PST2) -> compareDistance(PST1, PST2, binVehicle, dstLocation))
            .findFirst()
            .orElse(null);
         
    if(trackVehicle == null){
      LOG.warn("No change-track vehicles can be utilized at the moment.");
      return null;
    }
    
    String trackOrderPrefix = namePrefixFor(OrderConstants.TYPE_CHANGE_TRACK);
    TransportOrderCreationTO psbTO = new TransportOrderCreationTO(trackOrderPrefix+BIN_ORDER_SUFFIX)
        .withDestinations(BinDestinations(trackVehicle, srcTrack, dstTrack, dstLocation))
        .withIntendedVehicleName(binVehicle.getName())
        .withType(OrderConstants.TYPE_CHANGE_TRACK);
    
    TransportOrderCreationTO pstTO = new TransportOrderCreationTO(trackOrderPrefix+TRACK_ORDER_SUFFIX)
        .withDestinations(TrackDestinations(trackVehicle, srcTrack, dstTrack))
        .withIntendedVehicleName(trackVehicle.getName())
        .withType(OrderConstants.TYPE_CHANGE_TRACK);
    
    try{
      TransportOrder psbOrder = orderService.createTransportOrder(psbTO);
      orderService.createTransportOrder(pstTO);
      trackOrderPool.put(trackOrderPrefix, 
                         new TrackOrderEntry(binVehicle.getName(),
                                            trackVehicle.getName(),
                                            srcTrack, 
                                            dstTrack));
      noVehicleTracks.add(srcTrack);
      noVehicleTracks.remove(dstTrack);
      return psbOrder.getName();
    }
    catch (ObjectUnknownException | ObjectExistsException exc) {
      throw new IllegalStateException("Unexpectedly interrupted",exc);
    }
    catch (KernelRuntimeException exc) {
      throw new KernelRuntimeException(exc.getCause());
    }
  }

  @Override
  public void notifyBinVehicle(@Nonnull String orderName) {
    LOG.debug("method entry");
    TrackOrderEntry entry = trackOrderPool.get(getPrefix(orderName));
    if(entry == null){
      LOG.error("Error track order entry of {} not found when notifying bin vehicle.",orderName);
      return;
    }
    controllerPool.getVehicleController(entry.getBinVehicle()).setTrackVehicleInPlace();
  }

  @Override
  public void notifyTrackVehicle(String orderName) {
    LOG.debug("method entry");
    TrackOrderEntry entry = trackOrderPool.get(getPrefix(orderName));
    if(entry == null){
      LOG.error("Error track order entry of {} not found when notifying track vehicle.",orderName);
      return;
    }
    controllerPool.getVehicleController(entry.getTrackVehicle()).setBinVehicleInPlace();
  }
  
  @Override
  public void updateTrackOrder(TCSObjectReference<TransportOrder> orderRef, 
                               TransportOrder.State state) {
    LOG.debug("method entry");
    if(state != TransportOrder.State.FINISHED){
      TrackOrderEntry entry = trackOrderPool.get(getPrefix(orderRef.getName()));
      if(entry != null){
        LOG.warn("Warning change-track order {} is failed.",orderRef.getName());
        noVehicleTracks.remove(entry.getSrcTrack());
        int currTrack = fetchObject(Point.class,
                                    fetchObject(Vehicle.class,entry.getBinVehicle())
                                    .getCurrentPosition())
                        .getPsbTrack();
        noVehicleTracks.add(currTrack);
        trackOrderPool.remove(getPrefix(orderRef.getName()));
        System.out.println(noVehicleTracks);//Test
      }
    }
    else if(fetchObjects(TransportOrder.class,
                         order -> order.getName().startsWith(getPrefix(orderRef.getName())))
        .stream()
        .allMatch(tOrder -> tOrder.hasState(TransportOrder.State.FINISHED))){
      trackOrderPool.remove(getPrefix(orderRef.getName()));
    }
  }

  @Override
  public boolean isEnteringFirstTrackPoint(Point dstPoint, String orderName) {
    TrackOrderEntry entry = trackOrderPool.get(getPrefix(orderName));
    if(entry == null){
      LOG.warn("Warning track order entry of {} not found when checking isEnteringFirstTrackPoint.",orderName);
      return false;
    }
    
    return isFirstTrackPoint(dstPoint, entry);
  }

  @Override
  public boolean isLeavingFirstTrackPoint(Point srcPoint, String orderName) {
    TrackOrderEntry entry = trackOrderPool.get(getPrefix(orderName));
    if(entry == null){
      LOG.warn("Error track order entry of {} not found when checking isLeavingFirstTrackPoint.",orderName);
      return false;
    }
    
    return isFirstTrackPoint(srcPoint, entry);
  }

  private String getPrefix(String orderName) {
    return orderName.substring(0, orderName.length()-BIN_ORDER_SUFFIX.length());
  }
  
  private String namePrefixFor(@Nonnull String prefix) {
    return objectNameProvider.getUniqueName(prefix);
  }
  
  private boolean canBeUtilized(Vehicle trackVehicle) {
    return trackVehicle.getType().equals(Vehicle.TRACK_VEHICLE_TYPE)
        && trackVehicle.getIntegrationLevel().equals(Vehicle.IntegrationLevel.TO_BE_UTILIZED);
  }
  
  private int compareDistance(Vehicle PST1, Vehicle PST2, Vehicle binVehicle, String locationName){
    Point point1 = fetchObject(Point.class, PST1.getCurrentPosition());
    Point point2 = fetchObject(Point.class, PST2.getCurrentPosition());
    Point srcPoint = fetchObject(Point.class, binVehicle.getCurrentPosition());
    Location dstLocation = fetchObject(Location.class, locationName);
    Integer distance1 = Math.abs(point1.getPstTrack()-srcPoint.getPstTrack())
        + Math.abs(point1.getPstTrack()-dstLocation.getPstTrack());
    Integer distance2 = Math.abs(point2.getPstTrack()-srcPoint.getPstTrack())
        + Math.abs(point2.getPstTrack()-dstLocation.getPstTrack());
    return distance1.compareTo(distance2);
  }

  private List<DestinationCreationTO> BinDestinations(Vehicle trackVehicle, int srcTrack, int dstTrack, String dstLocation) {
    List<DestinationCreationTO> result = TrackDestinations(trackVehicle, srcTrack, dstTrack);
    result.add(new DestinationCreationTO(dstLocation, DriveOrder.Destination.OP_NOP));
    return result;
  }

  private List<DestinationCreationTO> TrackDestinations(Vehicle trackVehicle, int srcTrack, int dstTrack) {
    List<DestinationCreationTO> result = new ArrayList<>();
    List<Point> trackPoints;
    Point srcTrackPoint;
    Point dstTrackPoint;
    
    int pstTrack = fetchObject(Point.class, trackVehicle.getCurrentPosition()).getPstTrack();
    trackPoints = fetchObjects(Point.class, p -> p.getPstTrack() == pstTrack).stream()
        .filter(point -> point.getPsbTrack() == srcTrack || point.getPsbTrack() == dstTrack)
        .collect(Collectors.toList());

    if(trackPoints.size()!=2){
      LOG.error("Error trackPoints can't be found when change track from Track {} to Track {}",srcTrack,dstTrack);
      throw new ObjectUnknownException("Track points can't be found when creating change-track order.");
    }
    
    if(trackPoints.get(0).getPsbTrack() == srcTrack){
      srcTrackPoint = trackPoints.get(0);
      dstTrackPoint = trackPoints.get(1);
    }
    else{
      srcTrackPoint = trackPoints.get(1);
      dstTrackPoint = trackPoints.get(0);
    }

    result.add(new DestinationCreationTO(srcTrackPoint.getName(),DriveOrder.Destination.OP_MOVE));
    result.add(new DestinationCreationTO(dstTrackPoint.getName(),DriveOrder.Destination.OP_MOVE));
    return result;
  }

  private boolean isFirstTrackPoint(Point point, TrackOrderEntry entry) {
    int psbTrack = entry.srcTrack;
    int pstTrack = fetchObject(Point.class, 
                            fetchObject(Vehicle.class,entry.trackVehicle)
                                .getCurrentPosition()).getPstTrack();
    return point.getPsbTrack() == psbTrack && point.getPstTrack() == pstTrack;
  }

  @Override
  public boolean isNoVehicleTrack(int psbTrack) {
    return noVehicleTracks.contains(psbTrack);
  }
  
  private static final class TrackOrderEntry{
    private final String binVehicle;
    private final String trackVehicle;
    private final int srcTrack;
    private final int dstTrack;
    
    public TrackOrderEntry(String binVehicle, String trackVehicle, int srcTrack, int dstTrack){
      this.binVehicle = requireNonNull(binVehicle, "binVehicle");
      this.trackVehicle = requireNonNull(trackVehicle, "trackVehicle");
      this.srcTrack = requireNonNull(srcTrack, "srcTrack");
      this.dstTrack = requireNonNull(dstTrack, "dstTrack");
    }

    public String getBinVehicle() {
      return binVehicle;
    }

    public String getTrackVehicle() {
      return trackVehicle;
    }

    public int getSrcTrack() {
      return srcTrack;
    }

    public int getDstTrack() {
      return dstTrack;
    }
    
  }
}
