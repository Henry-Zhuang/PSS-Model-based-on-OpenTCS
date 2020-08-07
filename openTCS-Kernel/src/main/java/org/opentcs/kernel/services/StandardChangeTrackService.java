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
import org.opentcs.components.kernel.services.DataBaseService;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.components.kernel.services.TransportOrderService;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.DriveOrder;
import org.opentcs.data.order.OrderConstants;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.data.order.TransportOrderBin;
import org.opentcs.drivers.vehicle.VehicleControllerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private final DataBaseService dataBaseService;
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
  
  @Inject
  public StandardChangeTrackService(TCSObjectService objectService,
                                    TransportOrderService orderService,
                                    ObjectNameProvider orderNameProvider,
                                    DataBaseService dataBaseService,
                                    VehicleControllerPool controllerPool) {
    super(objectService);
    this.orderService = requireNonNull(orderService, "orderService");
    this.objectNameProvider = requireNonNull(orderNameProvider, "orderNameProvider");
    this.dataBaseService = requireNonNull(dataBaseService, "dataBaseService");
    this.controllerPool = requireNonNull(controllerPool, "controllerPool");
  }


  @Override
  public void initTrackList() {
    if(dataBaseService.getLocPosition() == null)
      return;
    
    LOG.info("Initializing change track service");
    
    noVehicleTracks = IntStream.range(1, dataBaseService.getLocPosition().length + 1)
        .boxed().collect(Collectors.toSet());

    Set<Vehicle> vehicles = fetchObjects(Vehicle.class);
    if(vehicles.isEmpty())
      return;
    
    vehicles.stream()
        .filter(veh -> veh.getType().equals(Vehicle.BIN_VEHICLE_TYPE))
        .forEach(PSB -> noVehicleTracks.remove(fetchObject(Point.class,
                                                           PSB.getCurrentPosition()).getRow()));
  }

  @Override
  public void createChangeTrackOrder(String binVehicle) {
    synchronized(this){
      
      if(binVehicle == null){
        LOG.error("Error %s is not existed",binVehicle);
        return;
      }
      
      // 该PSB已被分配了换轨任务，不可再分配新的换轨任务
      for(Map.Entry<String, TrackOrderEntry> entry : trackOrderPool.entrySet()){
        if(binVehicle.equals(entry.getValue().getBinVehicle()))
          return;
      }
      
      TransportOrderBin noVehicleTOB = fetchObjects(TransportOrderBin.class).stream()
          .filter(tOrderBin -> tOrderBin.hasState(TransportOrderBin.State.AWAIT_DISPATCH))
          .filter(tOB -> noVehicleTracks.contains(tOB.getLocationRow()))
          .sorted(Comparator.comparing(TransportOrderBin::getDeadline))
          .findFirst()
          .orElse(null);
      if(noVehicleTOB == null)
        return;
      
      LOG.debug("method entry");
      Vehicle vehicle = fetchObject(Vehicle.class, binVehicle);

      createChangeTrackOrder(vehicle, noVehicleTOB);
    }
  }
  
  private void createChangeTrackOrder(Vehicle binVehicle, TransportOrderBin tOB) {
    Vehicle trackVehicle = 
        fetchObjects(Vehicle.class,this::canBeUtilized).stream()
            .sorted((Vehicle PST1, Vehicle PST2) -> compareDistance(binVehicle, PST1, PST2))
            .findFirst()
            .orElse(null);
         
    if(trackVehicle == null){
      LOG.warn("No change-track vehicles can be utilized at the moment.");
      return;
    }
    
    int srcTrack = fetchObject(Point.class,binVehicle.getCurrentPosition()).getRow();
    int dstTrack = tOB.getLocationRow();
    String dstLocation = tOB.getSourceLocationName();
    
    String orderName = nameFor(OrderConstants.TYPE_CHANGE_TRACK);
    TransportOrderCreationTO psbTO = new TransportOrderCreationTO(orderName+BIN_ORDER_SUFFIX)
        .withDestinations(BinDestinations(trackVehicle, srcTrack, dstTrack, dstLocation))
        .withIntendedVehicleName(binVehicle.getName())
        .withType(OrderConstants.TYPE_CHANGE_TRACK);
    
    TransportOrderCreationTO pstTO = new TransportOrderCreationTO(orderName+TRACK_ORDER_SUFFIX)
        .withDestinations(TrackDestinations(trackVehicle, srcTrack, dstTrack))
        .withIntendedVehicleName(trackVehicle.getName())
        .withType(OrderConstants.TYPE_CHANGE_TRACK);
    
    try{
      orderService.createTransportOrder(psbTO);
      orderService.createTransportOrder(pstTO);
      trackOrderPool.put(orderName, 
                         new TrackOrderEntry(binVehicle.getName(),
                                            trackVehicle.getName(),
                                            srcTrack, 
                                            dstTrack));
      noVehicleTracks.add(srcTrack);
      noVehicleTracks.remove(dstTrack);
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
                        .getRow();
        noVehicleTracks.add(currTrack);
        trackOrderPool.remove(getPrefix(orderRef.getName()));
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
    int srcTrack = entry.srcTrack;
    int srcColumn = fetchObject(Point.class, 
                                fetchObject(Vehicle.class,
                                            entry.trackVehicle).getCurrentPosition())
                    .getColumn();
    return dstPoint.getRow() == srcTrack
        && dstPoint.getColumn() == srcColumn;
  }

  @Override
  public boolean isLeavingFirstTrackPoint(Point srcPoint, String orderName) {
    TrackOrderEntry entry = trackOrderPool.get(getPrefix(orderName));
    if(entry == null){
      LOG.warn("Error track order entry of {} not found when checking isLeavingFirstTrackPoint.",orderName);
      return false;
    }
    int srcTrack = entry.srcTrack;
    int srcColumn = fetchObject(Point.class, 
                                fetchObject(Vehicle.class,
                                            entry.trackVehicle).getCurrentPosition())
                    .getColumn();
    return srcPoint.getRow() == srcTrack
        && srcPoint.getColumn() == srcColumn;
  }

  private String getPrefix(String orderName) {
    return orderName.substring(0, orderName.length()-BIN_ORDER_SUFFIX.length());
  }
  
  private String nameFor(@Nonnull String prefix) {
    return objectNameProvider.getUniqueName(prefix);
  }
  
  private boolean canBeUtilized(Vehicle trackVehicle) {
    return trackVehicle.getType().equals(Vehicle.TRACK_VEHICLE_TYPE)
        && trackVehicle.getIntegrationLevel().equals(Vehicle.IntegrationLevel.TO_BE_UTILIZED);
  }
  
  private int compareDistance(Vehicle binVehicle, Vehicle trackVehicle1, Vehicle trackVehicle2){
    Point point1 = fetchObject(Point.class, trackVehicle1.getCurrentPosition());
    Point point2 = fetchObject(Point.class, trackVehicle2.getCurrentPosition());
    Point sourcePoint = fetchObject(Point.class, binVehicle.getCurrentPosition());
    int distance1 = (int)Math.abs(point1.getPosition().getX()-sourcePoint.getPosition().getX());
    int distance2 = (int)Math.abs(point2.getPosition().getX()-sourcePoint.getPosition().getX());
    if (distance1 == distance2)
      return 0;
    else
      return distance1 > distance2 ? 1 : -1;
  }

  private List<DestinationCreationTO> BinDestinations(Vehicle trackVehicle, int srcTrack, int dstTrack, String dstLocation) {
    List<DestinationCreationTO> result = TrackDestinations(trackVehicle, srcTrack, dstTrack);
    result.add(new DestinationCreationTO(dstLocation, DriveOrder.Destination.OP_NOP));
    return result;
  }

  private List<DestinationCreationTO> TrackDestinations(Vehicle trackVehicle, int srcTrack, int dstTrack) {
    List<DestinationCreationTO> result = new ArrayList<>();
    Point srcTrackPoint;
    Point dstTrackPoint;
    
    int trackColumn = fetchObject(Point.class, trackVehicle.getCurrentPosition()).getColumn();
    List<Point> trackPoints = fetchObjects(Point.class).stream()
        .filter(p -> p.getColumn() == trackColumn)
        .filter(point -> point.getRow() == srcTrack || point.getRow() == dstTrack)
        .collect(Collectors.toList());
    
    if(trackPoints.size()!=2){
      LOG.error("Error trackPoints can't be found when change track from Track {} to Track {}",srcTrack,dstTrack);
      throw new ObjectUnknownException("Track points can't be found when creating change-track order.");
    }
    
    if(trackPoints.get(0).getRow() == srcTrack){
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
