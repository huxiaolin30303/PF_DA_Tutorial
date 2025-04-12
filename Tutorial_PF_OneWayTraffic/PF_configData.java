package Tutorial_PF_OneWayTraffic;

public class PF_configData {

	public static int numberofsteps = 100;//80; 
	public static double stepInterval = 30; //60;  // 18 seconds

	public static int numberofparticles = 1000;//6000;//1000;
	
	public static int currentStepIndex = 0;///
	public static int currentParticleIndex = 0;
	public static double currTime = 0;

	public static String DataPathName = "PF_OneWAyTraffic_Tutorial/";
	public static String realSensorDataFileName = "observationData.txt";
	
	public static long PF_globalRandSeed = 88788;//444;//
	
	public static long RTPrediction_randSeed = 66666;
	
}







