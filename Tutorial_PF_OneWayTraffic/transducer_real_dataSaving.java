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
import java.util.*;

import GenCol.entity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import genDevs.modeling.*;
import simView.*;

public class transducer_real_dataSaving extends ViewableAtomic{ 

	double observationInterval = PF_configData.stepInterval;
	double observationDuration = PF_configData.numberofsteps*observationInterval;
	PrintWriter eastMoving_input, eastMoving_output, westMoving_input, westMoving_output;
	PrintWriter observationData, signalChangeTime;

	int eastMoving_arrvCarCount, eastMoving_dptCarCount, westMoving_arrvCarCount, westMoving_dptCarCount;
	double remainingObservationTime;
	double carPassingTime_mean;
	
	//record individual car's arriving and departure time so that the waiting time can be computed
	double[] eastMovingArrivingTime = new double[5000];
	double[] westMovingArrivingTime = new double[5000];
	
	Random rand;
		
	public transducer_real_dataSaving(){
		this("transducer_dataSaving");		
	}


	public transducer_real_dataSaving(String nm){
		super(nm);    
		addInport("eastMoving_in");
		addInport("westMoving_in");
		addInport("signalChange_in");
		addInport("eastMoving_out");    
		addInport("westMoving_out");    
		addOutport("out");
		
		addInport("report_in");
		
		rand = new Random(8343);

		try{
			eastMoving_input = new PrintWriter(new FileOutputStream(PF_configData.DataPathName+"eastMoving_input.txt"), true);
			eastMoving_output = new PrintWriter(new FileOutputStream(PF_configData.DataPathName+"eastMoving_output.txt"), true);
			westMoving_input = new PrintWriter(new FileOutputStream(PF_configData.DataPathName+"westMoving_input.txt"), true);
			westMoving_output = new PrintWriter(new FileOutputStream(PF_configData.DataPathName+"westMoving_output.txt"), true);
			observationData = new PrintWriter(new FileOutputStream(PF_configData.DataPathName+"observationData.txt"), true);
			signalChangeTime = new PrintWriter(new FileOutputStream(PF_configData.DataPathName+"signalChangeTime.txt"), true);
		}
		catch (FileNotFoundException e) {System.out.println("File creation error!!!!!");}

		eastMoving_input.println("Time"+"\t"+"eventName"+"\t"+"event");
		westMoving_input.println("Time"+"\t"+"eventName"+"\t"+"event");
		eastMoving_output.println("Time"+"\t"+"eventName"+"\t"+"event"+"\t"+"eventArrivingTime"+"\t"+"waitingTime");
		westMoving_output.println("Time"+"\t"+"eventName"+"\t"+"event"+"\t"+"eventArrivingTime"+"\t"+"waitingTime");
		observationData.println("Time"
				+"\t"+"eastMoving_arrvCarCount_noise"
				+"\t"+"westMoving_arrvCarCount_noise"
				+"\t"+"eastMoving_dptCarCount_noise"
				+"\t"+"westMoving_dptCarCount_noise"
				+"\t"+"westSideQueue"
				+"\t"+"eastSideQueue"
				+"\t"+"TrafficLight"
				+"\t"+"phaseSigma"
				+"\t"+"elapseTimeInGreen"
				+"\t"+"carPassingTime_mean"
				+"\t"+"eastMoving_arrvCarCount"
				+"\t"+"westMoving_arrvCarCount"
				+"\t"+"eastMoving_dptCarCount"
				+"\t"+"westMoving_dptCarCount"
				);
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
					entity ent = x.getValOnPort("eastMoving_in", i);
					int index = Integer.valueOf(ent.getName().substring(14,ent.getName().length()));
					eastMovingArrivingTime[index]=this.getSimulationTime();
					eastMoving_arrvCarCount++;
					eastMoving_input.println(this.getSimulationTime()+"\t"+ent.getName()+"\t"+1); // use 1 to represent an event
				}
				else if (messageOnPort(x, "westMoving_in", i)) {
					entity ent = x.getValOnPort("westMoving_in", i);
					int index = Integer.valueOf(ent.getName().substring(14,ent.getName().length()));
					westMovingArrivingTime[index]=this.getSimulationTime();
					westMoving_arrvCarCount++;
					westMoving_input.println(this.getSimulationTime()+"\t"+ent.getName()+"\t"+1); // use 1 to represent an event
				}
				else if (messageOnPort(x, "eastMoving_out", i)) {
					entity ent = x.getValOnPort("eastMoving_out", i);
					int index = Integer.valueOf(ent.getName().substring(14,ent.getName().length()));
					double processingTime = this.getSimulationTime()-eastMovingArrivingTime[index];
					eastMoving_dptCarCount++;
					eastMoving_output.println(this.getSimulationTime()+"\t"+ent.getName()+"\t"+1
						+"\t"+eastMovingArrivingTime[index]+"\t"+processingTime); // use 1 to represent an event
				}
				else if (messageOnPort(x, "westMoving_out", i)) {
					entity ent = x.getValOnPort("westMoving_out", i);
					int index = Integer.valueOf(ent.getName().substring(14,ent.getName().length()));
					double processingTime = this.getSimulationTime()-westMovingArrivingTime[index];
					westMoving_dptCarCount++;
					westMoving_output.println(this.getSimulationTime()+"\t"+ent.getName()+"\t"+1
						+"\t"+westMovingArrivingTime[index]+"\t"+processingTime); // use 1 to represent an event
				}
				else if (messageOnPort(x, "signalChange_in", i)) {
					entity ent = x.getValOnPort("signalChange_in", i);
					if(ent.getName().endsWith("EastToWest")) {
						signalChangeTime.println(this.getSimulationTime()+"\t"+20); // use 20 to represent Agreen						
						signalChangeTime.println(this.getSimulationTime()+"\t"+0); // use 0 to represent Bgreen						
					}
					else if(ent.getName().endsWith("WestToEast")) {
						signalChangeTime.println(this.getSimulationTime()+"\t"+0); // use 20 to represent Agreen						
						signalChangeTime.println(this.getSimulationTime()+"\t"+20); // use 0 to represent Bgreen						
					}
				}
				else if (messageOnPort(x, "report_in", i)) {
					stateEntity ent = (stateEntity)x.getValOnPort("report_in", i);

					// add noise to the observations
					int eastMoving_arrvCarCount_noise=arrivalDepartureCar_addNoise(eastMoving_arrvCarCount);
					int eastMoving_dptCarCount_noise=arrivalDepartureCar_addNoise(eastMoving_dptCarCount);
					int westMoving_arrvCarCount_noise=arrivalDepartureCar_addNoise(westMoving_arrvCarCount);
					int westMoving_dptCarCount_noise=arrivalDepartureCar_addNoise(westMoving_dptCarCount);				

					observationData.println(this.getSimulationTime()
							+"\t"+eastMoving_arrvCarCount_noise
							+"\t"+westMoving_arrvCarCount_noise
							+"\t"+eastMoving_dptCarCount_noise
							+"\t"+westMoving_dptCarCount_noise
							+"\t"+ent.westSideQueue
							+"\t"+ent.eastSideQueue
							+"\t"+((ent.TrafficLightState.startsWith("eastMoving"))? 20: 0)
							+"\t"+ent.sigma							
							+"\t"+ent.elapseTimeInGreen
							+"\t"+ent.carPassingTime
							+"\t"+eastMoving_arrvCarCount
							+"\t"+westMoving_arrvCarCount
							+"\t"+eastMoving_dptCarCount
							+"\t"+westMoving_dptCarCount
							);

					//reset the count values
					eastMoving_arrvCarCount=0;
					eastMoving_dptCarCount=0;
					westMoving_arrvCarCount=0;
					westMoving_dptCarCount=0;				
					
					//System.out.println("remainingObservationTime: "+remainingObservationTime);
					double randomNumPrecisionBuffer = 0.0000001; // Note: this is a temporary solution to handle the double variable precision issue
					if(remainingObservationTime<=randomNumPrecisionBuffer) {
						System.out.println("Finishing Saving Data");
						passivate();
						eastMoving_input.close();
						eastMoving_output.close();
						westMoving_input.close();
						westMoving_output.close();
					}
					else
						holdIn("active", observationInterval);
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

	public int arrivalDepartureCar_addNoise(double base) {
		// we use a gaussian distribution to add noise
		double mean = base;
		double sigmaPct = 0.1;//0.2; // 10 percent
		double sigma = sigmaPct*mean;
		
		double temp = base + sigma*rand.nextGaussian();
		if(temp<0) temp = 0;
		
		int carNum = (int)(temp+0.5);
		return carNum;
		
	}
	
}



