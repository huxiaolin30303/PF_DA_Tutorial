package Tutorial_PF_OneWayTraffic;

import GenCol.*;

public class sensorDataEntity extends entity{
  int step;
  int eastMoving_ArrivalCount;
  int eastMoving_DepartureCount;
  int westMoving_ArrivalCount;
  int westMoving_DepartureCount;
  
  public sensorDataEntity(){
	  this(0,0,0,0,0);
  }
  
  public sensorDataEntity(int stp, int arrivA, int departA, int arrivB, int departB){
	  super("sensorDataEntity");
	  step = stp;
	  eastMoving_ArrivalCount = arrivA;
	  eastMoving_DepartureCount = departA;
	  westMoving_ArrivalCount = arrivB;
	  westMoving_DepartureCount = departB;
  }

  public String toString(){
	  //return name+"_"+processingTime;
	  return step+"_"+eastMoving_ArrivalCount+"_"+eastMoving_DepartureCount+"_"+westMoving_ArrivalCount+"_"+westMoving_DepartureCount;
  }
		
}
