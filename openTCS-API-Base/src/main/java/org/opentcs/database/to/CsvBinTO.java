/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentcs.database.to;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.Location.SKU;

/**
 * A bin transfer Object of a csv file.
 * @author Henry
 */
public final class CsvBinTO {
  public static final String[] CsvTitle={"BinID.", "SKUs", "Location", "Row", "Column", "Position", "Locked"};
  private String locationName="";
  private int locationRow=0;
  private int locationColumn=0;
  private String binID="";
  private int binPosition=0;
  private Boolean locked = false;
  private Set<SKU> SKUs = new HashSet<>();
  
  public CsvBinTO() {
  }

  /**
   * Create a new CsvBinTO to read from a csv or write to a csv.
   * @param locationName The location name.
   * @param binID The bin ID of a bin in this location.
   * @param binPosition The bin position in this location.
   * @param locationRow The row of the location.
   * @param locationColumn The column of the location.
   * @param locked Whether the bin is locked.
   * @param SKUs The SKUs stored in the bin.
   */
  public CsvBinTO(@Nonnull String binID, 
                  @Nonnull Set<SKU> SKUs, 
                  @Nonnull String locationName, 
                  @Nonnull int locationRow, 
                  @Nonnull int locationColumn, 
                  @Nonnull int binPosition, 
                  @Nonnull Boolean locked) {
    this.binID = requireNonNull(binID,"binID");
    this.SKUs = requireNonNull(SKUs,"SKU");
    this.locationName = requireNonNull(locationName,"locationName");
    this.locationRow = requireNonNull(locationRow,"locationRow");
    this.locationColumn = requireNonNull(locationColumn,"locationColumn");
    this.binPosition = requireNonNull(binPosition,"binPosition");
    this.locked = requireNonNull(locked,"locked");
  }
  
  public CsvBinTO(@Nonnull Location location,
                  @Nonnull int binPosition){
    this.binID = requireNonNull(location.getBin(binPosition).getBinID(),"binID");
    this.SKUs = requireNonNull(location.getBin(binPosition).getSKUs(),"SKUs");
    this.locationName = requireNonNull(location.getName(),"locationName");
    this.locationRow = requireNonNull(location.getRow(),"locationRow");
    this.locationColumn = requireNonNull(location.getColumn(),"locationColumn");
    this.binPosition = requireNonNull(binPosition,"binPosition");
    this.locked = requireNonNull(location.getBin(binPosition).isLocked(),"locked");
  }
  
  public CsvBinTO(@Nonnull String[] strData){
    this.binID = requireNonNull(strData[0],"binID");
    this.SKUs = requireNonNull(stringToSKUs(strData[1]),"SKU");
    this.locationName = requireNonNull(strData[2],"locationName");
    this.locationRow = requireNonNull(Integer.parseInt(strData[3]),"locationRow");
    this.locationColumn = requireNonNull(Integer.parseInt(strData[4]),"locationColumn");
    this.binPosition = requireNonNull(Integer.parseInt(strData[5]),"binPosition");
    this.locked = requireNonNull(Boolean.getBoolean(strData[6]),"locked");
  }
  
  public String getLocationName(){
    return locationName;
  }
  public CsvBinTO withLocationName(@Nonnull String locationName){
    return new CsvBinTO(binID, SKUs, locationName, locationRow, locationColumn, binPosition, locked);
  }
  public String getBinID(){
    return binID;
  }
  public CsvBinTO withBinID(@Nonnull String binID){
    return new CsvBinTO(binID, SKUs, locationName, locationRow, locationColumn, binPosition, locked);
  }
  public Integer getBinPosition(){
    return binPosition;
  }
  public CsvBinTO withBinPosition(@Nonnull Integer binPosition){
    return new CsvBinTO(binID, SKUs, locationName, locationRow, locationColumn, binPosition, locked);
  }
  public boolean isLocked(){
    return locked;
  }
  public CsvBinTO withLocked(@Nonnull boolean locked){
    return new CsvBinTO(binID, SKUs, locationName, locationRow, locationColumn, binPosition, locked);
  }
  public Set<SKU> getSKUs(){
    return SKUs;
  }
  public CsvBinTO withSKUs(@Nonnull Set<SKU> SKUs){
    return new CsvBinTO(binID, SKUs, locationName, locationRow, locationColumn, binPosition, locked);
  }
  public int getRow(){
    return locationRow;
  }
  public int getColumn(){
    return locationColumn;
  }
  
  public List<String> toList(){
    String[] dataStr = {binID, 
                        SKUsToString(SKUs), 
                        locationName, 
                        String.valueOf(locationRow), 
                        String.valueOf(locationColumn),
                        String.valueOf(binPosition), 
                        String.valueOf(locked)};
    List<String> dataList = Arrays.asList(dataStr);
    return dataList;
  }
  public Set<SKU> stringToSKUs(String skuString){
    List<SKU> Skus = Arrays.asList(skuString.split(Location.SKU_SEPARATOR))
                      .stream().filter(sku -> !sku.isEmpty())
                      .map(sku -> {
                        String[] tmpSku = sku.split(Location.QUANTITY_SEPARATOR);
                        return new SKU(tmpSku[0],Integer.parseInt(tmpSku[1]));
                      })
                      .collect(Collectors.toList());
    return new HashSet<>(Skus);
  }
  public String SKUsToString(Set<SKU> SKUs){
    return new ArrayList<>(SKUs).stream().map(SKU -> SKU.toString())
                                  .collect(Collectors.joining(Location.SKU_SEPARATOR));
  }
  public int getSKUQuantity(String skuID){
    for(SKU sku:SKUs){
      if(sku.getSkuID().equals(skuID))
        return sku.getQuantity();
    }
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof CsvBinTO){
      CsvBinTO tmpObj = (CsvBinTO)obj;
      return binID.equals(tmpObj.getBinID()); 
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 17 * hash + Objects.hashCode(this.binID);
    return hash;
  }
}