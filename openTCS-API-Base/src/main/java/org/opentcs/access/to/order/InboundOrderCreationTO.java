/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.access.to.order;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import javax.annotation.Nonnull;
import org.opentcs.access.to.CreationTO;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Location;

/**
 *
 * @author Henry
 */
public class InboundOrderCreationTO 
    extends CreationTO
    implements Serializable {
  
  /**
   * The new bin.
   */
  private final Bin bin ;
  
  /**
   * The point of time at which execution of the inbound order is supposed to be finished.
   */
  @Nonnull
  private Instant deadline = Instant.ofEpochMilli(Long.MAX_VALUE);
  
  /**
   * The bin stack which this new bin is assigned to.
   */
  private TCSObjectReference<Location> assignedBinStack;

  /**
   * Creates a new instance.
   *
   * @param name The name of this inbound order.
   * @param bin The new bin.
   */
  public InboundOrderCreationTO(@Nonnull String name,
                            @Nonnull Bin bin) {
    super(name);
    this.bin = requireNonNull(bin, "bin");
  }

  private InboundOrderCreationTO(@Nonnull String name, 
                                 @Nonnull Map<String, String> properties, 
                                 @Nonnull Bin bin, 
                                 @Nonnull Instant deadline, 
                                 TCSObjectReference<Location> assignedBinStack) {
    super(name, properties);
    this.bin = requireNonNull(bin, "bin");
    this.deadline = requireNonNull(deadline, "deadline");
    this.assignedBinStack = assignedBinStack;
  }
  
  /**
   * Creates a copy of this object with the given properties.
   *
   * @param properties The new properties.
   * @return A copy of this object, differing in the given value.
   */
  @Override
  public InboundOrderCreationTO withProperties(@Nonnull Map<String, String> properties) {
    return new InboundOrderCreationTO(getName(),
                                        properties, bin, 
                                        deadline,  assignedBinStack);
  }

  /**
   * Creates a copy of this object and adds the given property.
   * If value == null, then the key-value pair is removed from the properties.
   *
   * @param key the key.
   * @param value the value
   * @return A copy of this object that either
   * includes the given entry in it's current properties, if value != null or
   * excludes the entry otherwise.
   */
  @Override
  public InboundOrderCreationTO withProperty(@Nonnull String key, @Nonnull String value) {
    return new InboundOrderCreationTO(getName(),
                                        propertiesWith(key, value), bin, 
                                        deadline,  assignedBinStack);
  }

  public Bin getBin() {
    return bin;
  }
  
  public Instant getDeadline() {
    return deadline;
  }
  
  /**
   * Creates a copy of this object with the given
   * point of time at which execution of the transport order is supposed to be finished.
   *
   * @param deadline The deadline.
   * @return A copy of this object, differing in the given deadline.
   */
  public InboundOrderCreationTO withDeadline(@Nonnull Instant deadline) {
    return new InboundOrderCreationTO(getName(),
                                        getModifiableProperties(), bin, 
                                        deadline,  assignedBinStack);
  }

  public TCSObjectReference<Location> getAssignedBinStack() {
    return assignedBinStack;
  }

  public InboundOrderCreationTO withAssignedBinStack(TCSObjectReference<Location> assignedBinStack) {
    return new InboundOrderCreationTO(getName(),
                                        getModifiableProperties(), bin, 
                                        deadline,  assignedBinStack);
  }
}