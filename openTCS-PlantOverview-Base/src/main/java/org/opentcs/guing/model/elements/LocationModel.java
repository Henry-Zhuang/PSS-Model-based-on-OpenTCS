/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.guing.model.elements;

import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.ResourceBundle;
import org.opentcs.data.ObjectPropConstants;
import org.opentcs.data.model.visualization.ElementPropKeys;
import static org.opentcs.guing.I18nPlantOverviewBase.BUNDLE_PATH;
import org.opentcs.guing.components.properties.event.AttributesChangeEvent;
import org.opentcs.guing.components.properties.event.AttributesChangeListener;
import org.opentcs.guing.components.properties.type.CoordinateProperty;
import org.opentcs.guing.components.properties.type.KeyValueSetProperty;
import org.opentcs.guing.components.properties.type.LocationTypeProperty;
import org.opentcs.guing.components.properties.type.StringProperty;
import org.opentcs.guing.components.properties.type.SymbolProperty;
import org.opentcs.guing.model.AbstractConnectableModelComponent;
import org.opentcs.guing.model.PositionableModelComponent;
//modified by Henry
import org.opentcs.guing.components.properties.type.IntegerProperty;

/**
 * Basic implementation for every kind of location.
 *
 * @author Sebastian Naumann (ifak e.V. Magdeburg)
 */
public class LocationModel
    extends AbstractConnectableModelComponent
    implements AttributesChangeListener,
               PositionableModelComponent {

  /**
   * The property key for the location's type.
   */
  public static final String TYPE = "Type";
////////////////////////////////////////////////////modified by Henry
  /** 
   * The property key for the location's Bins.
   */
  public static final String[] BINIDs = {"BinID0","BinID1","BinID2","BinID3","BinID4","BinID5","BinID6","BinID7"};
  /** 
   * The property key for the SKUs stored in location's Bins.
   */
  public static final String[] BINSKUs = {"BinSKU0","BinSKU1","BinSKU2","BinSKU3","BinSKU4","BinSKU5","BinSKU6","BinSKU7"};
  /** 
   * The property key for the number of bins.
   */
  public static final String BINS_NUM = "BinsNum";
  /**
   * Whether it's allowed to edit a bin in modelling state.
   */
  public static boolean binModellingEditable = true;
////////////////////////////////////////////////////modified by Henry

  /**
   * This class's resource bundle.
   */
  private final ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PATH);
  /**
   * The model of the type.
   */
  private transient LocationTypeModel fLocationType;

  /**
   * Creates a new instance.
   */
  public LocationModel() {
    createProperties();
  }

  /**
   * Sets the location type.
   *
   * @param type The model of the type.
   */
  public void setLocationType(LocationTypeModel type) {
    if (fLocationType != null) {
      fLocationType.removeAttributesChangeListener(this);
    }

    if (type != null) {
      fLocationType = type;
      fLocationType.addAttributesChangeListener(this);
    }
  }

  /**
   * Returns the location type.
   *
   * @return The type.
   */
  public LocationTypeModel getLocationType() {
    return fLocationType;
  }

  /**
   * Refreshes the name of this location.
   */
  protected void updateName() {
    StringProperty property = getPropertyName();
    String oldName = property.getText();
    String newName = getName();
    property.setText(newName);
    if (!newName.equals(oldName)) {
      property.markChanged();
    }

    propertiesChanged(this);
  }

  @Override // AbstractModelComponent
  public String getDescription() {
    return bundle.getString("locationModel.description");
  }

  @Override // AttributesChangeListener
  public void propertiesChanged(AttributesChangeEvent e) {
    if (fLocationType.getPropertyName().hasChanged()) {
      updateName();
    }

    if (fLocationType.getPropertyDefaultRepresentation().hasChanged()) {
      propertiesChanged(this);
    }
  }

  public void updateTypeProperty(List<LocationTypeModel> types) {
    requireNonNull(types, "types");

    List<String> possibleValues = new ArrayList<>();
    String value = null;

    for (LocationTypeModel type : types) {
      possibleValues.add(type.getName());

      if (type == fLocationType) {
        value = type.getName();
      }
    }

    getPropertyType().setPossibleValues(possibleValues);
    getPropertyType().setValue(value);
    getPropertyType().markChanged();
  }

  public CoordinateProperty getPropertyModelPositionX() {
    return (CoordinateProperty) getProperty(MODEL_X_POSITION);
  }

  public CoordinateProperty getPropertyModelPositionY() {
    return (CoordinateProperty) getProperty(MODEL_Y_POSITION);
  }
  //////////////////////////////////////////////////////////////////Modified by Henry
  
  public CoordinateProperty getPropertyModelPositionZ() {
    return (CoordinateProperty) getProperty(MODEL_Z_POSITION);
  }
  
  public IntegerProperty getPropertyBinsNum() {
    return (IntegerProperty) getProperty(BINS_NUM);
  }
 
  public List<StringProperty> getPropertyBin(int index){
    List<StringProperty> bin = new ArrayList<>();
    bin.add((StringProperty) getProperty(BINIDs[index]));
    bin.add((StringProperty) getProperty(BINSKUs[index]));
    return bin;
  }
  public List<List<StringProperty>> getPropertyBins(){
    List<List<StringProperty>> pBins = new ArrayList<>();
    for(int i=0;i<BINIDs.length;i++){
      pBins.add(getPropertyBin(i));
    }
    return pBins;
  }
  public int getBinsSize(){
    return BINIDs.length;
  }
  //////////////////////////////////////////////////////////////////Modified end
  public LocationTypeProperty getPropertyType() {
    return (LocationTypeProperty) getProperty(TYPE);
  }

  public KeyValueSetProperty getPropertyMiscellaneous() {
    return (KeyValueSetProperty) getProperty(MISCELLANEOUS);
  }

  public SymbolProperty getPropertyDefaultRepresentation() {
    return (SymbolProperty) getProperty(ObjectPropConstants.LOC_DEFAULT_REPRESENTATION);
  }

  public StringProperty getPropertyLayoutPositionX() {
    return (StringProperty) getProperty(ElementPropKeys.LOC_POS_X);
  }

  public StringProperty getPropertyLayoutPositionY() {
    return (StringProperty) getProperty(ElementPropKeys.LOC_POS_Y);
  }

  public StringProperty getPropertyLabelOffsetX() {
    return (StringProperty) getProperty(ElementPropKeys.LOC_LABEL_OFFSET_X);
  }

  public StringProperty getPropertyLabelOffsetY() {
    return (StringProperty) getProperty(ElementPropKeys.LOC_LABEL_OFFSET_Y);
  }

  public StringProperty getPropertyLabelOrientationAngle() {
    return (StringProperty) getProperty(ElementPropKeys.LOC_LABEL_ORIENTATION_ANGLE);
  }

  private void createProperties() {
    StringProperty pName = new StringProperty(this);
    pName.setDescription(bundle.getString("locationModel.property_name.description"));
    pName.setHelptext(bundle.getString("locationModel.property_name.helptext"));
    setProperty(NAME, pName);
    
    ////////////////////////////////////////////////////////////////////////Modified by Henry {
    
    IntegerProperty pBinsNum = new IntegerProperty(this, 0);
    pBinsNum.setDescription(bundle.getString("locationModel.property_BinsNum.description"));
    pBinsNum.setHelptext(bundle.getString("locationModel.property_BinsNum.helptext"));
    pBinsNum.setModellingEditable(binModellingEditable);
    setProperty(BINS_NUM, pBinsNum);
    
    StringProperty pBinID0 = new StringProperty(this);
    pBinID0.setDescription(bundle.getString("locationModel.property_Bin0.description"));
    pBinID0.setHelptext(bundle.getString("locationModel.property_Bin0.helptext"));
    pBinID0.setModellingEditable(binModellingEditable);
    setProperty(BINIDs[0], pBinID0);
    
    StringProperty pBinSKU0 = new StringProperty(this);
    pBinSKU0.setDescription(bundle.getString("locationModel.property_SKU.description"));
    pBinSKU0.setHelptext(bundle.getString("locationModel.property_SKU.helptext"));
    pBinSKU0.setModellingEditable(binModellingEditable);
    setProperty(BINSKUs[0], pBinSKU0);
    
    StringProperty pBinID1 = new StringProperty(this);
    pBinID1.setDescription(bundle.getString("locationModel.property_Bin1.description"));
    pBinID1.setHelptext(bundle.getString("locationModel.property_Bin1.helptext"));
    pBinID1.setModellingEditable(binModellingEditable);
    setProperty(BINIDs[1], pBinID1);
    
    StringProperty pBinSKU1 = new StringProperty(this);
    pBinSKU1.setDescription(bundle.getString("locationModel.property_SKU.description"));
    pBinSKU1.setHelptext(bundle.getString("locationModel.property_SKU.helptext"));
    pBinSKU1.setModellingEditable(binModellingEditable);
    setProperty(BINSKUs[1], pBinSKU1);
    
    StringProperty pBinID2 = new StringProperty(this);
    pBinID2.setDescription(bundle.getString("locationModel.property_Bin2.description"));
    pBinID2.setHelptext(bundle.getString("locationModel.property_Bin2.helptext"));
    pBinID2.setModellingEditable(binModellingEditable);
    setProperty(BINIDs[2], pBinID2);
    
    StringProperty pBinSKU2 = new StringProperty(this);
    pBinSKU2.setDescription(bundle.getString("locationModel.property_SKU.description"));
    pBinSKU2.setHelptext(bundle.getString("locationModel.property_SKU.helptext"));
    pBinSKU2.setModellingEditable(binModellingEditable);
    setProperty(BINSKUs[2], pBinSKU2);
    
    StringProperty pBinID3 = new StringProperty(this);
    pBinID3.setDescription(bundle.getString("locationModel.property_Bin3.description"));
    pBinID3.setHelptext(bundle.getString("locationModel.property_Bin3.helptext"));
    pBinID3.setModellingEditable(binModellingEditable);
    setProperty(BINIDs[3], pBinID3);
    
    StringProperty pBinSKU3 = new StringProperty(this);
    pBinSKU3.setDescription(bundle.getString("locationModel.property_SKU.description"));
    pBinSKU3.setHelptext(bundle.getString("locationModel.property_SKU.helptext"));
    pBinSKU3.setModellingEditable(binModellingEditable);
    setProperty(BINSKUs[3], pBinSKU3);
    
    StringProperty pBinID4 = new StringProperty(this);
    pBinID4.setDescription(bundle.getString("locationModel.property_Bin4.description"));
    pBinID4.setHelptext(bundle.getString("locationModel.property_Bin4.helptext"));
    pBinID4.setModellingEditable(binModellingEditable);
    setProperty(BINIDs[4], pBinID4);
    
    StringProperty pBinSKU4 = new StringProperty(this);
    pBinSKU4.setDescription(bundle.getString("locationModel.property_SKU.description"));
    pBinSKU4.setHelptext(bundle.getString("locationModel.property_SKU.helptext"));
    pBinSKU4.setModellingEditable(binModellingEditable);
    setProperty(BINSKUs[4], pBinSKU4);
    
    StringProperty pBinID5 = new StringProperty(this);
    pBinID5.setDescription(bundle.getString("locationModel.property_Bin5.description"));
    pBinID5.setHelptext(bundle.getString("locationModel.property_Bin5.helptext"));
    pBinID5.setModellingEditable(binModellingEditable);
    setProperty(BINIDs[5], pBinID5);
    
    StringProperty pBinSKU5 = new StringProperty(this);
    pBinSKU5.setDescription(bundle.getString("locationModel.property_SKU.description"));
    pBinSKU5.setHelptext(bundle.getString("locationModel.property_SKU.helptext"));
    pBinSKU5.setModellingEditable(binModellingEditable);
    setProperty(BINSKUs[5], pBinSKU5);
    
    StringProperty pBinID6 = new StringProperty(this);
    pBinID6.setDescription(bundle.getString("locationModel.property_Bin6.description"));
    pBinID6.setHelptext(bundle.getString("locationModel.property_Bin6.helptext"));
    pBinID6.setModellingEditable(binModellingEditable);
    setProperty(BINIDs[6], pBinID6);
    
    StringProperty pBinSKU6 = new StringProperty(this);
    pBinSKU6.setDescription(bundle.getString("locationModel.property_SKU.description"));
    pBinSKU6.setHelptext(bundle.getString("locationModel.property_SKU.helptext"));
    pBinSKU6.setModellingEditable(binModellingEditable);
    setProperty(BINSKUs[6], pBinSKU6);
    
    StringProperty pBinID7 = new StringProperty(this);
    pBinID7.setDescription(bundle.getString("locationModel.property_Bin7.description"));
    pBinID7.setHelptext(bundle.getString("locationModel.property_Bin7.helptext"));
    pBinID7.setModellingEditable(binModellingEditable);
    setProperty(BINIDs[7], pBinID7);
    
    StringProperty pBinSKU7 = new StringProperty(this);
    pBinSKU7.setDescription(bundle.getString("locationModel.property_SKU.description"));
    pBinSKU7.setHelptext(bundle.getString("locationModel.property_SKU.helptext"));
    pBinSKU7.setModellingEditable(binModellingEditable);
    setProperty(BINSKUs[7], pBinSKU7);
   
    ////////////////////////////////////////////////////////////////////////Modified end }

    CoordinateProperty pPosX = new CoordinateProperty(this);
    pPosX.setDescription(bundle.getString("locationModel.property_modelPositionX.description"));
    pPosX.setHelptext(bundle.getString("locationModel.property_modelPositionX.helptext"));
    setProperty(MODEL_X_POSITION, pPosX);

    CoordinateProperty pPosY = new CoordinateProperty(this);
    pPosY.setDescription(bundle.getString("locationModel.property_modelPositionY.description"));
    pPosY.setHelptext(bundle.getString("locationModel.property_modelPositionY.helptext"));
    setProperty(MODEL_Y_POSITION, pPosY);
    
    CoordinateProperty pPosZ = new CoordinateProperty(this);
    pPosZ.setDescription(bundle.getString("locationModel.property_modelPositionZ.description"));
    pPosZ.setHelptext(bundle.getString("locationModel.property_modelPositionZ.helptext"));
    setProperty(MODEL_Z_POSITION, pPosZ);
 
    LocationTypeProperty pType = new LocationTypeProperty(this);
    pType.setDescription(bundle.getString("locationModel.property_type.description"));
    pType.setHelptext(bundle.getString("locationModel.property_type.helptext"));
    setProperty(TYPE, pType);
    
    SymbolProperty pSymbol = new SymbolProperty(this);
    pSymbol.setDescription(bundle.getString("locationModel.property_symbol.description"));
    pSymbol.setHelptext(bundle.getString("locationModel.property_symbol.helptext"));
    pSymbol.setCollectiveEditable(true);
    setProperty(ObjectPropConstants.LOC_DEFAULT_REPRESENTATION, pSymbol);

    StringProperty pLocPosX = new StringProperty(this);
    pLocPosX.setDescription(bundle.getString("locationModel.property_positionX.description"));
    pLocPosX.setHelptext(bundle.getString("locationModel.property_positionX.helptext"));
    pLocPosX.setModellingEditable(false);
    setProperty(ElementPropKeys.LOC_POS_X, pLocPosX);

    StringProperty pLocPosY = new StringProperty(this);
    pLocPosY.setDescription(bundle.getString("locationModel.property_positionY.description"));
    pLocPosY.setHelptext(bundle.getString("locationModel.property_positionY.helptext"));
    pLocPosY.setModellingEditable(false);
    setProperty(ElementPropKeys.LOC_POS_Y, pLocPosY);

    StringProperty pLocLabelOffsetX = new StringProperty(this);
    pLocLabelOffsetX.setDescription(bundle.getString("locationModel.property_labelOffsetX.description"));
    pLocLabelOffsetX.setHelptext(bundle.getString("locationModel.property_labelOffsetX.helptext"));
    pLocLabelOffsetX.setModellingEditable(false);
    setProperty(ElementPropKeys.LOC_LABEL_OFFSET_X, pLocLabelOffsetX);

    StringProperty pLocLabelOffsetY = new StringProperty(this);
    pLocLabelOffsetY.setDescription(bundle.getString("locationModel.property_labelOffsetY.description"));
    pLocLabelOffsetY.setHelptext(bundle.getString("locationModel.property_labelOffsetY.helptext"));
    pLocLabelOffsetY.setModellingEditable(false);
    setProperty(ElementPropKeys.LOC_LABEL_OFFSET_Y, pLocLabelOffsetY);

    StringProperty pLocLabelOrientationAngle = new StringProperty(this);
    pLocLabelOrientationAngle.setDescription(bundle.getString("locationModel.property_labelOrientationAngle.description"));
    pLocLabelOrientationAngle.setHelptext(bundle.getString("locationModel.property_labelOrientationAngle.helptext"));
    pLocLabelOrientationAngle.setModellingEditable(false);
    setProperty(ElementPropKeys.LOC_LABEL_ORIENTATION_ANGLE, pLocLabelOrientationAngle);

    KeyValueSetProperty pMiscellaneous = new KeyValueSetProperty(this);
    pMiscellaneous.setDescription(bundle.getString("locationModel.property_miscellaneous.description"));
    pMiscellaneous.setHelptext(bundle.getString("locationModel.property_miscellaneous.helptext"));
    setProperty(MISCELLANEOUS, pMiscellaneous);
  }
}
