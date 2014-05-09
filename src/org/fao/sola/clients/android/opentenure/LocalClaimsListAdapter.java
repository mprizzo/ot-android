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
package org.fao.sola.clients.android.opentenure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.fao.sola.clients.android.opentenure.model.Claim;
import org.fao.sola.clients.android.opentenure.model.Person;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class LocalClaimsListAdapter extends ArrayAdapter<ClaimListTO> implements Filterable {
	private final Context context;
	private final List<ClaimListTO> originalClaims;
	private List<ClaimListTO> filteredClaims;
	private List<ClaimListTO> claims;
	LayoutInflater inflater;

	public LocalClaimsListAdapter(Context context, List<ClaimListTO> claims) {
		super(context, R.layout.claims_list_item, claims);
		this.context = context;
		this.inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.originalClaims = new ArrayList<ClaimListTO>(claims);
		this.claims = claims;
		this.filteredClaims = null;
	}

	@Override
	public Filter getFilter() {

		Filter filter = new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {

				String filterString = constraint.toString();
				
				filteredClaims = new ArrayList<ClaimListTO>();
				for (ClaimListTO cto : originalClaims) {
					String lcase = cto.getSlogan().toLowerCase(Locale.getDefault());
					if (lcase.contains(filterString.toLowerCase(Locale.getDefault()))) {
						filteredClaims.add(cto);
					}
				}

				FilterResults results = new FilterResults();
				results.count = filteredClaims.size();
				results.values = filteredClaims;
				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				claims = (ArrayList<ClaimListTO>) results.values;

				if (results.count > 0) {
					notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}
			}
		};
		return filter;
	}

	@Override
	public int getCount() {
		return claims.size();
	}

	static class ViewHolder {
		TextView id;
		TextView slogan;
		TextView status;
		ImageView picture;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.claims_list_item,
					parent, false);
			vh = new ViewHolder();
			vh.slogan = (TextView) convertView.findViewById(R.id.claim_slogan);
			vh.id = (TextView) convertView.findViewById(R.id.claim_id);
			vh.status = (TextView) convertView.findViewById(R.id.claim_status);
			vh.picture = (ImageView) convertView
					.findViewById(R.id.claimant_picture);
			convertView.setTag(vh);
		} else {
			vh = (ViewHolder) convertView.getTag();
		}
		vh.slogan.setText(claims.get(position).getSlogan());
		vh.status.setText(claims.get(position).getStatus());
		vh.id.setTextSize(8);
		vh.id.setText(claims.get(position).getId());
		vh.picture.setImageBitmap(Person.getPersonPicture(
				context,
				Person.getPersonPictureFile(Claim
						.getClaim(claims.get(position).getId()).getPerson()
						.getPersonId()), 96));

		return convertView;
	}
}