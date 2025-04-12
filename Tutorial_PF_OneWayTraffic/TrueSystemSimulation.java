package Tutorial_PF_OneWayTraffic;


import GenCol.*;
import genDevs.modeling.*;
import genDevs.simulation.*;
import genDevs.simulation.realTime.*;


public class TrueSystemSimulation{

protected static digraph testDig;

  public TrueSystemSimulation(){}

  public static void main(String[ ] args)
  {
      testDig = new trafficControlSys_Real();
      genDevs.simulation.coordinator cs = new genDevs.simulation.coordinator(testDig);

      cs.initialize();
      cs.simulate_TN(PF_configData.numberofsteps*PF_configData.stepInterval);
      System.out.println("simulation finsihed");
  }
}
