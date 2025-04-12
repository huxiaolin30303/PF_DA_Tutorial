package Tutorial_PF_OneWayTraffic;

import GenCol.*;
import genDevs.modeling.DevsInterface;

public class stateEntity extends entity{
  protected String TrafficLightState; 
  protected double sigma;
  protected double elapseTimeInGreen;
  protected int westSideQueue, eastSideQueue;
  
  //For parameter estimation 
  protected double carPassingTime;
  
  
  public stateEntity(){
	  this(0, 0, "eastMovingGreen_passive", DevsInterface.INFINITY, 0, model_configData.carPassingTime_mean);
  }
  
  public stateEntity(int Wq, int Eq, String GreenLtSt, double sgm, double elapseTimeGreen, double carPTime){
	  super("stateEntity");
	  westSideQueue = Wq;
	  eastSideQueue = Eq;
	  TrafficLightState = GreenLtSt;
	  sigma = sgm;
	  elapseTimeInGreen = elapseTimeGreen;
	  carPassingTime= carPTime;
  }
  
  public String toString(){
	  //return name+"_"+processingTime;
	  return westSideQueue+"_"+eastSideQueue+"_"+TrafficLightState
			  +"_"+sigma+"_"+elapseTimeInGreen+"_"+carPassingTime;
//			  ((double)((int)(elapseTimeInGreen*1000)))/1000;
  }
		
}
