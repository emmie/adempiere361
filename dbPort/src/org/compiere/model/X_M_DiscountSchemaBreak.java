/******************************************************************************
 * Product: Compiere ERP & CRM Smart Business Solution                        *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software;
 you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program;
 if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

/** Generated Model - DO NOT CHANGE */
import java.util.*;
import java.sql.*;
import java.math.*;
import org.compiere.util.*;
/** Generated Model for M_DiscountSchemaBreak
 *  @author Jorg Janke (generated) 
 *  @version Release 2.6.0a - $Id$ */
public class X_M_DiscountSchemaBreak extends PO
{
/** Standard Constructor
@param ctx context
@param M_DiscountSchemaBreak_ID id
@param trxName transaction
*/
public X_M_DiscountSchemaBreak (Properties ctx, int M_DiscountSchemaBreak_ID, String trxName)
{
super (ctx, M_DiscountSchemaBreak_ID, trxName);
/** if (M_DiscountSchemaBreak_ID == 0)
{
setBreakDiscount (Env.ZERO);
setBreakValue (Env.ZERO);
setIsBPartnerFlatDiscount (false);	// N
setM_DiscountSchemaBreak_ID (0);
setM_DiscountSchema_ID (0);
setSeqNo (0);	// @SQL=SELECT NVL(MAX(SeqNo),0)+10 AS DefaultValue FROM M_DiscountSchemaBreak WHERE M_DiscountSchema_ID=@M_DiscountSchema_ID@
}
 */
}
/** Load Constructor 
@param ctx context
@param rs result set 
@param trxName transaction
*/
public X_M_DiscountSchemaBreak (Properties ctx, ResultSet rs, String trxName)
{
super (ctx, rs, trxName);
}
/** AD_Table_ID=476 */
public static final int Table_ID=476;

/** TableName=M_DiscountSchemaBreak */
public static final String Table_Name="M_DiscountSchemaBreak";

protected static KeyNamePair Model = new KeyNamePair(476,"M_DiscountSchemaBreak");

protected BigDecimal accessLevel = new BigDecimal(3);
/** AccessLevel
@return 3 - Client - Org 
*/
protected int get_AccessLevel()
{
return accessLevel.intValue();
}
/** Load Meta Data
@param ctx context
@return PO Info
*/
protected POInfo initPO (Properties ctx)
{
POInfo poi = POInfo.getPOInfo (ctx, Table_ID);
return poi;
}
/** Info
@return info
*/
public String toString()
{
StringBuffer sb = new StringBuffer ("X_M_DiscountSchemaBreak[").append(get_ID()).append("]");
return sb.toString();
}
/** Set Break Discount %.
@param BreakDiscount Trade Discount in Percent for the break level */
public void setBreakDiscount (BigDecimal BreakDiscount)
{
if (BreakDiscount == null) throw new IllegalArgumentException ("BreakDiscount is mandatory.");
set_Value ("BreakDiscount", BreakDiscount);
}
/** Get Break Discount %.
@return Trade Discount in Percent for the break level */
public BigDecimal getBreakDiscount() 
{
BigDecimal bd = (BigDecimal)get_Value("BreakDiscount");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set Break Value.
@param BreakValue Low Value of trade discount break level */
public void setBreakValue (BigDecimal BreakValue)
{
if (BreakValue == null) throw new IllegalArgumentException ("BreakValue is mandatory.");
set_Value ("BreakValue", BreakValue);
}
/** Get Break Value.
@return Low Value of trade discount break level */
public BigDecimal getBreakValue() 
{
BigDecimal bd = (BigDecimal)get_Value("BreakValue");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set B.Partner Flat Discount.
@param IsBPartnerFlatDiscount Use flat discount defined on Business Partner Level */
public void setIsBPartnerFlatDiscount (boolean IsBPartnerFlatDiscount)
{
set_Value ("IsBPartnerFlatDiscount", new Boolean(IsBPartnerFlatDiscount));
}
/** Get B.Partner Flat Discount.
@return Use flat discount defined on Business Partner Level */
public boolean isBPartnerFlatDiscount() 
{
Object oo = get_Value("IsBPartnerFlatDiscount");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Discount Schema Break.
@param M_DiscountSchemaBreak_ID Trade Discount Break */
public void setM_DiscountSchemaBreak_ID (int M_DiscountSchemaBreak_ID)
{
if (M_DiscountSchemaBreak_ID < 1) throw new IllegalArgumentException ("M_DiscountSchemaBreak_ID is mandatory.");
set_ValueNoCheck ("M_DiscountSchemaBreak_ID", new Integer(M_DiscountSchemaBreak_ID));
}
/** Get Discount Schema Break.
@return Trade Discount Break */
public int getM_DiscountSchemaBreak_ID() 
{
Integer ii = (Integer)get_Value("M_DiscountSchemaBreak_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Discount Schema.
@param M_DiscountSchema_ID Schema to calculate the trade discount percentage */
public void setM_DiscountSchema_ID (int M_DiscountSchema_ID)
{
if (M_DiscountSchema_ID < 1) throw new IllegalArgumentException ("M_DiscountSchema_ID is mandatory.");
set_ValueNoCheck ("M_DiscountSchema_ID", new Integer(M_DiscountSchema_ID));
}
/** Get Discount Schema.
@return Schema to calculate the trade discount percentage */
public int getM_DiscountSchema_ID() 
{
Integer ii = (Integer)get_Value("M_DiscountSchema_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Product Category.
@param M_Product_Category_ID Category of a Product */
public void setM_Product_Category_ID (int M_Product_Category_ID)
{
if (M_Product_Category_ID <= 0) set_Value ("M_Product_Category_ID", null);
 else 
set_Value ("M_Product_Category_ID", new Integer(M_Product_Category_ID));
}
/** Get Product Category.
@return Category of a Product */
public int getM_Product_Category_ID() 
{
Integer ii = (Integer)get_Value("M_Product_Category_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Product.
@param M_Product_ID Product, Service, Item */
public void setM_Product_ID (int M_Product_ID)
{
if (M_Product_ID <= 0) set_Value ("M_Product_ID", null);
 else 
set_Value ("M_Product_ID", new Integer(M_Product_ID));
}
/** Get Product.
@return Product, Service, Item */
public int getM_Product_ID() 
{
Integer ii = (Integer)get_Value("M_Product_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Sequence.
@param SeqNo Method of ordering records;
 lowest number comes first */
public void setSeqNo (int SeqNo)
{
set_Value ("SeqNo", new Integer(SeqNo));
}
/** Get Sequence.
@return Method of ordering records;
 lowest number comes first */
public int getSeqNo() 
{
Integer ii = (Integer)get_Value("SeqNo");
if (ii == null) return 0;
return ii.intValue();
}
/** Get Record ID/ColumnName
@return ID/ColumnName pair
*/public KeyNamePair getKeyNamePair() 
{
return new KeyNamePair(get_ID(), String.valueOf(getSeqNo()));
}
}
