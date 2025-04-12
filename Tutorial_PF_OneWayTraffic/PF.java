package Tutorial_PF_OneWayTraffic;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import genDevs.modeling.DevsInterface;
import genDevs.simulation.*;

public class PF {
	public double[] weight = new double[PF_configData.numberofparticles]; // save particle's weights -- not normalized
	public double[] normalizedWeight = new double[PF_configData.numberofparticles];
	int[] selectedParticles = new int[PF_configData.numberofparticles];
	int[] preselectedParticles = new int[PF_configData.numberofparticles];
	stateEntity[] particleStateArray = new stateEntity[PF_configData.numberofparticles];
	stateEntity[] newParticleStateArray = new stateEntity[PF_configData.numberofparticles];
	
	trafficControlSys_DA[] particleArray = new trafficControlSys_DA[PF_configData.numberofparticles];
	
	// array to store data from the "real" system
	stateEntity[] realStateArray = new stateEntity[PF_configData.numberofsteps];
	sensorDataEntity[] sensorDataArray = new sensorDataEntity[PF_configData.numberofsteps];
	
	Random globalRand;
	
//	//The following arrays are used to summarize the results
//	int[] numOfAGreen = new int[PF_configData.numberofsteps];
//	int[] numOfBGreen = new int[PF_configData.numberofsteps];	
//	double[] averageRemainingTimeForAGreen = new double[PF_configData.numberofsteps];
//	double[] averageRemainingTimeForBGreen = new double[PF_configData.numberofsteps];
	
	PrintWriter DA_result, step_result;
	
	boolean batchRun=false;
	
	public PF(int run) {
		
		try{
			DA_result = new PrintWriter(new FileOutputStream(PF_configData.DataPathName+"DAResult"+run+".txt"), true);
		}
		catch (FileNotFoundException e) {System.out.println("File creation error!!!!!");}

		DA_result.println("step"+"\t"+"time"
				+"\t"+"real_TrafficState"
				+"\t"+"real_elapseTimeInGreen"
				+"\t"+"real_westSideQueue"
				+"\t"+"real_eastSideQueue"
				+"\t"+"real_carPassingT"
				+"\t"+"DA_eastMovingGreenPct"
				+"\t"+"DA_eastGreenElapseTime"
				+"\t"+"DA_westMovingGreenPct"
				+"\t"+"DA_westGreenElapseTime"
				+"\t"+"combinedElapseTime"
//				+"\t"+"mapedCycleTime_Real"
//				+"\t"+"aveMappedCycleTime"
//				+"\t"+"aveMappedCycleTime_std"
//				+"\t"+"aveError"
				+"\t"+"aveWestSideQueue"
				+"\t"+"westSideQsize_std"
				+"\t"+"aveEastSideQueue"
				+"\t"+"eastSideQsize_std"
				+"\t"+"aveCarPassingT"
				+"\t"+"carPassingT_std"
				+"\t"+"eastMovWaitTime_RTPred"
				+"\t"+"westMovWaitTime_RTPred"
				);
	}
	
    void startComputing(int run){
		if(!batchRun) {
			globalRand = new Random(PF_configData.PF_globalRandSeed);
		}
		else {
			globalRand = new Random(System.currentTimeMillis());
		}			
		RMSE_overallSum=0;

		readRealSensorData();

    	for(int step=0; step<PF_configData.numberofsteps; step++){
            System.out.println("----------------------------------PF algorithm Step:" + step);
    		samplingAndComputeWeights(step);
    		for(int i=0; i<PF_configData.numberofparticles;i++)
    			preselectedParticles[i]=selectedParticles[i];
    		for(int i=0; i<PF_configData.numberofparticles;i++)
    			particleStateArray[i]=newParticleStateArray[i];
    		normalizeWeights(PF_configData.numberofparticles);
    		resampling(PF_configData.numberofparticles, selectedParticles);    		
    		
    		analyzeAndSaveResults(run, step);
    	}
    	System.out.println("!!!!!!!!!!!!!finishing all steps of PF algorithm");
    }
    
    /**
     * The following variables are used to compute an overall error score for evaluating the data assimilation
     * We focus only on the westSideQueue and eastSideQueue 
     */
    double westSideQueueRMSE_step, eastSideQueueRMSE_step;
    double RMSE_overallSum;

    
    public void analyzeAndSaveResults(int run, int step) {
        westSideQueueRMSE_step=0;
        eastSideQueueRMSE_step=0;
        
		// summarize estimation results 
		int eastMovingGreen=0, westMovingGreen=0;
		double eastGreenElapseTime_sum=0, westGreenElapseTime_sum=0, combinedElapseTime_sum=0; 
		try{
			step_result = new PrintWriter(new FileOutputStream(PF_configData.DataPathName+"run"+run+"_step_"+step+".txt"), true);
		}
		catch (FileNotFoundException e) {System.out.println("File creation error!!!!!");}
		step_result.println("particleIdx"+"\t"+"tfState"+"\t"+"sigma"+"\t"+"elapseTimeInGreen"+"\t"+"westSideQueue"+"\t"+"eastSideQueue"
		+"\t"+"carPassingTValue"+"\t"+"eastMovingDepartureCount"+"\t"+"eastMovingArrivalCount"+"\t"+"selectedParticlesIdx");
				
		double westSideQueueSizeSum=0, eastSideQueueSizeSum=0;
		double[] westSideQArray = new double[PF_configData.numberofparticles];
		double[] eastSideQArray = new double[PF_configData.numberofparticles];
		double carPassingTSum=0;
		double[] carPassingTArray = new double[PF_configData.numberofparticles];
		for (int i=0;i<PF_configData.numberofparticles;i++){
    		String tfState = particleStateArray[selectedParticles[i]].TrafficLightState; 
    		double sigma = particleStateArray[selectedParticles[i]].sigma;
    		double elapseTimeInGreen= particleStateArray[selectedParticles[i]].elapseTimeInGreen;
			int westSideQueueSize = particleStateArray[selectedParticles[i]].westSideQueue;
			int eastSideQueueSize = particleStateArray[selectedParticles[i]].eastSideQueue;
			double carPassingTValue = particleStateArray[selectedParticles[i]].carPassingTime;
			
			westSideQueueSizeSum+=westSideQueueSize;
			eastSideQueueSizeSum+=eastSideQueueSize;
			westSideQArray[i] = westSideQueueSize;
			eastSideQArray[i] = eastSideQueueSize;
			carPassingTSum+=carPassingTValue;
			carPassingTArray[i]=carPassingTValue;

			if(tfState.startsWith("eastMoving")) {
				eastMovingGreen++;
				eastGreenElapseTime_sum +=elapseTimeInGreen; 
				combinedElapseTime_sum +=elapseTimeInGreen;
			}
			else if(tfState.startsWith("westMoving")) {
				westMovingGreen++;
				westGreenElapseTime_sum +=elapseTimeInGreen; 
				combinedElapseTime_sum +=elapseTimeInGreen;
			}
						
			int eastMovingDepartureCount = particleArray[selectedParticles[i]].trand.eastMoving_dptCarCount;
			int eastMovingArrivalCount = particleArray[selectedParticles[i]].trand.eastMoving_arrvCarCount;

			westSideQueueRMSE_step+=(westSideQueueSize-realStateArray[step].westSideQueue)*(westSideQueueSize-realStateArray[step].westSideQueue);
			eastSideQueueRMSE_step+=(eastSideQueueSize-realStateArray[step].eastSideQueue)*(eastSideQueueSize-realStateArray[step].eastSideQueue);
			
			step_result.println(i+"\t"+tfState+"\t"+sigma+"\t"+elapseTimeInGreen+"\t"+westSideQueueSize+"\t"+eastSideQueueSize+"\t"
			+carPassingTValue+"\t"+eastMovingDepartureCount+"\t"+eastMovingArrivalCount+"\t"+preselectedParticles[i]);
		}

		double aveWestSideQueue = westSideQueueSizeSum/PF_configData.numberofparticles;
		double aveEastSideQueue = eastSideQueueSizeSum/PF_configData.numberofparticles;
		double westSideQsize_std, westSideQsize_std_sum=0, eastSideQsize_std, eastSideQsize_std_sum=0;
		double aveCarPassingT = carPassingTSum/PF_configData.numberofparticles;
		double carPassingT_std, carPassingT_std_sum=0;
		for(int i=0; i<PF_configData.numberofparticles;i++) {
			westSideQsize_std_sum+= (westSideQArray[i]-aveWestSideQueue)*(westSideQArray[i]-aveWestSideQueue);
			eastSideQsize_std_sum+= (eastSideQArray[i]-aveEastSideQueue)*(eastSideQArray[i]-aveEastSideQueue);
			carPassingT_std_sum+= (carPassingTArray[i]-aveCarPassingT)*(carPassingTArray[i]-aveCarPassingT);
		}
		westSideQsize_std = Math.sqrt(westSideQsize_std_sum/PF_configData.numberofparticles);
		eastSideQsize_std = Math.sqrt(eastSideQsize_std_sum/PF_configData.numberofparticles);
		carPassingT_std = Math.sqrt(carPassingT_std_sum/PF_configData.numberofparticles);
		
//		westSideQueueRMSE_step=(aveWestSideQueue-realStateArray[step].westSideQueue)*(aveWestSideQueue-realStateArray[step].westSideQueue);
//		eastSideQueueRMSE_step=(aveEastSideQueue-realStateArray[step].eastSideQueue)*(aveEastSideQueue-realStateArray[step].eastSideQueue);
		westSideQueueRMSE_step=westSideQueueRMSE_step/PF_configData.numberofparticles;
		eastSideQueueRMSE_step=eastSideQueueRMSE_step/PF_configData.numberofparticles;
		RMSE_overallSum += Math.sqrt((westSideQueueRMSE_step+eastSideQueueRMSE_step)/2.0);
				
		DA_result.println(step+"\t"+step*PF_configData.stepInterval
				+"\t"+((realStateArray[step].TrafficLightState.startsWith("eastMoving"))? 20: 0)
				+"\t"+realStateArray[step].elapseTimeInGreen
				+"\t"+realStateArray[step].westSideQueue
				+"\t"+realStateArray[step].eastSideQueue
				+"\t"+realStateArray[step].carPassingTime
				+"\t"+(eastMovingGreen*1.0/PF_configData.numberofparticles*20.0)
				+"\t"+((eastMovingGreen!=0)?eastGreenElapseTime_sum/eastMovingGreen:0)
				+"\t"+(westMovingGreen*1.0/PF_configData.numberofparticles*20.0)
				+"\t"+((westMovingGreen!=0)?westGreenElapseTime_sum/westMovingGreen:0)
				+"\t"+combinedElapseTime_sum/PF_configData.numberofparticles
				+"\t"+aveWestSideQueue
				+"\t"+westSideQsize_std
				+"\t"+aveEastSideQueue
				+"\t"+eastSideQsize_std
				+"\t"+aveCarPassingT
				+"\t"+carPassingT_std
				);
		
    	
    }
    
    public void samplingAndComputeWeights(int step) {
       	for (int idx = 0; idx < PF_configData.numberofparticles; idx++) {  // for each particle
            //
//       		if(step==53 && idx==564)
//       		System.out.println("PF algorithm Step:" + step + "particles:" + idx);

            PF_configData.currentParticleIndex = idx; // set the index to be used by cell space model
            trafficControlSys_DA model;
            
            // --------------- create particle -----------------------------     	
    		String tfState;
    		double phaseSigma;
    		double elaspedGreenTime;
    		int westSideQueueSize,eastSideQueueSize;
    		double carPassingTime_mean;
    		
        	if(step==0) {
        		// initialize the state using random distribution
        		if(globalRand.nextDouble()>=0.5) { // eastMoving is green
        			tfState = "eastMoving_passive";
        			phaseSigma = DevsInterface.INFINITY; // all Infinity?
        			elaspedGreenTime= model_configData.maxElapseTimeWhenBusy*globalRand.nextDouble();
        		}
        		else {
        			tfState = "westMoving_passive";
        			phaseSigma = DevsInterface.INFINITY; // all Infinity?
        			elaspedGreenTime= model_configData.maxElapseTimeWhenBusy*globalRand.nextDouble();        			
        		}
        		// randomly sample the queue size
        		int westSideQueue_initCapacityBound = 40;
        		int eastSideQueue_initCapacityBound = 40;
        		westSideQueueSize = (int)(globalRand.nextDouble() * westSideQueue_initCapacityBound); //20;
        		eastSideQueueSize = (int)(globalRand.nextDouble() * eastSideQueue_initCapacityBound); //20;
        	
        		//randomly sample the carPassingTime_mean between carPassingTime_LowBound and carPassingTime_UpBound
        		carPassingTime_mean = model_configData.carPassingTime_mean;
//        		carPassingTime_mean = model_configData.carPassingTime_LowBound
//        				+(model_configData.carPassingTime_UpBound-model_configData.carPassingTime_LowBound)*globalRand.nextDouble();
        	}
        	else{
        		//initialize the state From Previous Particles
        		tfState = particleStateArray[selectedParticles[idx]].TrafficLightState; 
        		phaseSigma = particleStateArray[selectedParticles[idx]].sigma;
        		elaspedGreenTime= particleStateArray[selectedParticles[idx]].elapseTimeInGreen;
				westSideQueueSize = particleStateArray[selectedParticles[idx]].westSideQueue;
				eastSideQueueSize = particleStateArray[selectedParticles[idx]].eastSideQueue;
				carPassingTime_mean = particleStateArray[selectedParticles[idx]].carPassingTime;
//           		if(step==53 && idx==564) {
//               		System.out.println("***debug ---- initialize " );
//               		System.out.println(selectedParticles[idx]+"  "+particleStateArray[selectedParticles[idx]]);
//           		}

				// Add noise to the state --- we do it here because DEVS model is a deterministic model
			
				//For now, we do not add any noise to the tfState and phaseSigma
				tfState=tfState;
				phaseSigma = phaseSigma;
				
				/**
				 * We use a Gaussian distribution to add noise to the elaspedGreenTime 
				 * If the new elaspedGreenTime is less than 0 or larger than the traffic duration 
				 * for the corresponding traffic state, we change the traffic state to the other state
				 * and update the elaspedGreenTime accordingly.
				 * 
				 * For a Gaussian, distribution, there is 68% chance of falling in between plus minus sigma range
				 * 95% chance of falling in between pluse minus 2*sigma range
				 */
				double elaspedGreenTimeSigma = 2;//3; // 2 seems to be good, 1 is too small, 3 needs further test 
				double noiseBound = 2*elaspedGreenTimeSigma;
				double newElaspedGreenTime = getTruncatedNormalDistribution(elaspedGreenTime-noiseBound,
						elaspedGreenTime+noiseBound,elaspedGreenTime, elaspedGreenTimeSigma);
				if(newElaspedGreenTime<0)
					newElaspedGreenTime =0;
				elaspedGreenTime = newElaspedGreenTime;
				
				// Add Gaussian noise to the Queue length
				int Queue_maxCapacity = 100;
				double QueueLengthSigmaPct =  0.2; // 20% of the current queue length
				//System.out.println("before -- westSideQueueSize:"+westSideQueueSize+" eastSideQueueSize:"+eastSideQueueSize);
				double QueueLengthSigma = /*(westSideQueueSize<5)? 1.0:*/ QueueLengthSigmaPct*westSideQueueSize;
				double QnoiseBound = 5; // limit the change to 5
				double noiseLevel = getTruncatedNormalDistribution(-QnoiseBound, QnoiseBound, 0, QueueLengthSigma);
				westSideQueueSize = westSideQueueSize+ (int)noiseLevel; 
				if(westSideQueueSize<0) westSideQueueSize=0;
				if(westSideQueueSize>Queue_maxCapacity) westSideQueueSize=Queue_maxCapacity;
				QueueLengthSigma = /*(eastSideQueueSize<5)? 1.0:*/ QueueLengthSigmaPct*eastSideQueueSize;
				noiseLevel = getTruncatedNormalDistribution(-QnoiseBound, QnoiseBound, 0, QueueLengthSigma);
				eastSideQueueSize = eastSideQueueSize+ (int)noiseLevel; 
				if(eastSideQueueSize<0) eastSideQueueSize=0;
				if(eastSideQueueSize>Queue_maxCapacity) eastSideQueueSize=Queue_maxCapacity;
				//System.out.println("after -- westSideQueueSize:"+westSideQueueSize+" eastSideQueueSize:"+eastSideQueueSize);       	
        	}
        	// create the model
    		model = new trafficControlSys_DA(
    				"Model"+idx, model_configData.generator_eastMoving_lambda, globalRand, 
    				model_configData.generator_westMoving_lambda, globalRand,
    				tfState, phaseSigma, elaspedGreenTime, westSideQueueSize, eastSideQueueSize, carPassingTime_mean, globalRand); 
    		particleArray[idx]= model;
    		
            // --------------- run simulation -----------------------------     	
                coordinator temp_c = new coordinator(model);
                temp_c.initialize();
                temp_c.simulate_TN(PF_configData.stepInterval);
                //System.gc();		

            // --------------- collect simulation results and store in stateList -----------------------------     	
                if(!model.trand.stateDataReceived)
                	System.out.println("State Data Not Received!!!");
                String TrafficLightState = model.trand.TrafficLightState; 
                double phaseSgm = model.trand.phaseSigma;
                double elapseTimeInGreen = model.trand.elapseTimeInGreen;
                int westSideQueue = model.trand.westSideQueue;
                int eastSideQueue = model.trand.eastSideQueue;
                double carPassT = model.trand.carPassingTime_mean;
                
                stateEntity sEnt = new stateEntity(westSideQueue, eastSideQueue, TrafficLightState, phaseSgm, elapseTimeInGreen, carPassT);
                newParticleStateArray[idx]=sEnt;
//           		if(step==52 && idx==521) {
//               		System.out.println("***debug: after simulation");      
//               		System.out.println(idx+"  "+particleStateArray[idx]);
//           		}
            // --------------- compute weight -----------------------------     	
				double wt = computeWeight(step, model.trand);
				weight[idx]= wt;
				//System.out.println("particle "+idx+" weight="+wt);                
       	}
    	
    }

    public double computeWeight(int step, transducer_DA trand) {
    	double weightSigma_constant = 0.6;//0.8;
    	// get the measurement data from the transducer model (trand) for this particle
    	int eastMoving_dptCarCount = trand.eastMoving_dptCarCount;
    	int westMoving_arrivalCarCount = trand.westMoving_arrvCarCount;
    	
    	// get the real sensor data at this step
    	int eastMoving_dptCarCount_real = sensorDataArray[step].eastMoving_DepartureCount;
    	int westMoving_arrivalCarCount_real = sensorDataArray[step].westMoving_ArrivalCount;
    	
    	double diff1 = eastMoving_dptCarCount-eastMoving_dptCarCount_real;
    	double diff2 = westMoving_arrivalCarCount-westMoving_arrivalCarCount_real;
    	
    	double sigma1 = weightSigma_constant;
    	double sigma2 = weightSigma_constant;
    	
    	double weight1 = Math.exp( -diff1 * diff1 / (2 * sigma1 * sigma1));
    	double weight2 = Math.exp( -diff2 * diff2 / (2 * sigma2 * sigma2));

//    	int eastMoving_arrivalCarCount = trand.eastMoving_arrvCarCount;
//    	int westMoving_dptCarCount = trand.westMoving_dptCarCount;
//    	int eastMoving_arrivalCarCount_real = sensorDataArray[step].eastMoving_ArrivalCount;
//    	int westMoving_dptCarCount_real = sensorDataArray[step].westMoving_DepartureCount;
//    	double diff3 = eastMoving_arrivalCarCount-eastMoving_arrivalCarCount_real;
//    	double diff4 = westMoving_dptCarCount-westMoving_dptCarCount_real;
////    	double sigma3 = (eastMoving_arrivalCarCount_real<=3)? 0.6: eastMoving_arrivalCarCount_real*sigmaPct;
////    	double sigma4 = (westMoving_dptCarCount_real<=3)? 0.6: westMoving_dptCarCount_real*sigmaPct;
//    	double sigma3 = weightSigma_constant;
//    	double sigma4 = weightSigma_constant;
//    	double weight3 = Math.exp( -diff3 * diff3 / (2 * sigma3 * sigma3));
//    	double weight4 = Math.exp( -diff4 * diff4 / (2 * sigma4 * sigma4));
    	
    	double weight = weight1*weight2;//*weight3*weight4;
    	return weight;
    }

    void normalizeWeights(int totalNumOfParticles) {
    	double sumofWeights = 0;
    	for (int i = 0; i < totalNumOfParticles; i++) {
    		sumofWeights = sumofWeights + weight[i];
    	}
    	for (int j = 0; j < totalNumOfParticles; j++) {
    		normalizedWeight[j]= weight[j] / sumofWeights;
    	}

    }

    // the following method implements a standard resampling method
    public void resampling(int totalNumOfParticles, int[] selectedParticles) {
    	double u_temperature[] = new double[totalNumOfParticles];
    	double q_temperature[] = new double[totalNumOfParticles];

    	for (int i = 0; i < totalNumOfParticles; i++) {
    		if (i == 0) {
    			q_temperature[i] = normalizedWeight[0];
    		}
    		else{
    			q_temperature[i] = q_temperature[i - 1] + normalizedWeight[i];
    		}
    	}

    	for (int j = 0; j < totalNumOfParticles; j++) {
    		u_temperature[j] = globalRand.nextDouble();
    	}
    	java.util.Arrays.sort(u_temperature);

    	int sampleIndex = 0;
    	for (int j = 0; j < totalNumOfParticles; j++) {
    		while (q_temperature[sampleIndex] < u_temperature[j]) {
    			sampleIndex = sampleIndex + 1;
    		}
    		selectedParticles[j] = sampleIndex;
    	}
    }

    public void readRealSensorData(){
		String realSensorDataFile = PF_configData.DataPathName+PF_configData.realSensorDataFileName; // the read step is step+1
		try{
			FileInputStream MyInputStream = new FileInputStream(realSensorDataFile);
			TextReader input;
			input = new TextReader(MyInputStream);
			
			//skip the first line
			input.readLine();

			for(int i=0;i<PF_configData.numberofsteps;i++){
				//System.out.println("read data for step "+i);
				input.readDouble(); // skip the time
				int eastMoving_arriv = input.readInt();
				int westMoving_arriv = input.readInt(); // skip the westMoving arrival count
				int eastMoving_depart = input.readInt();
				int westMoving_depart = input.readInt(); // skip the westMoving departure
				
				int westSideQueue = input.readInt();
				int eastSideQueue = input.readInt();
				int trafficState = input.readInt();
				double sgm = input.readDouble();
				double remainingT = input.readDouble();
				double carPassingT = input.readDouble();
				String aa=input.readLine(); // skip the rest of the line
				
				sensorDataArray[i] = new sensorDataEntity(i,eastMoving_arriv, eastMoving_depart, westMoving_arriv, westMoving_depart);
				realStateArray[i] = new stateEntity(westSideQueue, eastSideQueue, ((trafficState==20)?"eastMovingGreen":"westMovingGreen"), sgm, remainingT, carPassingT);
			}
		}
		catch (IOException e){
			throw new RuntimeException(e.toString());
		}
    }
	
    public static void main(String[] args){
    	long starttime = System.currentTimeMillis();
    	PF PFA = new PF(0);
        PFA.startComputing(0);
        long endtime = System.currentTimeMillis();
        double resulttime = (double)((endtime - starttime)*0.001);
        System.out.println("Particle number:"+PF_configData.numberofparticles+" RMSE = "+PFA.RMSE_overallSum/PF_configData.numberofsteps);
        System.out.println("Total time is: "+resulttime);
        
        
//        // The follow code is used if using batch run
//        int numberOfRuns = 20;
//        double[] RMSE = new double[numberOfRuns];
//        double[] runTime = new double[numberOfRuns];
//        double RMSE_sum=0, RMSE_ave=0;
//        double runTime_sum=0, runTime_ave=0;
//        for(int i=0;i<numberOfRuns;i++) {
//        	System.out.println("************************* Start run:"+i);
//        	long starttime = System.currentTimeMillis();
//        	PF PFA = new PF(i);
//        	PFA.batchRun=true;
//        	PFA.startComputing(i);
//        	long endtime = System.currentTimeMillis();
//        	double resulttime = (double)((endtime - starttime)*0.001);
//        	RMSE[i]=PFA.RMSE_overallSum/PF_configData.numberofsteps;
//        	RMSE_sum+=RMSE[i];
//        	runTime[i]=resulttime;
//        	runTime_sum+=resulttime;
//        }
//        RMSE_ave=RMSE_sum/numberOfRuns;
//        runTime_ave = runTime_sum/numberOfRuns;
//        double stdv_sum = 0;
//        for(int i=0; i<numberOfRuns; i++) {
//        	System.out.println("RMSE_"+i+" = "+RMSE[i]+" runTime="+runTime[i]);
//        	stdv_sum+=(RMSE[i]-RMSE_ave)*(RMSE[i]-RMSE_ave);
//        }
//        double stdv = Math.sqrt(stdv_sum/numberOfRuns);
//        
//        // calculate the median and 25th and 75th percentiles
//        Arrays.sort(RMSE, 0, RMSE.length);
//        System.out.println("25th percentiles="+percentile(RMSE, 25));
//        System.out.println("50th percentiles="+percentile(RMSE, 50));
//        System.out.println("75th percentiles="+percentile(RMSE, 75));
//        
//        System.out.println("CASEII: particle number:"+PF_configData.numberofparticles+" RMSE_ave = "+RMSE_ave+" stdv="+stdv+ " runTime_ave = "+runTime_ave);
        
     }
	
   /////////////////////////////////////////////////////////////////////////////////
    private double getTruncatedNormalDistribution(double low, double high, double mean, double sigma) {
    	double u = mean + sigma * globalRand.nextGaussian();
    	while (u<low || u > high) // resample 
    		u= mean + sigma * globalRand.nextGaussian();
    	return u;
    }
    
    public static double percentile(double[] latencies, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * latencies.length);
        return latencies[index-1];
    }
}
