/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.kernel.extensions.servicewebapi.v1.order.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Size;

/**
 * A customer order to be processed by the kernel.
 *
 * @author Henry
 */
public class CustomerOrder {

  @JsonPropertyDescription("The (optional) deadline of the transport order")
  private Instant deadline;

  @JsonPropertyDescription("The skus")
  @JsonProperty(required = true)
  @Valid
  @Size(min = 1)
  private List<Sku> skus = new LinkedList<>();
  
  @JsonPropertyDescription("The transport order's properties")
  @Size(min = 0)
  private List<Property> properties = new LinkedList<>();

  @JsonPropertyDescription("The transport order's dependencies")
  @Size(min = 0)
  private List<String> dependencies = new LinkedList<>();

  /**
   * Creates a new instance.
   */
  public CustomerOrder() {
  }

  public Instant getDeadline() {
    return deadline;
  }

  public void setDeadline(Instant deadline) {
    this.deadline = deadline;
  }

  public List<Sku> getSkus() {
    return skus;
  }

  public void setSkus(List<Sku> skus) {
    this.skus = skus;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public void setProperties(List<Property> properties) {
    this.properties = properties;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<String> dependencies) {
    this.dependencies = dependencies;
  }
}
