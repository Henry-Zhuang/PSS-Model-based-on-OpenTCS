/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.data.order;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentcs.data.ObjectHistory;
import org.opentcs.data.TCSObject;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Bin.SKU;
import static org.opentcs.data.order.TransportOrderHistoryCodes.ORDER_CREATED;

/**
 *
 * @author Henry
 */
public class OutboundOrder
    extends TCSObject<OutboundOrder>
    implements Serializable{
  
  /**
   * 该订单中SKU的总需求.
   */
  private Set<SKU> requiredSKUs = new HashSet<>();
  /**
   * 该订单中未预订过的SKU集合.
   */
  private Set<SKU> leftSKUs = new HashSet<>();
  /**
   * 该订单的货物需求由哪些料箱来满足.
   */
  private Set<TCSObjectReference<Bin>> assignedBins = new HashSet<>();
  /**
   * 该订单中已预订的所有SKU量.
   */
  private double reservedAmount = 0;
  /**
   * 该订单中已分拣的所有SKU量.
   */
  private double pickedAmount = 0;
  /**
   * 该订单要求的总SKU量.
   */
  private double totalAmount = 0;
  /**
   * 该出库订单的状态.
   */
  private State state = State.WAITING;
  /**
   * 该订单的创建时间.
   */
  private final Instant creationTime;
  /**
   * 该订单的DDL.
   */
  private Instant deadline = Instant.ofEpochMilli(Long.MAX_VALUE);
  /**
   * 该订单的最终完成时间.
   */
  private Instant finishedTime = Instant.ofEpochMilli(Long.MAX_VALUE);

  public OutboundOrder(String name) {
    super(name,
          new HashMap<>(),
          new ObjectHistory().withEntryAppended(new ObjectHistory.Entry(ORDER_CREATED)));
    this.creationTime = Instant.now();
    this.deadline = Instant.ofEpochMilli(Long.MAX_VALUE);
  }

  public OutboundOrder(String name,
                        Map<String, String> properties, 
                        ObjectHistory history,
                        Set<SKU> requiredSKUs,
                        Set<SKU> leftSKUs,
                        Set<TCSObjectReference<Bin>> assignedBins,
                        double reservedAmount,
                        double pickedAmount,
                        double totalAmount,
                        State state,
                        Instant creationTime, 
                        Instant deadline,
                        Instant finishedTime){ 
    super(name,properties,history);
    this.requiredSKUs = requireNonNull(requiredSKUs,"requiredSKUs");
    this.leftSKUs = requireNonNull(leftSKUs,"notAssignedSKUs");
    this.assignedBins = requireNonNull(assignedBins,"assignedBins");
    this.reservedAmount = reservedAmount;
    this.pickedAmount = pickedAmount;
    this.totalAmount = totalAmount;
    this.state = requireNonNull(state,"state");
    this.creationTime = requireNonNull(creationTime, "creationTime");
    this.deadline = requireNonNull(deadline, "deadline");
    this.finishedTime = requireNonNull(finishedTime, "finishedTime");
  }
  
  @Override
  public OutboundOrder withProperty(String key, String value) {
    return new OutboundOrder(getName(),
                            propertiesWith(key, value), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  @Override
  public OutboundOrder withProperties(Map<String, String> properties) {
    return new OutboundOrder(getName(),
                            properties, 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  @Override
  public OutboundOrder withHistoryEntry(ObjectHistory.Entry entry) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory().withEntryAppended(entry),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  @Override
  public OutboundOrder withHistory(ObjectHistory history) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            history,
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  public Set<SKU> getRequiredSKUs() {
    return requiredSKUs;
  }

  public OutboundOrder withRequiredSKUs(Set<SKU> requiredSKUs) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  public Set<SKU> getLeftSKUs() {
    return leftSKUs;
  }
  
  public OutboundOrder afterReservation(Set<SKU> reservedSKUs){
    // 未预订过的SKU需求表
    Map<String, Double> leftSkuMap = leftSKUs.stream()
        .collect(Collectors.toMap(SKU::getSkuID,SKU::getQuantity));
    
    // 根据预订集更新未预定过的SKU需求表
    reservedSKUs.forEach(sku -> {
      Double leftQuantity = leftSkuMap.get(sku.getSkuID()) - sku.getQuantity();
      leftSkuMap.put(sku.getSkuID(), leftQuantity);
    });
    
    // 预订过后，将剩下的SKU需求表封装成SKU集合，同时统计剩下的SKU总需求量
    Double newLeftAmount = 0.0;
    Set<SKU> newLeftSKUs = new HashSet<>();
    for(Map.Entry<String,Double> skuEntry : leftSkuMap.entrySet()){
      if(skuEntry.getValue() > 0){
        newLeftSKUs.add(new SKU(skuEntry.getKey(),skuEntry.getValue()));
        newLeftAmount += skuEntry.getValue();
      }
    }
    // 更新该出库订单剩下的SKU需求集合
    // 同时根据剩下的SKU总需求量，计算出已预订的SKU量
    this.leftSKUs = newLeftSKUs;
    this.reservedAmount = this.totalAmount - newLeftAmount;
    return this;
  }
  
  public OutboundOrder afterPicking(Set<SKU> pickedSKUs){
    Double skuAmount = pickedSKUs.stream().map(SKU::getQuantity).reduce(Double::sum).orElse(0.0);
    this.pickedAmount += skuAmount;
    return this;
  }
  
  public Set<TCSObjectReference<Bin>> getAssignedBins() {
    return assignedBins;
  }

  public OutboundOrder withAssignedBins(Set<TCSObjectReference<Bin>> assignedBins) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  public double getReservedAmount() {
    return reservedAmount;
  }

  public OutboundOrder withreservedAmount(double reservedAmount) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  public double getPickedAmount() {
    return pickedAmount;
  }

  public OutboundOrder withPickedAmount(double pickedAmount) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  public OutboundOrder updateTotalAmount(){
    totalAmount = requiredSKUs.stream().map(SKU::getQuantity).reduce(Double::sum).orElse(0.0);
    return this;
  }
  
  public double getTotalAmount() {
    return totalAmount;
  }

  public OutboundOrder withTotalAmount(double totalAmount) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }
  
  public Double getReservedCompletion(){
    // 获取该出库订单的预订完成进度
    return reservedAmount / totalAmount;
  }
  
  public Double getPickedCompletion(){
    // 获取该出库订单的拣选完成进度
    return pickedAmount / totalAmount;
  }

  public State getState() {
    return state;
  }
  
  public boolean hasState(State state){
    return this.state == state;
  }

  public OutboundOrder withState(State state) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  public Instant getCreationTime() {
    return creationTime;
  }
  
  public Instant getDeadline() {
    return deadline;
  }

  public OutboundOrder withDeadline(Instant deadline) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }

  public Instant getFinishedTime() {
    return finishedTime;
  }

  public OutboundOrder withFinishedTime(Instant finishedTime) {
    return new OutboundOrder(getName(),
                            getProperties(), 
                            getHistory(),
                            requiredSKUs, leftSKUs,
                            assignedBins,
                            reservedAmount,
                            pickedAmount,
                            totalAmount,
                            state,
                            creationTime, 
                            deadline,
                            finishedTime);
  }
  
  public enum State {
    WAITING,
    WORKING,
    FINISHED,
    FAILED;
    
    public boolean isFinalState() {
      return this.equals(FINISHED)
          || this.equals(FAILED);
    }
  }
}
