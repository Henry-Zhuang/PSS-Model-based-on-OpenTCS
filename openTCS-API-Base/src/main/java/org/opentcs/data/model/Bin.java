/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentcs.data.ObjectHistory;
import org.opentcs.data.TCSObject;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.order.BinOrder;

/**
 * A bin stored in the location, containing SKUs.
 * @author Henry
 */
public class Bin 
  extends TCSResource<Bin>
    implements Serializable,
               Cloneable {
  /**
   * The quantity separator which separating the skuID with the quantity 
   * when converting a SKU to String.
   */
  public static final String QUANTITY_SEPARATOR = ":";
  /**
   * The SKU separator when converting a set of SKUs to String.
   */
  public static final String SKU_SEPARATOR = " ; ";
  /**
   * A refercence to a {@link Location} where the bin is located.
   * <p>It can be {@code null} if the bin is transported by a {@link Vehicle}.</p>
   */
  private TCSObjectReference<Location> attachedLocation;
  /**
   * A refercence to a {@link Vehicle} which the bin is transported by.
   * <p>It can be {@code null} if the bin is located in a {@link Location}.</p>
   */
  private TCSObjectReference<Vehicle> attachedVehicle;
  /**
   * A refercence to a {@link BinOrder} which the bin is assigned to.
   */
  private TCSObjectReference<BinOrder> assignedBinOrder;
  /**
   * The source location's row.
   */
  private int psbTrack;
  /**
   * The source location's column.
   */
  private int pstTrack;
  /**
   * The bin's position in the location's bin stack.
   */
  private int binPosition;
  /**
   * A set of SKUs which are stored in the bin.
   */
  private Set<SKU> SKUs = new HashSet<>();
  /**
   * The bin's state.
   */
  private State state;
  /**
   * Whether the bin is locked or not.
   */
  private volatile boolean locked;

  public Bin(String binID){
    super(binID);
    this.locked = false;
  }
  
  public Bin(String binID, 
             Map<String, String> properties, 
             ObjectHistory history, 
             TCSObjectReference<Location> attachedLocation,
             TCSObjectReference<Vehicle> attachedVehicle,
             TCSObjectReference<BinOrder> assignedBinOrder,
             int locationRow, 
             int locationColumn,
             int binPosition,
             Set<SKU> SKUs,
             State state,
             boolean locked) {
    super(binID, properties, history);
    this.attachedLocation = attachedLocation;
    this.attachedVehicle = attachedVehicle;
    this.assignedBinOrder = assignedBinOrder;
    this.psbTrack = locationRow;
    this.pstTrack = locationColumn;
    this.binPosition = binPosition;
    this.SKUs = requireNonNull(SKUs,"SKUs");
    this.state = state;
    this.locked = requireNonNull(locked, "locked");
  }

  @Override
  public TCSObject<Bin> withProperty(String key, String value) {
    return new Bin(getName(),
                  propertiesWith(key, value), 
                  getHistory(),
                  attachedLocation,
                  attachedVehicle, assignedBinOrder,
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  @Override
  public TCSObject<Bin> withProperties(Map<String, String> properties) {
    return new Bin(getName(),
                  properties, 
                  getHistory(),
                  attachedLocation,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  @Override
  public TCSObject<Bin> withHistoryEntry(ObjectHistory.Entry entry) {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory().withEntryAppended(entry),
                  attachedLocation,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  @Override
  public TCSObject<Bin> withHistory(ObjectHistory history) {
    return new Bin(getName(),
                  getProperties(), 
                  history,
                  attachedLocation,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  public TCSObjectReference<Location> getAttachedLocation() {
    return attachedLocation;
  }

  public Bin withAttachedLocation(TCSObjectReference<Location> attachedLocation) {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory(),
                  attachedLocation,
                  null, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  public TCSObjectReference<Vehicle> getAttachedVehicle() {
    return attachedVehicle;
  }

  public Bin withAttachedVehicle(TCSObjectReference<Vehicle> attachedVehicle) {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory(),
                  null,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  public TCSObjectReference<BinOrder> getAssignedBinOrder() {
    return assignedBinOrder;
  }

  public Bin withAssignedBinOrder(TCSObjectReference<BinOrder> assignedBinOrder) {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory(),
                  null,
                  attachedVehicle, 
                  assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }
  
  public int getPsbTrack() {
    return psbTrack;
  }

  public Bin withPsbTrack(int psbTrack) {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory(),
                  attachedLocation,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  public int getPstTrack() {
    return pstTrack;
  }

  public Bin withPstTrack(int pstTrack) {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory(),
                  attachedLocation,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  public int getBinPosition() {
    return binPosition;
  }

  public Bin withBinPosition(int binPosition) {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory(),
                  attachedLocation,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }

  public Set<SKU> getSKUs() {
    return SKUs;
  }

  public Bin withSKUs(Set<SKU> SKUs) {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory(),
                  attachedLocation,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }
  
  public SKU getSKU(String skuID){
    return SKUs.stream().filter(sku -> sku.getSkuID().equals(skuID)).findAny().orElse(null);
  }
  
  public String getAllSKUString(){
    return SKUs.stream().map(SKU -> SKU.toString())
                                .collect(Collectors.joining(SKU_SEPARATOR));
  }
    
  public Bin withAllSKUString(String skuString){
    Set<SKU> Skus = Arrays.asList(skuString.split(SKU_SEPARATOR))
                    .stream().filter(sku -> !sku.isEmpty())
                    .map(sku -> {
                      String[] tmpSku = sku.split(org.opentcs.data.model.Bin.QUANTITY_SEPARATOR);
                      return new SKU(tmpSku[0],Integer.parseInt(tmpSku[1]));
                        })
                    .collect(Collectors.toSet());
    return this.withSKUs(Skus);
  }
  
  public int getQuantity(String skuID){
    for(SKU sku:SKUs){
      if(sku.getSkuID().equals(skuID))
        return sku.getQuantity();
    }
    return 0;
  }

  public State getState() {
    return state;
  }

  public Bin withState(State state) {
    this.state = state;
    return this;
  }
  
  /**
  * Check if the bin is locked.
  * @return {@code true} if, and only if the bin is locked
  */
  public boolean isLocked() {
    return locked;
  }
  
  public Bin lock(){
    locked = true;
    return this;
  }
  public Bin unlock(){
    locked = false;
    return this;
  }
  
  @SuppressWarnings("deprecation")
  @Override
  public Bin clone() {
    return new Bin(getName(),
                  getProperties(), 
                  getHistory(),
                  attachedLocation,
                  attachedVehicle, assignedBinOrder, 
                  psbTrack, 
                  pstTrack, 
                  binPosition, 
                  SKUs, state,
                  locked);
  }
  
  public List<String> toList(){
    if(attachedLocation != null){
      String[] dataStr = {getName(), 
                        SKUsToString(SKUs), 
                        attachedLocation.getName(), 
                        String.valueOf(psbTrack), 
                        String.valueOf(pstTrack),
                        String.valueOf(binPosition), 
                        String.valueOf(locked)};
    List<String> dataList = Arrays.asList(dataStr);
    return dataList;
    }
    else{
      String[] dataStr = {getName(), 
                        SKUsToString(SKUs), 
                        attachedVehicle.getName(), 
                        String.valueOf(psbTrack), 
                        String.valueOf(pstTrack),
                        String.valueOf(binPosition), 
                        String.valueOf(locked)};
    List<String> dataList = Arrays.asList(dataStr);
    return dataList;
    }
  }
  
  private String SKUsToString(Set<SKU> SKUs){
    return new ArrayList<>(SKUs).stream().map(SKU -> SKU.toString())
                                  .collect(Collectors.joining(Bin.SKU_SEPARATOR));
  }
  
  public static class SKU
      implements Serializable,
                 Cloneable {
    private final String skuID;
    private final int quantity;
    
    public SKU(){
      skuID = "";
      quantity = 0;
    }
    
    public SKU(String skuID, int quantity){
      this.skuID = requireNonNull(skuID,"skuID");
      this.quantity = requireNonNull(quantity,"quantity");
    }
    
    public String getSkuID(){
      return skuID;
    }
    
    public int getQuantity(){
      return quantity;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public SKU clone(){
      return new SKU(skuID, quantity);
    }
    
    @Override
    public String toString(){
      return skuID + QUANTITY_SEPARATOR + quantity;
    }
    
    @Override
    public boolean equals(Object obj) {
      if(obj instanceof SKU){
        SKU tmpObj = (SKU) obj;
        return skuID.equals(tmpObj.getSkuID());
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return skuID.hashCode();
    }
  }
  
  public static enum State{
    /**
     * A state indicates that the bin is being transported by a bin vehicle(PSB) 
     * or an outbound conveyor.
     */
    Transporting,
    /**
     * A state indicates that the bin is still (stored in a {@link Location}).
     */
    Still,
    /**
     * A state indicates that the bin has been picked.
     */
    Picked,
    /**
     * A state indicates that the bin is returning to the repository.
     */
    Returning
  }
}
