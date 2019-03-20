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

import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.indexing.SpatiallyIndexable;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;

public class Nexus implements SpatiallyIndexable{
	private ArrayList<EFlowpath> upFlows = new ArrayList<EFlowpath>();
	private ArrayList<EFlowpath> downFlows = new ArrayList<EFlowpath>();
	private ECatchment bankCatchment = null;
	private final int id;
	private final Point point;
	private NexusType type = NexusType.UNKNOWN;

	public Nexus(int id, Point point) {
		this.id = id;
		this.point = point;
	}

	public int getId() {
		return id;
	}

	public void addUpFlow(EFlowpath edge) {
		upFlows.add(edge);
	}

	public void addDownFlow(EFlowpath edge) {
		downFlows.add(edge);
	}

	public List<EFlowpath> getUpFlows(){
		return Collections.unmodifiableList(upFlows);
	}

	public List<EFlowpath> getDownFlows() {
		return Collections.unmodifiableList(downFlows);
	}

	public ECatchment getBankCatchment() {
		return bankCatchment;
	}

	public void setBankCatchment(ECatchment bankCatchment) {
		this.bankCatchment = bankCatchment;
	}

	public Point getPoint() {
		return point;
	}
	
	public NexusType getType() {
		return type;
	}

	public void setType(NexusType type) {
		this.type = type;
	}

	@Override
	public Envelope getEnvelope() {
		return point.getEnvelopeInternal();
	}

	@Override
	public double distance(Point p) {
		return point.distance(p);
	}

}
