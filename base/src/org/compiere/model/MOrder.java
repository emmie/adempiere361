/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.adempiere.exceptions.BPartnerNoBillToAddressException;
import org.adempiere.exceptions.BPartnerNoShipToAddressException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.print.ReportEngine;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;


/**
 *  Order Model.
 * 	Please do not set DocStatus and C_DocType_ID directly. 
 * 	They are set in the process() method. 
 * 	Use DocAction and C_DocTypeTarget_ID instead.
 *
 *  @author Jorg Janke
 *
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962
 *  @version $Id: MOrder.java,v 1.5 2006/10/06 00:42:24 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>BF [ 2419978 ] Voiding PO, requisition don't set on NULL
 * 			<li>BF [ 2892578 ] Order should autoset only active price lists
 * 				https://sourceforge.net/tracker/?func=detail&aid=2892578&group_id=176962&atid=879335
 * @author Michael Judd, www.akunagroup.com
 *          <li>BF [ 2804888 ] Incorrect reservation of products with attributes
 */
public class MOrder extends X_C_Order implements DocAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -846451837699702120L;

	/**
	 * 	Create new Order by copying
	 * 	@param from order
	 * 	@param dateDoc date of the document date
	 * 	@param C_DocTypeTarget_ID target document type
	 * 	@param isSOTrx sales order 
	 * 	@param counter create counter links
	 *	@param copyASI copy line attributes Attribute Set Instance, Resaouce Assignment
	 * 	@param trxName trx
	 *	@return Order
	 */
	public static MOrder copyFrom (MOrder from, Timestamp dateDoc, 
		int C_DocTypeTarget_ID, boolean isSOTrx, boolean counter, boolean copyASI, 
		String trxName)
	{
		MOrder to = new MOrder (from.getCtx(), 0, trxName);
		to.set_TrxName(trxName);
		PO.copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.set_ValueNoCheck ("C_Order_ID", I_ZERO);
		to.set_ValueNoCheck ("DocumentNo", null);
		//
		to.setDocStatus (DOCSTATUS_Drafted);		//	Draft
		to.setDocAction(DOCACTION_Complete);
		//
		to.setC_DocType_ID(0);
		to.setC_DocTypeTarget_ID (C_DocTypeTarget_ID);
		to.setIsSOTrx(isSOTrx);
		//
		to.setIsSelected (false);
		to.setDateOrdered (dateDoc);
		to.setDateAcct (dateDoc);
		to.setDatePromised (dateDoc);	//	assumption
		to.setDatePrinted(null);
		to.setIsPrinted (false);
		//
		to.setIsApproved (false);
		to.setIsCreditApproved(false);
		to.setC_Payment_ID(0);
		to.setC_CashLine_ID(0);
		//	Amounts are updated  when adding lines
		to.setGrandTotal(Env.ZERO);
		to.setTotalLines(Env.ZERO);
		//
		to.setIsDelivered(false);
		to.setIsInvoiced(false);
		to.setIsSelfService(false);
		to.setIsTransferred (false);
		to.setPosted (false);
		to.setProcessed (false);
		if (counter)
			to.setRef_Order_ID(from.getC_Order_ID());
		else
			to.setRef_Order_ID(0);
		//
		if (!to.save(trxName))
			throw new IllegalStateException("Could not create Order");
		if (counter)
			from.setRef_Order_ID(to.getC_Order_ID());

		if (to.copyLinesFrom(from, counter, copyASI) == 0)
			throw new IllegalStateException("Could not create Order Lines");
		
		// don't copy linked PO/SO
		to.setLink_Order_ID(0);
		
		return to;
	}	//	copyFrom
	
	
	/**************************************************************************
	 *  Default Constructor
	 *  @param ctx context
	 *  @param  C_Order_ID    order to load, (0 create new order)
	 *  @param trxName trx name
	 */
	public MOrder(Properties ctx, int C_Order_ID, String trxName)
	{
		super (ctx, C_Order_ID, trxName);
		//  New
		if (C_Order_ID == 0)
		{
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Prepare);
			//
			setDeliveryRule (DELIVERYRULE_Availability);
			setFreightCostRule (FREIGHTCOSTRULE_FreightIncluded);
			setInvoiceRule (INVOICERULE_Immediate);
			setPaymentRule(PAYMENTRULE_OnCredit);
			setPriorityRule (PRIORITYRULE_Medium);
			setDeliveryViaRule (DELIVERYVIARULE_Pickup);
			//
			setIsDiscountPrinted (false);
			setIsSelected (false);
			setIsTaxIncluded (false);
			setIsSOTrx (true);
			setIsDropShip(false);
			setSendEMail (false);
			//
			setIsApproved(false);
			setIsPrinted(false);
			setIsCreditApproved(false);
			setIsDelivered(false);
			setIsInvoiced(false);
			setIsTransferred(false);
			setIsSelfService(false);
			//
			super.setProcessed(false);
			setProcessing(false);
			setPosted(false);

			setDateAcct (new Timestamp(System.currentTimeMillis()));
			setDatePromised (new Timestamp(System.currentTimeMillis()));
			setDateOrdered (new Timestamp(System.currentTimeMillis()));

			setFreightAmt (Env.ZERO);
			setChargeAmt (Env.ZERO);
			setTotalLines (Env.ZERO);
			setGrandTotal (Env.ZERO);
		}
	}	//	MOrder

	/**************************************************************************
	 *  Project Constructor
	 *  @param  project Project to create Order from
	 *  @param IsSOTrx sales order
	 * 	@param	DocSubTypeSO if SO DocType Target (default DocSubTypeSO_OnCredit)
	 */
	public MOrder (MProject project, boolean IsSOTrx, String DocSubTypeSO)
	{
		this (project.getCtx(), 0, project.get_TrxName());
		setAD_Client_ID(project.getAD_Client_ID());
		setAD_Org_ID(project.getAD_Org_ID());
		setC_Campaign_ID(project.getC_Campaign_ID());
		setSalesRep_ID(project.getSalesRep_ID());
		//
		setC_Project_ID(project.getC_Project_ID());
		setDescription(project.getName());
		Timestamp ts = project.getDateContract();
		if (ts != null)
			setDateOrdered (ts);
		ts = project.getDateFinish();
		if (ts != null)
			setDatePromised (ts);
		//
		setC_BPartner_ID(project.getC_BPartner_ID());
		setC_BPartner_Location_ID(project.getC_BPartner_Location_ID());
		setAD_User_ID(project.getAD_User_ID());
		//
		setM_Warehouse_ID(project.getM_Warehouse_ID());
		setM_PriceList_ID(project.getM_PriceList_ID());
		setC_PaymentTerm_ID(project.getC_PaymentTerm_ID());
		//
		setIsSOTrx(IsSOTrx);
		if (IsSOTrx)
		{
			if (DocSubTypeSO == null || DocSubTypeSO.length() == 0)
				setC_DocTypeTarget_ID(DocSubTypeSO_OnCredit);
			else
				setC_DocTypeTarget_ID(DocSubTypeSO);
		}
		else
			setC_DocTypeTarget_ID();
	}	//	MOrder

	/**
	 *  Load Constructor
	 *  @param ctx context
	 *  @param rs result set record
	 *  @param trxName transaction
	 */
	public MOrder (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MOrder

	/**	Order Lines					*/
	private MOrderLine[] 	m_lines = null;
	/**	Tax Lines					*/
	private MOrderTax[] 	m_taxes = null;
	/** Force Creation of order		*/
	private boolean			m_forceCreation = false;
	
	/**
	 * 	Overwrite Client/Org if required
	 * 	@param AD_Client_ID client
	 * 	@param AD_Org_ID org
	 */
	public void setClientOrg (int AD_Client_ID, int AD_Org_ID)
	{
		super.setClientOrg(AD_Client_ID, AD_Org_ID);
	}	//	setClientOrg


	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);
	}	//	addDescription
	
	/**
	 * 	Set Business Partner (Ship+Bill)
	 *	@param C_BPartner_ID bpartner
	 */
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		super.setC_BPartner_ID (C_BPartner_ID);
		super.setBill_BPartner_ID (C_BPartner_ID);
	}	//	setC_BPartner_ID
	
	/**
	 * 	Set Business Partner Location (Ship+Bill)
	 *	@param C_BPartner_Location_ID bp location
	 */
	public void setC_BPartner_Location_ID (int C_BPartner_Location_ID)
	{
		super.setC_BPartner_Location_ID (C_BPartner_Location_ID);
		super.setBill_Location_ID(C_BPartner_Location_ID);
	}	//	setC_BPartner_Location_ID

	/**
	 * 	Set Business Partner Contact (Ship+Bill)
	 *	@param AD_User_ID user
	 */
	public void setAD_User_ID (int AD_User_ID)
	{
		super.setAD_User_ID (AD_User_ID);
		super.setBill_User_ID (AD_User_ID);
	}	//	setAD_User_ID

	/**
	 * 	Set Ship Business Partner
	 *	@param C_BPartner_ID bpartner
	 */
	public void setShip_BPartner_ID (int C_BPartner_ID)
	{
		super.setC_BPartner_ID (C_BPartner_ID);
	}	//	setShip_BPartner_ID
	
	/**
	 * 	Set Ship Business Partner Location
	 *	@param C_BPartner_Location_ID bp location
	 */
	public void setShip_Location_ID (int C_BPartner_Location_ID)
	{
		super.setC_BPartner_Location_ID (C_BPartner_Location_ID);
	}	//	setShip_Location_ID

	/**
	 * 	Set Ship Business Partner Contact
	 *	@param AD_User_ID user
	 */
	public void setShip_User_ID (int AD_User_ID)
	{
		super.setAD_User_ID (AD_User_ID);
	}	//	setShip_User_ID
	
	
	/**
	 * 	Set Warehouse
	 *	@param M_Warehouse_ID warehouse
	 */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		super.setM_Warehouse_ID (M_Warehouse_ID);
	}	//	setM_Warehouse_ID
	
	/**
	 * 	Set Drop Ship
	 *	@param IsDropShip drop ship
	 */
	public void setIsDropShip (boolean IsDropShip)
	{
		super.setIsDropShip (IsDropShip);
	}	//	setIsDropShip
	
	/*************************************************************************/

	/** Sales Order Sub Type - SO	*/
	public static final String		DocSubTypeSO_Standard = "SO";
	/** Sales Order Sub Type - OB	*/
	public static final String		DocSubTypeSO_Quotation = "OB";
	/** Sales Order Sub Type - ON	*/
	public static final String		DocSubTypeSO_Proposal = "ON";
	/** Sales Order Sub Type - PR	*/
	public static final String		DocSubTypeSO_Prepay = "PR";
	/** Sales Order Sub Type - WR	*/
	public static final String		DocSubTypeSO_POS = "WR";
	/** Sales Order Sub Type - WP	*/
	public static final String		DocSubTypeSO_Warehouse = "WP";
	/** Sales Order Sub Type - WI	*/
	public static final String		DocSubTypeSO_OnCredit = "WI";
	/** Sales Order Sub Type - RM	*/
	public static final String		DocSubTypeSO_RMA = "RM";

	/**
	 * 	Set Target Sales Document Type
	 * 	@param DocSubTypeSO_x SO sub type - see DocSubTypeSO_*
	 */
	public void setC_DocTypeTarget_ID (String DocSubTypeSO_x)
	{
		String sql = "SELECT C_DocType_ID FROM C_DocType "
			+ "WHERE AD_Client_ID=? AND AD_Org_ID IN (0," + getAD_Org_ID()
			+ ") AND DocSubTypeSO=? "
			+ " AND IsActive='Y' "
			+ "ORDER BY AD_Org_ID DESC, IsDefault DESC";
		int C_DocType_ID = DB.getSQLValue(null, sql, getAD_Client_ID(), DocSubTypeSO_x);
		if (C_DocType_ID <= 0)
			log.severe ("Not found for AD_Client_ID=" + getAD_Client_ID () + ", SubType=" + DocSubTypeSO_x);
		else
		{
			log.fine("(SO) - " + DocSubTypeSO_x);
			setC_DocTypeTarget_ID (C_DocType_ID);
			setIsSOTrx(true);
		}
	}	//	setC_DocTypeTarget_ID

	/**
	 * 	Set Target Document Type.
	 * 	Standard Order or PO
	 */
	public void setC_DocTypeTarget_ID ()
	{
		if (isSOTrx())		//	SO = Std Order
		{
			setC_DocTypeTarget_ID(DocSubTypeSO_Standard);
			return;
		}
		//	PO
		String sql = "SELECT C_DocType_ID FROM C_DocType "
			+ "WHERE AD_Client_ID=? AND AD_Org_ID IN (0," + getAD_Org_ID()
			+ ") AND DocBaseType='POO' "
			+ "ORDER BY AD_Org_ID DESC, IsDefault DESC";
		int C_DocType_ID = DB.getSQLValue(null, sql, getAD_Client_ID());
		if (C_DocType_ID <= 0)
			log.severe ("No POO found for AD_Client_ID=" + getAD_Client_ID ());
		else
		{
			log.fine("(PO) - " + C_DocType_ID);
			setC_DocTypeTarget_ID (C_DocType_ID);
		}
	}	//	setC_DocTypeTarget_ID


	/**
	 * 	Set Business Partner Defaults & Details.
	 * 	SOTrx should be set.
	 * 	@param bp business partner
	 */
	public void setBPartner (MBPartner bp)
	{
		if (bp == null)
			return;

		setC_BPartner_ID(bp.getC_BPartner_ID());
		//	Defaults Payment Term
		int ii = 0;
		if (isSOTrx())
			ii = bp.getC_PaymentTerm_ID();
		else
			ii = bp.getPO_PaymentTerm_ID();
		if (ii != 0)
			setC_PaymentTerm_ID(ii);
		//	Default Price List
		if (isSOTrx())
			ii = bp.getM_PriceList_ID();
		else
			ii = bp.getPO_PriceList_ID();
		if (ii != 0)
			setM_PriceList_ID(ii);
		//	Default Delivery/Via Rule
		String ss = bp.getDeliveryRule();
		if (ss != null)
			setDeliveryRule(ss);
		ss = bp.getDeliveryViaRule();
		if (ss != null)
			setDeliveryViaRule(ss);
		//	Default Invoice/Payment Rule
		ss = bp.getInvoiceRule();
		if (ss != null)
			setInvoiceRule(ss);
		ss = bp.getPaymentRule();
		if (ss != null)
			setPaymentRule(ss);
		//	Sales Rep
		ii = bp.getSalesRep_ID();
		if (ii != 0)
			setSalesRep_ID(ii);


		//	Set Locations
		MBPartnerLocation[] locs = bp.getLocations(false);
		if (locs != null)
		{
			for (int i = 0; i < locs.length; i++)
			{
				if (locs[i].isShipTo())
					super.setC_BPartner_Location_ID(locs[i].getC_BPartner_Location_ID());
				if (locs[i].isBillTo())
					setBill_Location_ID(locs[i].getC_BPartner_Location_ID());
			}
			//	set to first
			if (getC_BPartner_Location_ID() == 0 && locs.length > 0)
				super.setC_BPartner_Location_ID(locs[0].getC_BPartner_Location_ID());
			if (getBill_Location_ID() == 0 && locs.length > 0)
				setBill_Location_ID(locs[0].getC_BPartner_Location_ID());
		}
		if (getC_BPartner_Location_ID() == 0)
		{	
			throw new BPartnerNoShipToAddressException(bp);
		}	
			
		if (getBill_Location_ID() == 0)
		{
			throw new BPartnerNoBillToAddressException(bp);
		}	

		//	Set Contact
		MUser[] contacts = bp.getContacts(false);
		if (contacts != null && contacts.length == 1)
			setAD_User_ID(contacts[0].getAD_User_ID());
	}	//	setBPartner


	/**
	 * 	Copy Lines From other Order
	 *	@param otherOrder order
	 *	@param counter set counter info
	 *	@param copyASI copy line attributes Attribute Set Instance, Resaouce Assignment
	 *	@return number of lines copied
	 */
	public int copyLinesFrom (MOrder otherOrder, boolean counter, boolean copyASI)
	{
		if (isProcessed() || isPosted() || otherOrder == null)
			return 0;
		MOrderLine[] fromLines = otherOrder.getLines(false, null);
		int count = 0;
		for (int i = 0; i < fromLines.length; i++)
		{
			MOrderLine line = new MOrderLine (this);
			PO.copyValues(fromLines[i], line, getAD_Client_ID(), getAD_Org_ID());
			line.setC_Order_ID(getC_Order_ID());
			//
			line.setQtyDelivered(Env.ZERO);
			line.setQtyInvoiced(Env.ZERO);
			line.setQtyReserved(Env.ZERO);
			line.setDateDelivered(null);
			line.setDateInvoiced(null);
			//
			line.setOrder(this);
			line.set_ValueNoCheck ("C_OrderLine_ID", I_ZERO);	//	new
			//	References
			if (!copyASI)
			{
				line.setM_AttributeSetInstance_ID(0);
				line.setS_ResourceAssignment_ID(0);
			}
			if (counter)
				line.setRef_OrderLine_ID(fromLines[i].getC_OrderLine_ID());
			else
				line.setRef_OrderLine_ID(0);

			// don't copy linked lines
			line.setLink_OrderLine_ID(0);
			//	Tax
			if (getC_BPartner_ID() != otherOrder.getC_BPartner_ID())
				line.setTax();		//	recalculate
			//
			//
			line.setProcessed(false);
			if (line.save(get_TrxName()))
				count++;
			//	Cross Link
			if (counter)
			{
				fromLines[i].setRef_OrderLine_ID(line.getC_OrderLine_ID());
				fromLines[i].save(get_TrxName());
			}
		}
		if (fromLines.length != count)
			log.log(Level.SEVERE, "Line difference - From=" + fromLines.length + " <> Saved=" + count);
		return count;
	}	//	copyLinesFrom

	
	/**************************************************************************
	 * 	String Representation
	 *	@return info
	 */
	public String toString ()
	{
		StringBuffer sb = new StringBuffer ("MOrder[")
			.append(get_ID()).append("-").append(getDocumentNo())
			.append(",IsSOTrx=").append(isSOTrx())
			.append(",C_DocType_ID=").append(getC_DocType_ID())
			.append(", GrandTotal=").append(getGrandTotal())
			.append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		return dt.getName() + " " + getDocumentNo();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
		ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.ORDER, getC_Order_ID(), get_TrxName());
		if (re == null)
			return null;
		return re.getPDF(file);
	}	//	createPDF
	
	/**
	 * 	Set Price List (and Currency, TaxIncluded) when valid
	 * 	@param M_PriceList_ID price list
	 */
	public void setM_PriceList_ID (int M_PriceList_ID)
	{
		MPriceList pl = MPriceList.get(getCtx(), M_PriceList_ID, null);
		if (pl.get_ID() == M_PriceList_ID)
		{
			super.setM_PriceList_ID(M_PriceList_ID);
			setC_Currency_ID(pl.getC_Currency_ID());
			setIsTaxIncluded(pl.isTaxIncluded());
		}
	}	//	setM_PriceList_ID

	
	/**************************************************************************
	 * 	Get Lines of Order
	 * 	@param whereClause where clause or null (starting with AND)
	 * 	@param orderClause order clause
	 * 	@return lines
	 */
	public MOrderLine[] getLines (String whereClause, String orderClause)
	{
		//red1 - using new Query class from Teo / Victor's MDDOrder.java implementation
		StringBuffer whereClauseFinal = new StringBuffer(MOrderLine.COLUMNNAME_C_Order_ID+"=? ");
		if (!Util.isEmpty(whereClause, true))
			whereClauseFinal.append(whereClause);
		if (orderClause.length() == 0)
			orderClause = MOrderLine.COLUMNNAME_Line;
		//
		List<MOrderLine> list = new Query(getCtx(), I_C_OrderLine.Table_Name, whereClauseFinal.toString(), get_TrxName())
										.setParameters(get_ID())
										.setOrderBy(orderClause)
										.list();
		for (MOrderLine ol : list) {
			ol.setHeaderInfo(this);
		}
		//
		return list.toArray(new MOrderLine[list.size()]);		
	}	//	getLines

	/**
	 * 	Get Lines of Order
	 * 	@param requery requery
	 * 	@param orderBy optional order by column
	 * 	@return lines
	 */
	public MOrderLine[] getLines (boolean requery, String orderBy)
	{
		if (m_lines != null && !requery) {
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		//
		String orderClause = "";
		if (orderBy != null && orderBy.length() > 0)
			orderClause += orderBy;
		else
			orderClause += "Line";
		m_lines = getLines(null, orderClause);
		return m_lines;
	}	//	getLines

	/**
	 * 	Get Lines of Order.
	 * 	(used by web store)
	 * 	@return lines
	 */
	public MOrderLine[] getLines()
	{
		return getLines(false, null);
	}	//	getLines
	
	/**
	 * 	Renumber Lines
	 *	@param step start and step
	 */
	public void renumberLines (int step)
	{
		int number = step;
		MOrderLine[] lines = getLines(true, null);	//	Line is default
		for (int i = 0; i < lines.length; i++)
		{
			MOrderLine line = lines[i];
			line.setLine(number);
			line.save(get_TrxName());
			number += step;
		}
		m_lines = null;
	}	//	renumberLines
	
	/**
	 * 	Does the Order Line belong to this Order
	 *	@param C_OrderLine_ID line
	 *	@return true if part of the order
	 */
	public boolean isOrderLine(int C_OrderLine_ID)
	{
		if (m_lines == null)
			getLines();
		for (int i = 0; i < m_lines.length; i++)
			if (m_lines[i].getC_OrderLine_ID() == C_OrderLine_ID)
				return true;
		return false;
	}	//	isOrderLine

	/**
	 * 	Get Taxes of Order
	 *	@param requery requery
	 *	@return array of taxes
	 */
	public MOrderTax[] getTaxes(boolean requery)
	{
		if (m_taxes != null && !requery)
			return m_taxes;
		//
		List<MOrderTax> list = new Query(getCtx(), I_C_OrderTax.Table_Name, "C_Order_ID=?", get_TrxName())
									.setParameters(get_ID())
									.list();
		m_taxes = list.toArray(new MOrderTax[list.size()]);
		return m_taxes;
	}	//	getTaxes
	
	
	/**
	 * 	Get Invoices of Order
	 * 	@return invoices
	 */
	public MInvoice[] getInvoices()
	{
		final String whereClause = "EXISTS (SELECT 1 FROM C_InvoiceLine il, C_OrderLine ol"
							        +" WHERE il.C_Invoice_ID=C_Invoice.C_Invoice_ID"
							        		+" AND il.C_OrderLine_ID=ol.C_OrderLine_ID"
							        		+" AND ol.C_Order_ID=?)";
		List<MInvoice> list = new Query(getCtx(), I_C_Invoice.Table_Name, whereClause, get_TrxName())
									.setParameters(get_ID())
									.setOrderBy("C_Invoice_ID DESC")
									.list();
		return list.toArray(new MInvoice[list.size()]);
	}	//	getInvoices

	/**
	 * 	Get latest Invoice of Order
	 * 	@return invoice id or 0
	 */
	public int getC_Invoice_ID()
	{
 		String sql = "SELECT C_Invoice_ID FROM C_Invoice "
			+ "WHERE C_Order_ID=? AND DocStatus IN ('CO','CL') "
			+ "ORDER BY C_Invoice_ID DESC";
		int C_Invoice_ID = DB.getSQLValue(get_TrxName(), sql, get_ID());
		return C_Invoice_ID;
	}	//	getC_Invoice_ID


	/**
	 * 	Get Shipments of Order
	 * 	@return shipments
	 */
	public MInOut[] getShipments()
	{
		final String whereClause = "EXISTS (SELECT 1 FROM M_InOutLine iol, C_OrderLine ol"
			+" WHERE iol.M_InOut_ID=M_InOut.M_InOut_ID"
			+" AND iol.C_OrderLine_ID=ol.C_OrderLine_ID"
			+" AND ol.C_Order_ID=?)";
		List<MInvoice> list = new Query(getCtx(), I_M_InOut.Table_Name, whereClause, get_TrxName())
									.setParameters(get_ID())
									.setOrderBy("M_InOut_ID DESC")
									.list();
		return list.toArray(new MInOut[list.size()]);
	}	//	getShipments

	/**
	 *	Get ISO Code of Currency
	 *	@return Currency ISO
	 */
	public String getCurrencyISO()
	{
		return MCurrency.getISO_Code (getCtx(), getC_Currency_ID());
	}	//	getCurrencyISO
	
	/**
	 * 	Get Currency Precision
	 *	@return precision
	 */
	public int getPrecision()
	{
		return MCurrency.getStdPrecision(getCtx(), getC_Currency_ID());
	}	//	getPrecision

	/**
	 * 	Get Document Status
	 *	@return Document Status Clear Text
	 */
	public String getDocStatusName()
	{
		return MRefList.getListName(getCtx(), 131, getDocStatus());
	}	//	getDocStatusName

	/**
	 * 	Set DocAction
	 *	@param DocAction doc action
	 */
	public void setDocAction (String DocAction)
	{
		setDocAction (DocAction, false);
	}	//	setDocAction

	/**
	 * 	Set DocAction
	 *	@param DocAction doc action
	 *	@param forceCreation force creation
	 */
	public void setDocAction (String DocAction, boolean forceCreation)
	{
		super.setDocAction (DocAction);
		m_forceCreation = forceCreation;
	}	//	setDocAction
	
	/**
	 * 	Set Processed.
	 * 	Propagate to Lines/Taxes
	 *	@param processed processed
	 */
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		String set = "SET Processed='"
			+ (processed ? "Y" : "N")
			+ "' WHERE C_Order_ID=" + getC_Order_ID();
		int noLine = DB.executeUpdateEx("UPDATE C_OrderLine " + set, get_TrxName());
		int noTax = DB.executeUpdateEx("UPDATE C_OrderTax " + set, get_TrxName());
		m_lines = null;
		m_taxes = null;
		log.fine("setProcessed - " + processed + " - Lines=" + noLine + ", Tax=" + noTax);
	}	//	setProcessed
	
	
	
	/**
	 * 	Validate Order Pay Schedule
	 *	@return pay schedule is valid
	 */
	public boolean validatePaySchedule()
	{
		MOrderPaySchedule[] schedule = MOrderPaySchedule.getOrderPaySchedule
			(getCtx(), getC_Order_ID(), 0, get_TrxName());
		log.fine("#" + schedule.length);
		if (schedule.length == 0)
		{
			setIsPayScheduleValid(false);
			return false;
		}
		//	Add up due amounts
		BigDecimal total = Env.ZERO;
		for (int i = 0; i < schedule.length; i++)
		{
			schedule[i].setParent(this);
			BigDecimal due = schedule[i].getDueAmt();
			if (due != null)
				total = total.add(due);
		}
		boolean valid = getGrandTotal().compareTo(total) == 0;
		setIsPayScheduleValid(valid);
		
		//	Update Schedule Lines
		for (int i = 0; i < schedule.length; i++)
		{
			if (schedule[i].isValid() != valid)
			{
				schedule[i].setIsValid(valid);
				schedule[i].save(get_TrxName());				
			}
		}
		return valid;
	}	//	validatePaySchedule

	
	/**************************************************************************
	 * 	Before Save
	 *	@param newRecord new
	 *	@return save
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		//	Client/Org Check
		if (getAD_Org_ID() == 0)
		{
			int context_AD_Org_ID = Env.getAD_Org_ID(getCtx());
			if (context_AD_Org_ID != 0)
			{
				setAD_Org_ID(context_AD_Org_ID);
				log.warning("Changed Org to Context=" + context_AD_Org_ID);
			}
		}
		if (getAD_Client_ID() == 0)
		{
			m_processMsg = "AD_Client_ID = 0";
			return false;
		}
		
		//	New Record Doc Type - make sure DocType set to 0
		if (newRecord && getC_DocType_ID() == 0)
			setC_DocType_ID (0);

		//	Default Warehouse
		if (getM_Warehouse_ID() == 0)
		{
			int ii = Env.getContextAsInt(getCtx(), "#M_Warehouse_ID");
			if (ii != 0)
				setM_Warehouse_ID(ii);
			else
			{
				throw new FillMandatoryException(COLUMNNAME_M_Warehouse_ID);
			}
		}
		MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
		//	Warehouse Org
		if (newRecord 
			|| is_ValueChanged("AD_Org_ID") || is_ValueChanged("M_Warehouse_ID"))
		{
			if (wh.getAD_Org_ID() != getAD_Org_ID())
				log.saveWarning("WarehouseOrgConflict", "");
		}

		boolean disallowNegInv = wh.isDisallowNegativeInv();
		String DeliveryRule = getDeliveryRule();
		if((disallowNegInv && DELIVERYRULE_Force.equals(DeliveryRule)) ||
				(DeliveryRule == null || DeliveryRule.length()==0))
			setDeliveryRule(DELIVERYRULE_Availability);
		
		//	Reservations in Warehouse
		if (!newRecord && is_ValueChanged("M_Warehouse_ID"))
		{
			MOrderLine[] lines = getLines(false,null);
			for (int i = 0; i < lines.length; i++)
			{
				if (!lines[i].canChangeWarehouse())
					return false;
			}
		}
		
		//	No Partner Info - set Template
		if (getC_BPartner_ID() == 0)
			setBPartner(MBPartner.getTemplate(getCtx(), getAD_Client_ID()));
		if (getC_BPartner_Location_ID() == 0)
			setBPartner(new MBPartner(getCtx(), getC_BPartner_ID(), null));
		//	No Bill - get from Ship
		if (getBill_BPartner_ID() == 0)
		{
			setBill_BPartner_ID(getC_BPartner_ID());
			setBill_Location_ID(getC_BPartner_Location_ID());
		}
		if (getBill_Location_ID() == 0)
			setBill_Location_ID(getC_BPartner_Location_ID());

		//	Default Price List
		if (getM_PriceList_ID() == 0)
		{
			int ii = DB.getSQLValueEx(null,
				"SELECT M_PriceList_ID FROM M_PriceList "
				+ "WHERE AD_Client_ID=? AND IsSOPriceList=? AND IsActive=?"
				+ "ORDER BY IsDefault DESC", getAD_Client_ID(), isSOTrx(), true);
			if (ii != 0)
				setM_PriceList_ID (ii);
		}
		//	Default Currency
		if (getC_Currency_ID() == 0)
		{
			String sql = "SELECT C_Currency_ID FROM M_PriceList WHERE M_PriceList_ID=?";
			int ii = DB.getSQLValue (null, sql, getM_PriceList_ID());
			if (ii != 0)
				setC_Currency_ID (ii);
			else
				setC_Currency_ID(Env.getContextAsInt(getCtx(), "#C_Currency_ID"));
		}

		//	Default Sales Rep
		if (getSalesRep_ID() == 0)
		{
			int ii = Env.getContextAsInt(getCtx(), "#SalesRep_ID");
			if (ii != 0)
				setSalesRep_ID (ii);
		}

		//	Default Document Type
		if (getC_DocTypeTarget_ID() == 0)
			setC_DocTypeTarget_ID(DocSubTypeSO_Standard);

		//	Default Payment Term
		if (getC_PaymentTerm_ID() == 0)
		{
			int ii = Env.getContextAsInt(getCtx(), "#C_PaymentTerm_ID");
			if (ii != 0)
				setC_PaymentTerm_ID(ii);
			else
			{
				String sql = "SELECT C_PaymentTerm_ID FROM C_PaymentTerm WHERE AD_Client_ID=? AND IsDefault='Y'";
				ii = DB.getSQLValue(null, sql, getAD_Client_ID());
				if (ii != 0)
					setC_PaymentTerm_ID (ii);
			}
		}
		
		return true;
	}	//	beforeSave
	
	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return true if can be saved
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success || newRecord)
			return success;
		
		// TODO: The changes here with UPDATE are not being saved on change log - audit problem  
		
		//	Propagate Description changes
		if (is_ValueChanged("Description") || is_ValueChanged("POReference"))
		{
			String sql = "UPDATE C_Invoice i"
				+ " SET (Description,POReference)="
					+ "(SELECT Description,POReference "
					+ "FROM C_Order o WHERE i.C_Order_ID=o.C_Order_ID) "
				+ "WHERE DocStatus NOT IN ('RE','CL') AND C_Order_ID=" + getC_Order_ID();
			int no = DB.executeUpdateEx(sql, get_TrxName());
			log.fine("Description -> #" + no);
		}

		//	Propagate Changes of Payment Info to existing (not reversed/closed) invoices
		if (is_ValueChanged("PaymentRule") || is_ValueChanged("C_PaymentTerm_ID")
			|| is_ValueChanged("C_Payment_ID")
			|| is_ValueChanged("C_CashLine_ID"))
		{
			String sql = "UPDATE C_Invoice i "
				+ "SET (PaymentRule,C_PaymentTerm_ID,C_Payment_ID,C_CashLine_ID)="
					+ "(SELECT PaymentRule,C_PaymentTerm_ID,C_Payment_ID,C_CashLine_ID "
					+ "FROM C_Order o WHERE i.C_Order_ID=o.C_Order_ID)"
				+ "WHERE DocStatus NOT IN ('RE','CL') AND C_Order_ID=" + getC_Order_ID();
			//	Don't touch Closed/Reversed entries
			int no = DB.executeUpdate(sql, get_TrxName());
			log.fine("Payment -> #" + no);
		}
	      
		//	Propagate Changes of Date Account to existing (not completed/reversed/closed) invoices
		if (is_ValueChanged("DateAcct"))
		{
			String sql = "UPDATE C_Invoice i "
				+ "SET (DateAcct)="
					+ "(SELECT DateAcct "
					+ "FROM C_Order o WHERE i.C_Order_ID=o.C_Order_ID)"
				+ "WHERE DocStatus NOT IN ('CO','RE','CL') AND Processed='N' AND C_Order_ID=" + getC_Order_ID();
			//	Don't touch Completed/Closed/Reversed entries
			int no = DB.executeUpdate(sql, get_TrxName());
			log.fine("DateAcct -> #" + no);
		}
	      
		//	Sync Lines
		if (   is_ValueChanged("AD_Org_ID")
		    || is_ValueChanged(MOrder.COLUMNNAME_C_BPartner_ID)
		    || is_ValueChanged(MOrder.COLUMNNAME_C_BPartner_Location_ID)
		    || is_ValueChanged(MOrder.COLUMNNAME_DateOrdered)
		    || is_ValueChanged(MOrder.COLUMNNAME_DatePromised)
		    || is_ValueChanged(MOrder.COLUMNNAME_M_Warehouse_ID)
		    || is_ValueChanged(MOrder.COLUMNNAME_M_Shipper_ID)
		    || is_ValueChanged(MOrder.COLUMNNAME_C_Currency_ID)) {
			MOrderLine[] lines = getLines();
			for (MOrderLine line : lines) {
				if (is_ValueChanged("AD_Org_ID"))
					line.setAD_Org_ID(getAD_Org_ID());
				if (is_ValueChanged(MOrder.COLUMNNAME_C_BPartner_ID))
					line.setC_BPartner_ID(getC_BPartner_ID());
				if (is_ValueChanged(MOrder.COLUMNNAME_C_BPartner_Location_ID))
					line.setC_BPartner_Location_ID(getC_BPartner_Location_ID());
				if (is_ValueChanged(MOrder.COLUMNNAME_DateOrdered))
					line.setDateOrdered(getDateOrdered());
				if (is_ValueChanged(MOrder.COLUMNNAME_DatePromised))
					line.setDatePromised(getDatePromised());
				if (is_ValueChanged(MOrder.COLUMNNAME_M_Warehouse_ID))
					line.setM_Warehouse_ID(getM_Warehouse_ID());
				if (is_ValueChanged(MOrder.COLUMNNAME_M_Shipper_ID))
					line.setM_Shipper_ID(getM_Shipper_ID());
				if (is_ValueChanged(MOrder.COLUMNNAME_C_Currency_ID))
					line.setC_Currency_ID(getC_Currency_ID());
				line.saveEx();
			}
		}
		//
		return true;
	}	//	afterSave
	
	/**
	 * 	Before Delete
	 *	@return true of it can be deleted
	 */
	protected boolean beforeDelete ()
	{
		if (isProcessed())
			return false;
		
		for (MOrderLine line : getLines()) {
			line.deleteEx(true);
		}
		return true;
	}	//	beforeDelete
	
	/**************************************************************************
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if performed
	 */
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;

	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	public boolean unlockIt()
	{
		log.info("unlockIt - " + toString());
		setProcessing(false);
		return true;
	}	//	unlockIt
	
	/**
	 * 	Invalidate Document
	 * 	@return true if success 
	 */
	public boolean invalidateIt()
	{
		log.info(toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt
	
	
	/**************************************************************************
	 *	Prepare Document
	 * 	@return new status (In Progress or Invalid) 
	 */
	public String prepareIt()
	{
		log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		MDocType dt = MDocType.get(getCtx(), getC_DocTypeTarget_ID());

		//	Std Period open?
		if (!MPeriod.isOpen(getCtx(), getDateAcct(), dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		
		//	Lines
		MOrderLine[] lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
		if (lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		
		// Bug 1564431
		if (getDeliveryRule() != null && getDeliveryRule().equals(MOrder.DELIVERYRULE_CompleteOrder)) 
		{
			for (int i = 0; i < lines.length; i++) 
			{
				MOrderLine line = lines[i];
				MProduct product = line.getProduct();
				if (product != null && product.isExcludeAutoDelivery())
				{
					m_processMsg = "@M_Product_ID@ "+product.getValue()+" @IsExcludeAutoDelivery@";
					return DocAction.STATUS_Invalid;
				}
			}
		}
		
		//	Convert DocType to Target
		if (getC_DocType_ID() != getC_DocTypeTarget_ID() )
		{
			//	Cannot change Std to anything else if different warehouses
			if (getC_DocType_ID() != 0)
			{
				MDocType dtOld = MDocType.get(getCtx(), getC_DocType_ID());
				if (MDocType.DOCSUBTYPESO_StandardOrder.equals(dtOld.getDocSubTypeSO())		//	From SO
					&& !MDocType.DOCSUBTYPESO_StandardOrder.equals(dt.getDocSubTypeSO()))	//	To !SO
				{
					for (int i = 0; i < lines.length; i++)
					{
						if (lines[i].getM_Warehouse_ID() != getM_Warehouse_ID())
						{
							log.warning("different Warehouse " + lines[i]);
							m_processMsg = "@CannotChangeDocType@";
							return DocAction.STATUS_Invalid;
						}
					}
				}
			}
			
			//	New or in Progress/Invalid
			if (DOCSTATUS_Drafted.equals(getDocStatus()) 
				|| DOCSTATUS_InProgress.equals(getDocStatus())
				|| DOCSTATUS_Invalid.equals(getDocStatus())
				|| getC_DocType_ID() == 0)
			{
				setC_DocType_ID(getC_DocTypeTarget_ID());
			}
			else	//	convert only if offer
			{
				if (dt.isOffer())
					setC_DocType_ID(getC_DocTypeTarget_ID());
				else
				{
					m_processMsg = "@CannotChangeDocType@";
					return DocAction.STATUS_Invalid;
				}
			}
		}	//	convert DocType

		//	Mandatory Product Attribute Set Instance
		String mandatoryType = "='Y'";	//	IN ('Y','S')
		String sql = "SELECT COUNT(*) "
			+ "FROM C_OrderLine ol"
			+ " INNER JOIN M_Product p ON (ol.M_Product_ID=p.M_Product_ID)" 
			+ " INNER JOIN M_AttributeSet pas ON (p.M_AttributeSet_ID=pas.M_AttributeSet_ID) "
			+ "WHERE pas.MandatoryType" + mandatoryType		
			+ " AND (ol.M_AttributeSetInstance_ID is NULL OR ol.M_AttributeSetInstance_ID = 0)"
			+ " AND ol.C_Order_ID=?";
		int no = DB.getSQLValue(get_TrxName(), sql, getC_Order_ID());
		if (no != 0)
		{
			m_processMsg = "@LinesWithoutProductAttribute@ (" + no + ")";
			return DocAction.STATUS_Invalid;
		}

		//	Lines
		if (explodeBOM())
			lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
		if (!reserveStock(dt, lines))
		{
			m_processMsg = "Cannot reserve Stock";
			return DocAction.STATUS_Invalid;
		}
		if (!calculateTaxTotal())
		{
			m_processMsg = "Error calculating tax";
			return DocAction.STATUS_Invalid;
		}
		
		if (   getGrandTotal().signum() != 0
			&& PAYMENTRULE_OnCredit.equals(getPaymentRule()))
		{
			if (!createPaySchedule())
			{
				m_processMsg = "@ErrorPaymentSchedule@";
				return DocAction.STATUS_Invalid;
			}
		} else {
			if (MOrderPaySchedule.getOrderPaySchedule(getCtx(), getC_Order_ID(), 0, get_TrxName()).length > 0) 
			{
				m_processMsg = "@ErrorPaymentSchedule@";
				return DocAction.STATUS_Invalid;
			}
		}
		
		//	Credit Check
		if (isSOTrx())
		{
			if (   MDocType.DOCSUBTYPESO_POSOrder.equals(dt.getDocSubTypeSO())
					&& PAYMENTRULE_Cash.equals(getPaymentRule())
					&& !MSysConfig.getBooleanValue("CHECK_CREDIT_ON_CASH_POS_ORDER", true, getAD_Client_ID(), getAD_Org_ID())) {
				// ignore -- don't validate for Cash POS Orders depending on sysconfig parameter
			} else if (MDocType.DOCSUBTYPESO_PrepayOrder.equals(dt.getDocSubTypeSO())
					&& !MSysConfig.getBooleanValue("CHECK_CREDIT_ON_PREPAY_ORDER", true, getAD_Client_ID(), getAD_Org_ID())) {
				// ignore -- don't validate Prepay Orders depending on sysconfig parameter
			} else {
				MBPartner bp = new MBPartner (getCtx(), getBill_BPartner_ID(), get_TrxName()); // bill bp is guaranteed on beforeSave

				if (MBPartner.SOCREDITSTATUS_CreditStop.equals(bp.getSOCreditStatus()))
				{
					m_processMsg = "@BPartnerCreditStop@ - @TotalOpenBalance@=" 
						+ bp.getTotalOpenBalance()
						+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
					return DocAction.STATUS_Invalid;
				}
				if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus()))
				{
					m_processMsg = "@BPartnerCreditHold@ - @TotalOpenBalance@=" 
						+ bp.getTotalOpenBalance() 
						+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
					return DocAction.STATUS_Invalid;
				}
				BigDecimal grandTotal = MConversionRate.convertBase(getCtx(), 
						getGrandTotal(), getC_Currency_ID(), getDateOrdered(), 
						getC_ConversionType_ID(), getAD_Client_ID(), getAD_Org_ID());
				if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus(grandTotal)))
				{
					m_processMsg = "@BPartnerOverOCreditHold@ - @TotalOpenBalance@=" 
						+ bp.getTotalOpenBalance() + ", @GrandTotal@=" + grandTotal
						+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
					return DocAction.STATUS_Invalid;
				}
			}
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		m_justPrepared = true;
	//	if (!DOCACTION_Complete.equals(getDocAction()))		don't set for just prepare 
	//		setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	/**
	 * 	Explode non stocked BOM.
	 * 	@return true if bom exploded
	 */
	private boolean explodeBOM()
	{
		boolean retValue = false;
		String where = "AND IsActive='Y' AND EXISTS "
			+ "(SELECT * FROM M_Product p WHERE C_OrderLine.M_Product_ID=p.M_Product_ID"
			+ " AND	p.IsBOM='Y' AND p.IsVerified='Y' AND p.IsStocked='N')";
		//
		String sql = "SELECT COUNT(*) FROM C_OrderLine "
			+ "WHERE C_Order_ID=? " + where; 
		int count = DB.getSQLValue(get_TrxName(), sql, getC_Order_ID());
		while (count != 0)
		{
			retValue = true;
			renumberLines (1000);		//	max 999 bom items	

			//	Order Lines with non-stocked BOMs
			MOrderLine[] lines = getLines (where, MOrderLine.COLUMNNAME_Line);
			for (int i = 0; i < lines.length; i++)
			{
				MOrderLine line = lines[i];
				MProduct product = MProduct.get (getCtx(), line.getM_Product_ID());
				log.fine(product.getName());
				//	New Lines
				int lineNo = line.getLine ();
				//find default BOM with valid dates and to this product
				MPPProductBOM bom = MPPProductBOM.get(product, getAD_Org_ID(),getDatePromised(), get_TrxName());
				if(bom != null)
				{	
					MPPProductBOMLine[] bomlines = bom.getLines(getDatePromised());
					for (int j = 0; j < bomlines.length; j++)
					{
						MPPProductBOMLine bomline = bomlines[j];
						MOrderLine newLine = new MOrderLine (this);
						newLine.setLine (++lineNo);
						newLine.setM_Product_ID (bomline.getM_Product_ID ());
						newLine.setC_UOM_ID (bomline.getC_UOM_ID ());
						newLine.setQty (line.getQtyOrdered ().multiply (
							bomline.getQtyBOM()));
						if (bomline.getDescription () != null)
							newLine.setDescription (bomline.getDescription ());
						//
						newLine.setPrice ();
						newLine.save (get_TrxName());
					}
				}	
				
				/*MProductBOM[] boms = MProductBOM.getBOMLines (product);
				for (int j = 0; j < boms.length; j++)
				{
					//MProductBOM bom = boms[j];
					MPPProductBOMLine bom = boms[j];
					MOrderLine newLine = new MOrderLine (this);
					newLine.setLine (++lineNo);
					//newLine.setM_Product_ID (bom.getProduct ()
					//	.getM_Product_ID ());
					newLine.setM_Product_ID (bom.getM_Product_ID ());
					//newLine.setC_UOM_ID (bom.getProduct ().getC_UOM_ID ());
					newLine.setC_UOM_ID (bom.getC_UOM_ID ());
					//newLine.setQty (line.getQtyOrdered ().multiply (
					//		bom.getBOMQty ()));
					newLine.setQty (line.getQtyOrdered ().multiply (
						bom.getQtyBOM()));
					if (bom.getDescription () != null)
						newLine.setDescription (bom.getDescription ());
					//
					newLine.setPrice ();
					newLine.save (get_TrxName());
				}*/
				
				//	Convert into Comment Line
				line.setM_Product_ID (0);
				line.setM_AttributeSetInstance_ID (0);
				line.setPrice (Env.ZERO);
				line.setPriceLimit (Env.ZERO);
				line.setPriceList (Env.ZERO);
				line.setLineNetAmt (Env.ZERO);
				line.setFreightAmt (Env.ZERO);
				//
				String description = product.getName ();
				if (product.getDescription () != null)
					description += " " + product.getDescription ();
				if (line.getDescription () != null)
					description += " " + line.getDescription ();
				line.setDescription (description);
				line.save (get_TrxName());
			}	//	for all lines with BOM

			m_lines = null;		//	force requery
			count = DB.getSQLValue (get_TrxName(), sql, getC_Invoice_ID ());
			renumberLines (10);
		}	//	while count != 0
		return retValue;
	}	//	explodeBOM


	/**
	 * 	Reserve Inventory.
	 * 	Counterpart: MInOut.completeIt()
	 * 	@param dt document type or null
	 * 	@param lines order lines (ordered by M_Product_ID for deadlock prevention)
	 * 	@return true if (un) reserved
	 */
	private boolean reserveStock (MDocType dt, MOrderLine[] lines)
	{
		if (dt == null)
			dt = MDocType.get(getCtx(), getC_DocType_ID());

		//	Binding
		boolean binding = !dt.isProposal();
		//	Not binding - i.e. Target=0
		if (DOCACTION_Void.equals(getDocAction())
			//	Closing Binding Quotation
			|| (MDocType.DOCSUBTYPESO_Quotation.equals(dt.getDocSubTypeSO()) 
				&& DOCACTION_Close.equals(getDocAction())) 
			) // || isDropShip() )
			binding = false;
		boolean isSOTrx = isSOTrx();
		log.fine("Binding=" + binding + " - IsSOTrx=" + isSOTrx);
		//	Force same WH for all but SO/PO
		int header_M_Warehouse_ID = getM_Warehouse_ID();
		if (MDocType.DOCSUBTYPESO_StandardOrder.equals(dt.getDocSubTypeSO())
			|| MDocType.DOCBASETYPE_PurchaseOrder.equals(dt.getDocBaseType()))
			header_M_Warehouse_ID = 0;		//	don't enforce
		
		BigDecimal Volume = Env.ZERO;
		BigDecimal Weight = Env.ZERO;
		
		//	Always check and (un) Reserve Inventory		
		for (int i = 0; i < lines.length; i++)
		{
			MOrderLine line = lines[i];
			//	Check/set WH/Org
			if (header_M_Warehouse_ID != 0)	//	enforce WH
			{
				if (header_M_Warehouse_ID != line.getM_Warehouse_ID())
					line.setM_Warehouse_ID(header_M_Warehouse_ID);
				if (getAD_Org_ID() != line.getAD_Org_ID())
					line.setAD_Org_ID(getAD_Org_ID());
			}
			//	Binding
			BigDecimal target = binding ? line.getQtyOrdered() : Env.ZERO; 
			BigDecimal difference = target
				.subtract(line.getQtyReserved())
				.subtract(line.getQtyDelivered()); 
			if (difference.signum() == 0)
			{
				MProduct product = line.getProduct();
				if (product != null)
				{
					Volume = Volume.add(product.getVolume().multiply(line.getQtyOrdered()));
					Weight = Weight.add(product.getWeight().multiply(line.getQtyOrdered()));
				}
				continue;
			}
			
			log.fine("Line=" + line.getLine() 
				+ " - Target=" + target + ",Difference=" + difference
				+ " - Ordered=" + line.getQtyOrdered() 
				+ ",Reserved=" + line.getQtyReserved() + ",Delivered=" + line.getQtyDelivered());

			//	Check Product - Stocked and Item
			MProduct product = line.getProduct();
			if (product != null) 
			{
				if (product.isStocked())
				{
					BigDecimal ordered = isSOTrx ? Env.ZERO : difference;
					BigDecimal reserved = isSOTrx ? difference : Env.ZERO;
					int M_Locator_ID = 0; 
					//	Get Locator to reserve
					if (line.getM_AttributeSetInstance_ID() != 0)	//	Get existing Location
						M_Locator_ID = MStorage.getM_Locator_ID (line.getM_Warehouse_ID(), 
							line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(), 
							ordered, get_TrxName());
					//	Get default Location
					if (M_Locator_ID == 0)
					{
						// try to take default locator for product first
						// if it is from the selected warehouse
						MWarehouse wh = MWarehouse.get(getCtx(), line.getM_Warehouse_ID());
						M_Locator_ID = product.getM_Locator_ID();
						if (M_Locator_ID!=0) {
							MLocator locator = new MLocator(getCtx(), product.getM_Locator_ID(), get_TrxName());
							//product has default locator defined but is not from the order warehouse
							if(locator.getM_Warehouse_ID()!=wh.get_ID()) {
								M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
							}
						} else {
							M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
						}
					}
					//	Update Storage
					if (!MStorage.add(getCtx(), line.getM_Warehouse_ID(), M_Locator_ID, 
						line.getM_Product_ID(), 
						line.getM_AttributeSetInstance_ID(), line.getM_AttributeSetInstance_ID(),
						Env.ZERO, reserved, ordered, get_TrxName()))
						return false;
				}	//	stockec
				//	update line
				line.setQtyReserved(line.getQtyReserved().add(difference));
				if (!line.save(get_TrxName()))
					return false;
				//
				Volume = Volume.add(product.getVolume().multiply(line.getQtyOrdered()));
				Weight = Weight.add(product.getWeight().multiply(line.getQtyOrdered()));
			}	//	product
		}	//	reverse inventory
		
		setVolume(Volume);
		setWeight(Weight);
		return true;
	}	//	reserveStock

	/**
	 * 	Calculate Tax and Total
	 * 	@return true if tax total calculated
	 */
	public boolean calculateTaxTotal()
	{
		log.fine("");
		//	Delete Taxes
		DB.executeUpdateEx("DELETE C_OrderTax WHERE C_Order_ID=" + getC_Order_ID(), get_TrxName());
		m_taxes = null;
		
		//	Lines
		BigDecimal totalLines = Env.ZERO;
		ArrayList<Integer> taxList = new ArrayList<Integer>();
		MOrderLine[] lines = getLines();
		for (int i = 0; i < lines.length; i++)
		{
			MOrderLine line = lines[i];
			Integer taxID = new Integer(line.getC_Tax_ID());
			if (!taxList.contains(taxID))
			{
				MOrderTax oTax = MOrderTax.get (line, getPrecision(), 
					false, get_TrxName());	//	current Tax
				oTax.setIsTaxIncluded(isTaxIncluded());
				if (!oTax.calculateTaxFromLines())
					return false;
				if (!oTax.save(get_TrxName()))
					return false;
				taxList.add(taxID);
			}
			totalLines = totalLines.add(line.getLineNetAmt());
		}
		
		//	Taxes
		BigDecimal grandTotal = totalLines;
		MOrderTax[] taxes = getTaxes(true);
		for (int i = 0; i < taxes.length; i++)
		{
			MOrderTax oTax = taxes[i];
			MTax tax = oTax.getTax();
			if (tax.isSummary())
			{
				MTax[] cTaxes = tax.getChildTaxes(false);
				for (int j = 0; j < cTaxes.length; j++)
				{
					MTax cTax = cTaxes[j];
					BigDecimal taxAmt = cTax.calculateTax(oTax.getTaxBaseAmt(), isTaxIncluded(), getPrecision());
					//
					MOrderTax newOTax = new MOrderTax(getCtx(), 0, get_TrxName());
					newOTax.setClientOrg(this);
					newOTax.setC_Order_ID(getC_Order_ID());
					newOTax.setC_Tax_ID(cTax.getC_Tax_ID());
					newOTax.setPrecision(getPrecision());
					newOTax.setIsTaxIncluded(isTaxIncluded());
					newOTax.setTaxBaseAmt(oTax.getTaxBaseAmt());
					newOTax.setTaxAmt(taxAmt);
					if (!newOTax.save(get_TrxName()))
						return false;
					//
					if (!isTaxIncluded())
						grandTotal = grandTotal.add(taxAmt);
				}
				if (!oTax.delete(true, get_TrxName()))
					return false;
				if (!oTax.save(get_TrxName()))
					return false;
			}
			else
			{
				if (!isTaxIncluded())
					grandTotal = grandTotal.add(oTax.getTaxAmt());
			}
		}		
		//
		setTotalLines(totalLines);
		setGrandTotal(grandTotal);
		return true;
	}	//	calculateTaxTotal
	
	
	/**
	 * 	(Re) Create Pay Schedule
	 *	@return true if valid schedule
	 */
	private boolean createPaySchedule()
	{
		if (getC_PaymentTerm_ID() == 0)
			return false;
		MPaymentTerm pt = new MPaymentTerm(getCtx(), getC_PaymentTerm_ID(), null);
		log.fine(pt.toString());

		int numSchema = pt.getSchedule(false).length;
		
		MOrderPaySchedule[] schedule = MOrderPaySchedule.getOrderPaySchedule
			(getCtx(), getC_Order_ID(), 0, get_TrxName());
		
		if (schedule.length > 0) {
			if (numSchema == 0)
				return false; // created a schedule for a payment term that doesn't manage schedule
			return validatePaySchedule();
		} else {
			boolean isValid = pt.applyOrder(this);		//	calls validate pay schedule
			if (numSchema == 0)
				return true; // no schedule, no schema, OK
			else
				return isValid;
		}
	}	//	createPaySchedule
	
	
	/**
	 * 	Approve Document
	 * 	@return true if success 
	 */
	public boolean  approveIt()
	{
		log.info("approveIt - " + toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	/**
	 * 	Reject Approval
	 * 	@return true if success 
	 */
	public boolean rejectIt()
	{
		log.info("rejectIt - " + toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt
	
	
	/**************************************************************************
	 * 	Complete Document
	 * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
	 */
	public String completeIt()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		String DocSubTypeSO = dt.getDocSubTypeSO();
		
		//	Just prepare
		if (DOCACTION_Prepare.equals(getDocAction()))
		{
			setProcessed(false);
			return DocAction.STATUS_InProgress;
		}
		//	Offers
		if (MDocType.DOCSUBTYPESO_Proposal.equals(DocSubTypeSO)
			|| MDocType.DOCSUBTYPESO_Quotation.equals(DocSubTypeSO)) 
		{
			//	Binding
			if (MDocType.DOCSUBTYPESO_Quotation.equals(DocSubTypeSO))
				reserveStock(dt, getLines(true, MOrderLine.COLUMNNAME_M_Product_ID));
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
			if (m_processMsg != null)
				return DocAction.STATUS_Invalid;
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
			if (m_processMsg != null)
				return DocAction.STATUS_Invalid;
			// Set the definite document number after completed (if needed)
			setDefiniteDocumentNo();
			setProcessed(true);
			return DocAction.STATUS_Completed;
		}
		//	Waiting Payment - until we have a payment
		if (!m_forceCreation 
			&& MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO) 
			&& getC_Payment_ID() == 0 && getC_CashLine_ID() == 0)
		{
			setProcessed(true);
			return DocAction.STATUS_WaitingPayment;
		}
		
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		getLines(true,null);
		log.info(toString());
		StringBuffer info = new StringBuffer();
		
		boolean realTimePOS = MSysConfig.getBooleanValue("REAL_TIME_POS", false , getAD_Client_ID());
		
		//	Create SO Shipment - Force Shipment
		MInOut shipment = null;
		if (MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO)		//	(W)illCall(I)nvoice
			|| MDocType.DOCSUBTYPESO_WarehouseOrder.equals(DocSubTypeSO)	//	(W)illCall(P)ickup	
			|| MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO)			//	(W)alkIn(R)eceipt
			|| MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO)) 
		{
			if (!DELIVERYRULE_Force.equals(getDeliveryRule()))
			{
				MWarehouse wh = new MWarehouse (getCtx(), getM_Warehouse_ID(), get_TrxName());
				if (!wh.isDisallowNegativeInv())
					setDeliveryRule(DELIVERYRULE_Force);
			}
			//
			shipment = createShipment (dt, realTimePOS ? null : getDateOrdered());
			if (shipment == null)
				return DocAction.STATUS_Invalid;
			info.append("@M_InOut_ID@: ").append(shipment.getDocumentNo());
			String msg = shipment.getProcessMsg();
			if (msg != null && msg.length() > 0)
				info.append(" (").append(msg).append(")");
		}	//	Shipment
		

		//	Create SO Invoice - Always invoice complete Order
		if ( MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO)
			|| MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO) 	
			|| MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO)) 
		{
			MInvoice invoice = createInvoice (dt, shipment, realTimePOS ? null : getDateOrdered());
			if (invoice == null)
				return DocAction.STATUS_Invalid;
			info.append(" - @C_Invoice_ID@: ").append(invoice.getDocumentNo());
			String msg = invoice.getProcessMsg();
			if (msg != null && msg.length() > 0)
				info.append(" (").append(msg).append(")");
		}	//	Invoice
		
		//	Counter Documents
		MOrder counter = createCounterDoc();
		if (counter != null)
			info.append(" - @CounterDoc@: @Order@=").append(counter.getDocumentNo());
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			if (info.length() > 0)
				info.append(" - ");
			info.append(valid);
			m_processMsg = info.toString();
			return DocAction.STATUS_Invalid;
		}

		// Set the definite document number after completed (if needed)
		setDefiniteDocumentNo();

		setProcessed(true);	
		m_processMsg = info.toString();
		//
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	/**
	 * 	Set the definite document number after completed
	 */
	private void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			/* a42niem - BF IDEMPIERE-63 - check if document has been completed before */ 
			if (this.getProcessedOn().compareTo(Env.ZERO) == 0) {
				setDateOrdered(new Timestamp (System.currentTimeMillis()));
			}
		}
		if (dt.isOverwriteSeqOnComplete()) {
			/* a42niem - BF IDEMPIERE-63 - check if document has been completed before */ 
			if (this.getProcessedOn().compareTo(Env.ZERO) == 0) {
				String value = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
				if (value != null)
					setDocumentNo(value);
			}
		}
	}

	/**
	 * 	Create Shipment
	 *	@param dt order document type
	 *	@param movementDate optional movement date (default today)
	 *	@return shipment or null
	 */
	private MInOut createShipment(MDocType dt, Timestamp movementDate)
	{
		log.info("For " + dt);
		MInOut shipment = new MInOut (this, dt.getC_DocTypeShipment_ID(), movementDate);
	//	shipment.setDateAcct(getDateAcct());
		if (!shipment.save(get_TrxName()))
		{
			m_processMsg = "Could not create Shipment";
			return null;
		}
		//
		MOrderLine[] oLines = getLines(true, null);
		for (int i = 0; i < oLines.length; i++)
		{
			MOrderLine oLine = oLines[i];
			//
			MInOutLine ioLine = new MInOutLine(shipment);
			//	Qty = Ordered - Delivered
			BigDecimal MovementQty = oLine.getQtyOrdered().subtract(oLine.getQtyDelivered()); 
			//	Location
			int M_Locator_ID = MStorage.getM_Locator_ID (oLine.getM_Warehouse_ID(), 
					oLine.getM_Product_ID(), oLine.getM_AttributeSetInstance_ID(), 
					MovementQty, get_TrxName());
			if (M_Locator_ID == 0)		//	Get default Location
			{
				MWarehouse wh = MWarehouse.get(getCtx(), oLine.getM_Warehouse_ID());
				M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
			}
			//
			ioLine.setOrderLine(oLine, M_Locator_ID, MovementQty);
			ioLine.setQty(MovementQty);
			if (oLine.getQtyEntered().compareTo(oLine.getQtyOrdered()) != 0)
				ioLine.setQtyEntered(MovementQty
					.multiply(oLine.getQtyEntered())
					.divide(oLine.getQtyOrdered(), 6, BigDecimal.ROUND_HALF_UP));
			if (!ioLine.save(get_TrxName()))
			{
				m_processMsg = "Could not create Shipment Line";
				return null;
			}
		}
		//	Manually Process Shipment
		shipment.processIt(DocAction.ACTION_Complete);
		shipment.saveEx(get_TrxName());
		if (!DOCSTATUS_Completed.equals(shipment.getDocStatus()))
		{
			m_processMsg = "@M_InOut_ID@: " + shipment.getProcessMsg();
			return null;
		}
		return shipment;
	}	//	createShipment

	/**
	 * 	Create Invoice
	 *	@param dt order document type
	 *	@param shipment optional shipment
	 *	@param invoiceDate invoice date
	 *	@return invoice or null
	 */
	private MInvoice createInvoice (MDocType dt, MInOut shipment, Timestamp invoiceDate)
	{
		log.info(dt.toString());
		MInvoice invoice = new MInvoice (this, dt.getC_DocTypeInvoice_ID(), invoiceDate);
		if (!invoice.save(get_TrxName()))
		{
			m_processMsg = "Could not create Invoice";
			return null;
		}
		
		//	If we have a Shipment - use that as a base
		if (shipment != null)
		{
			if (!INVOICERULE_AfterDelivery.equals(getInvoiceRule()))
				setInvoiceRule(INVOICERULE_AfterDelivery);
			//
			MInOutLine[] sLines = shipment.getLines(false);
			for (int i = 0; i < sLines.length; i++)
			{
				MInOutLine sLine = sLines[i];
				//
				MInvoiceLine iLine = new MInvoiceLine(invoice);
				iLine.setShipLine(sLine);
				//	Qty = Delivered	
				if (sLine.sameOrderLineUOM())
					iLine.setQtyEntered(sLine.getQtyEntered());
				else
					iLine.setQtyEntered(sLine.getMovementQty());
				iLine.setQtyInvoiced(sLine.getMovementQty());
				if (!iLine.save(get_TrxName()))
				{
					m_processMsg = "Could not create Invoice Line from Shipment Line";
					return null;
				}
				//
				sLine.setIsInvoiced(true);
				if (!sLine.save(get_TrxName()))
				{
					log.warning("Could not update Shipment line: " + sLine);
				}
			}
		}
		else	//	Create Invoice from Order
		{
			if (!INVOICERULE_Immediate.equals(getInvoiceRule()))
				setInvoiceRule(INVOICERULE_Immediate);
			//
			MOrderLine[] oLines = getLines();
			for (int i = 0; i < oLines.length; i++)
			{
				MOrderLine oLine = oLines[i];
				//
				MInvoiceLine iLine = new MInvoiceLine(invoice);
				iLine.setOrderLine(oLine);
				//	Qty = Ordered - Invoiced	
				iLine.setQtyInvoiced(oLine.getQtyOrdered().subtract(oLine.getQtyInvoiced()));
				if (oLine.getQtyOrdered().compareTo(oLine.getQtyEntered()) == 0)
					iLine.setQtyEntered(iLine.getQtyInvoiced());
				else
					iLine.setQtyEntered(iLine.getQtyInvoiced().multiply(oLine.getQtyEntered())
						.divide(oLine.getQtyOrdered(), 12, BigDecimal.ROUND_HALF_UP));
				if (!iLine.save(get_TrxName()))
				{
					m_processMsg = "Could not create Invoice Line from Order Line";
					return null;
				}
			}
		}
		
		// Copy payment schedule from order to invoice if any
		for (MOrderPaySchedule ops : MOrderPaySchedule.getOrderPaySchedule(getCtx(), getC_Order_ID(), 0, get_TrxName())) {
			MInvoicePaySchedule ips = new MInvoicePaySchedule(getCtx(), 0, get_TrxName());
			PO.copyValues(ops, ips);
			ips.setC_Invoice_ID(invoice.getC_Invoice_ID());
			ips.setAD_Org_ID(ops.getAD_Org_ID());
			ips.setProcessing(ops.isProcessing());
			ips.setIsActive(ops.isActive());
			if (!ips.save()) {
				m_processMsg = "ERROR: creating pay schedule for invoice from : "+ ops.toString();
				return null;
			}
		}
		
		//	Manually Process Invoice
		invoice.processIt(DocAction.ACTION_Complete);
		invoice.saveEx(get_TrxName());
		setC_CashLine_ID(invoice.getC_CashLine_ID());
		if (!DOCSTATUS_Completed.equals(invoice.getDocStatus()))
		{
			m_processMsg = "@C_Invoice_ID@: " + invoice.getProcessMsg();
			return null;
		}
		return invoice;
	}	//	createInvoice
	
	/**
	 * 	Create Counter Document
	 * 	@return counter order
	 */
	private MOrder createCounterDoc()
	{
		//	Is this itself a counter doc ?
		if (getRef_Order_ID() != 0)
			return null;
		
		//	Org Must be linked to BPartner
		MOrg org = MOrg.get(getCtx(), getAD_Org_ID());
		int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(get_TrxName()); 
		if (counterC_BPartner_ID == 0)
			return null;
		//	Business Partner needs to be linked to Org
		MBPartner bp = new MBPartner (getCtx(), getC_BPartner_ID(), get_TrxName());
		int counterAD_Org_ID = bp.getAD_OrgBP_ID_Int(); 
		if (counterAD_Org_ID == 0)
			return null;
		
		MBPartner counterBP = new MBPartner (getCtx(), counterC_BPartner_ID, null);
		MOrgInfo counterOrgInfo = MOrgInfo.get(getCtx(), counterAD_Org_ID, get_TrxName());
		log.info("Counter BP=" + counterBP.getName());

		//	Document Type
		int C_DocTypeTarget_ID = 0;
		MDocTypeCounter counterDT = MDocTypeCounter.getCounterDocType(getCtx(), getC_DocType_ID());
		if (counterDT != null)
		{
			log.fine(counterDT.toString());
			if (!counterDT.isCreateCounter() || !counterDT.isValid())
				return null;
			C_DocTypeTarget_ID = counterDT.getCounter_C_DocType_ID();
		}
		else	//	indirect
		{
			C_DocTypeTarget_ID = MDocTypeCounter.getCounterDocType_ID(getCtx(), getC_DocType_ID());
			log.fine("Indirect C_DocTypeTarget_ID=" + C_DocTypeTarget_ID);
			if (C_DocTypeTarget_ID <= 0)
				return null;
		}
		//	Deep Copy
		MOrder counter = copyFrom (this, getDateOrdered(), 
			C_DocTypeTarget_ID, !isSOTrx(), true, false, get_TrxName());
		//
		counter.setAD_Org_ID(counterAD_Org_ID);
		counter.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());
		//
		counter.setBPartner(counterBP);
		counter.setDatePromised(getDatePromised());		// default is date ordered 
		//	Refernces (Should not be required
		counter.setSalesRep_ID(getSalesRep_ID());
		counter.save(get_TrxName());
		
		//	Update copied lines
		MOrderLine[] counterLines = counter.getLines(true, null);
		for (int i = 0; i < counterLines.length; i++)
		{
			MOrderLine counterLine = counterLines[i];
			counterLine.setOrder(counter);	//	copies header values (BP, etc.)
			counterLine.setPrice();
			counterLine.setTax();
			counterLine.save(get_TrxName());
		}
		log.fine(counter.toString());
		
		//	Document Action
		if (counterDT != null)
		{
			if (counterDT.getDocAction() != null)
			{
				counter.setDocAction(counterDT.getDocAction());
				counter.processIt(counterDT.getDocAction());
				counter.save(get_TrxName());
			}
		}
		return counter;
	}	//	createCounterDoc
	
	/**
	 * 	Void Document.
	 * 	Set Qtys to 0 - Sales: reverse all documents
	 * 	@return true if success 
	 */
	public boolean voidIt()
	{
		log.info(toString());
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;

		MOrderLine[] lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
		for (int i = 0; i < lines.length; i++)
		{
			MOrderLine line = lines[i];
			BigDecimal old = line.getQtyOrdered();
			if (old.signum() != 0)
			{
				line.addDescription(Msg.getMsg(getCtx(), "Voided") + " (" + old + ")");
				line.setQty(Env.ZERO);
				line.setLineNetAmt(Env.ZERO);
				line.save(get_TrxName());
			}
			//AZ Goodwill	
			if (!isSOTrx())
			{
				deleteMatchPOCostDetail(line);
			}
		}
		
		// update taxes
		MOrderTax[] taxes = getTaxes(true);
		for (MOrderTax tax : taxes )
		{
			if ( !(tax.calculateTaxFromLines() && tax.save()) )
				return false;
		}
		
		addDescription(Msg.getMsg(getCtx(), "Voided"));
		//	Clear Reservations
		if (!reserveStock(null, lines))
		{
			m_processMsg = "Cannot unreserve Stock (void)";
			return false;
		}
		
		// UnLink All Requisitions
		MRequisitionLine.unlinkC_Order_ID(getCtx(), get_ID(), get_TrxName());
		
		if (!createReversals())
			return false;
		
		/* globalqss - 2317928 - Reactivating/Voiding order must reset posted */
		MFactAcct.deleteEx(MOrder.Table_ID, getC_Order_ID(), get_TrxName());
		setPosted(false);
		
		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}	//	voidIt
	
	/**
	 * 	Create Shipment/Invoice Reversals
	 * 	@return true if success
	 */
	private boolean createReversals()
	{
		//	Cancel only Sales 
		if (!isSOTrx())
			return true;
		
		log.info("createReversals");
		StringBuffer info = new StringBuffer();
		
		//	Reverse All *Shipments*
		info.append("@M_InOut_ID@:");
		MInOut[] shipments = getShipments();
		for (int i = 0; i < shipments.length; i++)
		{
			MInOut ship = shipments[i];
			//	if closed - ignore
			if (MInOut.DOCSTATUS_Closed.equals(ship.getDocStatus())
				|| MInOut.DOCSTATUS_Reversed.equals(ship.getDocStatus())
				|| MInOut.DOCSTATUS_Voided.equals(ship.getDocStatus()) )
				continue;
			ship.set_TrxName(get_TrxName());
		
			//	If not completed - void - otherwise reverse it
			if (!MInOut.DOCSTATUS_Completed.equals(ship.getDocStatus()))
			{
				if (ship.voidIt())
					ship.setDocStatus(MInOut.DOCSTATUS_Voided);
			}
			else if (ship.reverseCorrectIt())	//	completed shipment
			{
				ship.setDocStatus(MInOut.DOCSTATUS_Reversed);
				info.append(" ").append(ship.getDocumentNo());
			}
			else
			{
				m_processMsg = "Could not reverse Shipment " + ship;
				return false;
			}
			ship.setDocAction(MInOut.DOCACTION_None);
			ship.save(get_TrxName());
		}	//	for all shipments
			
		//	Reverse All *Invoices*
		info.append(" - @C_Invoice_ID@:");
		MInvoice[] invoices = getInvoices();
		for (int i = 0; i < invoices.length; i++)
		{
			MInvoice invoice = invoices[i];
			//	if closed - ignore
			if (MInvoice.DOCSTATUS_Closed.equals(invoice.getDocStatus())
				|| MInvoice.DOCSTATUS_Reversed.equals(invoice.getDocStatus())
				|| MInvoice.DOCSTATUS_Voided.equals(invoice.getDocStatus()) )
				continue;			
			invoice.set_TrxName(get_TrxName());
			
			//	If not completed - void - otherwise reverse it
			if (!MInvoice.DOCSTATUS_Completed.equals(invoice.getDocStatus()))
			{
				if (invoice.voidIt())
					invoice.setDocStatus(MInvoice.DOCSTATUS_Voided);
			}
			else if (invoice.reverseCorrectIt())	//	completed invoice
			{
				invoice.setDocStatus(MInvoice.DOCSTATUS_Reversed);
				info.append(" ").append(invoice.getDocumentNo());
			}
			else
			{
				m_processMsg = "Could not reverse Invoice " + invoice;
				return false;
			}
			invoice.setDocAction(MInvoice.DOCACTION_None);
			invoice.save(get_TrxName());
		}	//	for all shipments
		
		m_processMsg = info.toString();
		return true;
	}	//	createReversals
	
	
	/**
	 * 	Close Document.
	 * 	Cancel not delivered Qunatities
	 * 	@return true if success 
	 */
	public boolean closeIt()
	{
		log.info(toString());
		// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
		if (m_processMsg != null)
			return false;
		
		//	Close Not delivered Qty - SO/PO
		MOrderLine[] lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
		for (int i = 0; i < lines.length; i++)
		{
			MOrderLine line = lines[i];
			BigDecimal old = line.getQtyOrdered();
			if (old.compareTo(line.getQtyDelivered()) != 0)
			{
				line.setQtyLostSales(line.getQtyOrdered().subtract(line.getQtyDelivered()));
				line.setQtyOrdered(line.getQtyDelivered());
				//	QtyEntered unchanged
				line.addDescription("Close (" + old + ")");
				line.save(get_TrxName());
			}
		}
		//	Clear Reservations
		if (!reserveStock(null, lines))
		{
			m_processMsg = "Cannot unreserve Stock (close)";
			return false;
		}
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;
		return true;
	}	//	closeIt
	
	/**
	 * @author: phib
	 * re-open a closed order
	 * (reverse steps of close())
	 */
	public String reopenIt() {
		log.info(toString());
		if (!MOrder.DOCSTATUS_Closed.equals(getDocStatus()))
		{
			return "Not closed - can't reopen";
		}
		
		//	
		MOrderLine[] lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
		for (int i = 0; i < lines.length; i++)
		{
			MOrderLine line = lines[i];
			if (Env.ZERO.compareTo(line.getQtyLostSales()) != 0)
			{
				line.setQtyOrdered(line.getQtyLostSales().add(line.getQtyDelivered()));
				line.setQtyLostSales(Env.ZERO);
				//	QtyEntered unchanged
				
				// Strip Close() tags from description
				String desc = line.getDescription();
				if (desc == null)
					desc = "";
				Pattern pattern = Pattern.compile("( \\| )?Close \\(.*\\)");
				String[] parts = pattern.split(desc);
				desc = "";
				for (String s : parts) {
					desc = desc.concat(s);
				}
				line.setDescription(desc);
				if (!line.save(get_TrxName()))
					return "Couldn't save orderline";
			}
		}
		//	Clear Reservations
		if (!reserveStock(null, lines))
		{
			m_processMsg = "Cannot unreserve Stock (close)";
			return "Failed to update reservations";
		}

		setDocStatus(MOrder.DOCSTATUS_Completed);
		setDocAction(DOCACTION_Close);
		if (!this.save(get_TrxName()))
			return "Couldn't save reopened order";
		else
			return "";
	}	//	reopenIt
	/**
	 * 	Reverse Correction - same void
	 * 	@return true if success 
	 */
	public boolean reverseCorrectIt()
	{
		log.info(toString());
		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		return voidIt();
	}	//	reverseCorrectionIt
	
	/**
	 * 	Reverse Accrual - none
	 * 	@return false 
	 */
	public boolean reverseAccrualIt()
	{
		log.info(toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		return false;
	}	//	reverseAccrualIt
	
	/**
	 * 	Re-activate.
	 * 	@return true if success 
	 */
	public boolean reActivateIt()
	{
		log.info(toString());
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;	
				
		
		
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		String DocSubTypeSO = dt.getDocSubTypeSO();
		
		//	Replace Prepay with POS to revert all doc
		if (MDocType.DOCSUBTYPESO_PrepayOrder.equals (DocSubTypeSO))
		{
			MDocType newDT = null;
			MDocType[] dts = MDocType.getOfClient (getCtx());
			for (int i = 0; i < dts.length; i++)
			{
				MDocType type = dts[i];
				if (MDocType.DOCSUBTYPESO_PrepayOrder.equals(type.getDocSubTypeSO()))
				{
					if (type.isDefault() || newDT == null)
						newDT = type;
				}
			}
			if (newDT == null)
				return false;
			else
				setC_DocType_ID (newDT.getC_DocType_ID());
		}

		//	PO - just re-open
		if (!isSOTrx())
			log.info("Existing documents not modified - " + dt);
		//	Reverse Direct Documents
		else if (MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO)	//	(W)illCall(I)nvoice
			|| MDocType.DOCSUBTYPESO_WarehouseOrder.equals(DocSubTypeSO)	//	(W)illCall(P)ickup	
			|| MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO))			//	(W)alkIn(R)eceipt
		{
			if (!createReversals())
				return false;
		}
		else
		{
			log.info("Existing documents not modified - SubType=" + DocSubTypeSO);
		}

		/* globalqss - 2317928 - Reactivating/Voiding order must reset posted */
		MFactAcct.deleteEx(MOrder.Table_ID, getC_Order_ID(), get_TrxName());
		setPosted(false);
		
		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;
		
		setDocAction(DOCACTION_Complete);
		setProcessed(false);
		return true;
	}	//	reActivateIt
	
	
	/*************************************************************************
	 * 	Get Summary
	 *	@return Summary of Document
	 */
	public String getSummary()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getDocumentNo());
		//	: Grand Total = 123.00 (#1)
		sb.append(": ").
			append(Msg.translate(getCtx(),"GrandTotal")).append("=").append(getGrandTotal());
		if (m_lines != null)
			sb.append(" (#").append(m_lines.length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary
	
	/**
	 * 	Get Process Message
	 *	@return clear text error message
	 */
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	public int getDoc_User_ID()
	{
		return getSalesRep_ID();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Approval Amount
	 *	@return amount
	 */
	public BigDecimal getApprovalAmt()
	{
		return getGrandTotal();
	}	//	getApprovalAmt
	
	//AZ Goodwill
	private String deleteMatchPOCostDetail(MOrderLine line)
	{
		// Get Account Schemas to delete MCostDetail
		MAcctSchema[] acctschemas = MAcctSchema.getClientAcctSchema(getCtx(), getAD_Client_ID());
		for(int asn = 0; asn < acctschemas.length; asn++)
		{
			MAcctSchema as = acctschemas[asn];
			
			if (as.isSkipOrg(getAD_Org_ID()))
			{
				continue;
			}
			
			// update/delete Cost Detail and recalculate Current Cost
			MMatchPO[] mPO = MMatchPO.getOrderLine(getCtx(), line.getC_OrderLine_ID(), get_TrxName()); 
			// delete Cost Detail if the Matched PO has been deleted
			if (mPO.length == 0)
			{
				MCostDetail cd = MCostDetail.get(getCtx(), "C_OrderLine_ID=?", 
						line.getC_OrderLine_ID(), line.getM_AttributeSetInstance_ID(), 
						as.getC_AcctSchema_ID(), get_TrxName());
				if (cd !=  null)
				{
					cd.setProcessed(false);
					cd.delete(true);
				}
			}
		}
		
		return "";
	}

	/**
	 * 	Document Status is Complete or Closed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	{
		String ds = getDocStatus();
		return DOCSTATUS_Completed.equals(ds) 
			|| DOCSTATUS_Closed.equals(ds)
			|| DOCSTATUS_Reversed.equals(ds);
	}	//	isComplete
	
}	//	MOrder
