/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.HashMap;

import org.locationtech.jts.geom.Geometry;

public class DrainageArea {
	
	private final double area;
	private final Geometry g;
	private String id;
	private ArrayList<Coverage> coverage;
	
	//Pour l'affichage des classe dans la vue
	private double class1 = new Double(0);
	private double class2 = new Double(0);
	private double class3 = new Double(0);
	private double class4 = new Double(0);
	private double class5 = new Double(0);
	private double class6 = new Double(0);
	private double class7 = new Double(0);
	private double class8 = new Double(0);
	private double class9 = new Double(0);
	private double class10 = new Double(0);
	private double class11 = new Double(0);
	private double class12 = new Double(0);
	private double class13 = new Double(0);
	private double class14 = new Double(0);
	private double class15 = new Double(0);
	private double class16 = new Double(0);
	private double class17 = new Double(0);
	private double class18 = new Double(0);
	private double class19 = new Double(0);
		
	private HashMap<ECatchment.ECatchmentStat, Double> areaStats;
	
	public DrainageArea(Geometry g, double area) {
		this(g, area, null);
	}
	
	public DrainageArea(Geometry g, double area, String id) {
		this.area = area;
		this.g = g;
		this.id = id;
	}
		
	public DrainageArea(Geometry g, double area, String id,ArrayList<Coverage> coverage)
	{
		this.area = area;
		this.g = g;
		this.id = id;
		this.coverage = coverage;
	}

	public double getArea() {
		return area;
	}

	public Geometry getGeometry() {
		return g;
	}
	
	public String getId() {
		return this.id;
	}
	
	public ArrayList<Coverage> getCoverage()
	{
		return this.coverage;
	}
	
	public void setCoverage (ArrayList<Coverage> coverage)
	{
		this.coverage = coverage;
		setCoverageValue(coverage);
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public boolean hasStats() {
		return areaStats != null && areaStats.size() > 0;
	}
	
	
	public void setStat(ECatchment.ECatchmentStat stat, Double value) {
		if (areaStats == null) areaStats = new HashMap<>();
		areaStats.put(stat, value);
	}
	

	public Double getStat(ECatchment.ECatchmentStat stat) {
		if (areaStats == null) return null;
		return areaStats.get(stat);
	}
	
	private void setCoverageValue(ArrayList<Coverage> covList)
	{
		for(int i = 0; i < covList.size(); i++)
		{
			switch (covList.get(i).getId()) 
			{
			case 1:
				this.setClass1(covList.get(i).getPourcent());
				break;
			case 2:
				this.setClass2(covList.get(i).getPourcent());
				break;
			case 3:
				this.setClass3(covList.get(i).getPourcent());
				break;
			case 4:
				this.setClass4(covList.get(i).getPourcent());
				break;
			case 5:
				this.setClass5(covList.get(i).getPourcent());
				break;
			case 6:
				this.setClass6(covList.get(i).getPourcent());
				break;
			case 7:
				this.setClass7(covList.get(i).getPourcent());
				break;
			case 8:
				this.setClass8(covList.get(i).getPourcent());
				break;
			case 9:
				this.setClass9(covList.get(i).getPourcent());
				break;
			case 10:
				this.setClass10(covList.get(i).getPourcent());
				break;
			case 11:
				this.setClass11(covList.get(i).getPourcent());
				break;
			case 12:
				this.setClass12(covList.get(i).getPourcent());
				break;
			case 13:
				this.setClass13(covList.get(i).getPourcent());
				break;
			case 14:
				this.setClass14(covList.get(i).getPourcent());
				break;
			case 15:
				this.setClass15(covList.get(i).getPourcent());
				break;
			case 16:
				this.setClass16(covList.get(i).getPourcent());
				break;
			case 17:
				this.setClass17(covList.get(i).getPourcent());
				break;
			case 18:
				this.setClass18(covList.get(i).getPourcent());
				break;
			case 19:
				this.setClass19(covList.get(i).getPourcent());
				break;
			}
		}
		
	}
	
	//GET et SET des class de 1 à 19

	public double getClass1() {
		return class1;
	}

	public void setClass1(double class1) {
		this.class1 = class1;
	}

	public double getClass2() {
		return class2;
	}

	public void setClass2(double class2) {
		this.class2 = class2;
	}

	public double getClass3() {
		return class3;
	}

	public void setClass3(double class3) {
		this.class3 = class3;
	}

	public double getClass4() {
		return class4;
	}

	public void setClass4(double class4) {
		this.class4 = class4;
	}

	public double getClass5() {
		return class5;
	}

	public void setClass5(double class5) {
		this.class5 = class5;
	}

	public double getClass6() {
		return class6;
	}

	public void setClass6(double class6) {
		this.class6 = class6;
	}

	public double getClass7() {
		return class7;
	}

	public void setClass7(double class7) {
		this.class7 = class7;
	}

	public double getClass8() {
		return class8;
	}

	public void setClass8(double class8) {
		this.class8 = class8;
	}

	public double getClass9() {
		return class9;
	}

	public void setClass9(double class9) {
		this.class9 = class9;
	}

	public double getClass10() {
		return class10;
	}

	public void setClass10(double class10) {
		this.class10 = class10;
	}

	public double getClass11() {
		return class11;
	}

	public void setClass11(double class11) {
		this.class11 = class11;
	}

	public double getClass12() {
		return class12;
	}

	public void setClass12(double class12) {
		this.class12 = class12;
	}

	public double getClass13() {
		return class13;
	}

	public void setClass13(double class13) {
		this.class13 = class13;
	}

	public double getClass14() {
		return class14;
	}

	public void setClass14(double class14) {
		this.class14 = class14;
	}

	public double getClass15() {
		return class15;
	}

	public void setClass15(double class15) {
		this.class15 = class15;
	}

	public double getClass16() {
		return class16;
	}

	public void setClass16(double class16) {
		this.class16 = class16;
	}

	public double getClass17() {
		return class17;
	}

	public void setClass17(double class17) {
		this.class17 = class17;
	}

	public double getClass18() {
		return class18;
	}

	public void setClass18(double class18) {
		this.class18 = class18;
	}

	public double getClass19() {
		return class19;
	}

	public void setClass19(double class19) {
		this.class19 = class19;
	}
}
