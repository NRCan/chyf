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
import java.util.Collections;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.Rank;
import net.refractions.chyf.indexing.SpatiallyIndexable;

public class ECatchment implements SpatiallyIndexable {
	
	public static enum ECatchmentStat{
		MIN_ELEVATION("ELV_MIN"),
		MAX_ELEVATION("ELV_MAX"),
		MEAN_ELEVATION("ELV_MEAN"),
		
		MIN_SLOPE("SLOPE_MIN"),
		MAX_SLOPE("SLOPE_MAX"),
		MEAN_SLOPE("SLOPE_MEAN"),
		
		ASPECT_NORTH_PCT("NORTH_PCT"),
		ASPECT_SOUTH_PCT("SOUTH_PCT"),
		ASPECT_EAST_PCT("EAST_PCT"),
		ASPECT_WEST_PCT("WEST_PCT"),
		ASPECT_FLAT_PCT("FLAT_PCT"),
		
		MEAN_D2W_2D("D2W2D_MEAN"),
		MAX_D2W_2D("D2W2D_MAX");
		
		private String shpName;
		
		ECatchmentStat(String shapeName){
			this.shpName = shapeName;
		}
		
		public String getFieldName() {
			return this.shpName;
		}
		
		public void updateCatchment(ECatchment c, double value){
			switch(this) {
			case MAX_ELEVATION:
				c.maxElevation = value; return;
			case MIN_ELEVATION:
				c.minElevation = value; return;
			case ASPECT_EAST_PCT:
				c.eastPct = value; return;
			case ASPECT_FLAT_PCT:
				c.flatPct = value; return;
			case ASPECT_NORTH_PCT:
				c.northPct = value; return;
			case ASPECT_SOUTH_PCT:
				c.southPct = value; return;
			case ASPECT_WEST_PCT:
				c.westPct = value; return;
			case MAX_SLOPE:
				c.maxSlope = value; return;
			case MEAN_ELEVATION:
				c.avgElevation = value; return;
			case MEAN_SLOPE:
				c.avgSlope = value; return;
			case MIN_SLOPE:
				c.minSlope = value; return;		
			case MEAN_D2W_2D:
				c.meanDistance2w2d = value; return;
			case MAX_D2W_2D:
				c.maxDistance2w2d = value; return;
			}
		}
		
		public double getValue(ECatchment c) {
			switch(this) {
			case MAX_ELEVATION:
				return c.maxElevation;
			case MIN_ELEVATION:
				return c.minElevation;
			case ASPECT_EAST_PCT:
				return c.eastPct;
			case ASPECT_FLAT_PCT:
				return c.flatPct;
			case ASPECT_NORTH_PCT:
				return c.northPct;
			case ASPECT_SOUTH_PCT:
				return c.southPct;
			case ASPECT_WEST_PCT:
				return c.westPct;
			case MAX_SLOPE:
				return c.maxSlope;
			case MEAN_ELEVATION:
				return c.avgElevation;
			case MEAN_SLOPE:
				return c.avgSlope;
			case MIN_SLOPE:
				return c.minSlope;	
			case MEAN_D2W_2D:
				return c.meanDistance2w2d;
			case MAX_D2W_2D:
				return c.maxDistance2w2d;
			}
			return -1;
		}
	}
	
	private final int id;
	private final double area;
	private final Polygon polygon;
	private CatchmentType type;
	private Rank rank = Rank.UNKNOWN;
	private String name = null;
	private Integer strahlerOrder = null;
	private Integer hortonOrder = null;
	private Integer hackOrder = null;
	private List<EFlowpath> flowpaths;
	private List<Nexus> upNexuses; 
	private List<Nexus> downNexuses; 
	
	private double minElevation = Double.NaN;
	private double maxElevation = Double.NaN;
	private double avgElevation = Double.NaN;
	private double minSlope = Double.NaN;
	private double maxSlope = Double.NaN;
	private double avgSlope = Double.NaN;
	private double northPct = Double.NaN;
	private double southPct = Double.NaN;
	private double eastPct = Double.NaN;
	private double westPct = Double.NaN;
	private double flatPct = Double.NaN;
	private double meanDistance2w2d =  Double.NaN;
	private double maxDistance2w2d =  Double.NaN;
	
	public ECatchment(int id, CatchmentType type, double area, Polygon polygon) {
		this.id = id;
		this.type = type;
		this.polygon = polygon;
		this.area = area;
		this.flowpaths = new ArrayList<EFlowpath>(1);
		this.upNexuses = new ArrayList<Nexus>(1);
		this.downNexuses = new ArrayList<Nexus>(1);
	}
	
	public int getId() {
		return id;
	}

	public double getArea() {
		return area;
	}

	public CatchmentType getType() {
		return type;
	}

	public void setType(CatchmentType type) {
		this.type = type;
	}

	public Rank getRank() {
		return rank;
	}

	public void setRank(Rank rank) {
		this.rank = rank;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Integer getStrahlerOrder() {
		return strahlerOrder;
	}

	public void setStrahlerOrder(Integer strahlerOrder) {
		this.strahlerOrder = strahlerOrder;
	}

	public Integer getHortonOrder() {
		return hortonOrder;
	}

	public void setHortonOrder(Integer hortonOrder) {
		this.hortonOrder = hortonOrder;
	}

	public Integer getHackOrder() {
		return hackOrder;
	}

	public void setHackOrder(Integer hackOrder) {
		this.hackOrder = hackOrder;
	}

	public Polygon getPolygon() {
		return polygon;
	}

	@Override
	public Envelope getEnvelope() {
		return polygon.getEnvelopeInternal();
	}
	
	@Override
	public double distance(Point p) {
		return polygon.distance(p);
	}

	public void addFlowpath(EFlowpath flowpath) {
		if(flowpath.getRank() != Rank.UNKNOWN 
				&& (rank == Rank.UNKNOWN || (flowpath.getRank().ordinal() < rank.ordinal()))) {
			rank = flowpath.getRank();
		}
		flowpaths.add(flowpath);
	}

	public List<EFlowpath> getFlowpaths() {
		return Collections.unmodifiableList(flowpaths);
	}	

	public void addUpNexus(Nexus nexus) {
		upNexuses.add(nexus);
	}

	public void addDownNexus(Nexus nexus) {
		downNexuses.add(nexus);
	}
	
	public List<Nexus> getUpNexuses() {
		return Collections.unmodifiableList(upNexuses);
	}

	public List<Nexus> getDownNexuses() {
		return Collections.unmodifiableList(downNexuses);
	}

	public double getMinElevation() {
		return this.minElevation;
	}
	public double getMaxElevation() {
		return this.maxElevation;
	}
	public double getAverageElevation() {
		return this.avgElevation;
	}
	public double getMinSlope() {
		return this.minSlope;
	}
	public double getMaxSlope() {
		return this.maxSlope;
	}
	public double getAverageSlope() {
		return this.avgSlope;
	}
	public double getNorthPercent() {
		return this.northPct;
	}
	public double getSouthPercent() {
		return this.southPct;
	}
	public double getEastPercent() {
		return this.eastPct;
	}
	public double getWestPercent() {
		return this.westPct;
	}
	public double getFlatPercent() {
		return this.flatPct;
	}
	public double getMean2dDistance2Water() {
		return this.meanDistance2w2d;
	}
	public double getMaxdDistance2Water() {
		return this.maxDistance2w2d;
	}
}
