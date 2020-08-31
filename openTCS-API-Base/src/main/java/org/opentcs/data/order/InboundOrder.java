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
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Location;
import static org.opentcs.data.order.TransportOrderHistoryCodes.ORDER_CREATED;

/**
 * 这种订单仅对应于一个料箱，其目的是为了使某个料箱出库或入库.
 * 这种订单一般由客户订单分解而得.
 * @author Henry
 */
public class InboundOrder
    extends TCSObject<InboundOrder>
    implements Serializable,
               Cloneable {
  
  /**
   * 待入库料箱
   */
  private Bin bin;

  private TCSObjectReference<Location> assignedBinStack;
  
  private TCSObjectReference<TransportOrder> attachedTOrder;
  /**
   * This bin order's current state.
   */
  @Nonnull
  private State state = State.AWAIT_DISPATCH;
  /**
   * The point of time at which this bin order was created.
   */
  private final Instant creationTime;
  /**
   * The point of time at which processing of this bin order must be finished.
   */
  private Instant deadline = Instant.ofEpochMilli(Long.MAX_VALUE);
  /**
   * The point of time at which processing of this bin order was finished.
   */
  private Instant finishedTime = Instant.ofEpochMilli(Long.MAX_VALUE);

  public InboundOrder(@Nonnull String name, Bin bin) {
    super(name,
          new HashMap<>(),
          new ObjectHistory().withEntryAppended(new ObjectHistory.Entry(ORDER_CREATED)));
    this.bin = bin;
    this.creationTime = Instant.EPOCH;
    this.deadline = Instant.ofEpochMilli(Long.MAX_VALUE);
  }

  public InboundOrder(String name, 
                  Map<String, String> properties, 
                  ObjectHistory history, 
                  Bin bin, 
                  TCSObjectReference<Location> assignedBinStack,
                  TCSObjectReference<TransportOrder> attachedTOrder,
                  State state, 
                  Instant creationTime, 
                  Instant deadline, 
                  Instant finishedTime) {
    super(name, properties, history);
    this.bin = requireNonNull(bin, "binID");
    this.assignedBinStack = assignedBinStack;
    this.attachedTOrder = attachedTOrder;
    this.state = requireNonNull(state, "state");
    this.creationTime = requireNonNull(creationTime, "creationTime");
    this.deadline = requireNonNull(deadline, "deadline");
    this.finishedTime = requireNonNull(finishedTime, "finishedTime");
  }

  @Override
  public InboundOrder withProperty(String key, String value) {
    return new InboundOrder(getName(),
                        propertiesWith(key, value),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  @Override
  public InboundOrder withProperties(Map<String, String> properties) {
    return new InboundOrder(getName(),
                        properties,
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  @Override
  public InboundOrder withHistoryEntry(ObjectHistory.Entry entry) {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory().withEntryAppended(entry),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  @Override
  public InboundOrder withHistory(ObjectHistory history) {
    return new InboundOrder(getName(),
                        getProperties(),
                        history,
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  @Override
  @SuppressWarnings("deprecation")
  public InboundOrder clone() {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public Bin getBin() {
    return bin;
  }

  public InboundOrder withBin(Bin bin) {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public TCSObjectReference<Location> getAssignedBinStack() {
    return assignedBinStack;
  }

  public InboundOrder withAssignedBinStack(TCSObjectReference<Location> assignedBinStack) {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public TCSObjectReference<TransportOrder> getAttachedTransportOrder() {
    return attachedTOrder;
  }

  public InboundOrder withAttachedTransportOrder(TCSObjectReference<TransportOrder> attachedTOrder) {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
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

  public InboundOrder withState(State state) {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        state == State.FINISHED ? Instant.now() : finishedTime);
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  public InboundOrder withCreationTime(Instant creationTime) {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public Instant getDeadline() {
    return deadline;
  }

  public InboundOrder withDeadline(Instant deadline) {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
                        state,
                        creationTime,
                        deadline,
                        finishedTime);
  }

  public Instant getFinishedTime() {
    return finishedTime;
  }

  public InboundOrder withFinishedTime(Instant finishedTime) {
    return new InboundOrder(getName(),
                        getProperties(),
                        getHistory(),
                        bin,
                        assignedBinStack,
                        attachedTOrder,
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
