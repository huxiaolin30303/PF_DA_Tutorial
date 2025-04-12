package Tutorial_PF_OneWayTraffic;

import simView.*;
import statistics.rand;

import java.lang.*;
import java.util.Random;

import genDevs.modeling.*;
import genDevs.simulation.*;
import GenCol.*;

public class trafficControlModel extends ViewableAtomic{
	double maxElapseTimeWhenBusy = model_configData.maxElapseTimeWhenBusy;

	double carPassingTime_mean = model_configData.carPassingTime_mean; // ---- this is used if treating car passing as a truncated normal distribution between low and high

entity job, currentJob;
DEVSQueue queue_westSide, queue_eastSide;
String trafficLightState = "eastMovingGreen";
double initSigma; 
double elapsedTimeInGreen = 0;
double carPassingTime;
boolean destination_eastMoving=false;
protected Random r;

// the following two variables are to restore the phase and sigma after reporting
String phaseBeforeReporting;
double sigmaBeforeReporting;

/**
 * ------- The rule of the construction road segment
 * Only one car is allowed to be on the construction road segment at any time. 
 * When a car finishes passing, depending on which direction has the traffic control, the corresponding car
 * at the first of the queue starts to move.  
 * 
 * --------The four rules of the one way traffic control
 * When to change traffic direction: if there is a car on the segment, traffic control can change the moving direction only after the car finishes crossing.
 * 1. If the current direction (eastMoving and westMoving) has car and the other direction has car too, the green light switches if the elapsed time is larger than maxElapseTimeWhenBusy 
 * 2. If the current direction has car and the other direction has no car, the green light does not switch
 * 3. If the current direction has no car and the other direction has car to pass, switch traffic light immediately
 * 4. If the current direction has no car and the other direction has no car either, do not switch traffic light
 */

public trafficControlModel() {this("trafficControlModel");}

public trafficControlModel(String name){
    this(name,"eastMoving_passive", DevsInterface.INFINITY, 
    		0, 0, 0,model_configData.carPassingTime_mean, new Random(model_configData.randSeed_IntersectionM));
}

public trafficControlModel(String name, String tfcState, double sgm, double initElapseTimeGreen, int QWestSideSize, int QEastSideSize, double carPassingTime, Random rd){
    super(name);
    addInport("eastMoving_in");
    addInport("westMoving_in");
    addOutport("eastMoving_out");    
    addOutport("westMoving_out");    
    addOutport("signalChangeOut");
    addInport("report");
    addOutport("report_out");
        
    currentJob= new entity("no job");
    
    r = rd;
    trafficLightState = tfcState;
    initSigma = sgm;
    elapsedTimeInGreen = initElapseTimeGreen;
    carPassingTime_mean = carPassingTime;
    
    queue_westSide = new DEVSQueue();
    queue_eastSide = new DEVSQueue();
    for(int i=0; i< QWestSideSize; i++) {
    	queue_westSide.add(new entity("eastMovingCar_init_"+i));
    }
    for(int i=0; i< QEastSideSize; i++) {
    	queue_eastSide.add(new entity("westMovingCar_init_"+i));
    }

}

public void initialize(){
	holdIn(trafficLightState,initSigma);
}

public void  deltext(double e,message x){
	Continue(e);
	elapsedTimeInGreen+=e;

	//System.out.println(this.getSimulationTime()+" message="+x.toString());
	for (int i = 0; i < x.getLength(); i++) {
		if (messageOnPort(x, "eastMoving_in", i)) {
			job = x.getValOnPort("eastMoving_in", i);
			queue_westSide.add(job);
		}
		else if (messageOnPort(x, "westMoving_in", i)) {
			job = x.getValOnPort("westMoving_in", i);
			queue_eastSide.add(job);
		}
	}
	if(phaseIs("westMoving_passive")||phaseIs("eastMoving_passive")) {
		trafficLightState=phase;
		applyTrafficControlRules();
	}

	// handle the report message separately, after everything else in the message bag has been handled
	for (int i = 0; i < x.getLength(); i++) {
		if (messageOnPort(x, "report", i)) {
			phaseBeforeReporting = this.getPhase();
			sigmaBeforeReporting = this.getSigma();
			holdIn("report",0);
		}
	}
	
}

public void  deltint( ){
	elapsedTimeInGreen+=sigma;
		
	if(phaseIs("report")) {
		holdIn(phaseBeforeReporting, sigmaBeforeReporting);
	}
	else if(phaseIs("signalChange_EastToWest")) {
		trafficLightState = "westMovingGreen"; // change traffic light
		elapsedTimeInGreen = 0; // reset elapsedTimeInGreen
		applyTrafficControlRules();
	}
	else if(phaseIs("signalChange_WestToEast")) {
		trafficLightState = "eastMovingGreen"; // change traffic light
		elapsedTimeInGreen = 0; // reset elapsedTimeInGreen
		applyTrafficControlRules();
	}
	else {
		trafficLightState=phase;
		applyTrafficControlRules();
	}
}

protected void applyTrafficControlRules() {
	if(trafficLightState.startsWith("eastMoving")) { 							// eastMoving is green
		if(!queue_eastSide.isEmpty()&&(queue_westSide.isEmpty() 
				||elapsedTimeInGreen>=maxElapseTimeWhenBusy )) 	// need to change signal
			holdIn("signalChange_EastToWest",0);
		else {
			if(queue_westSide.isEmpty())						//wait
				passivateIn("eastMoving_passive");		
			else { 												//schedule another car to move forward
				currentJob = (entity)queue_westSide.pop();
				carPassingTime = getCarPassingTime();
				holdIn("eastMoving_active", carPassingTime);
			}
		}
	}
	else if(trafficLightState.startsWith("westMoving")) { 					// westMoving is green
		if(!queue_westSide.isEmpty() && (queue_eastSide.isEmpty()
				|| elapsedTimeInGreen>=maxElapseTimeWhenBusy))	//need to change signal
			holdIn("signalChange_WestToEast",0);
		else {
			if(queue_eastSide.isEmpty())						//wait
				passivateIn("westMoving_passive");
			else {                                  			//schedule another car to move forward
				currentJob = (entity)queue_eastSide.pop();
				carPassingTime = getCarPassingTime();
				holdIn("westMoving_active", carPassingTime);
			}
		}
	}		
}

public void deltcon(double e,message x){ //usual devs
	   deltint();
	   deltext(0,x);
}

public message  out( ){
	message  m = new message();
	if (phaseIs("eastMoving_active")) {
		m.add(makeContent("eastMoving_out", currentJob));
	}
	else if (phaseIs("westMoving_active")) {
		m.add(makeContent("westMoving_out", currentJob));
	}
	else if (phaseIs("signalChange_EastToWest")) {
		m.add(makeContent("signalChangeOut", new entity("signalChange_EastToWest")));
	}
	else if (phaseIs("signalChange_WestToEast")) {
		m.add(makeContent("signalChangeOut", new entity("signalChange_WestToEast")));
	}
	else if (phaseIs("report")) {
		m.add(makeContent("report_out", new stateEntity(queue_westSide.size(),
				queue_eastSide.size(), phaseBeforeReporting, sigmaBeforeReporting, elapsedTimeInGreen, carPassingTime_mean)));
	}

  return m;

}

private double getCarPassingTime() {
	double nextTime;
	double carPassingTime_sigma = model_configData.carPassingTime_sigma; //  ---- this is used if treating car passing as a truncated normal distribution between low and high
	double carPassingTime_LowBound = carPassingTime_mean-model_configData.carPassingTime_rangeDelta; // 1 second ---- this is used if treating car passing as a truncated normal distribution between low and high
	double carPassingTime_UpBound = carPassingTime_mean+model_configData.carPassingTime_rangeDelta; // 5 seconds ---- this is used if treating car passing as a truncated normal distribution between low and high
	nextTime = getTruncatedNormalDistributionRate(carPassingTime_LowBound, carPassingTime_UpBound,
			carPassingTime_mean, carPassingTime_sigma);
	
	return nextTime;
}

private double getTruncatedNormalDistributionRate(double low, double high, double mean, double sigma) {
	double u = mean + sigma * r.nextGaussian();
	while (u<low || u > high) // resample 
		u= mean + sigma * r.nextGaussian();
	return u;
}

public String getTooltipText () {
	   return super.getTooltipText() +
	       "\n" + "westSideQueue length: " + queue_westSide.size()
	       +"\n" + "eastSideQueue length: " + queue_eastSide.size()
	       +"\n" + "elapsedTimeInGreen:"+elapsedTimeInGreen
	       +"\n" + "currentJob: " + currentJob.getName()+" movingEast?:"+destination_eastMoving;
	 }

}

