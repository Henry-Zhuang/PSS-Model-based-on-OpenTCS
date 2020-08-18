/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.data.order;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import javax.annotation.Nonnull;
import org.opentcs.data.ObjectHistory;
import org.opentcs.data.TCSObject;
import org.opentcs.data.TCSObjectReference;
import static org.opentcs.data.order.TransportOrderHistoryCodes.ORDER_CREATED;

/**
 * 这种订单仅对应于一个料箱，其目的是为了使某个料箱出库或入库.
 * 这种订单一般由客户订单分解而得.
 * @author Henry
 */
public class BinOrder
    extends TCSObject<BinOrder>
    implements Serializable,
               Cloneable {

  /**
   * The type of this transport bin order.
   */
  @Nonnull
  private String type = OrderBinConstants.TYPE_NONE;
  
  private String binID = "";

  @Nonnull
  private Map<String, Integer> requiredSku = new HashMap<>();
  @Nonnull
  private String customerOrderName = "";

  private TCSObjectReference<TransportOrder> attachedTransportOrder;
  /**
   * This transport bin order's current state.
   */
  @Nonnull
  private State state = State.AWAIT_DISPATCH;
  /**
   * The point of time at which this transport order was created.
   */
  private final Instant creationTime;
  /**
   * The point of time at which processing of this transport order must be finished.
   */
  private Instant deadline = Instant.ofEpochMilli(Long.MAX_VALUE);
  /**
   * The point of time at which processing of this transport order was finished.
   */
  private Instant finishedTime = Instant.ofEpochMilli(Long.MAX_VALUE);

  public BinOrder(String name, String type) {
    super(name,
          new HashMap<>(),
          new ObjectHistory().withEntryAppended(new ObjectHistory.Entry(ORDER_CREATED)));
    this.type = requireNonNull(type, "type");
    this.creationTime = Instant.EPOCH;
    this.deadline = Instant.ofEpochMilli(Long.MAX_VALUE);
  }

  public BinOrder(String name, 
                  Map<String, String> properties, 
                  ObjectHistory history, 
                  String type, 
                  String binID, 
                  Map<String, Integer> requiredSku, 
                  String customerOrderName, 
                  TCSObjectReference<TransportOrder> attachedTransportOrder, 
                  State state, 
                  Instant creationTime, 
                  Instant deadline, 
                  Instant finishedTime) {
    super(name, properties, history);
    this.type = requireNonNull(type, "type");
    this.binID = requireNonNull(binID, "binID");
    this.requiredSku = requireNonNull(requiredSku, "requiredSku");
    this.customerOrderName = requireNonNull(customerOrderName, "customerOrderName");
    this.attachedTransportOrder = attachedTransportOrder;
    this.state = requireNonNull(state, "state");
    this.creationTime = requireNonNull(creationTime, "creationTime");
    this.deadline = requireNonNull(deadline, "deadline");
    this.finishedTime = requireNonNull(finishedTime, "finishedTime");
  }

  @Override
  public BinOrder withProperty(String key, String value) {
    return new BinOrder(getName(),
                        propertiesWith(key, value),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  @Override
  public BinOrder withProperties(Map<String, String> properties) {
    return new BinOrder(getName(),
                        properties,
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  @Override
  public BinOrder withHistoryEntry(ObjectHistory.Entry entry) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory().withEntryAppended(entry),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  @Override
  public BinOrder withHistory(ObjectHistory history) {
    return new BinOrder(getName(),
                        getProperties(),
                        history,
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  @Override
  @SuppressWarnings("deprecation")
  public BinOrder clone() {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public String getType() {
    return type;
  }

  public boolean hasType(String otherType) {
    requireNonNull(otherType, "otherType");
    return this.type.equals(otherType);
  }

  public BinOrder withType(String type) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public String getBinID() {
    return binID;
  }

  public BinOrder withBinID(String binID) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public Map<String, Integer> getRequiredSku() {
    return requiredSku;
  }

  public BinOrder withRequiredSku(Map<String, Integer> requiredSku) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public String getCustomerOrderName() {
    return customerOrderName;
  }

  public BinOrder withCustomerOrderName(String customerOrderName) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public TCSObjectReference<TransportOrder> getAttachedTransportOrder() {
    return attachedTransportOrder;
  }

  public BinOrder withAttachedTransportOrder(TCSObjectReference<TransportOrder> attachedTransportOrder) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public State getState() {
    return state;
  }

  public boolean hasState(State otherState) {
    requireNonNull(otherState, "otherState");
    return this.state.equals(otherState);
  }

  public BinOrder withState(State state) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        state == State.FINISHED ? Instant.now() : finishedTime);
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  public BinOrder withCreationTime(Instant creationTime) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public Instant getDeadline() {
    return deadline;
  }

  public BinOrder withDeadline(Instant deadline) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public Instant getFinishedTime() {
    return finishedTime;
  }

  public BinOrder withFinishedTime(Instant finishedTime) {
    return new BinOrder(getName(),
                        getProperties(),
                        getHistory(),
                        type,
                        binID,
                        requiredSku,
                        customerOrderName,
                        attachedTransportOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public enum State {
    AWAIT_DISPATCH,
    DISPATCHED,
    FINISHED,
    FAILED;
    
    public boolean isFinalState() {
      return this.equals(FINISHED)
          || this.equals(FAILED);
    }
  }
}
