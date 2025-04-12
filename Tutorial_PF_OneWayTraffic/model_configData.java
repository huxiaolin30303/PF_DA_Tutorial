package Tutorial_PF_OneWayTraffic;

public interface model_configData {
		
	double maxElapseTimeWhenBusy = 120;

	double carPassingTime_mean = 4.0;//2.0;//2.0; //1.5;// ---- this is used if treating car passing as a truncated normal distribution between low and high
	double carPassingTime_sigma = 0.5;//0.1;//1.0; //  ---- this is used if treating car passing as a truncated normal distribution between low and high
	double carPassingTime_rangeDelta = 1.0;//2.0; // Pluse minus 0.1 from mean
	double carPassingTime_LowBound = 2.0;//0.15;//1.0; // 1 second ---- this is used if treating car passing as a truncated normal distribution between low and high
	double carPassingTime_UpBound = 10.0;//0.35;//3.0; // 5 seconds ---- this is used if treating car passing as a truncated normal distribution between low and high

	double generator_eastMoving_lambda = 1.0/7.0;//1.0/8.0;// 0.3/2;
	double generator_westMoving_lambda = 1.0/10.0;  // 1 vehicle per 12 seconds 

	/**
	 * Case 1 random number seed
	 */
//	long randSeed_eastMoving = 8123997;
//	long randSeed_westMoving = 512799;
//	long randSeed_IntersectionM = 3987979;

	/**
	 * Case 2 random number seed
	 */
	long randSeed_eastMoving = 21111;
	long randSeed_westMoving = 222;
	long randSeed_IntersectionM = 333;
	
}







