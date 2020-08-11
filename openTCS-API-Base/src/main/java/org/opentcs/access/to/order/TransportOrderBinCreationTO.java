/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.access.to.order;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentcs.access.to.CreationTO;
import org.opentcs.data.model.Bin;
import org.opentcs.data.order.OrderBinConstants;

/**
 *
 * @author Henry
 */
public class TransportOrderBinCreationTO 
    extends CreationTO
    implements Serializable {
  
  @Nonnull
  private String binID = "";
  
  @Nonnull
  private Map<String, Integer> requiredSku = new HashMap<>();
  
  @Nullable
  private int quantityPerBin;
  
  @Nonnull
  private String customerOrderName = "";
  /**
   * The type of the transport binID order.
   */
  @Nonnull
  private String type = OrderBinConstants.TYPE_NONE;
  /**
   * The point of time at which execution of the transport order is supposed to be finished.
   */
  @Nonnull
  private Instant deadline = Instant.ofEpochMilli(Long.MAX_VALUE);

  /**
   * Creates a new instance.
   *
   * @param name The name of this transport order binID.
   * @param binID The specific binID of this transport order bin.
   * @param type The destinations that need to be travelled to.
   */
  public TransportOrderBinCreationTO(@Nonnull String name,
                                        @Nonnull String binID, 
                                        @Nonnull String type) {
    super(name);
    this.binID = requireNonNull(binID, "binID");
    this.type = requireNonNull(type, "type");
  }

  private TransportOrderBinCreationTO(@Nonnull String name, 
                                        @Nonnull String binID, 
                                        @Nonnull Map<String, String> properties, 
                                        @Nonnull String type, 
                                        @Nonnull Map<String,Integer> requiredSku, 
                                        @Nullable int quantityPerBin, 
                                        @Nonnull String customerOrderName, 
                                        @Nonnull Instant deadline) {
    super(name, properties);
    this.binID = requireNonNull(binID, "binID");
    this.type = requireNonNull(type, "type");
    this.requiredSku = requireNonNull(requiredSku, "requiredSku");
    this.quantityPerBin = quantityPerBin;
    this.customerOrderName = requireNonNull(customerOrderName, "customerOrderName");
    this.deadline = requireNonNull(deadline, "deadline");
  }
  
  /**
   * Creates a copy of this object with the given properties.
   *
   * @param properties The new properties.
   * @return A copy of this object, differing in the given value.
   */
  @Override
  public TransportOrderBinCreationTO withProperties(@Nonnull Map<String, String> properties) {
    return new TransportOrderBinCreationTO(getName(), 
                                        binID,
                                        properties,
                                        type, 
                                        requiredSku, 
                                        quantityPerBin, 
                                        customerOrderName, 
                                        deadline);
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
  public TransportOrderBinCreationTO withProperty(@Nonnull String key, @Nonnull String value) {
    return new TransportOrderBinCreationTO(getName(), 
                                        binID,
                                        propertiesWith(key, value),
                                        type, 
                                        requiredSku, 
                                        quantityPerBin, 
                                        customerOrderName, 
                                        deadline);
  }

  public String getBinID() {
    return binID;
  }
 
  public TransportOrderBinCreationTO withBinID(@Nonnull String binID) {
    return new TransportOrderBinCreationTO(getName(), 
                                        binID,
                                        getModifiableProperties(),
                                        type, 
                                        requiredSku, 
                                        quantityPerBin, 
                                        customerOrderName, 
                                        deadline);
  }
  
  /**
   * Returns the (optional) type of the transport order.
   *
   * @return The (optional) type of the transport order.
   */
  @Nonnull
  public String getType() {
    return type;
  }

  /**
   * Creates a copy of this object with the given (optional) type of the transport order.
   *
   * @param type The type.
   * @return A copy of this object, differing in the given type.
   */
  public TransportOrderBinCreationTO withType(@Nonnull String type) {
    return new TransportOrderBinCreationTO(getName(), 
                                        binID,
                                        getModifiableProperties(),
                                        type, 
                                        requiredSku, 
                                        quantityPerBin, 
                                        customerOrderName, 
                                        deadline);
  }

  public Map<String,Integer> getRequiredSku() {
    return requiredSku;
  }

  public TransportOrderBinCreationTO withRequiredSku(Map<String,Integer> requiredSku) {
    return new TransportOrderBinCreationTO(getName(), 
                                        binID,
                                        getModifiableProperties(),
                                        type, 
                                        requiredSku, 
                                        quantityPerBin, 
                                        customerOrderName, 
                                        deadline);
  }

  public int getQuantityPerBin() {
    return quantityPerBin;
  }

  public TransportOrderBinCreationTO withQuantityPerBin(int quantityPerBin) {
    return new TransportOrderBinCreationTO(getName(), 
                                        binID,
                                        getModifiableProperties(),
                                        type, 
                                        requiredSku, 
                                        quantityPerBin, 
                                        customerOrderName, 
                                        deadline);
  }

  public String getCustomerOrderName() {
    return customerOrderName;
  }

  public TransportOrderBinCreationTO withCustomerOrderName(String customerOrderName) {
    return new TransportOrderBinCreationTO(getName(), 
                                        binID,
                                        getModifiableProperties(),
                                        type, 
                                        requiredSku, 
                                        quantityPerBin, 
                                        customerOrderName, 
                                        deadline);
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
  public TransportOrderBinCreationTO withDeadline(@Nonnull Instant deadline) {
    return new TransportOrderBinCreationTO(getName(), 
                                        binID,
                                        getModifiableProperties(),
                                        type, 
                                        requiredSku, 
                                        quantityPerBin, 
                                        customerOrderName, 
                                        deadline);
  }
}