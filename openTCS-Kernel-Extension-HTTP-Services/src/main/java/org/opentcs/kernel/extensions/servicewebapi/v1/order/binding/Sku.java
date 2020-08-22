/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.kernel.extensions.servicewebapi.v1.order.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.LinkedList;
import java.util.List;
import javax.validation.constraints.Size;

/**
 * A sku of a inbound or outbound {@link TransportWithSku}.
 *
 * @author Henry
 */
public class Sku {

  @JsonProperty(required = true)
  @JsonPropertyDescription("The given skuID (inbound or outbound)")
  private String skuID;

  @JsonPropertyDescription("The (optional) quantity of the given SKU (inbound or outbound)")
  private double quantity;

  @JsonPropertyDescription("The (optional) quantity of the inbound SKU per bin")
  private int quantityPerBin;
  
  @JsonPropertyDescription("The drive order's properties")
  @Size(min = 0)
  private List<Property> properties = new LinkedList<>();

  /**
   * Creates a new instance.
   */
  public Sku() {
  }

  /**
   * Returns the sku ID.
   *
   * @return The sku ID
   */
  public String getSkuID() {
    return skuID;
  }

  /**
   * Sets the sku ID.
   *
   * @param skuID The new ID
   */
  public void setSkuID(String skuID) {
    this.skuID = skuID;
  }

  /**
   * Returns the sku quantity.
   *
   * @return The quantity
   */
  public double getQuantity() {
    return quantity;
  }

  /**
   * Sets the sku quantity.
   *
   * @param quantity The new operation
   */
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  /**
   * Returns the sku quantity per bin.
   *
   * @return The sku quantity per bin
   */
  public int getQuantityPerBin() {
    return quantityPerBin;
  }

  /**
   * Sets the sku quantity per bin.
   *
   * @param quantityPerBin The sku quantity per bin
   */
  public void setQuantityPerBin(int quantityPerBin) {
    this.quantityPerBin = quantityPerBin;
  }
  
  /**
   * Returns the sku's properties.
   *
   * @return The sku's properties.
   */
  public List<Property> getProperties() {
    return properties;
  }

  /**
   * Sets the sku's properties.
   *
   * @param properties The new sku's properties.
   */
  public void setProperties(List<Property> properties) {
    this.properties = properties;
  }
}
