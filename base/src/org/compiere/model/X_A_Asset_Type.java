/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.compiere.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.util.KeyNamePair;

/** Generated Model for A_Asset_Type
 *  @author Adempiere (generated) 
 *  @version Release 3.5.3a - $Id$ */
public class X_A_Asset_Type extends PO implements I_A_Asset_Type, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20081221L;

    /** Standard Constructor */
    public X_A_Asset_Type (Properties ctx, int A_Asset_Type_ID, String trxName)
    {
      super (ctx, A_Asset_Type_ID, trxName);
      /** if (A_Asset_Type_ID == 0)
        {
			setA_Asset_Type_ID (0);
			setIsDepreciable (null);
// 'XX'
			setIsInPosession (null);
// 'XX'
			setIsOwned (null);
// 'XX'
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_A_Asset_Type (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 7 - System - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_A_Asset_Type[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Asset Type.
		@param A_Asset_Type_ID Asset Type	  */
	public void setA_Asset_Type_ID (int A_Asset_Type_ID)
	{
		if (A_Asset_Type_ID < 1)
			 throw new IllegalArgumentException ("A_Asset_Type_ID is mandatory.");
		set_ValueNoCheck (COLUMNNAME_A_Asset_Type_ID, Integer.valueOf(A_Asset_Type_ID));
	}

	/** Get Asset Type.
		@return Asset Type	  */
	public int getA_Asset_Type_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_A_Asset_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** IsDepreciable AD_Reference_ID=1000058 */
	public static final int ISDEPRECIABLE_AD_Reference_ID=1000058;
	/** Yes = YX */
	public static final String ISDEPRECIABLE_Yes = "YX";
	/** No = NX */
	public static final String ISDEPRECIABLE_No = "NX";
	/** - = XX */
	public static final String ISDEPRECIABLE__ = "XX";
	/** - / Default No = XN */
	public static final String ISDEPRECIABLE__DefaultNo = "XN";
	/** - / Default Yes = XY */
	public static final String ISDEPRECIABLE__DefaultYes = "XY";
	/** Set Is Depreciable.
		@param IsDepreciable 
		This asset CAN be depreciated
	  */
	public void setIsDepreciable (String IsDepreciable)
	{

		set_Value (COLUMNNAME_IsDepreciable, IsDepreciable);
	}

	/** Get Is Depreciable.
		@return This asset CAN be depreciated
	  */
	public String getIsDepreciable () 
	{
		return (String)get_Value(COLUMNNAME_IsDepreciable);
	}

	/** IsInPosession AD_Reference_ID=1000058 */
	public static final int ISINPOSESSION_AD_Reference_ID=1000058;
	/** Yes = YX */
	public static final String ISINPOSESSION_Yes = "YX";
	/** No = NX */
	public static final String ISINPOSESSION_No = "NX";
	/** - = XX */
	public static final String ISINPOSESSION__ = "XX";
	/** - / Default No = XN */
	public static final String ISINPOSESSION__DefaultNo = "XN";
	/** - / Default Yes = XY */
	public static final String ISINPOSESSION__DefaultYes = "XY";
	/** Set In Possession.
		@param IsInPosession 
		The asset is in the possession of the organization
	  */
	public void setIsInPosession (String IsInPosession)
	{

		set_Value (COLUMNNAME_IsInPosession, IsInPosession);
	}

	/** Get In Possession.
		@return The asset is in the possession of the organization
	  */
	public String getIsInPosession () 
	{
		return (String)get_Value(COLUMNNAME_IsInPosession);
	}

	/** IsOwned AD_Reference_ID=1000058 */
	public static final int ISOWNED_AD_Reference_ID=1000058;
	/** Yes = YX */
	public static final String ISOWNED_Yes = "YX";
	/** No = NX */
	public static final String ISOWNED_No = "NX";
	/** - = XX */
	public static final String ISOWNED__ = "XX";
	/** - / Default No = XN */
	public static final String ISOWNED__DefaultNo = "XN";
	/** - / Default Yes = XY */
	public static final String ISOWNED__DefaultYes = "XY";
	/** Set Owned.
		@param IsOwned 
		The asset is owned by the organization
	  */
	public void setIsOwned (String IsOwned)
	{

		set_Value (COLUMNNAME_IsOwned, IsOwned);
	}

	/** Get Owned.
		@return The asset is owned by the organization
	  */
	public String getIsOwned () 
	{
		return (String)get_Value(COLUMNNAME_IsOwned);
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		if (Name == null)
			throw new IllegalArgumentException ("Name is mandatory.");
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		if (Value == null)
			throw new IllegalArgumentException ("Value is mandatory.");
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}