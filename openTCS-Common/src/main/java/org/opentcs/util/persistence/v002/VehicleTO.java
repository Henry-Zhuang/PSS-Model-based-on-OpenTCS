/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.util.persistence.v002;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static java.util.Objects.requireNonNull;
import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import org.opentcs.data.model.Bin;
import org.opentcs.data.model.Location;

import org.opentcs.util.persistence.v002.LocationTO.BinTO;
/**
 *
 * @author Martin Grzenia (Fraunhofer IML)
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(
    propOrder = {"name", "id", "length", "energyLevelCritical", "energyLevelGood",
                 "energyLevelFullyRecharged", "energyLevelSufficientlyRecharged",
                 "maxVelocity", "maxReverseVelocity",
                 "properties","bin"})
public class VehicleTO
    extends PlantModelElementTO {

  //max velocity in mm/s.
  private int maxVelocity;
  //max rev velocity in mm/s.
  private int maxReverseVelocity;
  private String type = "";
  private Long length = 0L;
  private Long energyLevelCritical = 0L;
  private Long energyLevelGood = 0L;
  private Long energyLevelFullyRecharged = 0L;
  private Long energyLevelSufficientlyRecharged = 0L;
  
  ////////////////////////////////////////modified by Henry
  private BinTO bin = new BinTO();
  
  @XmlElement(name = "bin", required = false)
  public BinTO getBin() {
    return bin;
  }

  public VehicleTO setBin(@Nonnull BinTO bin) {
    requireNonNull(bin, "bin");
    this.bin = bin;
    return this;
  }
  
  public Bin getLocationBin(){
    List<Bin.SKU> locSKUs = new ArrayList<>();
    for(BinTO.SkuTO sku : bin.getSku())
      locSKUs.add(new Bin.SKU(sku.getSkuID(), sku.getQuantity()));
    return new Bin(bin.getBinID()).withSKUs(new HashSet<>(locSKUs));
  }
  
  public VehicleTO setBinViaLocBin(Bin locBin){
    List<BinTO.SkuTO> skuList = new ArrayList<>();
    for(Bin.SKU locSKU : locBin.getSKUs())
      skuList.add(new BinTO.SkuTO().setSkuID(locSKU.getSkuID()).setQuantity(locSKU.getQuantity()));
    BinTO tmpBin = new BinTO().setBinID(locBin.getName()).setSku(skuList);
    this.bin = tmpBin;
    return this;
  }

  @XmlAttribute
  public String getType() {
    return type;
  }

  public VehicleTO setType(@Nonnull String type) {
    requireNonNull(type, "type");
    this.type = type;
    return this;
  }
  ////////////////////////////////////////modified end
  
  @XmlAttribute
  @XmlSchemaType(name = "unsignedInt")
  public Long getLength() {
    return length;
  }

  public VehicleTO setLength(@Nonnull Long length) {
    requireNonNull(length, "length");
    this.length = length;
    return this;
  }

  @XmlAttribute
  @XmlSchemaType(name = "unsignedInt")
  public Long getEnergyLevelCritical() {
    return energyLevelCritical;
  }

  public VehicleTO setEnergyLevelCritical(@Nonnull Long energyLevelCritical) {
    requireNonNull(energyLevelCritical, "energyLevelCritical");
    this.energyLevelCritical = energyLevelCritical;
    return this;
  }

  @XmlAttribute
  @XmlSchemaType(name = "unsignedInt")
  public Long getEnergyLevelGood() {
    return energyLevelGood;
  }

  public VehicleTO setEnergyLevelGood(@Nonnull Long energyLevelGood) {
    requireNonNull(energyLevelGood, "energyLevelGood");
    this.energyLevelGood = energyLevelGood;
    return this;
  }

  @XmlAttribute
  @XmlSchemaType(name = "unsignedInt")
  public Long getEnergyLevelFullyRecharged() {
    return energyLevelFullyRecharged;
  }

  public VehicleTO setEnergyLevelFullyRecharged(@Nonnull Long energyLevelFullyRecharged) {
    requireNonNull(energyLevelFullyRecharged, "energyLevelFullyRecharged");
    this.energyLevelFullyRecharged = energyLevelFullyRecharged;
    return this;
  }

  @XmlAttribute
  @XmlSchemaType(name = "unsignedInt")
  public Long getEnergyLevelSufficientlyRecharged() {
    return energyLevelSufficientlyRecharged;
  }

  public VehicleTO setEnergyLevelSufficientlyRecharged(
      @Nonnull Long energyLevelSufficientlyRecharged) {
    requireNonNull(energyLevelSufficientlyRecharged, "energyLevelSufficientlyRecharged");
    this.energyLevelSufficientlyRecharged = energyLevelSufficientlyRecharged;
    return this;
  }

  @XmlAttribute
  @XmlSchemaType(name = "unsignedInt")
  public int getMaxVelocity() {
    return maxVelocity;
  }

  public VehicleTO setMaxVelocity(@Nonnull int maxVelocity) {
    this.maxVelocity = maxVelocity;
    return this;
  }

  @XmlAttribute
  @XmlSchemaType(name = "unsignedInt")
  public int getMaxReverseVelocity() {
    return maxReverseVelocity;
  }

  public VehicleTO setMaxReverseVelocity(@Nonnull int maxReverseVelocity) {
    this.maxReverseVelocity = maxReverseVelocity;
    return this;
  }
}
