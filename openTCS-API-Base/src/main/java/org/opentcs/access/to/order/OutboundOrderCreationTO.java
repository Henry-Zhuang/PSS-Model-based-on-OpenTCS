/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.access.to.order;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import org.opentcs.access.to.CreationTO;
import org.opentcs.data.model.Bin;

/**
 *
 * @author Henry
 */
public class OutboundOrderCreationTO 
    extends CreationTO
    implements Serializable {
  
  private Set<Bin.SKU> requiredSKUs = new HashSet<>();
  
  private Instant deadline = Instant.ofEpochMilli(Long.MAX_VALUE);

  public OutboundOrderCreationTO(String name) {
    super(name);
  }

  public OutboundOrderCreationTO(String name,
                                 Map<String, String> properties,
                                 Set<Bin.SKU> requiredSKUs,
                                 Instant deadline){
    super(name, properties);
    this.requiredSKUs = requireNonNull(requiredSKUs,"requiredSKUs");
    this.deadline = requireNonNull(deadline, "deadline");
  }
  
  @Override
  public OutboundOrderCreationTO withProperty(String key, String value) {
    return new OutboundOrderCreationTO(getName(),
                                      propertiesWith(key, value),
                                      requiredSKUs,
                                      deadline);
  }

  @Override
  public OutboundOrderCreationTO withProperties(Map<String, String> properties) {
    return new OutboundOrderCreationTO(getName(),
                                      properties,
                                      requiredSKUs,
                                      deadline);
  }

  public Set<Bin.SKU> getRequiredSKUs() {
    return requiredSKUs;
  }

  public OutboundOrderCreationTO withRequiredSKUs(Set<Bin.SKU> requiredSKUs) {
    return new OutboundOrderCreationTO(getName(),
                                      getProperties(),
                                      requiredSKUs,
                                      deadline);
  }

  public Instant getDeadline() {
    return deadline;
  }

  public OutboundOrderCreationTO withDeadline(Instant deadline) {
    return new OutboundOrderCreationTO(getName(),
                                      getProperties(),
                                      requiredSKUs,
                                      deadline);
  }
}
