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
import javax.xml.bind.annotation.XmlType;
import org.opentcs.data.model.Bin;

/**
 *
 * @author Martin Grzenia (Fraunhofer IML)
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {"name", "id", "xPosition", "yPosition", "zPosition", "links", "bins", "properties"})
public class LocationTO
    extends PlantModelElementTO {

  private Long xPosition = 0L;
  private Long yPosition = 0L;
  private String type = "";
  //modified by Henry
  private Long zPosition = 0L;
  private List<BinTO> bins = new ArrayList<>();
  private List<Link> links = new ArrayList<>();
  
  @XmlAttribute
  public Long getxPosition() {
    return xPosition;
  }

  public LocationTO setxPosition(@Nonnull Long xPosition) {
    requireNonNull(xPosition, "xPosition");
    this.xPosition = xPosition;
    return this;
  }

  @XmlAttribute
  public Long getyPosition() {
    return yPosition;
  }

  public LocationTO setyPosition(@Nonnull Long yPosition) {
    requireNonNull(yPosition, "yPosition");
    this.yPosition = yPosition;
    return this;
  }
  
  //////////////////////////modified by Henry
  @XmlAttribute
  public Long getzPosition() {
    return zPosition;
  }

  public LocationTO setzPosition(@Nonnull Long zPosition) {
    requireNonNull(zPosition, "zPosition");
    this.zPosition = zPosition;
    return this;
  }
  //////////////////////////modified end
  @XmlAttribute
  public String getType() {
    return type;
  }

  public LocationTO setType(@Nonnull String type) {
    requireNonNull(type, "type");
    this.type = type;
    return this;
  }
  
 ////////////////////////////////////////////////////////Modified by Henry 
  
  @XmlElement(name = "bin", required = false)
  public List<BinTO> getBins() {
    return bins;
  }

  public LocationTO setBins(@Nonnull List<BinTO> bins) {
    requireNonNull(bins, "bins");
    this.bins = bins;
    return this;
  }
  
  @XmlAccessorType(XmlAccessType.PROPERTY)
  public static class BinTO{
    private String binID = "";
    private List<SkuTO> skuList = new ArrayList<>();
    
    @XmlAttribute(required = true)
    public String getBinID(){
      return binID;
    }
    public BinTO setBinID(@Nonnull String binID){
      requireNonNull(binID,"binID");
      this.binID = binID;
      return this;
    }
    
    @XmlElement(name = "sku", required = false)
    public List<SkuTO> getSku(){
      return skuList;
    }
    public BinTO setSku(@Nonnull List<SkuTO> skuList){
      requireNonNull(skuList,"skuList");
      this.skuList = skuList;
      return this;
    }

    @Override
    public boolean equals(Object obj) {
      if(obj instanceof BinTO){
        BinTO tmpObj = (BinTO) obj;
        return binID.equals(tmpObj.getBinID());
      }
      return false; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
      return binID.hashCode(); //To change body of generated methods, choose Tools | Templates.
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlType(propOrder = {"skuID", "quantity"})
    public static class SkuTO{
      private String skuID = "";
      private Double quantity = 0.0;
      
      @XmlAttribute(required = true)
      public String getSkuID(){
        return skuID;
      }
      public SkuTO setSkuID(@Nonnull String skuID){
        requireNonNull(skuID,"skuID");
        this.skuID = skuID;
        return this;
      }

      @XmlAttribute(required = true)
      public Double getQuantity(){
        return quantity;
      }
      public SkuTO setQuantity(@Nonnull Double quantity){
        requireNonNull(quantity,"quantity");
        this.quantity = quantity;
        return this;
      }
      
      @Override
      public boolean equals(Object obj) {
        if(obj instanceof SkuTO){
          SkuTO tmpObj = (SkuTO) obj;
          return skuID.equals(tmpObj.getSkuID());
        }
        return false; 
      }

      @Override
      public int hashCode() {
        return skuID.hashCode(); 
      }
    }
  }
  
  
  public List<Bin> getLocationBins(){
    List<Bin> locBins = new ArrayList<>();
    for(BinTO bin : bins){
      List<Bin.SKU> locSKUs = new ArrayList<>();
      for(BinTO.SkuTO sku:bin.getSku())
        locSKUs.add(new Bin.SKU(sku.getSkuID(), sku.getQuantity()));
      locBins.add(new Bin(bin.getBinID()).withSKUs(new HashSet<>(locSKUs)));
    }
    return locBins;
  }
  
  public LocationTO setBinsViaLocBins(List<Bin> locBins){
    List<BinTO> tmpBins = new ArrayList<>();
    for(Bin locBin : locBins){
      List<BinTO.SkuTO> skuList = new ArrayList<>();
      for(Bin.SKU locSKU : locBin.getSKUs())
        skuList.add(new BinTO.SkuTO().setSkuID(locSKU.getSkuID()).setQuantity(locSKU.getQuantity()));
      tmpBins.add(new BinTO().setBinID(locBin.getName()).setSku(skuList));
    }
    this.bins = tmpBins;
    return this;
  }
  
  ///////////////////////////////////////////////////////////////modified end
  @XmlElement(name = "link", required = true)
  public List<Link> getLinks() {
    return links;
  }

  public LocationTO setLinks(@Nonnull List<Link> links) {
    requireNonNull(links, "links");
    this.links = links;
    return this;
  }
  
  @XmlAccessorType(XmlAccessType.PROPERTY)
  @XmlType(propOrder = {"point", "allowedOperations"})
  public static class Link {

    private String point = "";
    private List<AllowedOperationTO> allowedOperations = new ArrayList<>();

    @XmlAttribute(required = true)
    public String getPoint() {
      return point;
    }

    public Link setPoint(@Nonnull String point) {
      requireNonNull(point, "point");
      this.point = point;
      return this;
    }

    @XmlElement(name = "allowedOperation")
    public List<AllowedOperationTO> getAllowedOperations() {
      return allowedOperations;
    }

    public Link setAllowedOperations(@Nonnull List<AllowedOperationTO> allowedOperations) {
      requireNonNull(allowedOperations, "allowedOperations");
      this.allowedOperations = allowedOperations;
      return this;
    }
  }
}
