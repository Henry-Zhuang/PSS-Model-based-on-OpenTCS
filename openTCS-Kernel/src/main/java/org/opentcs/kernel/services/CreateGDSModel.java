/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import javax.inject.Inject;
import org.opentcs.data.ObjectExistsException;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObject;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.LocationType;
import org.opentcs.data.model.Path;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Triple;
import org.opentcs.data.model.visualization.ModelLayoutElement;
import org.opentcs.data.model.visualization.VisualLayout;
import org.opentcs.kernel.workingset.Model;
import org.opentcs.kernel.workingset.TCSObjectPool;

/**
 *
 * @author Henry
 */
public class CreateGDSModel {
  // 每条PSB轨道之间的间隔，单位是mm
  public static long X_INTERVAL = 700;
  // 同一PSB轨道中每相邻两点的间隔，单位是mm
  public static long Y_INTERVAL = 500;
  
  public static int PSB_TRACK_NUM = 8;
  
  public static int POINT_NUM_PER_TRACK = 50;
  
  public static double X_SCALE = 10;
  
  public static double Y_SCALE = 10;
  
  public static String[] cargoOperations = {"Catch","Drop"};
  /**
   * The system's global object pool.
   */
  private final TCSObjectPool objectPool;
  /**
   * The model facade to the object pool.
   */
  private final Model model;
  
  @Inject
  public CreateGDSModel(TCSObjectPool globalPool, Model model) {
    this.objectPool = Objects.requireNonNull(globalPool);
    this.model = requireNonNull(model, "model");
  }
  
  @SuppressWarnings("deprecation")
  public void createPlantModelObjects()
      throws ObjectExistsException, ObjectUnknownException{
    model.setName("GDS");
    createPoint();
    createPath();
    createLocationType();
    createBinLocation();
    createVisualLayout();
  }
  
  @SuppressWarnings("deprecation")
  private void createPoint() {
    int nameID = 1;
    for(int x = 0;x < PSB_TRACK_NUM;x++){
      for(int y = 0;y < POINT_NUM_PER_TRACK;y++){
        Point newPoint = new Point(String.format("%s-%05d", "Point",nameID++))
            .withPosition(new Triple(x*X_INTERVAL, y*Y_INTERVAL, 0))
            .withType(Point.Type.HALT_POSITION);
        objectPool.addObject(newPoint);
        objectPool.emitObjectEvent(newPoint, null, TCSObjectEvent.Type.OBJECT_CREATED);
      }
    }
  }
  
  @SuppressWarnings("deprecation")
  private void createPath() {
    for(int x=0;x<PSB_TRACK_NUM;x++){
      for(int y=1;y<POINT_NUM_PER_TRACK;y++){
        Point srcPoint = objectPool.getObject(Point.class, String.format("%s-%05d", "Point",x*POINT_NUM_PER_TRACK+y));
        Point destPoint = objectPool.getObject(Point.class, String.format("%s-%05d", "Point",x*POINT_NUM_PER_TRACK+y+1));
        Path newPath = new Path(String.format("%s --- %s", srcPoint.getName(),destPoint.getName()),
                            srcPoint.getReference(),
                            destPoint.getReference())
        .withLength(Y_INTERVAL)
        .withMaxVelocity(5000)
        .withMaxReverseVelocity(5000)
        .withLocked(false);
        
        objectPool.addObject(newPath);

        objectPool.emitObjectEvent(newPath,
                                   null,
                                   TCSObjectEvent.Type.OBJECT_CREATED);
        addPointOutgoingPath(srcPoint.getReference(), newPath.getReference());
      }
    }
    for(int i=0;i<PSB_TRACK_NUM-1;i++){
      Point srcPoint = objectPool.getObject(Point.class, String.format("%s-%05d", "Point",i*POINT_NUM_PER_TRACK+1));
      Point destPoint = objectPool.getObject(Point.class, String.format("%s-%05d", "Point",(i+1)*POINT_NUM_PER_TRACK+1));
      Path newPath = new Path(String.format("%s --- %s", srcPoint.getName(),destPoint.getName()),
                          srcPoint.getReference(),
                          destPoint.getReference())
      .withLength(X_INTERVAL)
      .withMaxVelocity(5000)
      .withMaxReverseVelocity(5000)
      .withLocked(false);

      objectPool.addObject(newPath);

      objectPool.emitObjectEvent(newPath,
                                 null,
                                 TCSObjectEvent.Type.OBJECT_CREATED);
      addPointOutgoingPath(srcPoint.getReference(), newPath.getReference());
    }
    for(int i=1;i<PSB_TRACK_NUM;i++){
      Point srcPoint = objectPool.getObject(Point.class, String.format("%s-%05d", "Point",i*POINT_NUM_PER_TRACK));
      Point destPoint = objectPool.getObject(Point.class, String.format("%s-%05d", "Point",(i+1)*POINT_NUM_PER_TRACK));
      Path newPath = new Path(String.format("%s --- %s", srcPoint.getName(),destPoint.getName()),
                          srcPoint.getReference(),
                          destPoint.getReference())
      .withLength(X_INTERVAL)
      .withMaxVelocity(5000)
      .withMaxReverseVelocity(5000)
      .withLocked(false);

      objectPool.addObject(newPath);

      objectPool.emitObjectEvent(newPath,
                                 null,
                                 TCSObjectEvent.Type.OBJECT_CREATED);
      addPointOutgoingPath(srcPoint.getReference(), newPath.getReference());
    }
  }
  
  @SuppressWarnings("deprecation")
  private void createLocationType() {
    LocationType binType = new LocationType("Bin")
        .withAllowedOperations(Arrays.asList(cargoOperations));
    LocationType inBoundType = new LocationType("InBound")
        .withAllowedOperations(Arrays.asList(cargoOperations))
        .withProperty("tcs:defaultLocationTypeSymbol","LOAD_TRANSFER_GENERIC");
    LocationType outBoundType = new LocationType("OutBound")
        .withAllowedOperations(Arrays.asList(cargoOperations))
        .withProperty("tcs:defaultLocationTypeSymbol", "WORKING_GENERIC");
    objectPool.addObject(binType);
    objectPool.emitObjectEvent(binType,
                               null,
                               TCSObjectEvent.Type.OBJECT_CREATED);
    objectPool.addObject(inBoundType);
    objectPool.emitObjectEvent(inBoundType,
                               null,
                               TCSObjectEvent.Type.OBJECT_CREATED);
    objectPool.addObject(outBoundType);
    objectPool.emitObjectEvent(outBoundType,
                               null,
                               TCSObjectEvent.Type.OBJECT_CREATED);
  }
  
  @SuppressWarnings("deprecation")
  private void createBinLocation() {
    int nameID = 1;
    for(Point point :objectPool.getObjects(Point.class,p -> Integer.parseInt(p.getName().split("-")[1])%POINT_NUM_PER_TRACK==2)){
      createInBoundStation(point,nameID++);
    }
    
    nameID = 1;
    for(Point point :objectPool.getObjects(Point.class,p -> {
        int index = Integer.parseInt(p.getName().split("-")[1])%POINT_NUM_PER_TRACK;
        return index == POINT_NUM_PER_TRACK-3 || index == POINT_NUM_PER_TRACK-1;
      })){
      createOutBoundStation(point,nameID++);
    }
    
    nameID = 1;
    for(Point point :objectPool.getObjects(Point.class,p -> {
        int index = Integer.parseInt(p.getName().split("-")[1])% POINT_NUM_PER_TRACK;
        return index > 4 && index < POINT_NUM_PER_TRACK -6 && index % 3 !=0 && index != 100;
      })){
      createBinStack(point,nameID++);
    }
  }
  
  @SuppressWarnings("deprecation")
  public VisualLayout createVisualLayout()
      throws ObjectUnknownException, ObjectExistsException {
    VisualLayout newLayout = new VisualLayout("VLayout-1")
        .withScaleX(X_SCALE)
        .withScaleY(Y_SCALE);
    for (Location location : objectPool.getObjects(Location.class)) {
      TCSObject<?> object = location;
      ModelLayoutElement mle = new ModelLayoutElement(object.getReference());
      mle.setLayer(0);
      Map<String,String> properties = new HashMap<>();
      properties.put("LABEL_OFFSET_X", "-10");
      properties.put("LABEL_OFFSET_Y", "-20");
      properties.put("POSITION_X", String.valueOf(location.getPosition().getX() + 250));
      properties.put("POSITION_Y", String.valueOf(location.getPosition().getY()));
      mle.setProperties(properties);
      newLayout.getLayoutElements().add(mle);
    }
    for (Point point : objectPool.getObjects(Point.class)) {
      TCSObject<?> object = point;
      ModelLayoutElement mle = new ModelLayoutElement(object.getReference());
      mle.setLayer(0);
      Map<String,String> properties = new HashMap<>();
      properties.put("LABEL_OFFSET_X", "-10");
      properties.put("LABEL_OFFSET_Y", "-20");
      properties.put("POSITION_X", String.valueOf(point.getPosition().getX()));
      properties.put("POSITION_Y", String.valueOf(point.getPosition().getY()));
      mle.setProperties(properties);
      newLayout.getLayoutElements().add(mle);
    }
    objectPool.addObject(newLayout);
    objectPool.emitObjectEvent(newLayout,
                               null,
                               TCSObjectEvent.Type.OBJECT_CREATED);
    // Return the newly created layout.
    return newLayout;
  }
  
  @SuppressWarnings("deprecation")
  private Point addPointOutgoingPath(TCSObjectReference<Point> pointRef,
                                     TCSObjectReference<Path> pathRef)
      throws ObjectUnknownException {
    Point point = objectPool.getObject(Point.class, pointRef);
    Path path = objectPool.getObject(Path.class, pathRef);
    // Check if the point really is the path's source.
    if (!path.getSourcePoint().equals(point.getReference())) {
      throw new IllegalArgumentException("Point is not the path's source.");
    }
    Path previousState = path;
    Set<TCSObjectReference<Path>> outgoingPaths = new HashSet<>(point.getOutgoingPaths());
    outgoingPaths.add(path.getReference());
    point = objectPool.replaceObject(point.withOutgoingPaths(outgoingPaths));
    objectPool.emitObjectEvent(point,
                               previousState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
    return point;
  }

  private void createInBoundStation(Point point, int nameID) {
    LocationType type = objectPool.getObject(LocationType.class, "InBound");
    Location newLocation = new Location(String.format("%s-%05d","InBoundStation",nameID), type.getReference())
        .withPosition(point.getPosition());

    Set<Location.Link> locationLinks = new HashSet<>();
    Location.Link link = new Location.Link(newLocation.getReference(), point.getReference());
    locationLinks.add(link);
    
    newLocation = newLocation.withAttachedLinks(locationLinks);
    
    objectPool.addObject(newLocation);
    objectPool.emitObjectEvent(newLocation,
                               null,
                               TCSObjectEvent.Type.OBJECT_CREATED);

    // Add the location's links to the respective points, too.
    Set<Location.Link> pointLinks = new HashSet<>(point.getAttachedLinks());
    pointLinks.add(link);

    Point previousPointState = point;
    point = objectPool.replaceObject(point.withAttachedLinks(pointLinks));

    objectPool.emitObjectEvent(point,
                               previousPointState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);

  }

  private void createOutBoundStation(Point point, int nameID) {
    LocationType type = objectPool.getObject(LocationType.class, "OutBound");
    Location newLocation = new Location(String.format("%s-%05d","OutBoundStation",nameID), type.getReference())
        .withPosition(point.getPosition());

    Set<Location.Link> locationLinks = new HashSet<>();
    Location.Link link = new Location.Link(newLocation.getReference(), point.getReference());
    locationLinks.add(link);
    
    newLocation = newLocation.withAttachedLinks(locationLinks);
    
    objectPool.addObject(newLocation);
    objectPool.emitObjectEvent(newLocation,
                               null,
                               TCSObjectEvent.Type.OBJECT_CREATED);

    // Add the location's links to the respective points, too.
    Set<Location.Link> pointLinks = new HashSet<>(point.getAttachedLinks());
    pointLinks.add(link);

    Point previousPointState = point;
    point = objectPool.replaceObject(point.withAttachedLinks(pointLinks));

    objectPool.emitObjectEvent(point,
                               previousPointState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
  }

  private void createBinStack(Point point, int nameID) {
    LocationType type = objectPool.getObject(LocationType.class, "Bin");
    Location newLocation = new Location(String.format("%s-%05d","BinStack",nameID), type.getReference())
        .withPosition(point.getPosition());

    Set<Location.Link> locationLinks = new HashSet<>();
    Location.Link link = new Location.Link(newLocation.getReference(), point.getReference());
    locationLinks.add(link);
    
    newLocation = newLocation.withAttachedLinks(locationLinks);
    
    objectPool.addObject(newLocation);
    objectPool.emitObjectEvent(newLocation,
                               null,
                               TCSObjectEvent.Type.OBJECT_CREATED);

    // Add the location's links to the respective points, too.
    Set<Location.Link> pointLinks = new HashSet<>(point.getAttachedLinks());
    pointLinks.add(link);

    Point previousPointState = point;
    point = objectPool.replaceObject(point.withAttachedLinks(pointLinks));

    objectPool.emitObjectEvent(point,
                               previousPointState,
                               TCSObjectEvent.Type.OBJECT_MODIFIED);
  }
}
