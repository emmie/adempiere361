package org.adempiere.webui.apps.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import org.adempiere.webui.component.Panel;
import org.compiere.model.MColorSchema;
import org.compiere.model.MGoal;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.data.Range;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.RectangleInsets;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Image;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;

/**
 * 	Performance Indicator
 *	
 *  @author Jorg Janke
 *  @version $Id: PerformanceIndicator.java,v 1.2 2006/07/30 00:51:28 jjanke Exp $
 */
public class WPerformanceIndicator extends Panel implements EventListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3580494126343850939L;

	/**
	 * 	Constructor
	 *	@param goal goal model
	 */
	public WPerformanceIndicator(MGoal goal)
	{
		super();
		
		m_goal = goal;
//		setName(m_goal.getName());
        
		init();
		mRefresh.addEventListener(Events.ON_CLICK, this);
		popupMenu.appendChild(mRefresh);
		
		addEventListener(Events.ON_DOUBLE_CLICK, this);
		addEventListener(Events.ON_CLICK, this);
	}	//	PerformanceIndicator
	
	private MGoal				m_goal = null;
	/**	The Performance Name		*/
	private String				m_text = null;
	/** Performance Line			*/ 
//	private double				m_line = 0;
	
	/**	Height						*/
//	private static double		s_height = 45;
	/**	100% width					*/
//	private static double		s_width100 = 150;
	/**	Max width					*/
//	private static double		s_widthMax = 250;
	/** Integer Number Format		*/
	private static DecimalFormat	s_format = DisplayType.getNumberFormat(DisplayType.Integer);

	Menupopup 					popupMenu = new Menupopup();
	private Menuitem 			mRefresh = new Menuitem(Msg.getMsg(Env.getCtx(), "Refresh"), "/images/Refresh16.png");
        
    //Insert Pie Graph Kinamo (pelgrim)
//	private static Color				colorOK = Color.magenta;
//	private static Color				colorNotOK = Color.lightGray;
//	private static Dimension			indicatordimension = new Dimension(170,120);
//	private static Dimension			paneldimension = new Dimension(180, 150);
	ChartPanel chartPanel;
	//private static Dimension 

	/**
	 * 	Get Goal
	 *	@return goal
	 */
	public MGoal getGoal()
	{
		return m_goal;
	}	//	getGoal
	
	private  JFreeChart createChart()
	{
		JFreeChart chart = null;
		
		//	Set Text
		StringBuffer text = new StringBuffer(m_goal.getName());
		if (m_goal.isTarget())
			text.append(": ").append(m_goal.getPercent()).append("%");
		else
			text.append(": ").append(s_format.format(m_goal.getMeasureActual()));
		
		m_text = text.toString();

		//	ToolTip
		text = new StringBuffer();
		if (m_goal.getDescription() != null)
			text.append(m_goal.getDescription()).append(": ");
		text.append(s_format.format(m_goal.getMeasureActual()));
		if (m_goal.isTarget())
			text.append(" ").append(Msg.getMsg(Env.getCtx(), "of")).append(" ")
				.append(s_format.format(m_goal.getMeasureTarget()));
		setTooltiptext(text.toString());
		//
		//setBackground(m_goal.getColor());
//		setForeground(GraphUtil.getForeground(getBackground()));
		//	Performance Line
//		int percent = m_goal.getPercent();
//		if (percent > 100)			//	draw 100% line
//			m_line = s_width100;
//		else						//	draw Performance Line
//			m_line = s_width100 * m_goal.getGoalPerformanceDouble();
		

//        String title = m_text;        
        DefaultValueDataset data = new DefaultValueDataset((float)m_goal.getPercent());
        MeterPlot plot = new MeterPlot(data);

        MColorSchema colorSchema = m_goal.getColorSchema();		
        int rangeLo = 0; int rangeHi=0;
        for (int i=1; i<=4; i++){
            switch (i) {
             case 1: rangeHi = colorSchema.getMark1Percent(); break;
             case 2: rangeHi = colorSchema.getMark2Percent(); break;
             case 3: rangeHi = colorSchema.getMark3Percent(); break;
             case 4: rangeHi = colorSchema.getMark4Percent(); break;
            }
            if (rangeHi==9999)
            	rangeHi = (int) Math.floor(rangeLo*1.5);
            if (rangeLo < rangeHi) {
            	plot.addInterval(new MeterInterval("Normal", //label
                 	  new Range(rangeLo, rangeHi), //range
                 	  colorSchema.getColor(rangeHi),
                 	  new BasicStroke(7.0f),
                 	  //Color.lightGray
                 	  new Color(-13091716)
                 	  //Color.gray 
                ));
            	rangeLo = rangeHi;
            }
        }
        plot.setRange(new Range(0,rangeLo));
        
        plot.setDialBackgroundPaint(new Color(-13091716));//Color.GRAY);
        plot.setUnits(m_goal.getName());
        plot.setDialShape(DialShape.CHORD);//CIRCLE);        
        //plot.setDialBackgroundPaint(new GradientPaint(0, 0, m_goal.getColor(), 0, 1000, Color.black));
        plot.setNeedlePaint(Color.white);  
        plot.setTickSize(2000);
        plot.setTickLabelFont(new Font("SansSerif", Font.BOLD, 11));
        plot.setValueFont(new Font("SansSerif", Font.BOLD, 11));
        plot.setNoDataMessageFont(new Font("SansSerif", Font.BOLD, 11));
        plot.setTickLabelPaint(Color.white);
        plot.setInsets(new RectangleInsets(1.0, 2.0, 3.0, 4.0)); 

        chart = new JFreeChart( m_text, new Font("SansSerif", Font.BOLD, 11), plot,false);
        
		return chart;
	}
	
     /**
	 * 	Init Graph Display
	 *  Kinamo (pelgrim)
	 */
	private void init() 
	{
		JFreeChart chart = createChart();
				
		BufferedImage bi = chart.createBufferedImage(200, 120, BufferedImage.TRANSLUCENT , null);		
		try {
		    byte[] bytes = EncoderUtil.encode(bi, ImageFormat.PNG, true);
		    
		    AImage image = new AImage("", bytes);
		    Image myImage = new Image();
		    myImage.setContent(image);
		    appendChild(myImage);
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
		}

	    invalidate();
	}


	/**
	 * 	Update Display Data
	 */
	protected void updateDisplay()
	{
		chartPanel.setChart(createChart());
	    invalidate();
	}	//	updateData
	
    /**************************************************************************
     * Adds an <code>ActionListener</code> to the indicator.
     * @param l the <code>ActionListener</code> to be added
     */
/*    public void addActionListener(ActionListener l) 
    {
    	if (l != null)
    		listenerList.add(ActionListener.class, l);
    }	//	addActionListener
*/    
    /**
     * Removes an <code>ActionListener</code> from the indicator.
     * @param l the listener to be removed
     */
/*    public void removeActionListener(ActionListener l) 
    {
    	if (l != null)
    		listenerList.remove(ActionListener.class, l);
    }	//	removeActionListener
*/    
    /**
     * Returns an array of all the <code>ActionListener</code>s added
     * to this indicator with addActionListener().
     *
     * @return all of the <code>ActionListener</code>s added or an empty
     *         array if no listeners have been added
     */
/*    public ActionListener[] getActionListeners() 
    {
        return (ActionListener[])(listenerList.getListeners(ActionListener.class));
    }	//	getActionListeners
*/
    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the <code>event</code> 
     * parameter.
     *
     * @param event  the <code>ActionEvent</code> object
     * @see EventListenerList
     */
/*    protected void fireActionPerformed(MouseEvent event) 
    {
        // Guaranteed to return a non-null array
    	ActionListener[] listeners = getActionListeners();
        ActionEvent e = null;
        // Process the listeners first to last
        for (int i = 0; i < listeners.length; i++) 
        {
        	//	Lazily create the event:
        	if (e == null) 
        		e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
        			"pi", event.getWhen(), event.getModifiers());
        	listeners[i].actionPerformed(e);
        }
    }	//	fireActionPerformed
*/	
    
    /**************************************************************************
     * 	Mouse Clicked
     *	@param e mouse event
     */
/*	public void mouseClicked (MouseEvent e)
	{
		if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1)
			fireActionPerformed(e);
		if (SwingUtilities.isRightMouseButton(e))
			popupMenu.show((Component)e.getSource(), e.getX(), e.getY());
	}	//	mouseClicked

	public void mousePressed (MouseEvent e)
	{
	}

	public void mouseReleased (MouseEvent e)
	{
	}

	public void mouseEntered (MouseEvent e)
	{
	}

	public void mouseExited (MouseEvent e)
	{
	}
*/
	/**
	 * 	Action Listener.
	 * 	Update Display
	 *	@param e event
	 */
/*	public void actionPerformed (ActionEvent e)
	{
		if (e.getSource() == mRefresh)
		{
			m_goal.updateGoal(true);
			updateDisplay();
			//
			Container parent = getParent();
			if (parent != null)
				parent.invalidate();
			invalidate();
			if (parent != null)
				parent.repaint();
			else
				repaint();
		}
	}	//	actionPerformed
*/	
	public void onEvent(Event event) throws Exception 
	{
//		Component comp = event.getTarget();
//        String eventName = event.getName();        
	}
}
