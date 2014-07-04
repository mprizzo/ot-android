/**
 * ******************************************************************************************
 * Copyright (C) 2014 - Food and Agriculture Organization of the United Nations (FAO).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,this list
 *       of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice,this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *    3. Neither the name of FAO nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,STRICT LIABILITY,OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
package org.fao.sola.clients.android.opentenure.maps;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fao.sola.clients.android.opentenure.ClaimDispatcher;
import org.fao.sola.clients.android.opentenure.R;
import org.fao.sola.clients.android.opentenure.filesystem.FileSystemUtilities;
import org.fao.sola.clients.android.opentenure.maps.markers.ActiveMarkerRegistrar;
import org.fao.sola.clients.android.opentenure.maps.markers.DownMarker;
import org.fao.sola.clients.android.opentenure.maps.markers.LeftMarker;
import org.fao.sola.clients.android.opentenure.maps.markers.RightMarker;
import org.fao.sola.clients.android.opentenure.maps.markers.UpMarker;
import org.fao.sola.clients.android.opentenure.model.Adjacency;
import org.fao.sola.clients.android.opentenure.model.Attachment;
import org.fao.sola.clients.android.opentenure.model.Claim;
import org.fao.sola.clients.android.opentenure.model.MD5;
import org.fao.sola.clients.android.opentenure.model.PropertyLocation;
import org.fao.sola.clients.android.opentenure.model.Vertex;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.algorithm.distance.DistanceToPoint;
import com.vividsolutions.jts.algorithm.distance.PointPairDistance;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

public class EditablePropertyBoundary extends BasePropertyBoundary {

	public static final String DEFAULT_MAP_FILE_NAME = "_map_.jpg";
	private Map<Marker, Vertex> verticesMap;
	private List<BasePropertyBoundary> otherProperties;
	private ClaimDispatcher claimActivity;
	private ActiveMarkerRegistrar amr;
	private boolean allowDragging;

	private UpMarker up;
	private DownMarker down;
	private LeftMarker left;
	private RightMarker right;
	private Marker remove;
	private Marker moveTo;
	private Marker relativeEdit;
	private Marker cancel;
	private Marker target;
	private Marker add;
	private Marker selectedMarker;

	public boolean handleMarkerClick(final Marker mark){
		if(handleMarkerEditClick(mark)){
			return true;
		}else if(handleRelativeMarkerEditClick(mark)){
			return true;
		}else if(handlePropertyBoundaryMarkerClick(mark)){
			return true;
		}else if(handlePropertyLocationMarkerClick(mark)){
			return true;
		}else{
			return handleClick(mark);
		}
	}
	
	private boolean handleMarkerEditClick(Marker mark){
		if(remove == null || relativeEdit == null || cancel == null){
			return false;
		}

		if (mark.getId().equalsIgnoreCase(remove.getId())) {
			return removeSelectedMarker();
		}
		if (mark.getId().equalsIgnoreCase(relativeEdit.getId())) {
			showRelativeMarkerEditControls();
			return true;
		}
		if (mark.getId().equalsIgnoreCase(cancel.getId())) {
			deselect();
			return true;
		}
		return false;
	}

	private boolean handleRelativeMarkerEditClick(Marker mark){
		if(up == null || down == null || left == null || right == null || add == null || moveTo == null || cancel == null || target == null){
			return false;
		}
		
		if (amr.onClick(mark)) {
			return true;
		}else if (mark.getId().equalsIgnoreCase(add.getId())) {
			Log.d(this.getClass().getName(),"add");
			return addMarker();
		}else if (mark.getId().equalsIgnoreCase(moveTo.getId())) {
			Log.d(this.getClass().getName(),"moveTo");
			return moveMarker();
		}else if (mark.getId().equalsIgnoreCase(cancel.getId())) {
			Log.d(this.getClass().getName(),"cancel");
			deselect();
			return true;
		}else if (mark.getId().equalsIgnoreCase(target.getId())) {
			Log.d(this.getClass().getName(),"target");
			return true;
		}else{
			return false;
		}
	}
	
	private boolean handlePropertyBoundaryMarkerClick(final Marker mark){
		if (verticesMap.containsKey(mark)) {
			deselect();
			selectedMarker = mark;
			selectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker());
			selectedMarker.showInfoWindow();
			showMarkerEditControls();
			return true;
		}
		return false;
		
	}

	private boolean handlePropertyLocationMarkerClick(final Marker mark){
		if (propertyLocationsMap.containsKey(mark)) {
			deselect();
			selectedMarker = mark;
			selectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker());
			selectedMarker.showInfoWindow();
			showMarkerEditControls();
			return true;
		}
		return false;
		
	}

	private boolean handleClick(Marker mark){
		// Can only be a click on the property name, deselect and let the event flow
		deselect();
		if(propertyMarker != null && mark.getId().equalsIgnoreCase(propertyMarker.getId())){
			if(isPropertyLocationsVisible()){
				hidePropertyLocations();
			}else{
				showPropertyLocations();
			}
		}
		// Let the flow continue in order to center the map around selected marker and display info window
		return false;
	}

	public void onMarkerDragStart(Marker mark) {
		if(verticesMap.containsKey(mark)){
			onPropertyBoundaryMarkerDragStart(mark);
		}else if(propertyLocationsMap.containsKey(mark)){
			onPropertyLocationMarkerDragStart(mark);
		}
	}

	public void onMarkerDragEnd(Marker mark) {
		if(verticesMap.containsKey(mark)){
			onPropertyBoundaryMarkerDragEnd(mark);
		}else if(propertyLocationsMap.containsKey(mark)){
			onPropertyLocationMarkerDragEnd(mark);
		}
	}

	public void onMarkerDrag(Marker mark) {
		if(verticesMap.containsKey(mark)){
			onPropertyBoundaryMarkerDrag(mark);
		}else if(propertyLocationsMap.containsKey(mark)){
			onPropertyLocationMarkerDrag(mark);
		}
	}

	private boolean removeSelectedMarker(){

		if (verticesMap.containsKey(selectedMarker)) {
			return removeSelectedPropertyBoundaryVertex();
		}else if (propertyLocationsMap.containsKey(selectedMarker)) {
			return removeSelectedPropertyLocation();
		}
		return false;
		
	}
	
	private boolean removeSelectedPropertyLocation(){
		PropertyLocation loc = propertyLocationsMap.remove(selectedMarker);
		removePropertyLocationMarker(selectedMarker);
		loc.delete();
		hideMarkerEditControls();
		selectedMarker = null;
		return true;
	}
	
	private boolean removeSelectedPropertyBoundaryVertex(){
		removePropertyBoundaryMarker(selectedMarker);
		redrawBoundary();
		updateVertices();
		resetAdjacency(otherProperties);
		hideMarkerEditControls();
		selectedMarker = null;
		return true;
	}
	
	private boolean addMarker(){
		
		addMarker(target.getPosition());
		deselect();
		return true;
	}

	private boolean movePropertyLocationMarker(){
		Marker newMark = createLocationMarker(target.getPosition(), selectedMarker.getTitle());
		PropertyLocation loc = propertyLocationsMap.remove(selectedMarker);
		loc.setMapPosition(target.getPosition());
		loc.update();
		hideMarkerEditControls();
		selectedMarker.remove();
		selectedMarker = null;
		propertyLocationsMap.put(newMark, loc);
		return true;
	}
	
	private boolean movePropertyBoundaryMarker(){
		insertVertex(target.getPosition());
		removePropertyBoundaryMarker(selectedMarker);
		hideMarkerEditControls();
		selectedMarker = null;
		redrawBoundary();
		updateVertices();
		resetAdjacency(otherProperties);
		return true;
	}
	
	private boolean moveMarker(){

		// Insert a marker at target position and remove the selected

		if (verticesMap.containsKey(selectedMarker)) {
			return movePropertyBoundaryMarker();
		}else if (propertyLocationsMap.containsKey(selectedMarker)) {
			return movePropertyLocationMarker();
		}
		return false;
		
	}

	private void deselect(){
		hideMarkerEditControls();
		if(selectedMarker != null){
			selectedMarker.setIcon(BitmapDescriptorFactory
					.fromResource(R.drawable.ot_blue_marker));
		selectedMarker = null;
		}
	}
	
	private void hideMarkerEditControls(){
		if(up!=null){
			up.hide();
			amr.remove(up);
		}
		if(down != null){
			down.hide();
			amr.remove(down);
		}
		if(left != null){
			left.hide();
			amr.remove(left);
		}
		if(right != null){
			right.hide();
			amr.remove(right);
		}
		if(target != null){
			target.remove();
			target = null;
		}
		if(relativeEdit != null){
			relativeEdit.remove();
			relativeEdit = null;
		}
		if(remove != null){
			remove.remove();
			remove = null;
		}
		if(add != null){
			add.remove();
			add = null;
		}
		if(moveTo != null){
			moveTo.remove();
			moveTo = null;
		}
		if(cancel != null){
			cancel.remove();
			cancel = null;
		}

	}

	private Point getControlRelativeEditPosition(Point markerScreenPosition, int markerWidth, int markerHeight){
		return new Point(markerScreenPosition.x, markerScreenPosition.y + 2*markerHeight);
	}

	private Point getControlRemovePosition(Point markerScreenPosition, int markerWidth, int markerHeight){
		return new Point(markerScreenPosition.x - 2*markerWidth, markerScreenPosition.y + 2*markerHeight);
	}

	private Point getControlAddPosition(Point markerScreenPosition, int markerWidth, int markerHeight){
		return new Point(markerScreenPosition.x - 2*markerWidth, markerScreenPosition.y + 2*markerHeight);
	}

	private Point getControlMoveToPosition(Point markerScreenPosition, int markerWidth, int markerHeight){
		return new Point(markerScreenPosition.x, markerScreenPosition.y + 2*markerHeight);
	}

	private Point getControlCancelPosition(Point markerScreenPosition, int markerWidth, int markerHeight){
		return new Point(markerScreenPosition.x + 2*markerWidth, markerScreenPosition.y + 2*markerHeight);
	}

	private Point getControlTargetPosition(Point markerScreenPosition, int markerWidth, int markerHeight){
		return new Point(markerScreenPosition.x, markerScreenPosition.y);
	}
	
	public void refreshMarkerEditControls(){
		
		if(selectedMarker == null){
			return;
		}

		// Reposition visible edit controls (excluding target)
		
		Projection projection = map.getProjection();
		Point screenPosition = projection.toScreenLocation(selectedMarker.getPosition());

		Bitmap bmp = BitmapFactory
				.decodeResource(context.getResources(), R.drawable.ot_blue_marker);
		int iconHeight = bmp.getHeight();
		int iconWidth = bmp.getWidth();

		if(up!=null){
			up.refresh(screenPosition, iconWidth, iconHeight);
		}
		if(down != null){
			down.refresh(screenPosition, iconWidth, iconHeight);
		}
		if(left != null){
			left.refresh(screenPosition, iconWidth, iconHeight);
		}
		if(right != null){
			right.refresh(screenPosition, iconWidth, iconHeight);
		}
		if(relativeEdit != null){
			relativeEdit.setPosition(projection.fromScreenLocation(getControlRelativeEditPosition(screenPosition, iconWidth, iconHeight)));
		}
		if(remove != null){
			remove.setPosition(projection.fromScreenLocation(getControlRemovePosition(screenPosition, iconWidth, iconHeight)));
		}
		if(add != null){
			add.setPosition(projection.fromScreenLocation(getControlAddPosition(screenPosition, iconWidth, iconHeight)));
		}
		if(moveTo != null){
			moveTo.setPosition(projection.fromScreenLocation(getControlMoveToPosition(screenPosition, iconWidth, iconHeight)));
		}
		if(cancel != null){
			cancel.setPosition(projection.fromScreenLocation(getControlCancelPosition(screenPosition, iconWidth, iconHeight)));
		}

	}

	private void showMarkerEditControls() {
		
		hideMarkerEditControls();
		
		Projection projection = map.getProjection();
		Point markerScreenPosition = projection.toScreenLocation(selectedMarker.getPosition());

		Bitmap bmp = BitmapFactory
				.decodeResource(context.getResources(), R.drawable.ot_blue_marker);
		int markerHeight = bmp.getHeight();
		int markerWidth = bmp.getWidth();

		remove = map.addMarker(new MarkerOptions()
		.position(projection.fromScreenLocation(getControlRemovePosition(markerScreenPosition, markerWidth, markerHeight)))
		.anchor(0.5f, 0.5f)
		.icon(BitmapDescriptorFactory
				.fromResource(R.drawable.ic_menu_close_clear_cancel)));
		relativeEdit = map.addMarker(new MarkerOptions()
		.position(projection.fromScreenLocation(getControlRelativeEditPosition(markerScreenPosition, markerWidth, markerHeight)))
		.anchor(0.5f, 0.5f)
		.title("0.0 m")
		.icon(BitmapDescriptorFactory
				.fromResource(R.drawable.ic_action_move)));
		cancel = map.addMarker(new MarkerOptions()
		.position(projection.fromScreenLocation(getControlCancelPosition(markerScreenPosition, markerWidth, markerHeight)))
		.anchor(0.5f, 0.5f)
		.icon(BitmapDescriptorFactory
				.fromResource(R.drawable.ic_menu_block)));
	}

	private void showRelativeMarkerEditControls() {
		
		Projection projection = map.getProjection();
		Point markerScreenPosition = projection.toScreenLocation(selectedMarker.getPosition());

		Bitmap bmp = BitmapFactory
				.decodeResource(context.getResources(), R.drawable.ot_blue_marker);
		int markerHeight = bmp.getHeight();
		int markerWidth = bmp.getWidth();

		hideMarkerEditControls();
		
		target = map.addMarker(new MarkerOptions()
		.position(projection.fromScreenLocation(getControlTargetPosition(markerScreenPosition, markerWidth, markerHeight)))
		.anchor(0.5f, 0.5f)
		.title("0.0 m")
		.icon(BitmapDescriptorFactory
				.fromResource(R.drawable.ic_menu_mylocation)));

		add = map.addMarker(new MarkerOptions()
		.position(projection.fromScreenLocation(getControlAddPosition(markerScreenPosition, markerWidth, markerHeight)))
		.anchor(0.5f, 0.5f)
		.icon(BitmapDescriptorFactory
				.fromResource(R.drawable.ic_menu_add)));
		moveTo = map.addMarker(new MarkerOptions()
		.position(projection.fromScreenLocation(getControlMoveToPosition(markerScreenPosition, markerWidth, markerHeight)))
		.anchor(0.5f, 0.5f)
		.icon(BitmapDescriptorFactory
				.fromResource(R.drawable.ic_menu_goto)));
		cancel = map.addMarker(new MarkerOptions()
		.position(projection.fromScreenLocation(getControlCancelPosition(markerScreenPosition, markerWidth, markerHeight)))
		.anchor(0.5f, 0.5f)
		.icon(BitmapDescriptorFactory
				.fromResource(R.drawable.ic_menu_block)));

		up = new UpMarker(context,selectedMarker,target, map);
		up.show(projection, markerScreenPosition, markerWidth, markerHeight);
		amr.add(up);
		
		down = new DownMarker(context,selectedMarker,target, map);
		down.show(projection, markerScreenPosition, markerWidth, markerHeight);
		amr.add(down);

		left = new LeftMarker(context,selectedMarker,target, map);
		left.show(projection, markerScreenPosition, markerWidth, markerHeight);
		amr.add(left);

		right = new RightMarker(context,selectedMarker,target, map);
		right.show(projection, markerScreenPosition, markerWidth, markerHeight);
		amr.add(right);

	}

	public EditablePropertyBoundary(final Context context, final GoogleMap map, final Claim claim,
			final ClaimDispatcher claimActivity, final List<BasePropertyBoundary> existingProperties, boolean allowDragging) {
		super(context, map, claim);
		this.claimActivity = claimActivity;
		this.allowDragging = allowDragging;
		this.selectedMarker = null;
		this.otherProperties = existingProperties;
		this.amr = new ActiveMarkerRegistrar();
		verticesMap = new HashMap<Marker, Vertex>();
		if (vertices != null && vertices.size() > 0) {
			for (Vertex vertex : vertices) {
				Marker mark = createMarker(vertex.getSequenceNumber(), vertex.getMapPosition());
				verticesMap.put(mark, vertex);
			}
		}
	}
	
	public void setOtherProperties(List<BasePropertyBoundary> otherProperties) {
		this.otherProperties = otherProperties;
	}

	private List<BasePropertyBoundary> findAdjacentProperties(
			List<BasePropertyBoundary> properties) {
		List<BasePropertyBoundary> adjacentProperties = null;
		for (BasePropertyBoundary property : properties) {
			if (polygon != null && property.getPolygon() != null
					&& polygon.distance(property.getPolygon()) < SNAP_THRESHOLD) {
				if (adjacentProperties == null) {
					adjacentProperties = new ArrayList<BasePropertyBoundary>();
				}
				adjacentProperties.add(property);
			}
		}
		return adjacentProperties;
	}

	protected void reload(){
		for(Marker mark:verticesMap.keySet()){
			mark.remove();
		}
		claimId = claimActivity.getClaimId();
		super.reload();
		verticesMap = new HashMap<Marker, Vertex>();
		if (vertices != null && vertices.size() > 0) {
			for (Vertex vertex : vertices) {
				Marker mark = createMarker(vertex.getSequenceNumber(), vertex.getMapPosition());
				verticesMap.put(mark, vertex);
			}
		}
	}
	
	public void updateVertices() {

		Vertex.deleteVertices(claimActivity.getClaimId());

		for (int i = 0; i < vertices.size(); i++) {
			Vertex vertex = vertices.get(i);
			vertex.setSequenceNumber(i);
			Vertex.createVertex(vertex);
		}
		calculateGeometry();

	}

	public void updatePropertyLocations() {

		PropertyLocation.deletePropertyLocations(claimActivity.getClaimId());

		for (PropertyLocation location : propertyLocationsMap.values()) {
			location.create();
		}

	}
	protected void resetAdjacency(List<BasePropertyBoundary> existingProperties) {

		List<BasePropertyBoundary> adjacentProperties = findAdjacentProperties(existingProperties);
		Adjacency.deleteAdjacencies(claimId);

		if (adjacentProperties != null) {

			for (BasePropertyBoundary adjacentProperty : adjacentProperties) {

				Adjacency adj = new Adjacency();
				adj.setSourceClaimId(claimId);
				adj.setDestClaimId(adjacentProperty.getClaimId());
				adj.setCardinalDirection(getCardinalDirection(adjacentProperty));
				adj.create();
			}
		}
	}

	private void onPropertyLocationMarkerDragStart(Marker mark) {
		dragPropertyLocationMarker(mark);
	}

	private void onPropertyLocationMarkerDragEnd(Marker mark) {
		dragPropertyLocationMarker(mark);
		updatePropertyLocations();
	}

	private void onPropertyLocationMarkerDrag(Marker mark) {
		dragPropertyLocationMarker(mark);
	}

	private void onPropertyBoundaryMarkerDragStart(Marker mark) {
		dragPropertyBoundaryMarker(mark);
	}

	private void onPropertyBoundaryMarkerDragEnd(Marker mark) {
		dragPropertyBoundaryMarker(mark);
		updateVertices();
		resetAdjacency(otherProperties);
	}

	private void onPropertyBoundaryMarkerDrag(Marker mark) {
		dragPropertyBoundaryMarker(mark);
	}

	private void dragPropertyBoundaryMarker(Marker mark) {
		verticesMap.get(mark).setMapPosition(mark.getPosition());
		redrawBoundary();
	}

	private void dragPropertyLocationMarker(Marker mark) {
		propertyLocationsMap.get(mark).setMapPosition(mark.getPosition());
	}

	private void removePropertyLocationMarker(Marker mark) {
		propertyLocationsMap.remove(mark);
		mark.remove();
	}

	private void removePropertyBoundaryMarker(Marker mark) {
		vertices.remove(verticesMap.remove(mark));
		mark.remove();
	}

	public void insertVertex(LatLng position) {

		if (claimActivity.getClaimId() == null) {
			// Useless to add markers without a claim
			Toast toast = Toast.makeText(context,
					R.string.message_save_claim_before_adding_content,
					Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		Marker mark = createMarker(vertices.size(), position);
		Vertex newVertex = new Vertex(position);
		newVertex.setClaimId(claimActivity.getClaimId());
		verticesMap.put(mark, newVertex);

		if (vertices.size() < 2) {
			// no need to calculate the insertion point
			vertices.add(newVertex);
			return;
		}

		double minDistance = Double.MAX_VALUE;
		int insertIndex = 0;

		// calculate the insertion point
		for (int i = 0; i < vertices.size(); i++) {

			Vertex from = vertices.get(i);
			Vertex to = null;

			if (i == vertices.size() - 1) {
				to = vertices.get(0);
			} else {
				to = vertices.get(i + 1);
			}

			PointPairDistance ppd = new PointPairDistance();
			DistanceToPoint.computeDistance(
					new LineSegment(from.getMapPosition().longitude, from
							.getMapPosition().latitude,
							to.getMapPosition().longitude,
							to.getMapPosition().latitude),
					new Coordinate(newVertex.getMapPosition().longitude,
							newVertex.getMapPosition().latitude), ppd);

			double currDistance = ppd.getDistance();

			if (currDistance < minDistance) {
				minDistance = currDistance;
				insertIndex = i + 1;
			}

		}
		vertices.add(insertIndex, newVertex);
	}
	
	public void addMarker(final LatLng position){

		if (claimActivity.getClaimId() == null) {
			// Useless to add markers without a claim
			Toast toast = Toast.makeText(
					context,
					R.string.message_save_claim_before_adding_content,
					Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(
				context);
		dialog.setTitle(R.string.message_add_marker);
		dialog.setMessage("Lon: " + position.longitude + ", lat: "
				+ position.latitude);

		dialog.setNeutralButton(R.string.not_boundary,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						AlertDialog.Builder locationDescriptionDialog = new AlertDialog.Builder(
								context);
						locationDescriptionDialog
								.setTitle(R.string.title_add_non_boundary);
						final EditText locationDescriptionInput = new EditText(
								context);
						locationDescriptionInput
								.setInputType(InputType.TYPE_CLASS_TEXT);
						locationDescriptionDialog
								.setView(locationDescriptionInput);
						locationDescriptionDialog
								.setMessage(context.getResources()
										.getString(
												R.string.message_enter_description));

						locationDescriptionDialog
								.setPositiveButton(
										R.string.confirm,
										new OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												String locationDescription = locationDescriptionInput
														.getText()
														.toString();
												addPropertyLocation(position, locationDescription);
											}
										});
						locationDescriptionDialog
								.setNegativeButton(
										R.string.cancel,
										new OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
											}
										});

						locationDescriptionDialog.show();

					}
				});
		dialog.setPositiveButton(R.string.confirm,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {

						insertVertex(position);
						updateVertices();
						redrawBoundary();
						resetAdjacency(otherProperties);
					}
				});
		dialog.setNegativeButton(R.string.cancel,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {
					}
				});

		dialog.show();
	
	}

	public void addPropertyLocation(LatLng position, String description) {

		if (claimActivity.getClaimId() == null) {
			// Useless to add markers without a claim
			Toast toast = Toast.makeText(context,
					R.string.message_save_claim_before_adding_content,
					Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		Marker mark = createLocationMarker(position, description);
		PropertyLocation loc = new PropertyLocation(position);
		loc.setClaimId(claimActivity.getClaimId());
		loc.setDescription(description);
		loc.create();
		propertyLocationsMap.put(mark, loc);
	}

	private Marker createMarker(int index, LatLng position) {
		if(allowDragging){
			return map.addMarker(new MarkerOptions()
			.position(position)
			.title(index + ", Lat: " + position.latitude + ", Lon: " + position.longitude)
			.draggable(true)
			.icon(BitmapDescriptorFactory
					.fromResource(R.drawable.ot_blue_marker)));
		}else{
			return map.addMarker(new MarkerOptions()
			.position(position)
			.title(index + ", Lat: " + position.latitude + ", Lon: " + position.longitude)
			.icon(BitmapDescriptorFactory
					.fromResource(R.drawable.ot_blue_marker)));
		}
	}

	@Override
	protected Marker createLocationMarker(LatLng position, String description) {
		if(allowDragging){
			return map.addMarker(new MarkerOptions()
			.position(position)
			.title(description)
			.draggable(true)
			.icon(BitmapDescriptorFactory
					.fromResource(R.drawable.ot_blue_marker)));
		}else{
			return map.addMarker(new MarkerOptions()
			.position(position)
			.title(description)
			.icon(BitmapDescriptorFactory
					.fromResource(R.drawable.ot_blue_marker)));
		}
	}

	public void saveSnapshot() {

		if (claimActivity.getClaimId() != null) {
			map.snapshot(new SnapshotReadyCallback() {

				@Override
				public void onSnapshotReady(Bitmap bmp) {
					FileOutputStream out = null;
					String claimId = claimActivity.getClaimId();
					String path = FileSystemUtilities
							.getAttachmentFolder(claimId)
							+ File.separator
							+ DEFAULT_MAP_FILE_NAME;
					try {
						out = new FileOutputStream(path);
						bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
						Claim claim = Claim.getClaim(claimId);
						for (Attachment att : claim.getAttachments()) {
							if (att.getFileName().equals(DEFAULT_MAP_FILE_NAME)) {
								att.delete();
							}
						}
						Attachment att = new Attachment();
						att.setClaimId(claimId);
						att.setDescription("Map");
						att.setFileName(DEFAULT_MAP_FILE_NAME);
						att.setFileType("jpg");						
						att.setMimeType("image/jpeg");
						att.setMD5Sum(MD5.calculateMD5(new File(path)));
						att.setPath(path);
						att.setSize(new File(path).length());
						att.create();
						Toast toast = Toast.makeText(context,
								R.string.message_map_snapshot_saved,
								Toast.LENGTH_SHORT);
						toast.show();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (out != null) {
							try {
								out.close();
							} catch (Throwable ignore) {
							}
						}
					}
				}
			});
		}else{
			Toast toast = Toast.makeText(context,
					R.string.message_save_claim_before_adding_content,
					Toast.LENGTH_SHORT);
			toast.show();
		}
	}
}
