/**
 * This class implements an atomic model for the forest fire igniter.
 * It sends and ignition signal to a specified cell (cell id) in the cell space
 * The cell ignites based on the fireline intensity value for that cell (> 45kW/m)
 *
 * author: Lewis Ntaimo
 * Date: May 7, 2003
 * Extended by: Yi Sun, Xiaolin Hu, Sept. 2007
 */
package Tutorial_PF_OneWayTraffic;

import java.io.PrintWriter;
import java.util.Vector;

import GenCol.entity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import genDevs.modeling.*;
import simView.*;

public class transducer_DA extends ViewableAtomic{ 

	double observationInterval = PF_configData.stepInterval;
	double observationDuration = observationInterval;//observe for only 1 period 

	int eastMoving_arrvCarCount, eastMoving_dptCarCount, westMoving_arrvCarCount, westMoving_dptCarCount;
	String TrafficLightState; 
	double phaseSigma;
	double elapseTimeInGreen;
	int westSideQueue, eastSideQueue;
	double carPassingTime_mean;
	
	boolean stateDataReceived = false;
	
	double remainingObservationTime;
	
	public transducer_DA(){
		this("transducer_dataSaving");		
	}


	public transducer_DA(String nm){
		super(nm);    
		addInport("eastMoving_in");
		addInport("westMoving_in");
		addInport("eastMoving_out");    
		addInport("westMoving_out");    
		addOutport("out");
		
		addInport("report_in");

	}


	public void initialize(){
		remainingObservationTime=observationDuration;
		eastMoving_arrvCarCount=0;
		eastMoving_dptCarCount=0;
		westMoving_arrvCarCount=0;
		westMoving_dptCarCount=0;
		holdIn("active", observationInterval);
	}

	public void deltext(double e,message x){
		Continue(e);
		remainingObservationTime -=e;
		if(!phaseIs("passive")) {
			for (int i = 0; i < x.getLength(); i++) {
				if (messageOnPort(x, "eastMoving_in", i)) {
					eastMoving_arrvCarCount++;
				}
				else if (messageOnPort(x, "westMoving_in", i)) {
					westMoving_arrvCarCount++;
				}
				else if (messageOnPort(x, "eastMoving_out", i)) {
					eastMoving_dptCarCount++;
				}
				else if (messageOnPort(x, "westMoving_out", i)) {
					westMoving_dptCarCount++;
				}
				else if (messageOnPort(x, "report_in", i)) {
					stateEntity ent = (stateEntity)x.getValOnPort("report_in", i);
					TrafficLightState = ent.TrafficLightState; 
					phaseSigma = ent.sigma;
					elapseTimeInGreen = ent.elapseTimeInGreen;
					westSideQueue=ent.westSideQueue;
					eastSideQueue=ent.eastSideQueue;
					carPassingTime_mean=ent.carPassingTime;
										
					stateDataReceived= true; 
					
						passivate();
				}
			}
		}
	}

	public void   deltint(){
		remainingObservationTime-=sigma;
		passivateIn("waitForReport");
	}


	public message out(){
		message m = super.out();
		if(phaseIs("active")) {
			content con = makeContent("out", new entity("report"));
			m.add(con);
		}
		return m;
	}


}



