package org.alljoyn.bus.sample.chat;

public class ServAdv {
	
	private int varBatteryStatus;
	private int varCPUUsage;
	private int varMemoryUsage;
	private int varCurrentVoltage;
	
	public void setVarBatteryStatus(int bs){
		varBatteryStatus = bs;
	}
	
	public void setVarCPUUsage(int cpuUsage){
		varCPUUsage = cpuUsage;
	}
	
	public void setVarMemoryUsage(int memUsage){
		varMemoryUsage= memUsage;
	}
	
	public void setVarCurrentVoltage(int currentVoltage){
		varCurrentVoltage = currentVoltage;
	}
	
	public int setVarBatteryStatus(){
		return varBatteryStatus;
	}
	
	public int setVarCPUUsage(){
		return varCPUUsage;
	}
	
	public int getVarMemoryUsage(){
		return varMemoryUsage;
	}
	
	public int getVarCurrentVoltage(){
		return varCurrentVoltage;
	}
	
	
}
