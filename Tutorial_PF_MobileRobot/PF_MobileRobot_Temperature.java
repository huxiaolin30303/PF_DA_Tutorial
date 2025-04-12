package Tutorial_PF_MobileRobot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;


public class PF_MobileRobot_Temperature {
	static int stepNum = 40;
	static double deltT = 1.0;

	static int numberofparticles=2000;
	public static Random rand;

	static String folderName = "PF_VehicleTemp";

	static double realSpeed_mean= 10;//15.0;
	static double processNoise_P_stdv = realSpeed_mean*deltT/7.5;//0.5;
	static double processNoise_V_stdv = 0.5;//realSpeed_mean/10; //1.0;
	static double measurementNoise_stdv = 2.0; //1.5;

	// The following are for real observation data generation 
	static double[] realPosition = new double[stepNum];
	static double[] realVelocity = new double[stepNum];
	static double[] realObservation = new double[stepNum]; // temperature

	public static double[] weight = new double[numberofparticles]; // save particle's weights -- not normalized
	public static double[] normalizedWeight = new double[numberofparticles];
	static int[] selectedParticles = new int[numberofparticles];
	//static int[] preselectedParticles = new int[numberofparticles];
	static double[] particlePositionArray = new double[numberofparticles];
	static double[] particleVelocityArray = new double[numberofparticles];

	static double[] newParticlePositionArray = new double[numberofparticles];
	static double[] newParticleVelocityArray = new double[numberofparticles];

	static PrintWriter real_state_observation_Data, DA_result, step_result, step_prediction_weight;


	public static void main(String[] args) {
		rand = new Random(1999999);

//		generateRealObsevationData();  // run the "true system" simulation to generate real observation data

		ParticleFilter();

	}

	public static void ParticleFilter() {
		//First, read the sensor data
		readRealObservationData();

		try{
			DA_result = new PrintWriter(new FileOutputStream(folderName+"/DAResult.txt"), true);
		}
		catch (FileNotFoundException e) {System.out.println("File creation error!!!!!");}

		DA_result.println("step"
				+"\t"+"realPosition"
				+"\t"+"realVelocity"
				+"\t"+"DA_position"
				+"\t"+"DA_position_stdv"
				+"\t"+"DA_velocity"
				+"\t"+"DA_velocity_stdv"
				);

		// Implement the bootstrap filter algorithm
		for(int step=0; step<stepNum; step++){
			System.out.println("----------------------------------PF algorithm Step:" + step);

			try{
				step_prediction_weight = new PrintWriter(new FileOutputStream(folderName+"/pred_weight_step_"+step+".txt"), true);
				step_result = new PrintWriter(new FileOutputStream(folderName+"/step_"+step+".txt"), true);
			}
			catch (FileNotFoundException e) {System.out.println("File creation error!!!!!");}
			step_result.println("particleIdx"+"\t"+"position"+"\t"+"velocity");
			step_prediction_weight.println("particleIdx"+"\t"+"realPosition"+"\t"+"prePosition"+"\t"+"preVelocity"
					+"\t"+"weight"+"\t"+"resampled_position"+"\t"+"resampled_velocity");

			samplingAndComputeWeights(step);
			//    		for(int i=0; i<numberofparticles;i++)
			//    			preselectedParticles[i]=selectedParticles[i];

			// save the new state 
			for(int i=0; i<numberofparticles;i++) {
				particlePositionArray[i]=newParticlePositionArray[i];
				particleVelocityArray[i]=newParticleVelocityArray[i];
			}
			normalizeWeights(numberofparticles);

			resampling(numberofparticles, selectedParticles);    		

			for(int i=0;i<numberofparticles; i++)
				step_prediction_weight.println(i+"\t"+realPosition[step]+"\t"+particlePositionArray[i]+"\t"+particleVelocityArray[i]+"\t"+normalizedWeight[i]
						+"\t"+particlePositionArray[selectedParticles[i]]+"\t"+particleVelocityArray[selectedParticles[i]]);

			analyzeAndSaveResults(step);
		}
		System.out.println("!!!!!!!!!!!!!finishing all steps of PF algorithm");
	}

	public static void analyzeAndSaveResults(int step) {
		// summarize estimation results 
		double position_sum=0, velocity_sum=0; 

		for (int i=0;i<numberofparticles;i++){
			//			if(step == 53 && i==564)
			//    		 System.out.println("debug "+i+" "+preselectedParticles[i]+" "+particleStateArray[selectedParticles[i]].AQueue);

			position_sum+=particlePositionArray[selectedParticles[i]];
			velocity_sum+=particleVelocityArray[selectedParticles[i]];

			if(particlePositionArray[selectedParticles[i]]>=0 && particlePositionArray[selectedParticles[i]]<=400)
				step_result.println(selectedParticles[i]+"\t"+particlePositionArray[selectedParticles[i]]+"\t"+particleVelocityArray[selectedParticles[i]]);
		}

		//Add two boundary value to help histogram diagram to have a uniform x-range display  
		step_result.println("-1"+"\t"+"0"+"\t"+"0");
		step_result.println("-1"+"\t"+"400"+"\t"+"20");

		double avePosition = position_sum/numberofparticles;
		double aveVelocity = velocity_sum/numberofparticles;
		double pos_std_sum=0, vel_std_sum=0;
		for(int i=0; i<numberofparticles;i++) {
			pos_std_sum+= (particlePositionArray[selectedParticles[i]]-avePosition)*(particlePositionArray[selectedParticles[i]]-avePosition);
			vel_std_sum+= (particleVelocityArray[selectedParticles[i]]-aveVelocity)*(particleVelocityArray[selectedParticles[i]]-aveVelocity);
		}
		double position_std = Math.sqrt(pos_std_sum/numberofparticles);
		double velocity_std = Math.sqrt(vel_std_sum/numberofparticles);

		DA_result.println(step
				+"\t"+realPosition[step]
						+"\t"+realVelocity[step]
								+"\t"+avePosition
								+"\t"+position_std
								+"\t"+aveVelocity
								+"\t"+velocity_std
				);		
	}

	public static void samplingAndComputeWeights(int step) {
		for (int idx = 0; idx < numberofparticles; idx++) {  // for each particle
			double pos, vel;
			vehicleModel simulationModel;//
			// --------------- create the particle -----------------------------     	
			if(step==0) {
				// initialize the state using a uniform distribution
				double initPositionLow = 0, initPositionHigh=500;
				double initVelocityLow = 0, initVelocityHigh=50;
				pos = initPositionLow+(initPositionHigh-initPositionLow)*rand.nextDouble();
				vel = initVelocityLow+(initVelocityHigh-initVelocityLow)*rand.nextDouble();
			}
			else{
				//initialize the state from Previous Particles
				pos = particlePositionArray[selectedParticles[idx]]; 
				vel= particleVelocityArray[selectedParticles[idx]];
			}
			// create the simulation model
			simulationModel = new vehicleModel(pos,vel); 
			// --------------- run simulation to the next time step -----------------------------     	
			simulationModel.stateTransition(deltT);
			// --------------- collect simulation results and store them  -----------------------------     	
			newParticlePositionArray[idx]=simulationModel.pos;
			newParticleVelocityArray[idx]=simulationModel.vel;
			// --------------- compute weight -----------------------------     	
			double wt = computeWeight(step, newParticlePositionArray[idx], newParticleVelocityArray[idx]);
			weight[idx]= wt;
			//System.out.println("particle "+idx+" weight="+wt);       				
		}
	}

	public static double computeWeight(int step, double p, double v) {
		double predictedTemperature = measurementModel(p);
		double tempDiff = realObservation[step]-predictedTemperature;

		double weight = Math.exp( -tempDiff * tempDiff / (2 * measurementNoise_stdv * measurementNoise_stdv));
		return weight;
	}

	static void normalizeWeights(int totalNumOfParticles) {
		double sumofWeights = 0;
		for (int i = 0; i < totalNumOfParticles; i++) {
			sumofWeights = sumofWeights + weight[i];
		}
		for (int j = 0; j < totalNumOfParticles; j++) {
			normalizedWeight[j]= weight[j] / sumofWeights;
		}

	}

	// the following method implements a standard resampling method
	public static void resampling(int totalNumOfParticles, int[] selectedParticles) {
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
			u_temperature[j] = rand.nextDouble();
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

	public static void generateRealObsevationData() {
		try{
			real_state_observation_Data = new PrintWriter(new FileOutputStream(folderName+"/real_state_observation_Data.txt"), true);
		}
		catch (FileNotFoundException e) {System.out.println("File creation error!!!!!");}
		real_state_observation_Data.println("Time"+"\t"+"realPosition"+"\t"+"realVelocity"+"\t"+"realObservation");

		// set the "true system" initial condition
		double initPosition = 20.0;  // initial location
		double initVelocity = realSpeed_mean;
		//System.out.println(initPosition+"\t"+initVelocity);

		//vehicleModel vM = new vehicleModel(initPosition, initVelocity);

		for(int i=0; i<stepNum; i++) {
			double noise_p = rand.nextGaussian()*processNoise_P_stdv;
			double noise_v = 0;//rand.nextGaussian()*processNoise_V_stdv;
			if(i==0) { // first step
				realPosition[i]=initPosition+initVelocity*deltT+noise_p;
				realVelocity[i]=initVelocity+noise_v; // no change except for noise
			}
			else {
				realPosition[i]=realPosition[i-1]+realVelocity[i-1]*deltT+noise_p;
				realVelocity[i]=realVelocity[i-1]+noise_v; // no change except for noise
			}

			//    		vM.stateTransition(deltT);
			//    		realPosition[i] = vM.pos; 
			//    		realVelocity[i] = vM.vel;    		

			realObservation[i] = measurementModel(realPosition[i]);
			real_state_observation_Data.println(i*deltT+"\t"+realPosition[i]+"\t"+realVelocity[i]+"\t"+realObservation[i]);
			//System.out.println(realPosition[i]+"\t"+realVelocity[i]+"\t"+realObservation[i]);
		}    	
		
		System.out.println("Real Obsevation Data Generated!");
	}

	public static void readRealObservationData(){
		String realSensorDataFile = folderName+"/real_state_observation_Data.txt";
		try{
			FileInputStream MyInputStream = new FileInputStream(realSensorDataFile);
			TextReader input;
			input = new TextReader(MyInputStream);

			//skip the first line
			input.readLine();

			for(int i=0;i<stepNum;i++){
				System.out.println("read data for step "+i);
				input.readDouble(); // skip the time
				realPosition[i] = input.readDouble(); 
				realVelocity[i] = input.readDouble();    		
				realObservation[i] = input.readDouble();
			}
		}
		catch (IOException e){
			throw new RuntimeException(e.toString());
		}
	}

	public static double measurementModel(double position) {
		double firePlace_loc1 = 80;
		double firePlace_loc2 = 200;//140;
		double tempeture_distance_Sigma = 15;

		double temperature1 = 30*Math.exp(-1*Math.pow(((position-firePlace_loc1)/tempeture_distance_Sigma),2))+10;
		double temperature2 = 30*Math.exp(-1*Math.pow(((position-firePlace_loc2)/tempeture_distance_Sigma),2))+10;
		double temperature=0;
		if(temperature1>=temperature2)
			temperature=temperature1;
		else
			temperature=temperature2;

		// add noise
		double noise_measurement = rand.nextGaussian()*measurementNoise_stdv;
		temperature+=noise_measurement;
		return temperature;
	}


	private static class vehicleModel{
		double pos, vel;

		vehicleModel(double p, double v){
			pos = p;
			vel = v;
		}

		void stateTransition(double deltaT){
			double noise_p = rand.nextGaussian()*processNoise_P_stdv;
			double noise_v = rand.nextGaussian()*processNoise_V_stdv;
			pos+=vel*deltaT+noise_p;
			vel+=noise_v; // no change except for noise
		}
	}

}
