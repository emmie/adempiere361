/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2007 Low Heng Sin											  *
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
 *****************************************************************************/
package org.compiere.apps;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.*;
import javax.swing.*;

import org.compiere.process.*;
import org.compiere.swing.*;
import org.compiere.util.*;

/**
 * [ 1639242 ] Inconsistent appearance of Process/Report Dialog
 * 
 *	Modal Dialog to Start process.
 *	Displays information about the process
 *		and lets the user decide to start it
 *  	and displays results (optionally print them).
 *  Calls ProcessCtl to execute.
 *  @author 	Low Heng Sin
 *  @author     arboleda - globalqss
 *  - Implement ShowHelp option on processes and reports
 */
public class ProcessModalDialog extends CDialog
	implements ActionListener
{
	
	/**
	 * Dialog to start a process/report
	 * @param ctx
	 * @param parent
	 * @param title
	 * @param aProcess
	 * @param WindowNo
	 * @param AD_Process_ID
	 * @param tableId
	 * @param recordId
	 * @param autoStart
	 */
	public ProcessModalDialog (Properties ctx, Frame parent, String title, 
			ASyncProcess aProcess, int WindowNo, int AD_Process_ID,
			int tableId, int recordId, boolean autoStart)
	{
		super(parent, title, true);
		log.info("Process=" + AD_Process_ID );
		m_ctx = ctx;
		m_ASyncProcess = aProcess;
		m_WindowNo = WindowNo;
		m_AD_Process_ID = AD_Process_ID;
		m_tableId = tableId;
		m_recordId = recordId;
		m_autoStart = autoStart;
		try
		{
			jbInit();
			init();
		}
		catch(Exception ex)
		{
			log.log(Level.SEVERE, "", ex);
		}
	}	//	ProcessDialog

	private ASyncProcess m_ASyncProcess;
	private int m_WindowNo;
	private Properties m_ctx;
	private int m_tableId;
	private int m_recordId;
	private boolean m_autoStart;
	private int 		    m_AD_Process_ID;
	private String		    m_Name = null;
	private StringBuffer	m_messageText = new StringBuffer();
	private String          m_ShowHelp = null; // Determine if a Help Process Window is shown
	private boolean m_valid = true;
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(ProcessDialog.class);
	//

	private CPanel dialog = new CPanel()
	{
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			Dimension m = getMinimumSize();
			if ( d.height < m.height || d.width < m.width ) {
				Dimension d1 = new Dimension();
				d1.height = Math.max(d.height, m.height);
				d1.width = Math.max(d.width, m.width);
				return d1;
			} else
				return d;
		}
	};
	private BorderLayout mainLayout = new BorderLayout();
	private ConfirmPanel southPanel = new ConfirmPanel(true);
	private CButton bOK = null;
	private JEditorPane message = new JEditorPane()
	{
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			Dimension m = getMaximumSize();
			if ( d.height > m.height || d.width > m.width ) {
				Dimension d1 = new Dimension();
				d1.height = Math.min(d.height, m.height);
				d1.width = Math.min(d.width, m.width);
				return d1;
			} else
				return d;
		}
	};
	private JScrollPane messagePane = new JScrollPane(message);
	
	private CPanel centerPanel = null;
	private ProcessParameterPanel parameterPanel = null;
	private JSeparator separator = new JSeparator();
	private ProcessInfo m_pi = null;

	/**
	 *	Static Layout
	 *  @throws Exception
	 */
	private void jbInit() throws Exception
	{
		dialog.setLayout(mainLayout);
		dialog.setMinimumSize(new Dimension(500, 200));
		southPanel.addActionListener(this);
		bOK = southPanel.getOKButton();
		//
		message.setContentType("text/html");
		message.setEditable(false);
		message.setBackground(Color.white);
		message.setFocusable(false);
		getContentPane().add(dialog);
		dialog.add(southPanel, BorderLayout.SOUTH);
		dialog.add(messagePane, BorderLayout.NORTH);
		messagePane.setBorder(null);
		message.setMaximumSize(new Dimension(600, 300));
		centerPanel = new CPanel();
		centerPanel.setBorder(null);
		centerPanel.setLayout(new BorderLayout());
		dialog.add(centerPanel, BorderLayout.CENTER);
		//
		this.getRootPane().setDefaultButton(bOK);
	}	//	jbInit

	/**
	 * 	Set Visible 
	 * 	(set focus to OK if visible)
	 * 	@param visible true if visible
	 */
	public void setVisible (boolean visible)
	{
		super.setVisible(visible);
		if (visible) {
			bOK.requestFocus();
		}
	}	//	setVisible

	/**
	 *	Dispose
	 */
	public void dispose()
	{
		m_valid = false;
		super.dispose();
	}	//	dispose

	public boolean isValid()
	{
		return m_valid;
	}

	/**
	 *	Dynamic Init
	 *  @return true, if there is something to process (start from menu)
	 */
	public boolean init()
	{
		log.config("");
		//
		boolean trl = !Env.isBaseLanguage(m_ctx, "AD_Process");
		String sql = "SELECT Name, Description, Help, IsReport, ShowHelp "
				+ "FROM AD_Process "
				+ "WHERE AD_Process_ID=?";
		if (trl)
			sql = "SELECT t.Name, t.Description, t.Help, p.IsReport, p.ShowHelp "
				+ "FROM AD_Process p, AD_Process_Trl t "
				+ "WHERE p.AD_Process_ID=t.AD_Process_ID"
				+ " AND p.AD_Process_ID=? AND t.AD_Language=?";
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, m_AD_Process_ID);
			if (trl)
				pstmt.setString(2, Env.getAD_Language(m_ctx));
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{
				m_Name = rs.getString(1);
				m_ShowHelp = rs.getString(5);
				//
				m_messageText.append("<b>");
				String s = rs.getString(2);		//	Description
				if (rs.wasNull())
					m_messageText.append(Msg.getMsg(m_ctx, "StartProcess?"));
				else
					m_messageText.append(s);
				m_messageText.append("</b>");
				
				s = rs.getString(3);			//	Help
				if (!rs.wasNull())
					m_messageText.append("<p>").append(s).append("</p>");
			}

			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
			return false;
		}

		if (m_Name == null)
			return false;
		//
		this.setTitle(m_Name);
		message.setMargin(new Insets(10,10,10,10));
		message.setText(m_messageText.toString());

		//	Move from APanel.actionButton
		m_pi = new ProcessInfo(m_Name, m_AD_Process_ID, m_tableId, m_recordId);
		m_pi.setAD_User_ID (Env.getAD_User_ID(Env.getCtx()));
		m_pi.setAD_Client_ID(Env.getAD_Client_ID(Env.getCtx()));
		parameterPanel = new ProcessParameterPanel(m_WindowNo, m_pi);
		centerPanel.removeAll();
		if (parameterPanel.init()) {
			// hasfields
			centerPanel.add(separator, BorderLayout.NORTH);
			centerPanel.add(parameterPanel, BorderLayout.CENTER);
		} else {
			if (m_ShowHelp != null && m_ShowHelp.equals("N")) {
				m_autoStart = true;
			}
			if (m_autoStart)
				bOK.doClick();    // don't ask first click
		}
		dialog.revalidate();
		return true;
	}	//	init

	/**
	 *	ActionListener (Start)
	 *  @param e ActionEvent
	 */
	public void actionPerformed (ActionEvent e)
	{
		if (e.getSource() == bOK)
		{
			ProcessCtl.process(m_ASyncProcess, m_WindowNo, parameterPanel, m_pi, null);
			dispose();
		}

		else if (e.getSource() == southPanel.getCancelButton())
			dispose();
	}	//	actionPerformed

}	//	ProcessDialog
